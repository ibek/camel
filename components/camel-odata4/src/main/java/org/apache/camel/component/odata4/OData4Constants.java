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

import org.apache.camel.spi.Metadata;

public interface OData4Constants {

    String PROPERTY_PREFIX = "CamelOData4.";

    @Metadata(label = "producer", description = "Override the OData operation at runtime", javaType = "String")
    String OPERATION = PROPERTY_PREFIX + "operation";
    @Metadata(label = "producer", description = "Override the OData resource path at runtime", javaType = "String")
    String RESOURCE_PATH = PROPERTY_PREFIX + "resourcePath";
    @Metadata(label = "producer", description = "Entity key predicate without parentheses, e.g. 'russellwhyte'",
              javaType = "String")
    String KEY_PREDICATE = PROPERTY_PREFIX + "keyPredicate";
    @Metadata(label = "producer", description = "Override OData query parameters at runtime",
              javaType = "java.util.Map<String, String>")
    String QUERY_PARAMS = PROPERTY_PREFIX + "queryParams";
    @Metadata(label = "producer", description = "Per-request HTTP headers", javaType = "java.util.Map<String, String>")
    String ENDPOINT_HTTP_HEADERS = PROPERTY_PREFIX + "endpointHttpHeaders";
    @Metadata(label = "producer", description = "HTTP response headers from the OData service",
              javaType = "java.util.Map<String, String>")
    String RESPONSE_HTTP_HEADERS = PROPERTY_PREFIX + "responseHttpHeaders";
    @Metadata(label = "producer", description = "HTTP response status code", javaType = "Integer")
    String RESPONSE_CODE = PROPERTY_PREFIX + "responseCode";
}
