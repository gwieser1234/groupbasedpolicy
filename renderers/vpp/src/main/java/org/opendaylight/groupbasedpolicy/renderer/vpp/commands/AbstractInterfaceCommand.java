/*
 * Copyright (c) 2016 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractInterfaceCommand<T extends AbstractInterfaceCommand<T>> implements ConfigCommand {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractInterfaceCommand.class);

    protected General.Operations operation;
    protected String name;
    protected String description;
    protected Boolean enabled;

    protected enum linkUpDownTrap {
        ENABLED, DISABLED
    }

    public General.Operations getOperation() {
        return operation;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AbstractInterfaceCommand<T> setDescription(String description) {
        this.description = description;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public AbstractInterfaceCommand<T> setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public void execute(ReadWriteTransaction rwTx) {
        switch (getOperation()) {
            case PUT:
                LOG.debug("Executing Add operations for command: {}", this);
                put(rwTx);
                break;
            case DELETE:
                LOG.debug("Executing Delete operations for command: {}", this);
                delete(rwTx);
                break;
            case MERGE:
                LOG.debug("Executing Update operations for command: {}", this);
                merge(rwTx);
                break;
            default:
                LOG.error("Execution failed for command: {}", this);
                break;
        }
    }

    private void put(ReadWriteTransaction rwTx) {
        InterfaceBuilder interfaceBuilder = getInterfaceBuilder();

        rwTx.put(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getInterfaceIID(interfaceBuilder.getKey()),
                interfaceBuilder.build(), true);
    }

    private void merge(ReadWriteTransaction rwTx) {
        InterfaceBuilder interfaceBuilder = getInterfaceBuilder();

        rwTx.merge(LogicalDatastoreType.CONFIGURATION, VppIidFactory.getInterfaceIID(interfaceBuilder.getKey()),
                interfaceBuilder.build());
    }

    private void delete(ReadWriteTransaction readWriteTransaction) {
        try {
            readWriteTransaction.delete(LogicalDatastoreType.CONFIGURATION,
                    VppIidFactory.getInterfaceIID(new InterfaceKey(name)));
        } catch (IllegalStateException ex) {
            LOG.debug("Interface is not present in DS {}", this, ex);
        }

    }
}
