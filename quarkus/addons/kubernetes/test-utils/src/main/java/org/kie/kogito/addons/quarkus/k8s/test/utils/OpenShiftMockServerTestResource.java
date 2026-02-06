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
package org.kie.kogito.addons.quarkus.k8s.test.utils;

import java.util.Map;

import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Quarkus test resource that provides a Fabric8 mock server for OpenShift/Kubernetes testing.
 * Note: In Fabric8 7.x, OpenShiftServer was merged into KubernetesMockServer.
 */
public class OpenShiftMockServerTestResource implements QuarkusTestResourceLifecycleManager {

    private KubernetesMockServer server;

    @Override
    public Map<String, String> start() {
        server = new KubernetesMockServer(true); // CRUD mode enabled
        server.init(); // Start mock server

        return Map.of(
                "quarkus.kubernetes-client.master-url", server.createClient().getMasterUrl().toString(),
                "quarkus.kubernetes-client.trust-certs", "true");
    }

    @Override
    public void stop() {
        if (server != null) {
            server.destroy(); // Stop mock server
        }
    }

    public KubernetesMockServer getServer() {
        return server;
    }
}
