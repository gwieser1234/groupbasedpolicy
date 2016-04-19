/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.dao;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.DSAsyncDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.EPTemplateListener;
import org.opendaylight.groupbasedpolicy.sxp.mapper.api.SimpleCachedDao;
import org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util.SxpListenerUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.SxpMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Purpose: general dao for EndPoint templates
 */
public class EPForwardingTemplateDaoImpl implements DSAsyncDao<IpPrefix, EndpointForwardingTemplateBySubnet> {

    public static final ListenableFuture<Optional<EndpointForwardingTemplateBySubnet>> READ_FUTURE_ABSENT = Futures.immediateFuture(Optional.absent());
    private final DataBroker dataBroker;
    private final SimpleCachedDao<IpPrefix, EndpointForwardingTemplateBySubnet> cachedDao;

    public EPForwardingTemplateDaoImpl(final DataBroker dataBroker,
                                       final SimpleCachedDao<IpPrefix, EndpointForwardingTemplateBySubnet> cachedDao) {
        this.dataBroker = dataBroker;
        this.cachedDao = cachedDao;
    }

    @Override
    public ListenableFuture<Optional<EndpointForwardingTemplateBySubnet>> read(@Nonnull final IpPrefix key) {
        final Optional<EndpointForwardingTemplateBySubnet> value = lookup(cachedDao, key);
        final ListenableFuture<Optional<EndpointForwardingTemplateBySubnet>> readResult;
        if (value.isPresent()) {
            readResult = Futures.immediateFuture(value);
        } else if (!cachedDao.isEmpty()) {
            return READ_FUTURE_ABSENT;
        } else {
            final ReadOnlyTransaction rTx = dataBroker.newReadOnlyTransaction();
            final CheckedFuture<Optional<SxpMapper>, ReadFailedException> read =
                    rTx.read(LogicalDatastoreType.CONFIGURATION, buildReadPath(key));

            Futures.addCallback(read, SxpListenerUtil.createTxCloseCallback(rTx));

            readResult = Futures.transform(read, new Function<Optional<SxpMapper>, Optional<EndpointForwardingTemplateBySubnet>>() {
                @Nullable
                @Override
                public Optional<EndpointForwardingTemplateBySubnet> apply(@Nullable final Optional<SxpMapper> input) {
                    if (input.isPresent()) {
                        // clean cache
                        cachedDao.invalidateCache();

                        // iterate through all template entries and update cachedDao
                        final List<EndpointForwardingTemplateBySubnet> templateLot = input.get().getEndpointForwardingTemplateBySubnet();
                        if (templateLot != null) {
                            for (EndpointForwardingTemplateBySubnet template : templateLot) {
                                cachedDao.update(template.getIpPrefix(), template);
                            }
                        }
                        return cachedDao.find(key);
                    } else {
                        return Optional.absent();
                    }
                }
            });
        }
        return readResult;
    }

    protected InstanceIdentifier<SxpMapper> buildReadPath(final IpPrefix key) {
        return EPTemplateListener.SXP_MAPPER_TEMPLATE_PARENT_PATH;
    }

    private Optional<EndpointForwardingTemplateBySubnet> lookup(final SimpleCachedDao<IpPrefix, EndpointForwardingTemplateBySubnet> cachedDao, final IpPrefix key) {
        return cachedDao.find(key);
    }

}
