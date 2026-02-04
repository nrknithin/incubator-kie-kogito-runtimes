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

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

/**
 * Quarkus 3.27.2 / Fabric8 7.x upgrade:
 * - @QuarkusIntegrationTest runs the application in a separate JVM, so @Inject is not supported.
 * - Removed @Inject KubernetesClient and @BeforeEach that created the Knative service.
 * - Switched from KubernetesMockServerTestResource to ConfigValueExpanderTestResource,
 * which creates the Knative service in its start() method before the application boots.
 * - This ensures the mock Knative service is available when the application resolves
 * config values during startup.
 */
@QuarkusIntegrationTest
@QuarkusTestResource(ConfigValueExpanderTestResource.class)
class ConfigValueExpanderIT {

    @Test
    void test() {
        given().when()
                .get("/foo")
                .then()
                .statusCode(200)
                .body(is("http://serverless-workflow-greeting-quarkus.test.10.99.154.147.sslip.io/path"));
    }
}
