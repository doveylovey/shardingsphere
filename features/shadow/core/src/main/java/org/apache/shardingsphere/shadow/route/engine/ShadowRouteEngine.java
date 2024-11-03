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

package org.apache.shardingsphere.shadow.route.engine;

import org.apache.shardingsphere.infra.annotation.HighFrequencyInvocation;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.infra.route.context.RouteMapper;
import org.apache.shardingsphere.infra.route.context.RouteUnit;
import org.apache.shardingsphere.shadow.route.engine.finder.ShadowDataSourceMappingsFinder;
import org.apache.shardingsphere.shadow.rule.ShadowRule;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

/**
 * Shadow route engine.
 */
@HighFrequencyInvocation
public final class ShadowRouteEngine {
    
    /**
     * Route.
     *
     * @param routeContext route context
     * @param rule shadow rule
     * @param finder finder
     */
    public void route(final RouteContext routeContext, final ShadowRule rule, final ShadowDataSourceMappingsFinder finder) {
        Collection<RouteUnit> toBeRemovedRouteUnit = new LinkedList<>();
        Collection<RouteUnit> toBeAddedRouteUnit = new LinkedList<>();
        Map<String, String> shadowDataSourceMappings = finder.find(rule);
        for (RouteUnit each : routeContext.getRouteUnits()) {
            String logicName = each.getDataSourceMapper().getLogicName();
            String actualName = each.getDataSourceMapper().getActualName();
            Optional<String> productionDataSourceName = rule.findProductionDataSourceName(actualName);
            if (productionDataSourceName.isPresent()) {
                String shadowDataSourceName = shadowDataSourceMappings.get(productionDataSourceName.get());
                toBeRemovedRouteUnit.add(each);
                String dataSourceName = null == shadowDataSourceName ? productionDataSourceName.get() : shadowDataSourceName;
                toBeAddedRouteUnit.add(new RouteUnit(new RouteMapper(logicName, dataSourceName), each.getTableMappers()));
            }
        }
        routeContext.getRouteUnits().removeAll(toBeRemovedRouteUnit);
        routeContext.getRouteUnits().addAll(toBeAddedRouteUnit);
    }
}
