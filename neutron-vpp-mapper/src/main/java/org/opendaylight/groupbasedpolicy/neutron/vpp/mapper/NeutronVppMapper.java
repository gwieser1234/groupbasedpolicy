/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.neutron.vpp.mapper;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.processors.NeutronListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronVppMapper implements AutoCloseable {

    NeutronListener neutronListener;
    private static final Logger LOG = LoggerFactory.getLogger(NeutronVppMapper.class);

    public NeutronVppMapper(String socketPath, String socketPrefix, DataBroker dataBroker) {
        SocketInfo socketInfo = new SocketInfo(socketPath, socketPrefix);
        neutronListener = new NeutronListener(dataBroker, socketInfo);
        LOG.info("Neutron VPP started!");
    }

    @Override
    public void close() {
        neutronListener.close();
    }
}
