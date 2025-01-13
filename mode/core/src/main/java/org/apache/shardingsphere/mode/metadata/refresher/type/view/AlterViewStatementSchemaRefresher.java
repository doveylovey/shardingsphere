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

package org.apache.shardingsphere.mode.metadata.refresher.type.view;

import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.rule.RuleMetaData;
import org.apache.shardingsphere.infra.metadata.database.schema.builder.GenericSchemaBuilder;
import org.apache.shardingsphere.infra.metadata.database.schema.builder.GenericSchemaBuilderMaterial;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereSchema;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereTable;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereView;
import org.apache.shardingsphere.infra.metadata.database.schema.pojo.AlterSchemaMetaDataPOJO;
import org.apache.shardingsphere.infra.rule.attribute.datanode.MutableDataNodeRuleAttribute;
import org.apache.shardingsphere.mode.metadata.refresher.MetaDataRefresher;
import org.apache.shardingsphere.mode.metadata.refresher.util.TableRefreshUtils;
import org.apache.shardingsphere.mode.persist.service.divided.MetaDataManagerPersistService;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.statement.ddl.AlterViewStatement;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

/**
 * Schema refresher for alter view statement.
 */
public final class AlterViewStatementSchemaRefresher implements MetaDataRefresher<AlterViewStatement> {
    
    @Override
    public void refresh(final MetaDataManagerPersistService metaDataManagerPersistService, final ShardingSphereDatabase database, final Collection<String> logicDataSourceNames,
                        final String schemaName, final DatabaseType databaseType, final AlterViewStatement sqlStatement, final ConfigurationProperties props) throws SQLException {
        String viewName = TableRefreshUtils.getTableName(databaseType, sqlStatement.getView().getTableName().getIdentifier());
        AlterSchemaMetaDataPOJO alterSchemaMetaDataPOJO = new AlterSchemaMetaDataPOJO(database.getName(), schemaName, logicDataSourceNames);
        Optional<SimpleTableSegment> renameView = sqlStatement.getRenameView();
        if (renameView.isPresent()) {
            String renameViewName = renameView.get().getTableName().getIdentifier().getValue();
            String originalView = database.getSchema(schemaName).getView(viewName).getViewDefinition();
            ShardingSphereSchema schema = getSchema(database, logicDataSourceNames, schemaName, renameViewName, originalView, props);
            alterSchemaMetaDataPOJO.getAlteredTables().add(schema.getTable(renameViewName));
            alterSchemaMetaDataPOJO.getAlteredViews().add(schema.getView(renameViewName));
            alterSchemaMetaDataPOJO.getDroppedTables().add(viewName);
            alterSchemaMetaDataPOJO.getDroppedViews().add(viewName);
        }
        Optional<String> viewDefinition = sqlStatement.getViewDefinition();
        if (viewDefinition.isPresent()) {
            ShardingSphereSchema schema = getSchema(database, logicDataSourceNames, schemaName, viewName, viewDefinition.get(), props);
            alterSchemaMetaDataPOJO.getAlteredTables().add(schema.getTable(viewName));
            alterSchemaMetaDataPOJO.getAlteredViews().add(schema.getView(viewName));
        }
        metaDataManagerPersistService.alterSchemaMetaData(alterSchemaMetaDataPOJO);
    }
    
    private ShardingSphereSchema getSchema(final ShardingSphereDatabase database, final Collection<String> logicDataSourceNames,
                                           final String schemaName, final String viewName, final String viewDefinition, final ConfigurationProperties props) throws SQLException {
        RuleMetaData ruleMetaData = new RuleMetaData(new LinkedList<>(database.getRuleMetaData().getRules()));
        if (TableRefreshUtils.isSingleTable(viewName, database)) {
            ruleMetaData.getAttributes(MutableDataNodeRuleAttribute.class).forEach(each -> each.put(logicDataSourceNames.iterator().next(), schemaName, viewName));
        }
        GenericSchemaBuilderMaterial material = new GenericSchemaBuilderMaterial(database.getResourceMetaData().getStorageUnits(), ruleMetaData.getRules(), props, schemaName);
        Map<String, ShardingSphereSchema> schemas = GenericSchemaBuilder.build(Collections.singletonList(viewName), database.getProtocolType(), material);
        Optional<ShardingSphereTable> actualViewMetaData = Optional.ofNullable(schemas.get(schemaName)).map(optional -> optional.getTable(viewName));
        ShardingSphereSchema result = new ShardingSphereSchema(schemaName);
        actualViewMetaData.ifPresent(result::putTable);
        result.putView(new ShardingSphereView(viewName, viewDefinition));
        return result;
    }
    
    @Override
    public Class<AlterViewStatement> getType() {
        return AlterViewStatement.class;
    }
}
