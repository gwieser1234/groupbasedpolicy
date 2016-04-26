/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/*
* Generated file
*
* Generated from: yang module name: neutron-mapper-impl yang module local name: neutron-mapper-impl
* Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
* Generated at: Thu Feb 19 12:58:22 CET 2015
*
* Do not modify this file unless it is present under src/main directory
*/

package org.opendaylight.controller.config.yang.config.neutron_mapper.impl;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.spi.Module;
import org.osgi.framework.BundleContext;

public class NeutronMapperModuleFactory extends org.opendaylight.controller.config.yang.config.neutron_mapper.impl.AbstractNeutronMapperModuleFactory {

    /**
     * @see org.opendaylight.controller.config.yang.config.neutron_mapper.impl.AbstractNeutronMapperModuleFactory#createModule(java.lang.String, org.opendaylight.controller.config.api.DependencyResolver, org.osgi.framework.BundleContext)
     */
    @Override
    public Module createModule(String instanceName, DependencyResolver dependencyResolver, BundleContext bundleContext) {
        NeutronMapperModule module = (NeutronMapperModule) super.createModule(instanceName, dependencyResolver, bundleContext);
        return module;
    }

    /**
     * @see org.opendaylight.controller.config.yang.config.neutron_mapper.impl.AbstractNeutronMapperModuleFactory#createModule(java.lang.String, org.opendaylight.controller.config.api.DependencyResolver, org.opendaylight.controller.config.api.DynamicMBeanWithInstance, org.osgi.framework.BundleContext)
     */
    @Override
    public Module createModule(String instanceName, DependencyResolver dependencyResolver, DynamicMBeanWithInstance old,
            BundleContext bundleContext) throws Exception {
        NeutronMapperModule module = (NeutronMapperModule) super.createModule(instanceName, dependencyResolver, old, bundleContext);
        return module;
    }

}
