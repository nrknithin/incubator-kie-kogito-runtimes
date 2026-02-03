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
package org.kie.kogito.addons.quarkus.kubernetes;

import java.io.InputStream;
import java.util.Map;

import org.kie.kogito.addons.quarkus.k8s.test.utils.KubernetesMockServerTestResource;

import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.client.utils.Serialization;

/**
 * Custom test resource for ConfigValueExpanderIT that sets up mock server expectations
 * BEFORE the Quarkus application starts.
 * 
 * FABRIC8 7.X COMPATIBILITY:
 * - This resource extends KubernetesMockServerTestResource to reuse the mock server
 * - Mock expectations are configured in start() method, which runs before application startup
 * - This ensures the Knative service is available when the application tries to resolve it during startup
 */
public class ConfigValueExpanderTestResource extends KubernetesMockServerTestResource {

    private static final String NAMESPACE = "default";
    private static final String SERVICENAME = "serverless-workflow-greeting-quarkus";
    private static final String REMOTE_SERVICE_URL = "http://serverless-workflow-greeting-quarkus.test.10.99.154.147.sslip.io";

    @Override
    public Map<String, String> start() {
        // First, start the parent mock server
        Map<String, String> config = super.start();

        // Load the Knative service from YAML
        InputStream is = getClass().getClassLoader().getResourceAsStream("knative/quarkus-greeting.yaml");
        Service service = Serialization.unmarshal(is, Service.class);

        // Set the URL in the status
        if (service.getStatus() == null) {
            service.setStatus(new io.fabric8.knative.serving.v1.ServiceStatus());
        }
        service.getStatus().setUrl(REMOTE_SERVICE_URL);

        // FABRIC8 7.X COMPATIBILITY: Create the service directly using the mock server's client
        // This ensures the service exists BEFORE the application starts, avoiding SSL issues
        // during service discovery at startup time
        try {
            getClient().adapt(io.fabric8.knative.client.KnativeClient.class)
                    .services()
                    .inNamespace(NAMESPACE)
                    .resource(service)
                    .create();
        } catch (Exception e) {
            // Service might already exist, ignore
        }

        return config;
    }
}

// Made with Bob
