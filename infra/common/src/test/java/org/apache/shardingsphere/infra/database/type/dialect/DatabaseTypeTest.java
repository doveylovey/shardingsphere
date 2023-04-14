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

package org.apache.shardingsphere.infra.database.type.dialect;

import org.apache.shardingsphere.infra.fixture.InfraDatabaseTypeFixture;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseTypeTest {
    
    @Test
    void assertGetSchema() throws SQLException {
        Connection connection = mock(Connection.class);
        when(connection.getSchema()).thenReturn("ds");
        assertThat(new InfraDatabaseTypeFixture().getSchema(connection), is("ds"));
    }
    
    @Test
    void assertFormatTableNamePattern() {
        assertThat(new InfraDatabaseTypeFixture().formatTableNamePattern("tbl"), is("tbl"));
    }
}
