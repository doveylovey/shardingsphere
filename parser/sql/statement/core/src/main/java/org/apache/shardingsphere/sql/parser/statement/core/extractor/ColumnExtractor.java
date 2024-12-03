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

package org.apache.shardingsphere.sql.parser.statement.core.extractor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.BetweenExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.BinaryOperationExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.InExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.RowExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.complex.CommonTableExpressionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.AggregationProjectionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.ColumnProjectionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.DatetimeProjectionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.ExpressionProjectionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.IntervalExpressionProjection;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.ProjectionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.item.SubqueryProjectionSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.join.OuterJoinExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.order.GroupBySegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.order.OrderBySegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.order.item.ColumnOrderByItemSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.order.item.ExpressionOrderByItemSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.order.item.OrderByItemSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.predicate.AndPredicate;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.predicate.HavingSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.predicate.WhereSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.CollectionTableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.JoinTableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.SubqueryTableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.segment.generic.table.TableSegment;
import org.apache.shardingsphere.sql.parser.statement.core.statement.dml.SelectStatement;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Column extractor.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ColumnExtractor {
    
    /**
     * Extract column segments from expression segment.
     *
     * @param expression to be extracted expression segment
     * @return column segments
     */
    public static Collection<ColumnSegment> extract(final ExpressionSegment expression) {
        Collection<ColumnSegment> result = new LinkedList<>();
        if (expression instanceof BinaryOperationExpression) {
            if (((BinaryOperationExpression) expression).getLeft() instanceof ColumnSegment) {
                result.add((ColumnSegment) ((BinaryOperationExpression) expression).getLeft());
            }
            if (((BinaryOperationExpression) expression).getRight() instanceof ColumnSegment) {
                result.add((ColumnSegment) ((BinaryOperationExpression) expression).getRight());
            }
            if (((BinaryOperationExpression) expression).getLeft() instanceof OuterJoinExpression) {
                result.add(((OuterJoinExpression) ((BinaryOperationExpression) expression).getLeft()).getColumnName());
            }
            if (((BinaryOperationExpression) expression).getRight() instanceof OuterJoinExpression) {
                result.add(((OuterJoinExpression) ((BinaryOperationExpression) expression).getRight()).getColumnName());
            }
        }
        if (expression instanceof InExpression && ((InExpression) expression).getLeft() instanceof ColumnSegment) {
            result.add((ColumnSegment) ((InExpression) expression).getLeft());
        }
        if (expression instanceof InExpression && ((InExpression) expression).getLeft() instanceof RowExpression) {
            extractColumnInRowExpression((InExpression) expression, result);
        }
        if (expression instanceof BetweenExpression && ((BetweenExpression) expression).getLeft() instanceof ColumnSegment) {
            result.add((ColumnSegment) ((BetweenExpression) expression).getLeft());
        }
        return result;
    }
    
    private static void extractColumnInRowExpression(final InExpression expression, final Collection<ColumnSegment> result) {
        for (ExpressionSegment each : ((RowExpression) expression.getLeft()).getItems()) {
            if (each instanceof ColumnSegment) {
                result.add((ColumnSegment) each);
            }
        }
    }
    
    /**
     * Extract column segments.
     *
     * @param columnSegments column segments
     * @param whereSegments where segments
     */
    public static void extractColumnSegments(final Collection<ColumnSegment> columnSegments, final Collection<WhereSegment> whereSegments) {
        for (WhereSegment each : whereSegments) {
            for (AndPredicate andPredicate : ExpressionExtractor.extractAndPredicates(each.getExpr())) {
                extractColumnSegments(columnSegments, andPredicate);
            }
        }
    }
    
    private static void extractColumnSegments(final Collection<ColumnSegment> columnSegments, final AndPredicate andPredicate) {
        for (ExpressionSegment each : andPredicate.getPredicates()) {
            columnSegments.addAll(extract(each));
        }
    }
    
    /**
     * Extract column segments.
     *
     * @param columnSegments column segments
     * @param statement select statement
     * @param containsSubQuery whether contains subquery
     */
    public static void extractFromSelectStatement(final Collection<ColumnSegment> columnSegments, final SelectStatement statement, final boolean containsSubQuery) {
        extractFromProjections(columnSegments, statement.getProjections().getProjections(), containsSubQuery);
        extractFromSelectStatementWithoutProjection(columnSegments, statement, containsSubQuery);
    }
    
    /**
     * Extract from select statement without projection.
     *
     * @param columnSegments column segments
     * @param statement select statement
     * @param containsSubQuery whether contains subquery
     */
    public static void extractFromSelectStatementWithoutProjection(final Collection<ColumnSegment> columnSegments, final SelectStatement statement, final boolean containsSubQuery) {
        statement.getFrom().ifPresent(optional -> extractFromTable(columnSegments, optional, containsSubQuery));
        statement.getWhere().ifPresent(optional -> extractFromWhere(columnSegments, optional, containsSubQuery));
        statement.getGroupBy().ifPresent(optional -> extractFromGroupBy(columnSegments, optional, containsSubQuery));
        statement.getHaving().ifPresent(optional -> extractFromHaving(columnSegments, optional, containsSubQuery));
        statement.getOrderBy().ifPresent(optional -> extractFromOrderBy(columnSegments, optional, containsSubQuery));
        statement.getCombine().ifPresent(optional -> extractFromSelectStatement(columnSegments, optional.getLeft().getSelect(), containsSubQuery));
        statement.getCombine().ifPresent(optional -> extractFromSelectStatement(columnSegments, optional.getRight().getSelect(), containsSubQuery));
    }
    
    /**
     * Extract column segments.
     *
     * @param columnSegments column segments
     * @param projections projection segments
     * @param containsSubQuery contains subquery
     */
    public static void extractFromProjections(final Collection<ColumnSegment> columnSegments, final Collection<ProjectionSegment> projections, final boolean containsSubQuery) {
        for (ProjectionSegment each : projections) {
            if (each instanceof ColumnProjectionSegment) {
                columnSegments.add(((ColumnProjectionSegment) each).getColumn());
            }
            if (each instanceof AggregationProjectionSegment) {
                for (ExpressionSegment parameter : ((AggregationProjectionSegment) each).getParameters()) {
                    columnSegments.addAll(ExpressionExtractor.extractColumns(parameter, containsSubQuery));
                }
            }
            if (each instanceof DatetimeProjectionSegment) {
                columnSegments.addAll(ExpressionExtractor.extractColumns(((DatetimeProjectionSegment) each).getLeft(), containsSubQuery));
                columnSegments.addAll(ExpressionExtractor.extractColumns(((DatetimeProjectionSegment) each).getRight(), containsSubQuery));
            }
            if (each instanceof ExpressionProjectionSegment) {
                columnSegments.addAll(ExpressionExtractor.extractColumns(((ExpressionProjectionSegment) each).getExpr(), containsSubQuery));
            }
            if (each instanceof IntervalExpressionProjection) {
                columnSegments.addAll(ExpressionExtractor.extractColumns(((IntervalExpressionProjection) each).getLeft(), containsSubQuery));
                columnSegments.addAll(ExpressionExtractor.extractColumns(((IntervalExpressionProjection) each).getRight(), containsSubQuery));
                columnSegments.addAll(ExpressionExtractor.extractColumns(((IntervalExpressionProjection) each).getMinus(), containsSubQuery));
            }
            if (each instanceof SubqueryProjectionSegment && containsSubQuery) {
                extractFromSelectStatement(columnSegments, ((SubqueryProjectionSegment) each).getSubquery().getSelect(), true);
            }
        }
    }
    
    private static void extractFromTable(final Collection<ColumnSegment> columnSegments, final TableSegment tableSegment, final boolean containsSubQuery) {
        if (tableSegment instanceof CollectionTableSegment) {
            columnSegments.addAll(ExpressionExtractor.extractColumns(((CollectionTableSegment) tableSegment).getExpressionSegment(), containsSubQuery));
        }
        if (tableSegment instanceof JoinTableSegment) {
            extractFromTable(columnSegments, ((JoinTableSegment) tableSegment).getLeft(), containsSubQuery);
            extractFromTable(columnSegments, ((JoinTableSegment) tableSegment).getRight(), containsSubQuery);
            columnSegments.addAll(ExpressionExtractor.extractColumns(((JoinTableSegment) tableSegment).getCondition(), containsSubQuery));
            columnSegments.addAll(((JoinTableSegment) tableSegment).getUsing());
            columnSegments.addAll(((JoinTableSegment) tableSegment).getDerivedUsing());
        }
        if (tableSegment instanceof SubqueryTableSegment && containsSubQuery) {
            extractFromSelectStatement(columnSegments, ((SubqueryTableSegment) tableSegment).getSubquery().getSelect(), true);
        }
        if (tableSegment instanceof CommonTableExpressionSegment && containsSubQuery) {
            extractFromSelectStatement(columnSegments, ((CommonTableExpressionSegment) tableSegment).getSubquery().getSelect(), true);
        }
    }
    
    /**
     * Extract column segments.
     *
     * @param columnSegments column segments
     * @param whereSegment where segment
     * @param containsSubQuery contains subquery
     */
    public static void extractFromWhere(final Collection<ColumnSegment> columnSegments, final WhereSegment whereSegment, final boolean containsSubQuery) {
        columnSegments.addAll(ExpressionExtractor.extractColumns(whereSegment.getExpr(), containsSubQuery));
    }
    
    private static void extractFromGroupBy(final Collection<ColumnSegment> columnSegments, final GroupBySegment groupBySegment, final boolean containsSubQuery) {
        for (OrderByItemSegment each : groupBySegment.getGroupByItems()) {
            if (each instanceof ColumnOrderByItemSegment) {
                columnSegments.add(((ColumnOrderByItemSegment) each).getColumn());
            }
            if (each instanceof ExpressionOrderByItemSegment) {
                columnSegments.addAll(ExpressionExtractor.extractColumns(((ExpressionOrderByItemSegment) each).getExpr(), containsSubQuery));
            }
        }
    }
    
    private static void extractFromHaving(final Collection<ColumnSegment> columnSegments, final HavingSegment havingSegment, final boolean containsSubQuery) {
        columnSegments.addAll(ExpressionExtractor.extractColumns(havingSegment.getExpr(), containsSubQuery));
    }
    
    private static void extractFromOrderBy(final Collection<ColumnSegment> columnSegments, final OrderBySegment orderBySegment, final boolean containsSubQuery) {
        for (OrderByItemSegment each : orderBySegment.getOrderByItems()) {
            if (each instanceof ColumnOrderByItemSegment) {
                columnSegments.add(((ColumnOrderByItemSegment) each).getColumn());
            }
            if (each instanceof ExpressionOrderByItemSegment) {
                columnSegments.addAll(ExpressionExtractor.extractColumns(((ExpressionOrderByItemSegment) each).getExpr(), containsSubQuery));
            }
        }
    }
}
