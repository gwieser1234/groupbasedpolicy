/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.ofoverlay_provider.impl;


import org.opendaylight.controller.sal.common.util.NoopAutoCloseable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Deprecated
public class OFOverlayProviderModule extends org.opendaylight.controller.config.yang.config.ofoverlay_provider.impl.AbstractOFOverlayProviderModule {
    private static final Logger LOG = LoggerFactory
            .getLogger(OFOverlayProviderModule.class);

    public OFOverlayProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public OFOverlayProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.ofoverlay_provider.impl.OFOverlayProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return NoopAutoCloseable.INSTANCE;
    }

}
