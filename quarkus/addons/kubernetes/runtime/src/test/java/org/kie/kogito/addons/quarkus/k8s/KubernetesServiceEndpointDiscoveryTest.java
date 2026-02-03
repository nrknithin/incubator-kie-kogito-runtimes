/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.addons.quarkus.k8s;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.kie.kogito.addons.k8s.Endpoint;
import org.kie.kogito.addons.k8s.EndpointDiscovery;
import org.kie.kogito.addons.quarkus.k8s.test.utils.KubernetesMockServerTestResource;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
public class KubernetesServiceEndpointDiscoveryTest {

    @Inject
    KubernetesClient client;

    @Named("default")
    @Inject
    EndpointDiscovery endpointDiscovery;

    /**
     * Helper method to create a Kubernetes Service with mock server expectations.
     *
     * FABRIC8 7.X COMPATIBILITY: This method has been updated to work with Fabric8 Kubernetes Client 7.x
     * by configuring explicit mock server expectations for all HTTP operations.
     *
     * @param name the service name
     * @param labels the service labels (used for label selector queries)
     * @param ports the service ports
     */
    private void createServiceIfNotExist(final String name, Map<String, String> labels, Integer... ports) {
        final List<ServicePort> sPorts = new ArrayList<>();
        for (Integer portNumber : ports) {
            final ServicePort port = new ServicePort();
            port.setPort(portNumber);
            sPorts.add(port);
        }

        final Service svc = new ServiceBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace("test")
                .withLabels(labels)
                .endMetadata()
                .withSpec(new ServiceSpec())
                .build();

        svc.getSpec().setClusterIP("127.0.0.1");
        svc.getSpec().setPorts(sPorts);

        // FABRIC8 7.X COMPATIBILITY: Access mock server via static method instead of CDI injection
        // Reason: QuarkusTestResource doesn't make managed objects available as CDI beans.
        // Previously used: @Inject KubernetesMockServer mockServer (which caused "Unsatisfied dependency" errors)
        // Now using: KubernetesMockServerTestResource.getServer() static method
        KubernetesMockServer mockServer = KubernetesMockServerTestResource.getServer();

        // FABRIC8 7.X COMPATIBILITY: Configure POST expectation for service creation
        // Reason: Fabric8 7.x requires explicit mock expectations for all HTTP operations.
        mockServer.expect().post()
                .withPath("/api/v1/namespaces/test/services")
                .andReturn(201, svc)
                .once();

        // FABRIC8 7.X COMPATIBILITY: Configure GET expectation for service retrieval by name
        // Using always() because the service may be queried multiple times during endpoint discovery.
        mockServer.expect().get()
                .withPath("/api/v1/namespaces/test/services/" + name)
                .andReturn(200, svc)
                .always();

        // FABRIC8 7.X COMPATIBILITY: Configure GET expectation for label selector queries
        // Only needed when labels are provided (for testGetURLOnRandomPort test)
        if (!labels.isEmpty()) {
            // FABRIC8 7.X COMPATIBILITY: Build URL-encoded label selector query parameter
            // Reason: Fabric8 7.x performs strict path matching including query parameters.
            // Label selectors must be URL-encoded:
            // - "=" becomes "%3D" (e.g., "app=test1" becomes "app%3Dtest1")
            // - "," becomes "%2C" (for multiple labels, e.g., "app=test1,env=dev" becomes "app%3Dtest1%2Cenv%3Ddev")
            // Previously, the mock server was more lenient and would match paths without exact query parameters.
            String labelSelector = labels.entrySet().stream()
                    .map(e -> e.getKey() + "%3D" + e.getValue())
                    .reduce((a, b) -> a + "%2C" + b)
                    .orElse("");
            mockServer.expect().get()
                    .withPath("/api/v1/namespaces/test/services?labelSelector=" + labelSelector)
                    .andReturn(200, new io.fabric8.kubernetes.api.model.ServiceListBuilder().addToItems(svc).build())
                    .always();
        }

        // Create the service using the Kubernetes client (which will hit the mock server)
        client.resource(svc).inNamespace("test").create();
    }

    @Test
    public void testGetURLOnStandardPort() {
        createServiceIfNotExist("svc1", Collections.emptyMap(), 80, 8776);
        final Optional<Endpoint> endpoint = endpointDiscovery.findEndpoint("test", "svc1");
        assertTrue(endpoint.isPresent());
        assertFalse(endpoint.get().getUrl().isEmpty());
        try {
            new URL(endpoint.get().getUrl());
        } catch (MalformedURLException e) {
            fail("The generated URL " + endpoint.get().getUrl() + " is invalid");
        }
    }

    @Test
    public void testGetURLOnRandomPort() {
        createServiceIfNotExist("svc2", Collections.singletonMap("app", "test1"), 8778);
        final List<Endpoint> endpoints = endpointDiscovery.findEndpoint("test", Collections.singletonMap("app", "test1"));
        assertFalse(endpoints.isEmpty());
        assertFalse(endpoints.get(0).getUrl().isEmpty());
        try {
            new URL(endpoints.get(0).getUrl());
        } catch (MalformedURLException e) {
            fail("The generated URL " + endpoints.get(0).getUrl() + " is invalid");
        }
    }
}
