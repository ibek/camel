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

import java.net.URI;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OData4ProducerTest extends CamelTestSupport {

    private static WireMockServer wireMock;
    private static String serviceUri;

    private static final String PEOPLE_RESPONSE = """
            {
              "@odata.context": "$metadata#People",
              "value": [
                {"UserName": "russellwhyte", "FirstName": "Russell", "LastName": "Whyte"},
                {"UserName": "scottketchum", "FirstName": "Scott", "LastName": "Ketchum"}
              ]
            }""";

    private static final String PERSON_RESPONSE = """
            {
              "@odata.context": "$metadata#People/$entity",
              "UserName": "russellwhyte",
              "FirstName": "Russell",
              "LastName": "Whyte"
            }""";

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(0);
        wireMock.start();
        serviceUri = "http://localhost:" + wireMock.port() + "/odata";
        WireMock.configureFor("localhost", wireMock.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:read")
                        .toD("odata4:read/People?serviceUri=" + serviceUri);

                from("direct:readWithQuery")
                        .toD("odata4:read/People?serviceUri=" + serviceUri + "&$top=5&$orderby=FirstName%20asc");

                from("direct:readWithKeyPredicate")
                        .toD("odata4:read/Airports?serviceUri=" + serviceUri);

                from("direct:create")
                        .toD("odata4:create/People?serviceUri=" + serviceUri);

                from("direct:update")
                        .toD("odata4:update/People?serviceUri=" + serviceUri + "&keyPredicate='lewisblack'");

                from("direct:patch")
                        .toD("odata4:patch/People?serviceUri=" + serviceUri + "&keyPredicate='lewisblack'");

                from("direct:delete")
                        .toD("odata4:delete/People?serviceUri=" + serviceUri + "&keyPredicate='lewisblack'");

                from("direct:action")
                        .toD("odata4:action/ResetDataSource?serviceUri=" + serviceUri);

                from("direct:merge")
                        .toD("odata4:merge/People?serviceUri=" + serviceUri + "&keyPredicate='lewisblack'");
            }
        };
    }

    @Test
    void testReadEntitySet() {
        wireMock.stubFor(get(urlPathEqualTo("/odata/People"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PEOPLE_RESPONSE)));

        Exchange result = template.request("direct:read", exchange -> {
        });

        assertNotNull(result.getMessage().getBody(String.class));
        assertTrue(result.getMessage().getBody(String.class).contains("russellwhyte"));
        assertEquals(200, result.getMessage().getHeader(OData4Constants.RESPONSE_CODE, Integer.class));

        wireMock.verify(getRequestedFor(urlPathEqualTo("/odata/People"))
                .withHeader("OData-Version", equalTo("4.0"))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    void testReadWithQueryParams() {
        wireMock.stubFor(get(urlPathEqualTo("/odata/People"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PEOPLE_RESPONSE)));

        Exchange result = template.request("direct:readWithQuery", exchange -> {
        });

        assertNotNull(result.getMessage().getBody(String.class));
        wireMock.verify(getRequestedFor(urlPathEqualTo("/odata/People"))
                .withQueryParam("$top", equalTo("5"))
                .withQueryParam("$orderby", equalTo("FirstName asc")));
    }

    @Test
    void testReadWithKeyPredicateHeader() {
        wireMock.stubFor(get(urlPathEqualTo("/odata/Airports('KSFO')"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"IcaoCode\":\"KSFO\",\"Name\":\"San Francisco\"}")));

        Exchange result = template.request("direct:readWithKeyPredicate", exchange -> {
            exchange.getMessage().setHeader(OData4Constants.KEY_PREDICATE, "'KSFO'");
        });

        assertTrue(result.getMessage().getBody(String.class).contains("KSFO"));
    }

    @Test
    void testCreateEntity() {
        wireMock.stubFor(post(urlPathEqualTo("/odata/People"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PERSON_RESPONSE)));

        String newPerson = "{\"UserName\":\"newuser\",\"FirstName\":\"New\",\"LastName\":\"User\"}";

        Exchange result = template.request("direct:create", exchange -> {
            exchange.getMessage().setBody(newPerson);
        });

        assertEquals(201, result.getMessage().getHeader(OData4Constants.RESPONSE_CODE, Integer.class));
        wireMock.verify(postRequestedFor(urlPathEqualTo("/odata/People"))
                .withHeader("OData-Version", equalTo("4.0")));
    }

    @Test
    void testUpdateEntity() {
        wireMock.stubFor(put(urlPathEqualTo("/odata/People('lewisblack')"))
                .willReturn(aResponse().withStatus(204)));

        String updated = "{\"FirstName\":\"Lewis\",\"LastName\":\"Black\"}";

        Exchange result = template.request("direct:update", exchange -> {
            exchange.getMessage().setBody(updated);
        });

        assertEquals(204, result.getMessage().getHeader(OData4Constants.RESPONSE_CODE, Integer.class));
        wireMock.verify(putRequestedFor(urlPathEqualTo("/odata/People('lewisblack')")));
    }

    @Test
    void testPatchEntity() {
        wireMock.stubFor(patch(urlPathEqualTo("/odata/People('lewisblack')"))
                .willReturn(aResponse().withStatus(204)));

        String patchBody = "{\"FirstName\":\"Lewis-Updated\"}";

        Exchange result = template.request("direct:patch", exchange -> {
            exchange.getMessage().setBody(patchBody);
        });

        assertEquals(204, result.getMessage().getHeader(OData4Constants.RESPONSE_CODE, Integer.class));
        wireMock.verify(patchRequestedFor(urlPathEqualTo("/odata/People('lewisblack')")));
    }

    @Test
    void testDeleteEntity() {
        wireMock.stubFor(delete(urlPathEqualTo("/odata/People('lewisblack')"))
                .willReturn(aResponse().withStatus(204)));

        Exchange result = template.request("direct:delete", exchange -> {
        });

        assertEquals(204, result.getMessage().getHeader(OData4Constants.RESPONSE_CODE, Integer.class));
        wireMock.verify(deleteRequestedFor(urlPathEqualTo("/odata/People('lewisblack')")));
    }

    @Test
    void testAction() {
        wireMock.stubFor(post(urlPathEqualTo("/odata/ResetDataSource"))
                .willReturn(aResponse().withStatus(204)));

        Exchange result = template.request("direct:action", exchange -> {
        });

        assertEquals(204, result.getMessage().getHeader(OData4Constants.RESPONSE_CODE, Integer.class));
        wireMock.verify(postRequestedFor(urlPathEqualTo("/odata/ResetDataSource")));
    }

    @Test
    void testMerge() {
        wireMock.stubFor(patch(urlPathEqualTo("/odata/People('lewisblack')"))
                .willReturn(aResponse().withStatus(204)));

        String mergeBody = "{\"FirstName\":\"Lewis-Merged\"}";

        Exchange result = template.request("direct:merge", exchange -> {
            exchange.getMessage().setBody(mergeBody);
        });

        assertEquals(204, result.getMessage().getHeader(OData4Constants.RESPONSE_CODE, Integer.class));
        wireMock.verify(patchRequestedFor(urlPathEqualTo("/odata/People('lewisblack')"))
                .withHeader("X-HTTP-Method", equalTo("MERGE")));
    }

    @Test
    void testDynamicOperationOverride() {
        wireMock.stubFor(post(urlPathEqualTo("/odata/People"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody(PERSON_RESPONSE)));

        Exchange result = template.request("direct:read", exchange -> {
            exchange.getMessage().setHeader(OData4Constants.OPERATION, "create");
            exchange.getMessage().setBody("{\"UserName\":\"dynamic\"}");
        });

        assertEquals(201, result.getMessage().getHeader(OData4Constants.RESPONSE_CODE, Integer.class));
        wireMock.verify(postRequestedFor(urlPathEqualTo("/odata/People")));
    }

    @Test
    void testDynamicResourcePathOverride() {
        wireMock.stubFor(get(urlPathEqualTo("/odata/Airports"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"value\":[]}")));

        Exchange result = template.request("direct:read", exchange -> {
            exchange.getMessage().setHeader(OData4Constants.RESOURCE_PATH, "Airports");
        });

        assertEquals(200, result.getMessage().getHeader(OData4Constants.RESPONSE_CODE, Integer.class));
        wireMock.verify(getRequestedFor(urlPathEqualTo("/odata/Airports")));
    }

    @Test
    void testPerRequestHttpHeaders() {
        wireMock.stubFor(get(urlPathEqualTo("/odata/People"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(PEOPLE_RESPONSE)));

        Exchange result = template.request("direct:read", exchange -> {
            exchange.getMessage().setHeader(OData4Constants.ENDPOINT_HTTP_HEADERS,
                    Map.of("Authorization", "Bearer test-token"));
        });

        wireMock.verify(getRequestedFor(urlPathEqualTo("/odata/People"))
                .withHeader("Authorization", equalTo("Bearer test-token")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testResponseHeadersAreCaptured() {
        wireMock.stubFor(get(urlPathEqualTo("/odata/People"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("OData-Version", "4.0")
                        .withBody(PEOPLE_RESPONSE)));

        Exchange result = template.request("direct:read", exchange -> {
        });

        Map<String, String> responseHeaders
                = result.getMessage().getHeader(OData4Constants.RESPONSE_HTTP_HEADERS, Map.class);
        assertNotNull(responseHeaders);
        assertEquals("4.0", responseHeaders.get("OData-Version"));
    }

    @Test
    void testBuildUri() {
        URI uri = OData4Producer.buildUri(
                "http://host/svc", "People", "'key'",
                Map.of("$top", "5", "$orderby", "Name"));
        String s = uri.toString();
        assertTrue(s.startsWith("http://host/svc/People('key')?"));
        assertTrue(s.contains("%24top=5"));
        assertTrue(s.contains("%24orderby=Name"));
    }

    @Test
    void testBuildUriNoQueryNoKey() {
        URI uri = OData4Producer.buildUri("http://host/svc", "People", null, null);
        assertEquals("http://host/svc/People", uri.toString());
    }

    @Test
    void testBuildUriEmptyResource() {
        URI uri = OData4Producer.buildUri("http://host/svc", null, null, null);
        assertEquals("http://host/svc", uri.toString());
    }
}
