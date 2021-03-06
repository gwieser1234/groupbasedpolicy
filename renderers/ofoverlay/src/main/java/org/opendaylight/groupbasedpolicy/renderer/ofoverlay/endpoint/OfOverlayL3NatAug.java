/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.endpoint;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentation;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterL3PrefixEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.endpoints.EndpointL3Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.l3endpoint.rev151217.NatAddressInput;
import org.opendaylight.yangtools.yang.binding.Augmentation;

public class OfOverlayL3NatAug implements EpRendererAugmentation, AutoCloseable {

    private EpRendererAugmentationRegistry epRendererAugmentationRegistry;

    public OfOverlayL3NatAug(EpRendererAugmentationRegistry epRendererAugmentationRegistry) {
        this.epRendererAugmentationRegistry = epRendererAugmentationRegistry;
        this.epRendererAugmentationRegistry.register(this);
    }

    @Override
    public Map.Entry<Class<? extends Augmentation<Endpoint>>, Augmentation<Endpoint>> buildEndpointAugmentation(
            RegisterEndpointInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map.Entry<Class<? extends Augmentation<EndpointL3>>, Augmentation<EndpointL3>> buildEndpointL3Augmentation(
            RegisterEndpointInput input) {
        if (input.getAugmentation(NatAddressInput.class) != null) {
            return new SimpleImmutableEntry<Class<? extends Augmentation<EndpointL3>>, Augmentation<EndpointL3>>(
                    NatAddress.class,
                    new NatAddressBuilder(input.getAugmentation(NatAddressInput.class)).build());
        }
        return null;
    }

    @Override
    public Map.Entry<Class<? extends Augmentation<EndpointL3Prefix>>, Augmentation<EndpointL3Prefix>> buildL3PrefixEndpointAugmentation(
            RegisterL3PrefixEndpointInput input) {
        return null;
    }

    @Override
    public void close() throws Exception {
        this.epRendererAugmentationRegistry.unregister(this);
    }
}
