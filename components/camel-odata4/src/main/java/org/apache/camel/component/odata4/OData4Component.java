/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.odata4;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;

/**
 * OData V4 component that performs OData operations via direct HTTP requests.
 */
@Component("odata4")
public class OData4Component extends DefaultComponent {

    @Metadata
    private OData4Configuration configuration;

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        OData4Configuration config = this.configuration != null
                ? this.configuration.copy()
                : new OData4Configuration();

        // Parse remaining: "operation/resourcePath..."
        // Same pattern as olingo4: first segment is operation, rest is resource path
        String operationName;
        String resourcePath = null;

        int slashIndex = remaining.indexOf('/');
        if (slashIndex > 0) {
            operationName = remaining.substring(0, slashIndex);
            resourcePath = remaining.substring(slashIndex + 1);
        } else {
            operationName = remaining;
        }

        OData4Endpoint endpoint = new OData4Endpoint(uri, this, config);
        endpoint.setOperation(operationName);
        if (resourcePath != null) {
            endpoint.setResourcePath(resourcePath);
        }

        // Separate OData system query options ($top, $filter, ...) from endpoint parameters
        Map<String, String> queryParams = new HashMap<>();
        for (Iterator<Map.Entry<String, Object>> it = parameters.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Object> entry = it.next();
            if (entry.getKey().startsWith("$")) {
                queryParams.put(entry.getKey(), entry.getValue().toString());
                it.remove();
            }
        }

        setProperties(endpoint, parameters);
        setProperties(config, parameters);

        if (!queryParams.isEmpty()) {
            Map<String, String> existing = endpoint.getQueryParams();
            if (existing != null) {
                existing.putAll(queryParams);
            } else {
                endpoint.setQueryParams(queryParams);
            }
        }

        return endpoint;
    }

    public OData4Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Component-level shared configuration
     */
    public void setConfiguration(OData4Configuration configuration) {
        this.configuration = configuration;
    }
}
