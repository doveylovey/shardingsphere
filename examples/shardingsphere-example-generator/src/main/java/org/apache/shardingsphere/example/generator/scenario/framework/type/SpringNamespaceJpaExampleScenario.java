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

package org.apache.shardingsphere.example.generator.scenario.framework.type;

import org.apache.shardingsphere.example.generator.scenario.framework.FrameworkExampleScenario;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring namespace JPA example scenario.
 */
public final class SpringNamespaceJpaExampleScenario implements FrameworkExampleScenario {
    
    @Override
    public Map<String, String> getJavaClassTemplateMap() {
        Map<String, String> result = new HashMap<>(4, 1);
        result.put("java/main/SpringNamespaceJpaExampleMain.ftl", "ExampleMain.java");
        result.put("java/repository/jpa/OrderItemRepository.ftl", "repository/OrderItemRepository.java");
        result.put("java/repository/jpa/OrderRepository.ftl", "repository/OrderRepository.java");
        result.put("java/repository/jpa/AddressRepository.ftl", "repository/AddressRepository.java");
        return result;
    }
    
    @Override
    public Map<String, String> getResourceTemplateMap() {
        Map<String, String> result = new HashMap<>(2, 1F);
        result.put("resources/xml/application.ftl", "application.xml");
        result.put("resources/yaml/config.ftl", "config.yaml");
        return result;
    }
    
    @Override
    public Collection<String> getJavaClassPaths() {
        return Collections.singleton("repository");
    }
    
    @Override
    public Collection<String> getResourcePaths() {
        return Collections.emptySet();
    }
    
    @Override
    public String getType() {
        return "spring-namespace-jpa";
    }
}
