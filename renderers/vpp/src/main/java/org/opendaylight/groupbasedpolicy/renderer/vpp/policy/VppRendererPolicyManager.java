/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.policy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.opendaylight.controller.config.yang.config.vpp_provider.impl.VppRenderer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.NodeOperEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.event.RendererPolicyConfEvent;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.KeyFactory;
import org.opendaylight.groupbasedpolicy.util.IidFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.base_endpoint.rev160427.has.absolute.location.absolute.location.location.type.ExternalLocationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.endpoints.AddressEndpointWithLocation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.policy.configuration.renderer.endpoints.RendererEndpointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class VppRendererPolicyManager {

    private static final Logger LOG = LoggerFactory.getLogger(VppRendererPolicyManager.class);
    private final DataBroker dataProvider;
    private ForwardingManager fwManager;

    public VppRendererPolicyManager(@Nonnull ForwardingManager fwManager, @Nonnull DataBroker dataProvider) {
        this.fwManager = Preconditions.checkNotNull(fwManager);
        this.dataProvider = Preconditions.checkNotNull(dataProvider);
    }

    @Subscribe
    public void rendererPolicyChanged(RendererPolicyConfEvent event) {
        RendererPolicyBuilder responseBuilder = new RendererPolicyBuilder();
        switch (event.getDtoModificationType()) {
            case CREATED:
                LOG.debug("CREATED : {}", event.getIid());
                responseBuilder.setVersion(event.getAfter().get().getVersion());
                rendererPolicyCreated(event.getAfter().get());
                break;
            case UPDATED:
                LOG.debug("UPDATED: {}", event.getIid());
                RendererPolicy rPolicyBefore = event.getBefore().get();
                RendererPolicy rPolicyAfter = event.getAfter().get();
                responseBuilder.setVersion(rPolicyAfter.getVersion());
                if (rPolicyBefore.getConfiguration() == null && rPolicyAfter.getConfiguration() == null) {
                    LOG.debug("Configuration is not changed only updating config version from {} to {}",
                            rPolicyBefore.getVersion(), rPolicyAfter.getVersion());
                } else {
                    // TODO collect unconfigured rules and put them to responseBuilder
                    rendererPolicyUpdated(rPolicyBefore, rPolicyAfter);
                }
                break;
            case DELETED:
                LOG.debug("DELETED: {}", event.getIid());
                responseBuilder.setVersion(event.getBefore().get().getVersion());
                rendererPolicyDeleted(event.getBefore().get());
                break;
        }
        WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        RendererPolicy response = responseBuilder.build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, IidFactory.rendererIid(VppRenderer.NAME).child(RendererPolicy.class),
                response, true);
        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                LOG.debug("Renderer updated renderer policy {}", response);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("Renderer DIDN'T update renderer-policy {}", response);
            }
        });
    }

    private void rendererPolicyUpdated(RendererPolicy rPolicyBefore, RendererPolicy rPolicyAfter) {
        PolicyContext policyCtxBefore = new PolicyContext(rPolicyBefore);
        PolicyContext policyCtxAfter = new PolicyContext(rPolicyAfter);

        MapDifference<String, Collection<NodeId>> vppNodesByL2FlDiff =
                createDiffForVppNodesByL2Fd(policyCtxBefore, policyCtxAfter);
        SetMultimap<String, NodeId> removedVppNodesByL2Fd = HashMultimap.create();
        SetMultimap<String, NodeId> createdVppNodesByL2Fd = HashMultimap.create();
        for (Entry<String, ValueDifference<Collection<NodeId>>> entry : vppNodesByL2FlDiff.entriesDiffering()
            .entrySet()) {
            String bridgeDomain = entry.getKey();
            Collection<NodeId> beforeNodes = entry.getValue().leftValue();
            Collection<NodeId> afterNodes = entry.getValue().rightValue();
            if (beforeNodes != null && afterNodes != null) {
                SetView<NodeId> removedNodes = Sets.difference(new HashSet<>(beforeNodes), new HashSet<>(afterNodes));
                removedVppNodesByL2Fd.putAll(bridgeDomain, removedNodes);
                SetView<NodeId> createdNodes = Sets.difference(new HashSet<>(afterNodes), new HashSet<>(beforeNodes));
                createdVppNodesByL2Fd.putAll(bridgeDomain, createdNodes);
            } else if (beforeNodes != null) {
                removedVppNodesByL2Fd.putAll(bridgeDomain, beforeNodes);
            } else if (afterNodes != null) {
                createdVppNodesByL2Fd.putAll(bridgeDomain, afterNodes);
            }
        }
        Map<String, Collection<NodeId>> removedL2Fds = vppNodesByL2FlDiff.entriesOnlyOnLeft();
        for (Entry<String, Collection<NodeId>> entry : removedL2Fds.entrySet()) {
            String bridgeDomain = entry.getKey();
            Collection<NodeId> removedNodes = entry.getValue();
            if (removedNodes != null) {
                removedVppNodesByL2Fd.putAll(bridgeDomain, removedNodes);
            }
        }
        Map<String, Collection<NodeId>> createdL2Fds = vppNodesByL2FlDiff.entriesOnlyOnRight();
        for (Entry<String, Collection<NodeId>> entry : createdL2Fds.entrySet()) {
            String bridgeDomain = entry.getKey();
            Collection<NodeId> createdNodes = entry.getValue();
            if (createdNodes != null) {
                createdVppNodesByL2Fd.putAll(bridgeDomain, createdNodes);
            }
        }

        ImmutableSet<RendererEndpointKey> rendEpsBefore = policyCtxBefore.getPolicyTable().rowKeySet();
        ImmutableSet<RendererEndpointKey> rendEpsAfter = policyCtxAfter.getPolicyTable().rowKeySet();

        SetView<RendererEndpointKey> removedRendEps = Sets.difference(rendEpsBefore, rendEpsAfter);
        LOG.debug("Removed renderer endpoints {}", removedRendEps);
        removedRendEps.forEach(rEpKey -> fwManager.removeForwardingForEndpoint(rEpKey, policyCtxBefore));

        LOG.debug("Removed bridge domains on nodes {}", removedVppNodesByL2Fd);
        LOG.debug("Created bridge domains on nodes {}", createdVppNodesByL2Fd);
        fwManager.removeBridgeDomainOnNodes(removedVppNodesByL2Fd);
        fwManager.createBridgeDomainOnNodes(createdVppNodesByL2Fd);

        SetView<RendererEndpointKey> createdRendEps = Sets.difference(rendEpsAfter, rendEpsBefore);
        LOG.debug("Created renderer endpoints {}", createdRendEps);
        createdRendEps.forEach(rEpKey -> fwManager.createForwardingForEndpoint(rEpKey, policyCtxAfter));

        SetView<RendererEndpointKey> updatedRendEps = Sets.intersection(rendEpsBefore, rendEpsAfter);
        LOG.debug("Updated renderer endpoints {}", updatedRendEps);
        // update forwarding for endpoint
        updatedRendEps.forEach(rEpKey -> {
            AddressEndpointWithLocation addrEpWithLocBefore =
                    policyCtxBefore.getAddrEpByKey().get(KeyFactory.addressEndpointKey(rEpKey));
            AddressEndpointWithLocation addrEpWithLocAfter =
                    policyCtxAfter.getAddrEpByKey().get(KeyFactory.addressEndpointKey(rEpKey));
            if (isLocationChanged(addrEpWithLocBefore, addrEpWithLocAfter)) {
                LOG.debug("Location is changed in endpoint {}", rEpKey);
                LOG.debug("\nLocation before: {}\nLocation after: {}", addrEpWithLocBefore.getAbsoluteLocation(),
                        addrEpWithLocAfter.getAbsoluteLocation());
                fwManager.removeForwardingForEndpoint(rEpKey, policyCtxBefore);
                fwManager.createForwardingForEndpoint(rEpKey, policyCtxAfter);
            }
        });
    }

    private static boolean isLocationChanged(AddressEndpointWithLocation before, AddressEndpointWithLocation after) {
        ExternalLocationCase locationBefore = ForwardingManager.resolveAndValidateLocation(before);
        ExternalLocationCase locationAfter = ForwardingManager.resolveAndValidateLocation(after);
        return !locationBefore.equals(locationAfter);
    }

    private static MapDifference<String, Collection<NodeId>> createDiffForVppNodesByL2Fd(PolicyContext policyCtxBefore,
            PolicyContext policyCtxAfter) {
        ImmutableSet<RendererEndpointKey> rendEpsBefore = policyCtxBefore.getPolicyTable().rowKeySet();
        ImmutableSet<RendererEndpointKey> rendEpsAfter = policyCtxAfter.getPolicyTable().rowKeySet();
        SetMultimap<String, NodeId> vppNodesByL2FdBefore = resolveVppNodesByL2Fd(rendEpsBefore, policyCtxBefore);
        SetMultimap<String, NodeId> vppNodesByL2FdAfter = resolveVppNodesByL2Fd(rendEpsAfter, policyCtxAfter);
        return Maps.difference(vppNodesByL2FdBefore.asMap(), vppNodesByL2FdAfter.asMap());
    }

    private void rendererPolicyCreated(RendererPolicy rPolicy) {
        PolicyContext policyCtx = new PolicyContext(rPolicy);
        ImmutableSet<RendererEndpointKey> rEpKeys = policyCtx.getPolicyTable().rowKeySet();

        SetMultimap<String, NodeId> vppNodesByL2Fd = resolveVppNodesByL2Fd(rEpKeys, policyCtx);
        fwManager.createBridgeDomainOnNodes(vppNodesByL2Fd);

        rEpKeys.forEach(rEpKey -> fwManager.createForwardingForEndpoint(rEpKey, policyCtx));
    }

    private void rendererPolicyDeleted(RendererPolicy rendererPolicy) {
        PolicyContext policyCtx = new PolicyContext(rendererPolicy);
        ImmutableSet<RendererEndpointKey> rEpKeys = policyCtx.getPolicyTable().rowKeySet();

        rEpKeys.forEach(rEpKey -> fwManager.removeForwardingForEndpoint(rEpKey, policyCtx));

        SetMultimap<String, NodeId> vppNodesByL2Fd = resolveVppNodesByL2Fd(rEpKeys, policyCtx);
        fwManager.removeBridgeDomainOnNodes(vppNodesByL2Fd);
    }

    private static SetMultimap<String, NodeId> resolveVppNodesByL2Fd(Set<RendererEndpointKey> rEpKeys,
            PolicyContext policyCtx) {
        SetMultimap<String, NodeId> vppNodesByL2Fd = HashMultimap.create();
        rEpKeys.stream()
            .map(rEpKey -> KeyFactory.addressEndpointKey(rEpKey))
            .map(addrEpKey -> policyCtx.getAddrEpByKey().get(addrEpKey))
            .collect(Collectors.toSet())
            .forEach(addrEpWithLoc -> {
                java.util.Optional<String> optL2Fd = ForwardingManager.resolveL2FloodDomain(addrEpWithLoc, policyCtx);
                if (optL2Fd.isPresent()) {
                    ExternalLocationCase rEpLoc = ForwardingManager.resolveAndValidateLocation(addrEpWithLoc);
                    InstanceIdentifier<?> externalNodeMountPoint = rEpLoc.getExternalNodeMountPoint();
                    NodeId vppNode = externalNodeMountPoint.firstKeyOf(Node.class).getNodeId();
                    vppNodesByL2Fd.put(optL2Fd.get(), vppNode);
                }
            });
        return vppNodesByL2Fd;
    }

    @Subscribe
    public void vppNodeChanged(NodeOperEvent event) {
        switch (event.getDtoModificationType()) {
            case CREATED:
                if (event.isAfterConnected()) {
                    // TODO
                }
                break;
            case UPDATED:
                if (!event.isBeforeConnected() && event.isAfterConnected()) {
                    // TODO
                }
                break;
            case DELETED:
                if (event.isBeforeConnected()) {
                    // TODO
                }
                break;
        }
    }
}
