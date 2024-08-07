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

package org.apache.shardingsphere.mode.metadata.refresher.type.table;

import org.apache.shardingsphere.infra.config.props.ConfigurationProperties;
import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.schema.pojo.AlterSchemaMetaDataPOJO;
import org.apache.shardingsphere.mode.metadata.refresher.MetaDataRefresher;
import org.apache.shardingsphere.mode.metadata.refresher.util.TableRefreshUtils;
import org.apache.shardingsphere.mode.persist.service.MetaDataManagerPersistService;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.statement.ddl.DropTableStatement;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Schema refresher for drop table statement.
 */
public final class DropTableStatementSchemaRefresher implements MetaDataRefresher<DropTableStatement> {
    
    @Override
    public void refresh(final MetaDataManagerPersistService metaDataManagerPersistService, final ShardingSphereDatabase database, final Collection<String> logicDataSourceNames,
                        final String schemaName, final DatabaseType databaseType, final DropTableStatement sqlStatement, final ConfigurationProperties props) throws SQLException {
        AlterSchemaMetaDataPOJO alterSchemaMetaDataPOJO = new AlterSchemaMetaDataPOJO(database.getName(), schemaName);
        sqlStatement.getTables().forEach(each -> alterSchemaMetaDataPOJO.getDroppedTables().add(each.getTableName().getIdentifier().getValue()));
        boolean isRuleRefreshRequired = TableRefreshUtils.isRuleRefreshRequired(database.getRuleMetaData(), schemaName, sqlStatement.getTables());
        metaDataManagerPersistService.alterSchemaMetaData(alterSchemaMetaDataPOJO);
        for (SimpleTableSegment each : sqlStatement.getTables()) {
            if (isRuleRefreshRequired && TableRefreshUtils.isSingleTable(each.getTableName().getIdentifier().getValue(), database)) {
                metaDataManagerPersistService.alterSingleRuleConfiguration(database.getName(), database.getRuleMetaData().getConfigurations());
                break;
            }
        }
    }
    
    @Override
    public Class<DropTableStatement> getType() {
        return DropTableStatement.class;
    }
}
