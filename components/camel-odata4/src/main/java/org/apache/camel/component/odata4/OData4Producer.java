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

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.support.DefaultProducer;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ssl.TLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OData4Producer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(OData4Producer.class);

    private CloseableHttpClient httpClient;

    public OData4Producer(OData4Endpoint endpoint) {
        super(endpoint);
    }

    @Override
    public OData4Endpoint getEndpoint() {
        return (OData4Endpoint) super.getEndpoint();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        httpClient = createHttpClient();
    }

    @Override
    protected void doStop() throws Exception {
        IOHelper.close(httpClient);
        httpClient = null;
        super.doStop();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        OData4Endpoint endpoint = getEndpoint();
        OData4Configuration config = endpoint.getConfiguration();
        Message in = exchange.getMessage();

        String operation = resolveHeader(in, OData4Constants.OPERATION, endpoint.getOperation());
        String resourcePath = resolveHeader(in, OData4Constants.RESOURCE_PATH, endpoint.getResourcePath());
        String keyPredicate = resolveHeader(in, OData4Constants.KEY_PREDICATE, endpoint.getKeyPredicate());

        @SuppressWarnings("unchecked")
        Map<String, String> headerQueryParams = in.getHeader(OData4Constants.QUERY_PARAMS, Map.class);
        Map<String, String> effectiveQueryParams = mergeQueryParams(endpoint.getQueryParams(), headerQueryParams);

        @SuppressWarnings("unchecked")
        Map<String, String> endpointHttpHeaders = in.getHeader(OData4Constants.ENDPOINT_HTTP_HEADERS, Map.class);

        OData4Operation op = OData4Operation.fromString(operation);
        URI requestUri = buildUri(config.getServiceUri(), resourcePath, keyPredicate, effectiveQueryParams);

        HttpUriRequestBase request = createRequest(op, requestUri);

        // set body for write operations
        if (op != OData4Operation.READ && op != OData4Operation.DELETE) {
            String body = in.getBody(String.class);
            if (body != null) {
                request.setEntity(new StringEntity(body, ContentType.parse(config.getContentType())));
            }
        }

        // OData standard headers
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-Type", config.getContentType());
        request.setHeader("OData-Version", "4.0");

        if (op == OData4Operation.MERGE) {
            request.setHeader("X-HTTP-Method", "MERGE");
        }

        // component-level static headers
        if (config.getHttpHeaders() != null) {
            config.getHttpHeaders().forEach(request::setHeader);
        }

        // per-request dynamic headers
        if (endpointHttpHeaders != null) {
            endpointHttpHeaders.forEach(request::setHeader);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("OData4 {} {} → {}", op, operation, requestUri);
        }

        httpClient.execute(request, (ClassicHttpResponse response) -> {
            Message out = exchange.getMessage();

            String responseBody = response.getEntity() != null
                    ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
                    : null;
            out.setBody(responseBody);
            out.setHeader(OData4Constants.RESPONSE_CODE, response.getCode());

            Map<String, String> responseHeaders = new HashMap<>();
            Iterator<Header> it = response.headerIterator();
            while (it.hasNext()) {
                Header h = it.next();
                responseHeaders.put(h.getName(), h.getValue());
            }
            out.setHeader(OData4Constants.RESPONSE_HTTP_HEADERS, responseHeaders);

            return null;
        });
    }

    private HttpUriRequestBase createRequest(OData4Operation op, URI uri) {
        return switch (op) {
            case READ -> new HttpGet(uri);
            case CREATE, ACTION -> new HttpPost(uri);
            case UPDATE -> new HttpPut(uri);
            case DELETE -> new HttpDelete(uri);
            case PATCH, MERGE -> new HttpPatch(uri);
        };
    }

    static URI buildUri(
            String serviceUri, String resourcePath, String keyPredicate, Map<String, String> queryParams) {

        StringBuilder sb = new StringBuilder(serviceUri);

        if (ObjectHelper.isNotEmpty(resourcePath)) {
            if (!serviceUri.endsWith("/")) {
                sb.append('/');
            }
            sb.append(resourcePath);
        }
        if (ObjectHelper.isNotEmpty(keyPredicate)) {
            sb.append('(').append(keyPredicate).append(')');
        }
        if (queryParams != null && !queryParams.isEmpty()) {
            sb.append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (!first) {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                sb.append('=');
                sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                first = false;
            }
        }
        return URI.create(sb.toString());
    }

    private Map<String, String> mergeQueryParams(Map<String, String> base, Map<String, String> override) {
        if (base == null && override == null) {
            return null;
        }
        Map<String, String> merged = new HashMap<>();
        if (base != null) {
            merged.putAll(base);
        }
        if (override != null) {
            merged.putAll(override);
        }
        return merged;
    }

    private String resolveHeader(Message message, String headerName, String defaultValue) {
        String value = message.getHeader(headerName, String.class);
        return value != null ? value : defaultValue;
    }

    private CloseableHttpClient createHttpClient() {
        OData4Configuration config = getEndpoint().getConfiguration();

        PoolingHttpClientConnectionManagerBuilder connManagerBuilder
                = PoolingHttpClientConnectionManagerBuilder.create();

        connManagerBuilder.setDefaultConnectionConfig(
                ConnectionConfig.custom()
                        .setConnectTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                        .setSocketTimeout(config.getSocketTimeout(), TimeUnit.MILLISECONDS)
                        .build());

        SSLContextParameters sslParams = config.getSslContextParameters();
        if (sslParams != null) {
            try {
                connManagerBuilder.setSSLSocketFactory(
                        SSLConnectionSocketFactoryBuilder.create()
                                .setSslContext(sslParams.createSSLContext(getEndpoint().getCamelContext()))
                                .setTlsVersions(TLS.V_1_2, TLS.V_1_3)
                                .build());
            } catch (GeneralSecurityException | IOException e) {
                throw RuntimeCamelException.wrapRuntimeCamelException(e);
            }
        }

        HttpClientConnectionManager connManager = connManagerBuilder.build();

        return HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setResponseTimeout(config.getSocketTimeout(), TimeUnit.MILLISECONDS)
                                .build())
                .build();
    }
}
