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

package org.apache.shardingsphere.test.it.sql.parser.internal.asserts.statement.dal.impl;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.sql.parser.sql.common.statement.dal.ShowStatement;
import org.apache.shardingsphere.test.it.sql.parser.internal.asserts.SQLCaseAssertContext;
import org.apache.shardingsphere.test.it.sql.parser.internal.cases.parser.jaxb.statement.dal.ShowStatementTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Show statement assert.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShowStatementAssert {
    
    /**
     * Assert show statement is correct with expected parser result.
     * 
     * @param assertContext assert context
     * @param actual actual show statement
     * @param expected expected show statement test case
     */
    public static void assertIs(final SQLCaseAssertContext assertContext, final ShowStatement actual, final ShowStatementTestCase expected) {
        if (null == expected.getName()) {
            assertFalse(actual.getName().isPresent(), assertContext.getText("Name should not exist."));
        } else {
            assertTrue(actual.getName().isPresent(), assertContext.getText("Name should exist."));
            assertThat(assertContext.getText("Name assertion error: "), actual.getName().get(), is(expected.getName()));
        }
    }
}
