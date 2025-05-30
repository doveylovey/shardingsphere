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

package org.apache.shardingsphere.sqlfederation.executor.enumerable;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.shardingsphere.infra.binder.context.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.binder.context.type.TableAvailable;
import org.apache.shardingsphere.infra.binder.engine.SQLBindEngine;
import org.apache.shardingsphere.infra.connection.kernel.KernelProcessor;
import org.apache.shardingsphere.infra.database.core.metadata.database.metadata.option.table.DialectDriverQuerySystemCatalogOption;
import org.apache.shardingsphere.infra.database.core.metadata.database.system.SystemDatabase;
import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.database.core.type.DatabaseTypeRegistry;
import org.apache.shardingsphere.infra.exception.core.ShardingSpherePreconditions;
import org.apache.shardingsphere.infra.exception.kernel.connection.SQLExecutionInterruptedException;
import org.apache.shardingsphere.infra.executor.kernel.model.ExecutionGroup;
import org.apache.shardingsphere.infra.executor.kernel.model.ExecutionGroupContext;
import org.apache.shardingsphere.infra.executor.kernel.model.ExecutionGroupReportContext;
import org.apache.shardingsphere.infra.executor.sql.context.ExecutionContext;
import org.apache.shardingsphere.infra.executor.sql.context.ExecutionUnit;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.driver.jdbc.JDBCExecutionUnit;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.driver.jdbc.JDBCExecutor;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.driver.jdbc.JDBCExecutorCallback;
import org.apache.shardingsphere.infra.executor.sql.execute.result.ExecuteResult;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResult;
import org.apache.shardingsphere.infra.executor.sql.prepare.driver.DriverExecutionPrepareEngine;
import org.apache.shardingsphere.infra.executor.sql.process.ProcessEngine;
import org.apache.shardingsphere.infra.executor.sql.process.ProcessRegistry;
import org.apache.shardingsphere.infra.hint.HintValueContext;
import org.apache.shardingsphere.infra.merge.MergeEngine;
import org.apache.shardingsphere.infra.merge.result.MergedResult;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.rule.RuleMetaData;
import org.apache.shardingsphere.infra.metadata.database.schema.model.ShardingSphereTable;
import org.apache.shardingsphere.infra.metadata.statistics.ShardingSphereStatistics;
import org.apache.shardingsphere.infra.metadata.statistics.TableStatistics;
import org.apache.shardingsphere.infra.session.query.QueryContext;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;
import org.apache.shardingsphere.sqlfederation.executor.context.SQLFederationContext;
import org.apache.shardingsphere.sqlfederation.executor.context.SQLFederationExecutorContext;
import org.apache.shardingsphere.sqlfederation.executor.enumerator.JDBCRowEnumerator;
import org.apache.shardingsphere.sqlfederation.executor.enumerator.MemoryRowEnumerator;
import org.apache.shardingsphere.sqlfederation.executor.utils.StatisticsAssembleUtils;
import org.apache.shardingsphere.sqlfederation.compiler.context.CompilerContext;
import org.apache.shardingsphere.sqlfederation.compiler.metadata.schema.table.EmptyRowEnumerator;
import org.apache.shardingsphere.sqlfederation.compiler.metadata.schema.table.ScanExecutor;
import org.apache.shardingsphere.sqlfederation.compiler.metadata.schema.table.ScanExecutorContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Enumerable scan executor.
 */
@RequiredArgsConstructor
public final class EnumerableScanExecutor implements ScanExecutor {
    
    private final DriverExecutionPrepareEngine<JDBCExecutionUnit, Connection> prepareEngine;
    
    private final JDBCExecutor jdbcExecutor;
    
    private final JDBCExecutorCallback<? extends ExecuteResult> callback;
    
    private final CompilerContext compilerContext;
    
    private final SQLFederationExecutorContext executorContext;
    
    private final SQLFederationContext federationContext;
    
    private final RuleMetaData globalRuleMetaData;
    
    private final ShardingSphereStatistics statistics;
    
    private final ProcessEngine processEngine = new ProcessEngine();
    
    @Override
    public Enumerable<Object> execute(final ShardingSphereTable table, final ScanExecutorContext scanContext) {
        SQLStatementContext sqlStatementContext = federationContext.getQueryContext().getSqlStatementContext();
        if (containsSystemSchema(sqlStatementContext)) {
            return createMemoryEnumerable(sqlStatementContext, table);
        }
        QueryContext queryContext = createQueryContext(federationContext.getMetaData(), scanContext, sqlStatementContext.getDatabaseType(), federationContext.getQueryContext().isUseCache());
        ExecutionContext executionContext = new KernelProcessor().generateExecutionContext(queryContext, globalRuleMetaData, executorContext.getProps());
        if (federationContext.isPreview()) {
            federationContext.getPreviewExecutionUnits().addAll(executionContext.getExecutionUnits());
            return createEmptyEnumerable();
        }
        return createJDBCEnumerable(queryContext, federationContext.getMetaData().getDatabase(executorContext.getCurrentDatabaseName()), executionContext);
    }
    
    private boolean containsSystemSchema(final SQLStatementContext sqlStatementContext) {
        Collection<String> usedSchemaNames = sqlStatementContext instanceof TableAvailable ? ((TableAvailable) sqlStatementContext).getTablesContext().getSchemaNames() : Collections.emptyList();
        Collection<String> systemSchemas = new SystemDatabase(sqlStatementContext.getDatabaseType()).getSystemSchemas();
        for (String each : usedSchemaNames) {
            if (systemSchemas.contains(each)) {
                return true;
            }
        }
        return false;
    }
    
    private AbstractEnumerable<Object> createJDBCEnumerable(final QueryContext queryContext, final ShardingSphereDatabase database, final ExecutionContext context) {
        return new AbstractEnumerable<Object>() {
            
            @SneakyThrows
            @Override
            public Enumerator<Object> enumerator() {
                computeConnectionOffsets(context);
                // TODO pass grantee from proxy and jdbc adapter
                ExecutionGroupContext<JDBCExecutionUnit> executionGroupContext = prepareEngine.prepare(
                        database.getName(), context.getRouteContext(), executorContext.getConnectionOffsets(),
                        context.getExecutionUnits(), new ExecutionGroupReportContext(federationContext.getProcessId(), database.getName()));
                setParameters(executionGroupContext.getInputGroups());
                ShardingSpherePreconditions.checkState(!ProcessRegistry.getInstance().get(federationContext.getProcessId()).isInterrupted(), SQLExecutionInterruptedException::new);
                processEngine.executeSQL(executionGroupContext, federationContext.getQueryContext());
                List<QueryResult> queryResults = jdbcExecutor.execute(executionGroupContext, callback).stream().map(QueryResult.class::cast).collect(Collectors.toList());
                MergeEngine mergeEngine = new MergeEngine(federationContext.getMetaData(), database, executorContext.getProps(), federationContext.getQueryContext().getConnectionContext());
                MergedResult mergedResult = mergeEngine.merge(queryResults, queryContext.getSqlStatementContext());
                Collection<Statement> statements = getStatements(executionGroupContext.getInputGroups());
                return new JDBCRowEnumerator(mergedResult, queryResults.get(0).getMetaData(), statements);
            }
        };
    }
    
    private void computeConnectionOffsets(final ExecutionContext context) {
        for (ExecutionUnit each : context.getExecutionUnits()) {
            if (executorContext.getConnectionOffsets().containsKey(each.getDataSourceName())) {
                int connectionOffset = executorContext.getConnectionOffsets().get(each.getDataSourceName());
                executorContext.getConnectionOffsets().put(each.getDataSourceName(), ++connectionOffset);
            } else {
                executorContext.getConnectionOffsets().put(each.getDataSourceName(), 0);
            }
        }
    }
    
    private Enumerable<Object> createMemoryEnumerable(final SQLStatementContext sqlStatementContext, final ShardingSphereTable table) {
        DatabaseType databaseType = sqlStatementContext.getDatabaseType();
        Optional<DialectDriverQuerySystemCatalogOption> driverQuerySystemCatalogOption = new DatabaseTypeRegistry(databaseType).getDialectDatabaseMetaData().getDriverQuerySystemCatalogOption();
        if (driverQuerySystemCatalogOption.isPresent() && driverQuerySystemCatalogOption.get().isSystemTable(table.getName())) {
            return createMemoryEnumerator(StatisticsAssembleUtils.assembleTableStatistics(table, federationContext.getMetaData(), driverQuerySystemCatalogOption.get()), table, databaseType);
        }
        ShardingSpherePreconditions.checkState(sqlStatementContext instanceof TableAvailable,
                () -> new IllegalStateException(String.format("Can not support %s in sql federation", sqlStatementContext.getSqlStatement().getClass().getSimpleName())));
        String databaseName = ((TableAvailable) sqlStatementContext).getTablesContext().getDatabaseName().orElse(executorContext.getCurrentDatabaseName());
        String schemaName = ((TableAvailable) sqlStatementContext).getTablesContext().getSchemaName().orElse(executorContext.getCurrentSchemaName());
        Optional<TableStatistics> tableStatistics = Optional.ofNullable(statistics.getDatabaseStatistics(databaseName))
                .map(optional -> optional.getSchemaStatistics(schemaName)).map(optional -> optional.getTableStatistics(table.getName()));
        return tableStatistics.map(optional -> createMemoryEnumerator(optional, table, databaseType)).orElseGet(this::createEmptyEnumerable);
    }
    
    private Enumerable<Object> createMemoryEnumerator(final TableStatistics tableStatistics, final ShardingSphereTable table, final DatabaseType databaseType) {
        return new AbstractEnumerable<Object>() {
            
            @Override
            public Enumerator<Object> enumerator() {
                return new MemoryRowEnumerator(tableStatistics.getRows(), table.getAllColumns(), databaseType);
            }
        };
    }
    
    private Collection<Statement> getStatements(final Collection<ExecutionGroup<JDBCExecutionUnit>> inputGroups) {
        Collection<Statement> result = new LinkedList<>();
        for (ExecutionGroup<JDBCExecutionUnit> each : inputGroups) {
            for (JDBCExecutionUnit executionUnit : each.getInputs()) {
                result.add(executionUnit.getStorageResource());
            }
        }
        return result;
    }
    
    private void setParameters(final Collection<ExecutionGroup<JDBCExecutionUnit>> inputGroups) {
        for (ExecutionGroup<JDBCExecutionUnit> each : inputGroups) {
            for (JDBCExecutionUnit executionUnit : each.getInputs()) {
                if (!(executionUnit.getStorageResource() instanceof PreparedStatement)) {
                    continue;
                }
                setParameters((PreparedStatement) executionUnit.getStorageResource(), executionUnit.getExecutionUnit().getSqlUnit().getParameters());
            }
        }
    }
    
    @SneakyThrows(SQLException.class)
    private void setParameters(final PreparedStatement preparedStatement, final List<Object> params) {
        for (int i = 0; i < params.size(); i++) {
            preparedStatement.setObject(i + 1, params.get(i));
        }
    }
    
    private QueryContext createQueryContext(final ShardingSphereMetaData metaData, final ScanExecutorContext sqlString, final DatabaseType databaseType, final boolean useCache) {
        String sql = sqlString.getSql().replace(System.lineSeparator(), " ");
        SQLStatement sqlStatement = compilerContext.getSqlParserRule().getSQLParserEngine(databaseType).parse(sql, useCache);
        List<Object> params = getParameters(sqlString.getParamIndexes());
        HintValueContext hintValueContext = new HintValueContext();
        SQLStatementContext sqlStatementContext = new SQLBindEngine(metaData, executorContext.getCurrentDatabaseName(), hintValueContext).bind(sqlStatement, params);
        return new QueryContext(sqlStatementContext, sql, params, hintValueContext, federationContext.getQueryContext().getConnectionContext(), metaData, useCache);
    }
    
    private List<Object> getParameters(final int[] paramIndexes) {
        if (null == paramIndexes) {
            return Collections.emptyList();
        }
        List<Object> result = new ArrayList<>(paramIndexes.length);
        for (int each : paramIndexes) {
            result.add(federationContext.getQueryContext().getParameters().get(each));
        }
        return result;
    }
    
    private AbstractEnumerable<Object> createEmptyEnumerable() {
        return new AbstractEnumerable<Object>() {
            
            @Override
            public Enumerator<Object> enumerator() {
                return new EmptyRowEnumerator();
            }
        };
    }
}
