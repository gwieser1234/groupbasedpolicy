/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.sxp.mapper.impl.util;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointForwardingTemplateBySubnet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.groupbasedpolicy.sxp.mapper.model.rev160302.sxp.mapper.EndpointPolicyTemplateBySgt;

/**
 * Purpose: util methods for {@link EndpointForwardingTemplateBySubnet} and {@link EndpointPolicyTemplateBySgt}
 */
public final class EPTemplateUtil {

    public static final String FULL_IPV4_MASK_SUFFIX = "/32";

    private EPTemplateUtil() {
        throw new IllegalAccessError("constructing util class");
    }

    public static boolean isPlain(final IpPrefix key) {
        return key.getIpv4Prefix().getValue().endsWith(FULL_IPV4_MASK_SUFFIX);
    }

    public static SubnetInfoKeyDecorator buildSubnetInfoKey(@Nonnull final IpPrefix value) {
        return new SubnetInfoKeyDecorator(new SubnetUtils(value.getIpv4Prefix().getValue()).getInfo());
    }

    public static <L, R> ListenableFuture<OptionalMutablePair<L, R>> compositeRead(
            final ListenableFuture<Optional<L>> leftRead, final ListenableFuture<Optional<R>> rightRead) {

        final OptionalMutablePair<L, R> compositeResult = new OptionalMutablePair<>();
        final List<ListenableFuture<?>> results = new ArrayList<>(2);

        results.add(Futures.transform(leftRead, new Function<Optional<L>, OptionalMutablePair<L, R>>() {
            @Nullable
            @Override
            public OptionalMutablePair<L, R> apply(@Nullable final Optional<L> input) {
                compositeResult.setLeft(input);
                return compositeResult;
            }
        }));

        results.add(Futures.transform(rightRead, new Function<Optional<R>, OptionalMutablePair<L, R>>() {
            @Nullable
            @Override
            public OptionalMutablePair<L, R> apply(@Nullable final Optional<R> input) {
                compositeResult.setRight(input);
                return compositeResult;
            }
        }));

        return Futures.transform(Futures.successfulAsList(results),
                new Function<List<?>, OptionalMutablePair<L, R>>() {
                    @Nullable
                    @Override
                    public OptionalMutablePair<L, R> apply(@Nullable final List<?> input) {
                        return compositeResult;
                    }
                });
    }

    public static <K, V> ListenableFuture<Pair<K, V>> wrapToPair(
            final K keyItem,
            final ListenableFuture<Optional<V>> valueFromRead) {
        return Futures.transform(valueFromRead, new Function<Optional<V>, Pair<K, V>>() {
            @Nullable
            @Override
            public Pair<K, V> apply(@Nullable final Optional<V> input) {
                final MutablePair<K, V> pair = new MutablePair<>(keyItem, null);
                if (input != null && input.isPresent()) {
                    pair.setRight(input.get());
                }
                return pair;
            }
        });
    }

    public static <V> ListenableFuture<Optional<V>> wrapToOptional(final ListenableFuture<V> value) {
        return Futures.transform(value, new Function<V, Optional<V>>() {
            @Nullable
            @Override
            public Optional<V> apply(@Nullable final V input) {
                return Optional.fromNullable(input);
            }
        });
    }

    public static class OptionalMutablePair<L, R> extends MutablePair<Optional<L>, Optional<R>> {
        public OptionalMutablePair() {
            super(Optional.absent(), Optional.absent());
        }
    }
}
