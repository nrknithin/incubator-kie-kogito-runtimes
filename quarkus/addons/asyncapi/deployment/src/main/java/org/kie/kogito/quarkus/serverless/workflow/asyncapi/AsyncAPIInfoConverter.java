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
import java.util.Map.Entry;
import java.util.Optional;

import org.kie.kogito.serverless.workflow.asyncapi.AsyncChannelInfo;
import org.kie.kogito.serverless.workflow.asyncapi.AsyncInfo;
import org.kie.kogito.serverless.workflow.asyncapi.AsyncInfoConverter;

import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.channel.Channel;
import com.asyncapi.v3._0_0.model.operation.Operation;
import com.asyncapi.v3._0_0.model.operation.OperationAction;

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

    @SuppressWarnings("unchecked")
    private static AsyncInfo from(AsyncAPI asyncApi) {
        Map<String, AsyncChannelInfo> map = new HashMap<>();
        Map<String, Object> operations = asyncApi.getOperations();
        if (operations != null) {
            for (Entry<String, Object> entry : operations.entrySet()) {
                if (entry.getValue() instanceof Operation) {
                    Operation operation = (Operation) entry.getValue();
                    String operationId = entry.getKey();
                    if (operationId != null && operation.getChannel() != null) {
                        // In v3, 'send' means publishing (outgoing), 'receive' means subscribing (incoming)
                        boolean isSend = operation.getAction() == OperationAction.SEND;
                        String channelRef = getChannelName(operation, asyncApi.getChannels());
                        String channelName = isSend ? channelRef + "_out" : channelRef;
                        map.putIfAbsent(operationId, new AsyncChannelInfo(channelName, isSend));
                    }
                }
            }
        }
        return new AsyncInfo(map);
    }

    private static String getChannelName(Operation operation, Map<String, Object> channels) {
        Object channelRef = operation.getChannel();
        if (channelRef instanceof Channel) {
            Channel channel = (Channel) channelRef;
            // Try to get the address from the channel, or find the key from the channels map
            if (channel.getAddress() != null) {
                return channel.getAddress();
            }
        }
        // If it's a reference string, extract the channel name
        if (channelRef != null) {
            String refStr = channelRef.toString();
            if (refStr.contains("#/channels/")) {
                return refStr.substring(refStr.lastIndexOf("/") + 1);
            }
            return refStr;
        }
        return "unknown";
    }
}
