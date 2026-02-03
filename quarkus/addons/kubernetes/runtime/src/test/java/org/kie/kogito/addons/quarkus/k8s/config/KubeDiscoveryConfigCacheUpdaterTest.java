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
package org.kie.kogito.addons.quarkus.k8s.config;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.kogito.addons.k8s.resource.catalog.KubernetesServiceCatalog;
import org.kie.kogito.addons.quarkus.k8s.test.utils.KubernetesMockServerTestResource;

import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
class KubeDiscoveryConfigCacheUpdaterTest {

    private static final String remoteServiceUrl = "http://serverless-workflow-greeting-quarkus.test.10.99.154.147.sslip.io";

    @Inject
    KubernetesServiceCatalog kubernetesServiceCatalog;

    @Inject
    KubernetesClient client;

    KubeDiscoveryConfigCacheUpdater kubeDiscoveryConfigCacheUpdater;

    @BeforeEach
    void beforeEach() {
        // FABRIC8 7.X COMPATIBILITY: Load Knative service from YAML file
        // This approach is preferred over using the KubeTestUtils.createKnativeServiceIfNotExists() helper
        // because it gives us more control over mock server expectations.
        InputStream is = getClass().getClassLoader().getResourceAsStream("knative/quarkus-greeting.yaml");
        Service service = Serialization.unmarshal(is, Service.class);

        // Set the URL in the service status (simulating what Knative controller would do)
        if (service.getStatus() == null) {
            service.setStatus(new io.fabric8.knative.serving.v1.ServiceStatus());
        }
        service.getStatus().setUrl(remoteServiceUrl);

        // FABRIC8 7.X COMPATIBILITY: Configure mock server expectations explicitly
        // Reason: The previous approach using KubeTestUtils.createKnativeServiceIfNotExists() failed because:
        // 1. It tried to create services without configuring mock expectations first
        // 2. Fabric8 7.x requires explicit mock expectations for ALL HTTP operations
        // 3. The mock server returned 404 Not Found for POST requests without expectations
        //
        // Solution: Configure expectations BEFORE attempting to create the service

        // FABRIC8 7.X COMPATIBILITY: Access mock server via static method
        // Reason: QuarkusTestResource doesn't make managed objects available as CDI beans
        KubernetesMockServerTestResource.getServer().expect()
                .post()
                .withPath("/apis/serving.knative.dev/v1/namespaces/test/services")
                .andReturn(HttpURLConnection.HTTP_CREATED, service)
                .once();

        // FABRIC8 7.X COMPATIBILITY: Configure GET expectation with always() for repeated queries
        // Reason: The service discovery mechanism may query the service multiple times
        KubernetesMockServerTestResource.getServer().expect()
                .get()
                .withPath("/apis/serving.knative.dev/v1/namespaces/test/services/serverless-workflow-greeting-quarkus")
                .andReturn(HttpURLConnection.HTTP_OK, service)
                .always();

        // Create the Knative service using the Kubernetes client (which will hit the mock server)
        // This simulates the service being deployed in a real Kubernetes/Knative cluster
        client.adapt(io.fabric8.knative.client.KnativeClient.class)
                .services()
                .inNamespace("test")
                .resource(service)
                .create();

        // Initialize the cache updater with the service catalog
        kubeDiscoveryConfigCacheUpdater = new KubeDiscoveryConfigCacheUpdater(kubernetesServiceCatalog);
    }

    @Test
    void knativeService() {
        assertThat(kubeDiscoveryConfigCacheUpdater.update("knative:test/serverless-workflow-greeting-quarkus"))
                .hasValue(URI.create(remoteServiceUrl));
    }

    @Test
    void knativeResource() {
        assertThat(kubeDiscoveryConfigCacheUpdater.update("knative:services.v1.serving.knative.dev/test/serverless-workflow-greeting-quarkus"))
                .hasValue(URI.create(remoteServiceUrl));
    }
}
