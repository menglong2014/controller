/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl

import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction
import org.opendaylight.yangtools.yang.common.RpcResult
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import org.opendaylight.controller.sal.common.util.Rpcs
import java.util.Collections
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.controller.sal.core.api.data.DataStore
import java.util.HashSet

class HashMapDataStore implements DataStore, AutoCloseable {


    val Map<InstanceIdentifier, CompositeNode> configuration = new ConcurrentHashMap();
    val Map<InstanceIdentifier, CompositeNode> operational = new ConcurrentHashMap();
    
    
    
    override containsConfigurationPath(InstanceIdentifier path) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
        
    }
    
    override containsOperationalPath(InstanceIdentifier path) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }
    
    override getStoredConfigurationPaths() {
        configuration.keySet
    }
    
    override getStoredOperationalPaths() {
        operational.keySet
    }

    override readConfigurationData(InstanceIdentifier path) {
        configuration.get(path);
    }

    override readOperationalData(InstanceIdentifier path) {
        operational.get(path);
    }



    override requestCommit(DataModification<InstanceIdentifier, CompositeNode> modification) {
        return new HashMapDataStoreTransaction(modification, this);
    }

    def RpcResult<Void> rollback(HashMapDataStoreTransaction transaction) {
        return Rpcs.getRpcResult(true, null, Collections.emptySet);
    }

    def RpcResult<Void> finish(HashMapDataStoreTransaction transaction) {
        val modification = transaction.modification;
        for (removal : modification.removedConfigurationData) {
            remove(configuration,removal);
        }
        for (removal : modification.removedOperationalData) {
            remove(operational,removal);
        }
        configuration.putAll(modification.updatedConfigurationData);
        operational.putAll(modification.updatedOperationalData);

        return Rpcs.getRpcResult(true, null, Collections.emptySet);
    }
    
    def remove(Map<InstanceIdentifier, CompositeNode> map, InstanceIdentifier identifier) {
        val affected = new HashSet<InstanceIdentifier>();
        for(path : map.keySet) {
            if(identifier.contains(path)) {
                affected.add(path);
            }
        }
        for(pathToRemove : affected) {
            map.remove(pathToRemove);
        }
        
    }


    override close()  {
        // NOOP
    }
    
}

class HashMapDataStoreTransaction implements // 
DataCommitTransaction<InstanceIdentifier, CompositeNode> {
    @Property
    val DataModification<InstanceIdentifier, CompositeNode> modification

    @Property
    val HashMapDataStore datastore;

    new(
        DataModification<InstanceIdentifier, CompositeNode> modify,
        HashMapDataStore store
    ) {
        _modification = modify;
        _datastore = store;
    }

    override finish() throws IllegalStateException {
        datastore.finish(this);

    }

    override getModification() {
        this._modification;
    }

    override rollback() throws IllegalStateException {
        datastore.rollback(this);
    }
}
