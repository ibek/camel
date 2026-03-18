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

import java.util.Map;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.EndpointServiceLocation;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

/**
 * Perform OData V4 requests using direct HTTP calls. Replaces the deprecated camel-olingo4 component.
 */
@UriEndpoint(firstVersion = "4.19.0", scheme = "odata4", title = "OData V4",
             syntax = "odata4:operation/resourcePath", producerOnly = true,
             category = { Category.CLOUD }, headersClass = OData4Constants.class)
public class OData4Endpoint extends DefaultEndpoint implements EndpointServiceLocation {

    @UriPath(description = "OData operation: read, create, update, delete, patch, merge, action",
             enums = "read,create,update,delete,patch,merge,action")
    @Metadata(required = true)
    private String operation;

    @UriPath(description = "OData resource path, e.g. People, Airports, People/$count")
    private String resourcePath;

    @UriParam(description = "Entity key predicate without parentheses, e.g. 'russellwhyte'")
    private String keyPredicate;

    @UriParam(description = "OData query parameters as a map, e.g. {$top=5, $orderby=FirstName asc}")
    private Map<String, String> queryParams;

    @UriParam
    private OData4Configuration configuration;

    public OData4Endpoint(String endpointUri, OData4Component component, OData4Configuration configuration) {
        super(endpointUri, component);
        this.configuration = configuration;
    }

    @Override
    public Producer createProducer() {
        return new OData4Producer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        throw new UnsupportedOperationException("OData4 component does not support consumers");
    }

    @Override
    public String getServiceUrl() {
        return configuration.getServiceUri();
    }

    @Override
    public String getServiceProtocol() {
        return "odata";
    }

    @Override
    public OData4Component getComponent() {
        return (OData4Component) super.getComponent();
    }

    public OData4Configuration getConfiguration() {
        return configuration;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getKeyPredicate() {
        return keyPredicate;
    }

    public void setKeyPredicate(String keyPredicate) {
        this.keyPredicate = keyPredicate;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }
}
