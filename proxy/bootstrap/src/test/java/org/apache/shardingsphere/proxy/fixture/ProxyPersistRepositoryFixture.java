/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.proxy.fixture;

import org.apache.shardingsphere.infra.instance.ComputeNodeInstanceContext;
import org.apache.shardingsphere.mode.repository.cluster.ClusterPersistRepository;
import org.apache.shardingsphere.mode.repository.cluster.ClusterPersistRepositoryConfiguration;
import org.apache.shardingsphere.mode.repository.cluster.listener.DataChangedEventListener;
import org.apache.shardingsphere.mode.repository.cluster.lock.DistributedLock;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ProxyPersistRepositoryFixture implements ClusterPersistRepository {
    
    private static final Map<String, String> REGISTRY_DATA = new LinkedHashMap<>();
    
    @Override
    public void init(final ClusterPersistRepositoryConfiguration config, final ComputeNodeInstanceContext computeNodeInstanceContext) {
    }
    
    @Override
    public String query(final String key) {
        return REGISTRY_DATA.get(key);
    }
    
    @Override
    public List<String> getChildrenKeys(final String key) {
        return Collections.singletonList("db");
    }
    
    @Override
    public boolean isExisted(final String key) {
        return false;
    }
    
    @Override
    public void persist(final String key, final String value) {
        REGISTRY_DATA.put(key, value);
    }
    
    @Override
    public void update(final String key, final String value) {
    }
    
    @Override
    public void persistEphemeral(final String key, final String value) {
        REGISTRY_DATA.put(key, value);
    }
    
    @Override
    public boolean persistExclusiveEphemeral(final String key, final String value) {
        return true;
    }
    
    @Override
    public Optional<DistributedLock> getDistributedLock(final String lockKey) {
        return Optional.empty();
    }
    
    @Override
    public void delete(final String key) {
    }
    
    @Override
    public void watch(final String key, final DataChangedEventListener listener) {
    }
    
    @Override
    public void removeDataListener(final String key) {
    }
    
    @Override
    public void close() {
        REGISTRY_DATA.clear();
    }
    
    @Override
    public String getType() {
        return "PROXY_FIXTURE";
    }
}
