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

import org.apache.camel.spi.Configurer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;
import org.apache.camel.support.jsse.SSLContextParameters;

@UriParams
@Configurer
public class OData4Configuration {

    private static final String DEFAULT_CONTENT_TYPE = "application/json;charset=utf-8";
    private static final int DEFAULT_TIMEOUT = 30_000;

    @UriParam
    @Metadata(required = true)
    private String serviceUri;
    @UriParam(defaultValue = DEFAULT_CONTENT_TYPE)
    private String contentType = DEFAULT_CONTENT_TYPE;
    @UriParam
    private Map<String, String> httpHeaders;
    @UriParam(defaultValue = "30000", description = "HTTP connection timeout in milliseconds")
    private int connectTimeout = DEFAULT_TIMEOUT;
    @UriParam(defaultValue = "30000", description = "HTTP request timeout in milliseconds")
    private int socketTimeout = DEFAULT_TIMEOUT;
    @UriParam(label = "security")
    private SSLContextParameters sslContextParameters;

    /**
     * Target OData service base URI, e.g. http://services.odata.org/TripPinRESTierService
     */
    public String getServiceUri() {
        return serviceUri;
    }

    public void setServiceUri(String serviceUri) {
        this.serviceUri = serviceUri;
    }

    /**
     * Content-Type header value, defaults to application/json;charset=utf-8
     */
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Custom HTTP headers to inject into every request, e.g. Authorization tokens.
     */
    public Map<String, String> getHttpHeaders() {
        return httpHeaders;
    }

    public void setHttpHeaders(Map<String, String> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * To configure security using SSLContextParameters
     */
    public SSLContextParameters getSslContextParameters() {
        return sslContextParameters;
    }

    public void setSslContextParameters(SSLContextParameters sslContextParameters) {
        this.sslContextParameters = sslContextParameters;
    }

    public OData4Configuration copy() {
        OData4Configuration copy = new OData4Configuration();
        copy.serviceUri = this.serviceUri;
        copy.contentType = this.contentType;
        copy.httpHeaders = this.httpHeaders;
        copy.connectTimeout = this.connectTimeout;
        copy.socketTimeout = this.socketTimeout;
        copy.sslContextParameters = this.sslContextParameters;
        return copy;
    }
}
