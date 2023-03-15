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

package org.apache.shardingsphere.data.pipeline.core.check.consistency;

import org.apache.shardingsphere.data.pipeline.spi.check.consistency.DataConsistencyCalculateAlgorithm;
import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.infra.util.spi.type.typed.TypedSPILoader;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class DataConsistencyCalculateAlgorithmChooserTest {
    
    @Test
    public void assertChooseOnDifferentDatabaseTypes() {
        DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "Oracle");
        DatabaseType peerDatabaseType = TypedSPILoader.getService(DatabaseType.class, "PostgreSQL");
        DataConsistencyCalculateAlgorithm actual = DataConsistencyCalculateAlgorithmChooser.choose(databaseType, peerDatabaseType);
        assertNotNull(actual);
        assertThat(actual.getType(), is("DATA_MATCH"));
    }
    
    @Test
    public void assertChooseOnMySQL() {
        DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "MySQL");
        DatabaseType peerDatabaseType = TypedSPILoader.getService(DatabaseType.class, "MySQL");
        DataConsistencyCalculateAlgorithm actual = DataConsistencyCalculateAlgorithmChooser.choose(databaseType, peerDatabaseType);
        assertNotNull(actual);
        assertThat(actual.getType(), is("CRC32_MATCH"));
    }
    
    @Test
    public void assertChooseOnPostgreSQL() {
        DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "PostgreSQL");
        DatabaseType peerDatabaseType = TypedSPILoader.getService(DatabaseType.class, "PostgreSQL");
        DataConsistencyCalculateAlgorithm actual = DataConsistencyCalculateAlgorithmChooser.choose(databaseType, peerDatabaseType);
        assertNotNull(actual);
        assertThat(actual.getType(), is("DATA_MATCH"));
    }
}
