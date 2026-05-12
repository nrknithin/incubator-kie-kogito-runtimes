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
package org.kie.kogito.addon.cloudevents.spring.jackson3;

import java.io.IOException;
import java.util.Optional;

import org.kie.kogito.event.Converter;

import io.cloudevents.CloudEventData;
import io.cloudevents.core.data.PojoCloudEventData;

import tools.jackson.databind.ObjectMapper;

public abstract class AbstractCloudEventDataConverter<O> implements Converter<CloudEventData, O> {

    protected final ObjectMapper objectMapper;
    protected final Class<O> targetClass;

    protected AbstractCloudEventDataConverter(ObjectMapper objectMapper, Class<O> targetClass) {
        this.objectMapper = objectMapper;
        this.targetClass = targetClass;
    }

    @Override
    public O convert(CloudEventData value) throws IOException {
        if (value == null) {
            return null;
        }
        Optional<O> target = isTargetInstanceAlready(value);
        return target.isPresent() ? target.get() : toValue(value.toBytes());
    }

    protected Optional<O> isTargetInstanceAlready(CloudEventData value) {
        // Note: the Jackson 2 sibling of this class also fast-paths
        // io.cloudevents.jackson.JsonCloudEventData, but that helper is
        // permanently bound to Jackson 2 (its getNode() returns a
        // com.fasterxml.jackson.databind.JsonNode and PojoCloudEventDataMapper
        // requires a Jackson 2 ObjectMapper). In the Spring Boot Jackson 3
        // path a JsonCloudEventData instance should never appear, since no
        // Jackson-2 ObjectMapper is wired in to produce one; if it ever does
        // appear we fall through to toValue(value.toBytes()).
        if (value instanceof PojoCloudEventData) {
            Object pojo = ((PojoCloudEventData<?>) value).getValue();
            if (pojo == null) {
                return Optional.empty();
            }
            return Optional.of(targetClass.isAssignableFrom(pojo.getClass()) ? targetClass.cast(pojo) : objectMapper.convertValue(pojo, targetClass));
        }
        return Optional.empty();
    }

    protected abstract O toValue(byte[] value) throws IOException;
}
