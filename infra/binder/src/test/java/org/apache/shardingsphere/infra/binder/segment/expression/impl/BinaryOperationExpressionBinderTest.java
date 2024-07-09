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

package org.apache.shardingsphere.infra.binder.segment.expression.impl;

import org.apache.shardingsphere.infra.binder.segment.SegmentType;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementBinderContext;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.BinaryOperationExpression;
import org.apache.shardingsphere.sql.parser.statement.core.segment.dml.expr.simple.LiteralExpressionSegment;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

class BinaryOperationExpressionBinderTest {
    
    @Test
    void assertBinaryOperationExpression() {
        BinaryOperationExpression binaryOperationExpression = new BinaryOperationExpression(0, 0,
                new LiteralExpressionSegment(0, 0, "test"),
                new LiteralExpressionSegment(0, 0, "test"), "=", "test");
        SQLStatementBinderContext statementBinderContext = mock(SQLStatementBinderContext.class);
        BinaryOperationExpression actual = BinaryOperationExpressionBinder.bind(binaryOperationExpression, SegmentType.PROJECTION,
                statementBinderContext, Collections.emptyMap(), Collections.emptyMap());
        assertThat(actual.getLeft().getText(), is("test"));
        assertThat(actual.getRight().getText(), is("test"));
        assertThat(actual.getOperator(), is("="));
        assertThat(actual.getText(), is("test"));
    }
}
