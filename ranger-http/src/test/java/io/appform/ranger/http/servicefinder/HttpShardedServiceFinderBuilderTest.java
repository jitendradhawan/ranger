/*
 * Copyright 2024 Authors, Flipkart Internet Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.appform.ranger.http.servicefinder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.appform.ranger.core.healthcheck.HealthcheckStatus;
import io.appform.ranger.core.model.ServiceNode;
import io.appform.ranger.core.utils.RangerTestUtils;
import io.appform.ranger.http.config.HttpClientConfig;
import io.appform.ranger.http.model.ServiceNodesResponse;
import lombok.Data;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 *
 */
@WireMockTest
class HttpShardedServiceFinderBuilderTest {

    @Data
    private static final class NodeData {
        private final String name;

        public NodeData(@JsonProperty("name") String name) {
            this.name = name;
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void testFinder(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        val testNode = new NodeData("testNode");
        val node = ServiceNode.<NodeData>builder().host("127.0.0.1").port(80).nodeData(testNode).build();
        node.setHealthcheckStatus(HealthcheckStatus.healthy);
        node.setLastUpdatedTimeStamp(System.currentTimeMillis());
        val payload = MAPPER.writeValueAsBytes(
                ServiceNodesResponse.<NodeData>builder()
                        .data(Collections.singletonList(node))
                        .build());
        stubFor(get(urlPathEqualTo("/ranger/nodes/v1/testns/test"))
                               .willReturn(aResponse()
                                                   .withBody(payload)
                                                   .withStatus(200)));
        val clientConfig = HttpClientConfig.builder()
                .host("127.0.0.1")
                .port(wireMockRuntimeInfo.getHttpPort())
                .connectionTimeoutMs(30_000)
                .operationTimeoutMs(30_000)
                .build();

        val finder = new HttpShardedServiceFinderBuilder<NodeData>()
                .withClientConfig(clientConfig)
                .withNamespace("testns")
                .withServiceName("test")
                .withObjectMapper(MAPPER)
                .withDeserializer(data -> {
                    try {
                        return MAPPER.readValue(data, new TypeReference<ServiceNodesResponse<NodeData>>() {});
                    }
                    catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .withShardSelector((criteria, registry) -> registry.nodeList())
                .withNodeRefreshIntervalMs(1000)
                .build();
        finder.start();
        RangerTestUtils.sleepUntilFinderStarts(finder);
        Assertions.assertNotNull(finder.get(nodeData -> true).orElse(null));
    }

}