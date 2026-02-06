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
package org.kie.kogito.quarkus.serverless.workflow.opentelemetry.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for SonataFlow OpenTelemetry integration.
 */
@ConfigMapping(prefix = "sonataflow.otel")
public interface SonataFlowOtelConfig {

    /**
     * Whether SonataFlow OpenTelemetry integration is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The service name to use for OpenTelemetry traces.
     */
    @WithDefault("${quarkus.application.name:kogito-workflow-service}")
    String serviceName();

    /**
     * The service version to use for OpenTelemetry traces.
     */
    @WithDefault("${quarkus.application.version:unknown}")
    String serviceVersion();

    /**
     * Configuration for span generation.
     */
    SpanConfig spans();

    /**
     * Configuration for event generation.
     */
    EventConfig events();

    /**
     * Configuration for span generation.
     */
    interface SpanConfig {
        /**
         * Whether span generation is enabled.
         */
        @WithDefault("true")
        boolean enabled();
    }

    /**
     * Configuration for event generation.
     */
    interface EventConfig {
        /**
         * Whether event generation is enabled.
         */
        @WithDefault("true")
        boolean enabled();
    }

    /**
     * Configuration for test infrastructure.
     */
    TestInfrastructureConfig testInfrastructure();

    /**
     * Configuration for test infrastructure.
     */
    interface TestInfrastructureConfig {
        /**
         * Whether test infrastructure is enabled.
         */
        @WithDefault("false")
        boolean enabled();
    }
}