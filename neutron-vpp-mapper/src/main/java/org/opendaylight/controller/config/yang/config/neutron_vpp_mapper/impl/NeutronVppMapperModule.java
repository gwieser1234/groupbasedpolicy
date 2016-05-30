/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.neutron_vpp_mapper.impl;

import org.opendaylight.groupbasedpolicy.neutron.vpp.mapper.NeutronVppMapper;

public class NeutronVppMapperModule extends org.opendaylight.controller.config.yang.config.neutron_vpp_mapper.impl.AbstractNeutronVppMapperModule {
    public NeutronVppMapperModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NeutronVppMapperModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.neutron_vpp_mapper.impl.NeutronVppMapperModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        NeutronVppMapper neutronVppMapper = new NeutronVppMapper(getDataBrokerDependency());
        return neutronVppMapper;
    }

}