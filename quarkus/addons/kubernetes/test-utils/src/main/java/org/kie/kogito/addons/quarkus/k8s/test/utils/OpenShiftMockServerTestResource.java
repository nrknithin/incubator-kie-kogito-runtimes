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

import java.util.HashMap;
import java.util.Map;

import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class OpenShiftMockServerTestResource implements QuarkusTestResourceLifecycleManager {

    private OpenShiftMockServer server;
    private OpenShiftClient client;

    @Override
    public Map<String, String> start() {
        // Create and start the OpenShift mock server with CRUD mode enabled
        server = new OpenShiftMockServer(false);
        server.init();
        client = server.createOpenShiftClient();

        Map<String, String> config = new HashMap<>();
        config.put("quarkus.kubernetes-client.master-url", client.getMasterUrl().toString());
        config.put("quarkus.kubernetes-client.trust-certs", "true");
        return config;
    }

    @Override
    public void stop() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.destroy();
        }
    }

    public OpenShiftMockServer getServer() {
        return server;
    }
    
    public OpenShiftClient getClient() {
        return client;
    }
}
