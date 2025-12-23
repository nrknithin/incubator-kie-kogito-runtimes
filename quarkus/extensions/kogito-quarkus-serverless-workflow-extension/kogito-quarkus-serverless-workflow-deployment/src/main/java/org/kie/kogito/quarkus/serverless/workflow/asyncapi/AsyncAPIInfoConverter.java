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
package org.kie.kogito.quarkus.serverless.workflow.asyncapi;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.kie.kogito.serverless.workflow.asyncapi.AsyncChannelInfo;
import org.kie.kogito.serverless.workflow.asyncapi.AsyncInfo;
import org.kie.kogito.serverless.workflow.asyncapi.AsyncInfoConverter;

import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.operation.Operation;

import io.quarkiverse.asyncapi.config.AsyncAPIRegistry;

public class AsyncAPIInfoConverter implements AsyncInfoConverter {

    private final AsyncAPIRegistry registry;

    public AsyncAPIInfoConverter(AsyncAPIRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Optional<AsyncInfo> apply(String id) {
        return registry.getAsyncAPI(id).map(AsyncAPIInfoConverter::from);
    }

    private static AsyncInfo from(AsyncAPI asyncApi) {
        Map<String, AsyncChannelInfo> map = new HashMap<>();
        if (asyncApi.getOperations() != null) {
            asyncApi.getOperations().forEach((operationId, operationObj) -> {
                if (operationObj instanceof Operation) {
                    Operation operation = (Operation) operationObj;
                    String channelRef = operation.getChannel() != null ? String.valueOf(operation.getChannel()) : null;
                    if (channelRef != null) {
                        String channelName = channelRef.contains("/") ? channelRef.substring(channelRef.lastIndexOf('/') + 1) : channelRef;
                        boolean isPublish = "send".equalsIgnoreCase(String.valueOf(operation.getAction()));
                        addChannel(map, operationId, channelName, isPublish);
                    }
                }
            });
        }
        return new AsyncInfo(map);
    }

    private static void addChannel(Map<String, AsyncChannelInfo> map, String operationId, String channelName, boolean publish) {
        if (operationId != null) {
            map.putIfAbsent(operationId, new AsyncChannelInfo(channelName, publish));
        }
    }
}
