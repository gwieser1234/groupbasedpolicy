/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.CheckedFuture;

public class VppNodeWriter {

    private static final Logger LOG = LoggerFactory.getLogger(VppNodeWriter.class);
    private List<RendererNode> rendererNodesCache;

    public VppNodeWriter() {
        rendererNodesCache = new ArrayList<>();
    }

    public void cache(RendererNode node) {
        rendererNodesCache.add(node);
    }

    /**
     * Put all cached items to data store.
     *
     * @param dataBroker appropriate data provider
     */
    public void commitToDatastore(DataBroker dataBroker) {
        RendererNodes rendererNodes = buildRendererNodes();
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<RendererNodes> iid = VppIidFactory.getRendererNodesIid();
        try {
            wtx.merge(LogicalDatastoreType.OPERATIONAL, iid, rendererNodes, true);
            CheckedFuture<Void, TransactionCommitFailedException> submitFuture = wtx.submit();
            submitFuture.checkedGet();
            // Clear cache
            rendererNodesCache.clear();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Write transaction failed to {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
        }
    }

    /**
     * Removes all cached items from data store.
     *
     * @param dataBroker appropriate data provider
     */
    public void removeFromDatastore(DataBroker dataBroker) {
        WriteTransaction wtx = dataBroker.newWriteOnlyTransaction();
        for (RendererNode nodeToRemove : rendererNodesCache) {
            InstanceIdentifier<RendererNode> iid = VppIidFactory.getRendererNodeIid(nodeToRemove);
            try {
                wtx.delete(LogicalDatastoreType.OPERATIONAL, iid);
                CheckedFuture<Void, TransactionCommitFailedException> submitFuture = wtx.submit();
                submitFuture.checkedGet();
                // Clear cache
            } catch (TransactionCommitFailedException e) {
                LOG.error("Write transaction failed to {}", e.getMessage());
            } catch (Exception e) {
                LOG.error("Failed to .. {}", e.getMessage());
            }
        }
        rendererNodesCache.clear();
    }

    private RendererNodes buildRendererNodes() {
        RendererNodesBuilder rendererNodesBuilder = new RendererNodesBuilder();
        rendererNodesBuilder.setRendererNode(new ArrayList<>(rendererNodesCache));
        return rendererNodesBuilder.build();
    }
}
