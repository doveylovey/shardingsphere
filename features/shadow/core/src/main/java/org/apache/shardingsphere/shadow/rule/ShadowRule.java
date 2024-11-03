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

package org.apache.shardingsphere.shadow.rule;

import lombok.Getter;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.annotation.HighFrequencyInvocation;
import org.apache.shardingsphere.infra.rule.scope.DatabaseRule;
import org.apache.shardingsphere.infra.rule.attribute.RuleAttributes;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.shadow.config.ShadowRuleConfiguration;
import org.apache.shardingsphere.shadow.config.datasource.ShadowDataSourceConfiguration;
import org.apache.shardingsphere.shadow.config.table.ShadowTableConfiguration;
import org.apache.shardingsphere.shadow.spi.ShadowOperationType;
import org.apache.shardingsphere.shadow.spi.column.ColumnShadowAlgorithm;
import org.apache.shardingsphere.shadow.spi.hint.HintShadowAlgorithm;
import org.apache.shardingsphere.shadow.rule.attribute.ShadowDataSourceMapperRuleAttribute;
import org.apache.shardingsphere.shadow.spi.ShadowAlgorithm;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Databases shadow rule.
 */
@Getter
public final class ShadowRule implements DatabaseRule {
    
    private final ShadowRuleConfiguration configuration;
    
    private final Collection<String> hintShadowAlgorithmNames = new LinkedList<>();
    
    private final Map<String, ShadowDataSourceRule> shadowDataSourceMappings = new LinkedHashMap<>();
    
    private final Map<String, ShadowAlgorithm> shadowAlgorithms = new LinkedHashMap<>();
    
    private final Map<String, ShadowTableRule> shadowTableRules = new LinkedHashMap<>();
    
    private final ShadowAlgorithm defaultShadowAlgorithm;
    
    @Getter
    private final RuleAttributes attributes;
    
    public ShadowRule(final ShadowRuleConfiguration ruleConfig) {
        configuration = ruleConfig;
        initShadowDataSourceMappings(ruleConfig.getDataSources());
        initShadowAlgorithmConfigurations(ruleConfig.getShadowAlgorithms());
        defaultShadowAlgorithm = shadowAlgorithms.get(ruleConfig.getDefaultShadowAlgorithmName());
        if (defaultShadowAlgorithm instanceof HintShadowAlgorithm<?>) {
            hintShadowAlgorithmNames.add(ruleConfig.getDefaultShadowAlgorithmName());
        }
        initShadowTableRules(ruleConfig.getTables());
        attributes = new RuleAttributes(new ShadowDataSourceMapperRuleAttribute(shadowDataSourceMappings));
    }
    
    private void initShadowDataSourceMappings(final Collection<ShadowDataSourceConfiguration> dataSources) {
        dataSources.forEach(each -> shadowDataSourceMappings.put(each.getName(), new ShadowDataSourceRule(each.getProductionDataSourceName(), each.getShadowDataSourceName())));
    }
    
    private void initShadowAlgorithmConfigurations(final Map<String, AlgorithmConfiguration> shadowAlgorithmConfigs) {
        shadowAlgorithmConfigs.forEach((key, value) -> {
            ShadowAlgorithm algorithm = TypedSPILoader.getService(ShadowAlgorithm.class, value.getType(), value.getProps());
            if (algorithm instanceof HintShadowAlgorithm<?>) {
                hintShadowAlgorithmNames.add(key);
            }
            shadowAlgorithms.put(key, algorithm);
        });
    }
    
    private void initShadowTableRules(final Map<String, ShadowTableConfiguration> tables) {
        tables.forEach((key, value) -> shadowTableRules.put(key, new ShadowTableRule(key, value.getDataSourceNames(), value.getShadowAlgorithmNames(), shadowAlgorithms)));
    }
    
    /**
     * Get default shadow algorithm.
     *
     * @return shadow algorithm
     */
    @HighFrequencyInvocation
    public Optional<ShadowAlgorithm> getDefaultShadowAlgorithm() {
        return Optional.ofNullable(defaultShadowAlgorithm);
    }
    
    /**
     * Get related shadow tables.
     *
     * @param tableNames table names
     * @return related shadow tables
     */
    @HighFrequencyInvocation
    public Collection<String> getRelatedShadowTables(final Collection<String> tableNames) {
        Collection<String> result = new LinkedList<>();
        for (String each : tableNames) {
            if (shadowTableRules.containsKey(each)) {
                result.add(each);
            }
        }
        return result;
    }
    
    /**
     * Get all shadow table names.
     *
     * @return shadow table names
     */
    @HighFrequencyInvocation
    public Collection<String> getAllShadowTableNames() {
        return shadowTableRules.keySet();
    }
    
    /**
     * Get related hint shadow algorithms.
     *
     * @return related hint shadow algorithms
     */
    @HighFrequencyInvocation
    @SuppressWarnings("unchecked")
    public Collection<HintShadowAlgorithm<Comparable<?>>> getAllHintShadowAlgorithms() {
        Collection<HintShadowAlgorithm<Comparable<?>>> result = new LinkedList<>();
        for (String each : hintShadowAlgorithmNames) {
            result.add((HintShadowAlgorithm<Comparable<?>>) shadowAlgorithms.get(each));
        }
        return result;
    }
    
    /**
     * Get related hint shadow algorithms by table name.
     *
     * @param tableName table name
     * @return hint shadow algorithms
     */
    @HighFrequencyInvocation
    @SuppressWarnings("unchecked")
    public Collection<HintShadowAlgorithm<Comparable<?>>> getRelatedHintShadowAlgorithms(final String tableName) {
        Collection<HintShadowAlgorithm<Comparable<?>>> result = new LinkedList<>();
        Collection<String> hintShadowAlgorithmNames = shadowTableRules.get(tableName).getHintShadowAlgorithmNames();
        for (String each : hintShadowAlgorithmNames) {
            result.add((HintShadowAlgorithm<Comparable<?>>) shadowAlgorithms.get(each));
        }
        return result;
    }
    
    /**
     * Get related column shadow algorithms by table name.
     *
     * @param shadowOperationType shadow operation type
     * @param tableName table name
     * @param shadowColumn shadow column
     * @return column shadow algorithms
     */
    @HighFrequencyInvocation
    @SuppressWarnings("unchecked")
    public Collection<ColumnShadowAlgorithm<Comparable<?>>> getRelatedColumnShadowAlgorithms(final ShadowOperationType shadowOperationType, final String tableName, final String shadowColumn) {
        Collection<ColumnShadowAlgorithm<Comparable<?>>> result = new LinkedList<>();
        Map<ShadowOperationType, Collection<ShadowAlgorithmNameRule>> columnShadowAlgorithmNames = shadowTableRules.get(tableName).getColumnShadowAlgorithmNames();
        for (ShadowAlgorithmNameRule each : columnShadowAlgorithmNames.getOrDefault(shadowOperationType, Collections.emptyList())) {
            if (shadowColumn.equals(each.getShadowColumnName())) {
                result.add((ColumnShadowAlgorithm<Comparable<?>>) shadowAlgorithms.get(each.getShadowAlgorithmName()));
            }
        }
        return result;
    }
    
    /**
     * Get related shadow column names.
     *
     * @param shadowOperationType shadow operation type
     * @param tableName table name
     * @return related shadow column names
     */
    @HighFrequencyInvocation
    public Collection<String> getRelatedShadowColumnNames(final ShadowOperationType shadowOperationType, final String tableName) {
        Collection<String> result = new LinkedList<>();
        Map<ShadowOperationType, Collection<ShadowAlgorithmNameRule>> columnShadowAlgorithmNames = shadowTableRules.get(tableName).getColumnShadowAlgorithmNames();
        for (ShadowAlgorithmNameRule each : columnShadowAlgorithmNames.getOrDefault(shadowOperationType, Collections.emptyList())) {
            result.add(each.getShadowColumnName());
        }
        return result;
    }
    
    /**
     * Get shadow data source mappings.
     *
     * @param tableName table name
     * @return shadow data source rules
     */
    @HighFrequencyInvocation
    public Map<String, String> getRelatedShadowDataSourceMappings(final String tableName) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String each : shadowTableRules.get(tableName).getShadowDataSources()) {
            ShadowDataSourceRule dataSourceRule = shadowDataSourceMappings.get(each);
            result.put(dataSourceRule.getProductionDataSource(), dataSourceRule.getShadowDataSource());
        }
        return result;
    }
    
    /**
     * Get all shadow data source mappings.
     *
     * @return all shadow data source mappings
     */
    @HighFrequencyInvocation
    public Map<String, String> getAllShadowDataSourceMappings() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Entry<String, ShadowDataSourceRule> entry : shadowDataSourceMappings.entrySet()) {
            ShadowDataSourceRule rule = entry.getValue();
            result.put(rule.getProductionDataSource(), rule.getShadowDataSource());
        }
        return result;
    }
    
    /**
     * Find production data source name.
     *
     * @param actualDataSourceName actual data source name
     * @return found production data source name
     */
    @HighFrequencyInvocation
    public Optional<String> findProductionDataSourceName(final String actualDataSourceName) {
        ShadowDataSourceRule dataSourceRule = shadowDataSourceMappings.get(actualDataSourceName);
        return null == dataSourceRule ? Optional.empty() : Optional.of(dataSourceRule.getProductionDataSource());
    }
}
