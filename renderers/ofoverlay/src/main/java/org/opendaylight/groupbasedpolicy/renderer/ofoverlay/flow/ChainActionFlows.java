/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxNsiMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxNspMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxRegMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxTunIdMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.addNxTunIpv4DstMatch;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.applyActionIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.gotoTableIns;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.instructions;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadNshc1RegAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadNshc2RegAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadRegAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadTunIPv4Action;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxLoadTunIdAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.nxOutputRegAction;
import static org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.outputAction;

import java.math.BigInteger;

import org.apache.commons.lang3.StringUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfContext;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.OfWriter;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.PolicyManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils.RegMatch;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.OrdinalFactory.EndpointFwdCtxOrdinals;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.mapper.policyenforcer.PolicyEnforcer.NetworkElements;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.node.SwitchManager;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf.ChainAction;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sfcutils.SfcNshHeader;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.ofoverlay.rev140528.OfOverlayContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg7;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlanGpe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

/**
 * <h1>Creation of flows related to service chain</h1>
 *
 * These flows are built across most of gbp of tables and have higher priority than basic flows. It ensures, that
 * packet redirected to chain will be sent to tunnel output
 * <p>
 *
 * Flow that allows ALL traffic incoming from chain last hop
 * <p>
 * <i>Allow from chain flow</i><br>
 * Table = 0<br>
 * Priority = 1200<br>
 * Matches:<br>
 *      - Nshc1<br>
 *      - Nsp<br>
 *      - Nsi<br>
 *      - in_port (tunnel port) {@link NodeConnectorId}<br>
 * Actions:<br>
 *      - {@link GoToTable} SOURCE MAPPER table
 * <p>
 * TODO: looks like duplicity, the same flow is created in policy enforcer
 * <i>Allow from chain tunnel</i>
 * Table = 4<br>
 * Priority = 65000<br>
 * Matches:<br>
 *      - in_port (tunnel port) {@link NodeConnectorId}<br>
 *      - Reg7 (fixed value 0xffffff) {@link NxmNxReg7}
 * Actions:<br>
 *      - {@link GoToTable} SOURCE MAPPER table
 * <p>
 * <i>Create external flow</i>
 * Table = 6<br>
 * Priority = 1000 (if dst node == src node, priority = 1500)<br>
 * Matches:<br>
 *      - Reg6 {@link NxmNxReg6}<br>
 *      - tunnel ID<br>
 *      - nsp<br>
 *      - nsi<br>
 *      - tun_dst (only if dst node == src node)<br>
 * Actions:<br>
 *      - set nshc1<br>
 *      - set nshc2<br>
 *      - load tunnel ID<br>
 *      - load tunnel ipv4<br>
 *      - output:(tunnel port)<br>
 * <p>
 * <i>Chain tunnel flow</i><br>
 * Table = 2<br>
 * Priority = 150<br>
 * Matches:<br>
 *      - in_port (tunnel port) {@link NodeConnectorId}<br>
 *      - tunnel ID<br>
 *      - nsp<br>
 *      - nsi<br>
 * Actions:<br>
 *      - Reg0 {@link NxmNxReg0}<br>
 *      - Reg1 {@link NxmNxReg1}<br>
 *      - Reg4 {@link NxmNxReg4}<br>
 *      - Reg5 {@link NxmNxReg5}<br>
 *      - Reg6 {@link NxmNxReg6}<br>
 *      - {@link GoToTable} DESTINATION MAPPER table<br>
 * <p>
 * <i>Chain broadcast flow</i><br>
 * Table = 2<br>
 * Priority = 150<br>
 * Matches:<br>
 *      - in_port (tunnel port) {@link NodeConnectorId}<br>
 *      - tunnel ID<br>
 *      - nsp<br>
 *      - nsi<br>
 * Actions:<br>
 *      - load Reg5 {@link NxmNxReg5}<br>
 *      - {@link GoToTable} DESTINATION MAPPER table<br>
 */
public class ChainActionFlows {

    private static final Logger LOG = LoggerFactory.getLogger(ChainAction.class);

    public ChainActionFlows() {

    }

    public static void createChainTunnelFlows(SfcNshHeader sfcNshHeader, NetworkElements netElements, OfWriter ofWriter,
            OfContext ctx) {

        NodeId localNodeId = netElements.getLocalNodeId();
        NodeId destNodeId = netElements.getDstEp().getAugmentation(OfOverlayContext.class).getNodeId();
        EndpointFwdCtxOrdinals epOrdinals = netElements.getSrcEpOrdinals();

        NodeConnectorId localNodeTunPort = ctx.getSwitchManager().getTunnelPort(localNodeId, TunnelTypeVxlanGpe.class);
        NodeConnectorId destNodeTunPort = ctx.getSwitchManager().getTunnelPort(destNodeId, TunnelTypeVxlanGpe.class);
        Ipv4Address tunDestAddress = ctx.getSwitchManager()
            .getTunnelIP(netElements.getDstNodeId(), TunnelTypeVxlanGpe.class)
            .getIpv4Address();
        if (localNodeTunPort == null || destNodeTunPort == null) {
            LOG.error("createChainTunnelFlows: No valid VXLAN GPE tunnel for Node {} or Node {}", localNodeId,
                    destNodeId);
            return;
        }
        ofWriter.writeFlow(localNodeId, ctx.getPolicyManager().getTABLEID_PORTSECURITY(), allowFromChainPort(
                sfcNshHeader, localNodeTunPort, ctx.getPolicyManager().getTABLEID_PORTSECURITY(), ctx));

        ofWriter.writeFlow(localNodeId, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER(),
                allowFromChainTunnel(localNodeTunPort, ctx.getPolicyManager().getTABLEID_POLICY_ENFORCER()));

        ofWriter.writeFlow(localNodeId, ctx.getPolicyManager().getTABLEID_EXTERNAL_MAPPER(),
                createExternalFlow(sfcNshHeader, localNodeTunPort, netElements,
                        ctx.getPolicyManager(), ctx.getSwitchManager(), tunDestAddress));

        ofWriter.writeFlow(destNodeId, ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER(), createChainTunnelFlow(
                sfcNshHeader, destNodeTunPort, epOrdinals, ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER(), ctx));

        ofWriter.writeFlow(destNodeId, ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER(), createChainBroadcastFlow(
                sfcNshHeader, destNodeTunPort, epOrdinals, ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER(), ctx));
    }

    private static Flow createChainBroadcastFlow(SfcNshHeader sfcNshHeader, NodeConnectorId tunPort,
            EndpointFwdCtxOrdinals epFwdCtxOrds, short tableId, OfContext ctx) {

        int fdId = epFwdCtxOrds.getFdId();

        MatchBuilder mb = new MatchBuilder().setInPort(tunPort);

        addNxNsiMatch(mb, sfcNshHeader.getNshNsiFromChain());
        addNxNspMatch(mb, sfcNshHeader.getNshNspFromChain());
        addNxTunIdMatch(mb, fdId);

        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action fdReg =
                nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(fdId));

        Match match = mb.build();
        FlowId flowId = FlowIdUtils.newFlowId(tableId, "chainbroadcast", match);

        FlowBuilder flowb = base(tableId).setId(flowId)
            .setPriority(150)
            .setMatch(match)
            .setInstructions(instructions(applyActionIns(fdReg),
                    gotoTableIns(ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER())));
        return flowb.build();
    }

    @VisibleForTesting
    static Flow createExternalFlow(SfcNshHeader sfcNshHeader, NodeConnectorId tunPort, NetworkElements netElements,
                                   PolicyManager policyManager, SwitchManager switchManager, Ipv4Address ipTunDest) {

        short tableId = policyManager.getTABLEID_EXTERNAL_MAPPER();
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action loadC1;
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action outputAction;

        Integer priority = 1000;
        int matchTunnelId = sfcNshHeader.getNshMetaC2().intValue();
        Long l3c = (long) netElements.getSrcEpOrdinals().getL3Id();
        loadC1 = nxLoadNshc1RegAction(sfcNshHeader.getNshMetaC1());

        // Test for if SFF is on same node
        IpAddress ipAddress = switchManager.getTunnelIP(netElements.getLocalNodeId(), TunnelTypeVxlanGpe.class);

        if (ipAddress != null && ipAddress.getIpv4Address().equals(sfcNshHeader.getNshTunIpDst())) {
            Integer newPort = returnOfPortFromNodeConnector(tunPort);
            outputAction = FlowUtils.createActionResubmit(newPort, policyManager.getTABLEID_SFC_EGRESS());
        } else {
            outputAction = outputAction(tunPort);
        }

        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action loadC2 =
                nxLoadNshc2RegAction(sfcNshHeader.getNshMetaC2());
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action loadChainTunVnid =
                nxLoadTunIdAction(BigInteger.valueOf(sfcNshHeader.getNshMetaC2()), false);
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action loadChainTunDest =
                nxLoadTunIPv4Action(sfcNshHeader.getNshTunIpDst().getValue(), false);

        MatchBuilder mb = new MatchBuilder();
        addNxRegMatch(mb, RegMatch.of(NxmNxReg6.class, l3c));
        addNxTunIdMatch(mb, matchTunnelId);
        addNxNspMatch(mb, sfcNshHeader.getNshNspToChain());
        addNxNsiMatch(mb, sfcNshHeader.getNshNsiToChain());
        if (!netElements.getDstNodeId().equals(netElements.getSrcNodeId())) {
            addNxTunIpv4DstMatch(mb, ipTunDest);
            priority = 1500;
        }

        Match match = mb.build();
        FlowId flowId = FlowIdUtils.newFlowId(tableId, "chainexternal", match);
        FlowBuilder flowb =
                base(tableId).setId(flowId).setPriority(priority).setMatch(match).setInstructions(
                        instructions(applyActionIns(loadC1, loadC2, loadChainTunDest, loadChainTunVnid, outputAction)));
        return flowb.build();
    }

    private static Flow createChainTunnelFlow(SfcNshHeader sfcNshHeader, NodeConnectorId tunPort,
            EndpointFwdCtxOrdinals epFwdCtxOrds, short tableId, OfContext ctx) {

        int egId = epFwdCtxOrds.getEpgId();
        int bdId = epFwdCtxOrds.getBdId();
        int fdId = epFwdCtxOrds.getFdId();
        int l3Id = epFwdCtxOrds.getL3Id();
        int tunnelId = epFwdCtxOrds.getTunnelId();

        MatchBuilder mb = new MatchBuilder().setInPort(tunPort);
        addNxTunIdMatch(mb, tunnelId);
        addNxNspMatch(mb, sfcNshHeader.getNshNspFromChain());
        addNxNsiMatch(mb, sfcNshHeader.getNshNsiFromChain());

        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action segReg =
                nxLoadRegAction(NxmNxReg0.class, BigInteger.valueOf(egId));
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action scgReg =
                nxLoadRegAction(NxmNxReg1.class, BigInteger.valueOf(0xffffff));
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action bdReg =
                nxLoadRegAction(NxmNxReg4.class, BigInteger.valueOf(bdId));
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action fdReg =
                nxLoadRegAction(NxmNxReg5.class, BigInteger.valueOf(fdId));
        org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action vrfReg =
                nxLoadRegAction(NxmNxReg6.class, BigInteger.valueOf(l3Id));

        Match match = mb.build();
        FlowId flowId = FlowIdUtils.newFlowId(tableId, "chaintunnel", match);
        FlowBuilder flowb =
                base(tableId).setId(flowId).setPriority(150).setMatch(match).setInstructions(
                        instructions(applyActionIns(segReg, scgReg, bdReg, fdReg, vrfReg),
                                gotoTableIns(ctx.getPolicyManager().getTABLEID_DESTINATION_MAPPER())));
        return flowb.build();
    }

    private static Flow allowFromChainPort(SfcNshHeader sfcNshHeader, NodeConnectorId port, short tableId,
            OfContext ctx) {

        // Matching on last NSP/NSI that SFF leaves on but with C1=0
        MatchBuilder mb = new MatchBuilder();
        FlowUtils.addNxNshc1RegMatch(mb, 0L);
        FlowUtils.addNxNsiMatch(mb, sfcNshHeader.getNshNsiFromChain());
        FlowUtils.addNxNspMatch(mb, sfcNshHeader.getNshNspFromChain());
        Match match = mb.setInPort(port).build();

        FlowId flowId = FlowIdUtils.newFlowId(tableId, "chainport", match);
        FlowBuilder flowb =
                base(tableId).setId(flowId).setPriority(1200).setMatch(match).setInstructions(
                        FlowUtils.gotoTableInstructions(ctx.getPolicyManager().getTABLEID_SOURCE_MAPPER()));
        return flowb.build();
    }

    private static Flow allowFromChainTunnel(NodeConnectorId tunPort, short tableId) {

        MatchBuilder mb = new MatchBuilder().setInPort(tunPort);
        addNxRegMatch(mb, RegMatch.of(NxmNxReg1.class, 0xffffffL));
        Match match = mb.build();
        FlowId flowId = FlowIdUtils.newFlowId(tableId, "chainport", match);

        FlowBuilder flow = base(tableId).setId(flowId).setMatch(match).setPriority(65000).setInstructions(
                instructions(applyActionIns(nxOutputRegAction(NxmNxReg7.class))));
        return flow.build();

    }

    @VisibleForTesting
    static Integer returnOfPortFromNodeConnector(NodeConnectorId nodeConnectorId) {
        String[] elements = StringUtils.split(nodeConnectorId.getValue(), ":");
        if (elements.length != 3)
            return null;
        return new Integer(elements[2]);
    }

    /**
     * Get a base flow builder with some common features already set
     */
    private static FlowBuilder base(short tableId) {
        return new FlowBuilder().setTableId(tableId).setBarrier(false).setHardTimeout(0).setIdleTimeout(0);
    }
}
