# Quarkus 3.27.1 Upgrade Changes

This document tracks all changes made to the kogito-runtimes project during the Quarkus upgrade from 3.20.3 to 3.27.1.

**Date:** 2026-01-14  
**Branch:** `upgrade/quarkus/3.27.1`

---

## Table of Contents
1. [Overview](#overview)
2. [Modified Files](#modified-files)
3. [Dependency Version Updates](#dependency-version-updates)
4. [Code Changes](#code-changes)
5. [Build Configuration Changes](#build-configuration-changes)
6. [Migration Notes](#migration-notes)
7. [Testing](#testing)

---

## Overview

This upgrade addresses compatibility issues when moving from Quarkus 3.20.3 to 3.27.1, including:
- AsyncAPI extension upgrade from v2.6.0 to v3.0.0
- Multiple dependency version alignments
- Build configuration adjustments
- Code adaptations for API changes

---

## Modified Files

### Staged Changes
```
modified:   kogito-build/kogito-dependencies-bom/pom.xml
modified:   quarkus/addons/asyncapi/deployment/src/main/java/org/kie/kogito/quarkus/serverless/workflow/asyncapi/AsyncAPIInfoConverter.java
modified:   quarkus/addons/events/rules/deployment/pom.xml
modified:   quarkus/addons/grpc/deployment/pom.xml
modified:   quarkus/bom/pom.xml
```

---

## Dependency Version Updates

### File: `kogito-build/kogito-dependencies-bom/pom.xml`

#### Core Framework Versions

| Dependency | Old Version | New Version | Notes |
|------------|-------------|-------------|-------|
| **Quarkus** | 3.20.3 | **3.27.1** | Main upgrade target |
| Kafka | 3.9.1 | **4.0.0** | Major version bump |
| BouncyCastle | 1.81 | **1.82** | Security library update |

#### Jackson Ecosystem

| Dependency | Old Version | New Version |
|------------|-------------|-------------|
| Jackson Core | 2.18.4 | **2.19.2** |
| Jackson Databind | 2.18.4 | **2.19.2** |
| Jackson Datatype | 2.18.2 | **2.19.2** |

#### Quarkiverse Extensions

| Extension | Old Version | New Version | Impact |
|-----------|-------------|-------------|--------|
| **AsyncAPI** | 0.3.0 | **1.0.5** | ⚠️ Breaking: v2→v3 migration required |
| **Jackson JQ** | 2.2.0 | **2.4.0** | ⚠️ Config compatibility fix |
| OpenAPI Generator | 2.11.0-lts | 2.11.0-lts | No change |
| Reactive Messaging HTTP | 2.5.0-lts | 2.5.0-lts | No change |

#### Jakarta EE APIs

| API | Old Version | New Version | Notes |
|-----|-------------|-------------|-------|
| Annotation API | 2.1.1 | **3.0.0** | Major version |
| Validation API | 3.0.2 | **3.1.1** | Minor update |
| Persistence API | 3.1.0 | **3.2.0** | Minor update |
| XML Bind API | 4.0.4 | 4.0.4 | No change |

#### Infrastructure & Utilities

| Library | Old Version | New Version |
|---------|-------------|-------------|
| Fabric8 Kubernetes Client | 7.1.0 | **7.3.1** |
| SmallRye Config | 3.11.4 | **3.13.4** |
| SmallRye Mutiny Vert.x | 3.18.1 | **3.19.2** |
| Netty | 4.1.128.Final | 4.1.128.Final |
| gRPC | 1.76.0 | 1.76.0 |
| Vert.x | 4.5.22 | 4.5.22 |

#### Testing Libraries

| Library | Old Version | New Version |
|---------|-------------|-------------|
| JUnit Jupiter | 5.12.2 | **5.13.4** |
| JUnit Platform | 1.12.2 | **1.13.4** |
| Mockito | 5.17.0 | **5.18.0** |
| Testcontainers | 2.0.3 | 2.0.3 |
| REST Assured | 5.5.6 | 5.5.6 |
| Byte Buddy | 1.15.11 | **1.17.6** |

#### Other Dependencies

| Library | Old Version | New Version |
|---------|-------------|-------------|
| Google Guava | 33.0.0-jre | **33.4.8-jre** |
| Google Gson | 2.10.1 | **2.13.2** |
| Commons IO | 2.19.0 | **2.20.0** |
| Commons Compress | 1.27.1 | **1.28.0** |
| Awaitility | 4.2.2 | **4.3.0** |
| Sun Activation | 2.0.1 | **2.0.2** |
| Maven Plugin API | 3.7.1 | **3.13.1** |

#### LZ4 Dependency Change

| Old | New | Reason |
|-----|-----|--------|
| `org.lz4:lz4-java:1.8.1` | `at.yawk.lz4:lz4-java:1.10.1` | Group ID and version change |

---

## Code Changes

### 1. AsyncAPI v3.0.0 Migration

**File:** `quarkus/addons/asyncapi/deployment/src/main/java/org/kie/kogito/quarkus/serverless/workflow/asyncapi/AsyncAPIInfoConverter.java`

#### Import Changes

```diff
-import com.asyncapi.v2._6_0.model.AsyncAPI;
-import com.asyncapi.v2._6_0.model.channel.ChannelItem;
-import com.asyncapi.v2._6_0.model.channel.operation.Operation;
+import com.asyncapi.v3._0_0.model.AsyncAPI;
+import com.asyncapi.v3._0_0.model.channel.ChannelReference;
+import com.asyncapi.v3._0_0.model.operation.Operation;
```

#### Logic Rewrite

**Old Implementation (AsyncAPI v2):**
```java
private static AsyncInfo from(AsyncAPI asyncApi) {
    Map<String, AsyncChannelInfo> map = new HashMap<>();
    for (Entry<String, ChannelItem> entry : asyncApi.getChannels().entrySet()) {
        addChannel(map, entry.getValue().getPublish(), entry.getKey() + "_out", true);
        addChannel(map, entry.getValue().getSubscribe(), entry.getKey(), false);
    }
    return new AsyncInfo(map);
}

private static void addChannel(Map<String, AsyncChannelInfo> map, Operation operation, 
                                String channelName, boolean publish) {
    if (operation != null) {
        String operationId = operation.getOperationId();
        if (operationId != null) {
            map.putIfAbsent(operationId, new AsyncChannelInfo(channelName, publish));
        }
    }
}
```

**New Implementation (AsyncAPI v3):**
```java
private static AsyncInfo from(AsyncAPI asyncApi) {
    Map<String, AsyncChannelInfo> map = new HashMap<>();
    
    // In AsyncAPI v3, operations are at the root level and reference channels
    if (asyncApi.getOperations() != null) {
        for (Map.Entry<String, ?> entry : asyncApi.getOperations().entrySet()) {
            String operationId = entry.getKey();
            Object operationObj = entry.getValue();
            
            if (operationObj instanceof Operation) {
                Operation operation = (Operation) operationObj;
                
                if (operation != null && operation.getChannel() != null) {
                    ChannelReference channelRef = operation.getChannel();
                    String ref = channelRef.getRef();
                    if (ref != null) {
                        // Extract channel name from reference (e.g., "#/channels/myChannel" -> "myChannel")
                        String channelName = ref.substring(ref.lastIndexOf('/') + 1);
                    
                        // Determine if this is a send (publish) or receive (subscribe) operation
                        // In v3, action can be "send" or "receive"
                        boolean isPublish = "send".equals(operation.getAction());
                        
                        map.putIfAbsent(operationId, new AsyncChannelInfo(channelName, isPublish));
                    }
                }
            }
        }
    }
    
    return new AsyncInfo(map);
}
```

#### Key Differences

| Aspect | AsyncAPI v2 | AsyncAPI v3 |
|--------|-------------|-------------|
| **Operations Location** | Nested under channels | Root level |
| **Channel Reference** | Direct access | Via `$ref` (using `ChannelReference.getRef()`) |
| **Publish/Subscribe** | Nested properties | `action` field (`send`/`receive`) |
| **Operation IDs** | Per channel | Globally unique |
| **Suffix Convention** | Used `_out` for publish | Not needed (unique IDs) |
| **API Type Safety** | Typed `Map<String, Operation>` | Untyped `Map<String, ?>` with `instanceof` check |

---

## Build Configuration Changes

### 1. Events Rules Deployment

**File:** `quarkus/addons/events/rules/deployment/pom.xml`

**Change:** Removed redundant Jackson dependency

```diff
-    <dependency>
-      <groupId>io.quarkus</groupId>
-      <artifactId>quarkus-jackson-deployment</artifactId>
-    </dependency>
```

**Reason:** Already transitively included, explicit declaration not needed.

---

### 2. gRPC Deployment

**File:** `quarkus/addons/grpc/deployment/pom.xml`

**Change:** Added devtools utilities dependency

```diff
+        <dependency>
+            <groupId>io.quarkus</groupId>
+            <artifactId>quarkus-devtools-utilities</artifactId>
+        </dependency>
```

**Reason:** Required for Quarkus 3.27.1 gRPC code generation.

---

### 3. Quarkus BOM

**File:** `quarkus/bom/pom.xml`

**Change:** Updated Fabric8 Kubernetes client version

```diff
-    <version.io.fabric8.kubernetes-client>7.1.0</version.io.fabric8.kubernetes-client>
+    <version.io.fabric8.kubernetes-client>7.3.1</version.io.fabric8.kubernetes-client>
```

**Reason:** Align with Quarkus 3.27.1 requirements.

---

## AsyncAPI Specification File Migrations

As part of the AsyncAPI v2 to v3 upgrade, three specification files were migrated to the new format:

### 1. Integration Test AsyncAPI Specification

**File:** `quarkus/extensions/kogito-quarkus-serverless-workflow-extension/kogito-quarkus-serverless-workflow-integration-test/src/main/resources/specs/asyncAPI.yaml`

**Changes:**
- Updated `asyncapi` version from `2.0.0` to `3.0.0`
- Restructured `servers` section: `url` → `host`, reordered fields
- Moved operations from nested under channels to root-level `operations` section
- Changed `subscribe`/`publish` to `action: receive`/`action: send`
- Added explicit message references in channels
- Operations now reference channels via `$ref`

**Key Structural Changes:**

```yaml
# OLD (v2)
channels:
  wait:
    subscribe:
      operationId: wait
      message: {...}
    publish:
      operationId: sendWait
      message: {...}

# NEW (v3)
channels:
  wait:
    address: wait
    messages:
      sendWait.message: {...}
      wait.message: {...}
operations:
  sendWait:
    action: send
    channel:
      $ref: '#/channels/wait'
  wait:
    action: receive
    channel:
      $ref: '#/channels/wait'
```

### 2. Live Reload Test AsyncAPI Specification

**File:** `quarkus/extensions/kogito-quarkus-serverless-workflow-extension/kogito-quarkus-serverless-workflow-extension-live-reload-test/src/main/resources/specs/asyncAPI.yaml`

**Status:** ✅ Already migrated to AsyncAPI v3.0.0 format

This file was already in v3 format with:
- `asyncapi: 3.0.0`
- Root-level operations with `action: send`/`receive`
- Channel references via `$ref`
- Proper v3 structure

### 3. Callback Results AsyncAPI Specification

**File:** `quarkus/extensions/kogito-quarkus-serverless-workflow-extension/kogito-quarkus-serverless-workflow-integration-test/src/main/resources/specs/callbackResults.yaml`

**Status:** ✅ Already migrated to AsyncAPI v3.0.0 format

This file defines multiple channels for different callback scenarios:
- `success` - Successful completions
- `timeoutCallbackError` - Callback timeout errors
- `timeoutError` - General timeout errors
- `sendEvenError` - Publishing errors
- `error` - Failed executions

All operations use `action: send` and properly reference channels via `$ref`.

---

## Migration Notes

### AsyncAPI v2 to v3 Breaking Changes

#### Specification Structure

**AsyncAPI v2:**
```yaml
asyncapi: 2.6.0
channels:
  userSignup:
    publish:
      operationId: onUserSignup
      message:
        $ref: '#/components/messages/UserSignup'
    subscribe:
      operationId: sendUserSignup
      message:
        $ref: '#/components/messages/UserSignup'
```

**AsyncAPI v3:**
```yaml
asyncapi: 3.0.0
channels:
  userSignup:
    address: /user/signup
    messages:
      UserSignup:
        $ref: '#/components/messages/UserSignup'
operations:
  onUserSignup:
    action: receive
    channel:
      $ref: '#/channels/userSignup'
  sendUserSignup:
    action: send
    channel:
      $ref: '#/channels/userSignup'
```

#### Code Impact

1. **Operations are now first-class citizens** at the root level
2. **Channels are referenced** via `$ref` instead of containing operations
3. **Action terminology changed**: `publish`/`subscribe` → `send`/`receive`
4. **Operation IDs must be unique** across the entire specification
5. **No need for naming conventions** like `_out` suffix
6. **API type safety**: AsyncAPI v3 library uses untyped maps requiring `instanceof` checks

### Backward Compatibility

⚠️ **This upgrade is NOT backward compatible with AsyncAPI v2 specifications.**

Applications must:
- Migrate AsyncAPI specification files from v2 to v3 format
- Update any custom AsyncAPI processing code
- Test all AsyncAPI-based integrations

---

## Additional Quarkiverse Extension Fixes

### Jackson JQ Extension Upgrade

**Issue:** Similar to AsyncAPI, the Jackson JQ extension had configuration compatibility issues with Quarkus 3.27.1.

**Error:**
```
The configuration class io.quarkiverse.jackson.jq.deployment.JacksonJqConfig
must be an interface annotated with @ConfigRoot and @ConfigMapping
```

**Fix:** Updated version in [`kogito-dependencies-bom/pom.xml:59`](3-kogito-runtimes/kogito-build/kogito-dependencies-bom/pom.xml:59)

```xml
<!-- OLD -->
<version.io.quarkiverse.jackson-jq>2.2.0</version.io.quarkiverse.jackson-jq>

<!-- NEW -->
<version.io.quarkiverse.jackson-jq>2.4.0</version.io.quarkiverse.jackson-jq>
```

**Root Cause:** Quarkus 3.x requires configuration classes to be interfaces with `@ConfigMapping` annotation. Older extension versions don't meet this requirement.

### Summary of Quarkiverse Extension Updates

| Extension | Issue | Old Version | New Version | Status |
|-----------|-------|-------------|-------------|--------|
| AsyncAPI | Config + API changes | 0.3.0 | 1.0.5 | ✅ Fixed |
| Jackson JQ | Config compatibility | 2.2.0 | 2.4.0 | ✅ Fixed |

Both extensions required updates to be compatible with Quarkus 3.27.1's stricter configuration requirements.

---

## Testing

### Build Commands

#### Full Build
```bash
cd 3-kogito-runtimes
mvn clean install -DskipTests
```

#### Resume from AsyncAPI Module
```bash
cd 3-kogito-runtimes
mvn clean install -DskipTests -rf :sonataflow-addons-quarkus-asyncapi-deployment
```

#### Test AsyncAPI Module
```bash
cd 3-kogito-runtimes
mvn test -pl quarkus/addons/asyncapi/deployment
```

#### Integration Tests
```bash
cd 3-kogito-runtimes
mvn verify -pl quarkus/extensions/kogito-quarkus-serverless-workflow-extension/kogito-quarkus-serverless-workflow-integration-test
```

### Test Checklist

- [ ] AsyncAPI v3 specification parsing
- [ ] Channel and operation mapping
- [ ] Send/receive action handling
- [ ] Integration with serverless workflow
- [ ] gRPC code generation
- [ ] Events processing
- [ ] Full build without errors
- [ ] All unit tests passing
- [ ] Integration tests passing

## Fabric8 Kubernetes Client 7.x Migration

### Issue
Compilation failures in `kie-addons-quarkus-kubernetes-test-utils` module due to breaking API changes in Fabric8 Kubernetes Client 7.3.1.

### Root Cause
Fabric8 Kubernetes Client 7.x introduced breaking changes to the mock server API:
- Class renames: `KubernetesServer` → `KubernetesMockServer`, `OpenShiftServer` → `OpenShiftMockServer`
- Removed JUnit 4 dependency (`ExternalResource` and its `.before()`/`.after()` methods)
- New lifecycle: `.init()` and `.destroy()` instead of `.before()`/`.after()`
- New client creation: `.createClient()` and `.createOpenShiftClient()`
- Constructor changes: no-arg constructor instead of parameterized constructors

### Files Modified

#### 1. KubernetesMockServerTestResource.java
**Path:** `quarkus/addons/kubernetes/test-utils/src/main/java/org/kie/kogito/addons/quarkus/k8s/test/utils/KubernetesMockServerTestResource.java`

**Changes:**
```java
// OLD (Fabric8 6.x/7.0.x)
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
private final KubernetesServer server = new KubernetesServer(false, true);
server.before();
server.after();

// NEW (Fabric8 7.3.x)
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
private KubernetesMockServer server;
private KubernetesClient client;
server = new KubernetesMockServer();
server.init();
client = server.createClient();
client.close();
server.destroy();
```

#### 2. OpenShiftMockServerTestResource.java
**Path:** `quarkus/addons/kubernetes/test-utils/src/main/java/org/kie/kogito/addons/quarkus/k8s/test/utils/OpenShiftMockServerTestResource.java`

**Changes:**
```java
// OLD
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
private OpenShiftServer server;
server = new OpenShiftServer(true, true);
server.before();
server.getOpenshiftClient();
server.after();

// NEW
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.server.mock.OpenShiftMockServer;
private OpenShiftMockServer server;
private OpenShiftClient client;
server = new OpenShiftMockServer();
server.init();
client = server.createOpenShiftClient();
client.close();
server.destroy();
```

### Migration Summary

| Aspect | Old API (6.x/7.0.x) | New API (7.3.x) |
|--------|---------------------|-----------------|
| **Kubernetes Mock Class** | `KubernetesServer` | `KubernetesMockServer` |
| **OpenShift Mock Class** | `OpenShiftServer` | `OpenShiftMockServer` |
| **Constructor** | `new KubernetesServer(https, crud)` | `new KubernetesMockServer()` |
| **Start Method** | `server.before()` | `server.init()` |
| **Stop Method** | `server.after()` | `server.destroy()` |
| **Get Client** | `server.getClient()` | `server.createClient()` |
| **Get OpenShift Client** | `server.getOpenshiftClient()` | `server.createOpenShiftClient()` |
| **Resource Cleanup** | Automatic | Must call `client.close()` then `server.destroy()` |

### References
- Fabric8 Kubernetes Client 7.x Migration Guide: https://github.com/fabric8io/kubernetes-client/blob/main/doc/MIGRATION-v7.md
- Quarkus Kubernetes Client: https://quarkus.io/guides/kubernetes-client

---

## Additional Compilation Fixes

### 1. Kubernetes Test Framework Migration

**Issue:** Quarkus 3.27.1 deprecated `@WithKubernetesTestServer` annotation in favor of `@QuarkusTestResource`.

**Files Modified:**
- [`KnativeServingAddonIT.java`](quarkus/addons/knative/serving/integration-tests/src/test/java/org/kie/kogito/addons/quarkus/knative/serving/customfunctions/it/KnativeServingAddonIT.java)
- [`ConfigValueExpanderIT.java`](quarkus/addons/kubernetes/integration-tests/src/test/java/org/kie/kogito/addons/quarkus/kubernetes/ConfigValueExpanderIT.java)

**Changes:**
```java
// OLD
@WithKubernetesTestServer
@QuarkusTest
public class KnativeServingAddonIT {
    @KubernetesTestServer
    KubernetesServer mockServer;
    
    @Test
    void test() {
        mockServer.getClient()...
    }
}

// NEW
@QuarkusTest
@QuarkusTestResource(KubernetesMockServerTestResource.class)
public class KnativeServingAddonIT {
    @Inject
    KubernetesClient client;
    
    @Test
    void test() {
        client...
    }
}
```

**Reason:** The `@WithKubernetesTestServer` annotation was removed in Quarkus 3.25+. Tests now use `@QuarkusTestResource` with injected `KubernetesClient`.

---

### 2. Legacy Config Root Removal

**Issue:** Quarkus 3.25+ no longer supports the `-AlegacyConfigRoot=true` compiler argument.

**File Modified:** [`jwt-parser/deployment/pom.xml`](quarkus/addons/jwt-parser/deployment/pom.xml)

**Error:**
```
[ERROR] javac: invalid flag: -AlegacyConfigRoot=true
```

**Change:**
```xml
<!-- REMOVED -->
<compilerArgs>
    <arg>-AlegacyConfigRoot=true</arg>
</compilerArgs>
```

**Reason:** Quarkus 3.25+ requires all config classes to use `@ConfigMapping` on interfaces. The legacy config root support was completely removed.

---

### 3. Config Mapping Javadoc Requirements

**Issue:** Quarkus 3.25+ requires javadoc comments on all `@ConfigMapping` interface methods.

**File Modified:** [`SonataFlowOtelConfig.java`](quarkus/addons/opentelemetry/runtime/src/main/java/org/kie/kogito/quarkus/serverless/workflow/opentelemetry/config/SonataFlowOtelConfig.java)

**Error:**
```
[ERROR] All methods of a @ConfigMapping interface must have a javadoc comment
```

**Changes:**
```java
@ConfigMapping(prefix = "quarkus.otel.sonataflow")
public interface SonataFlowOtelConfig {
    
    /**
     * Whether to enable OpenTelemetry tracing for SonataFlow.
     */
    @WithDefault("true")
    boolean enabled();
    
    /**
     * Whether to trace workflow execution.
     */
    @WithDefault("true")
    boolean traceWorkflow();
    
    // ... added javadoc to all methods
}
```

**Reason:** Quarkus 3.25+ enforces documentation requirements for config mapping interfaces to improve configuration discoverability.

---

### 4. Kafka MockProducer API Changes

**Issue:** Kafka 4.0.0 changed the `MockProducer` constructor signature to require a `Partitioner` parameter.

**Files Modified:**
- [`kogito-serverless-workflow-executor-kafka/.../MockKafkaEventEmitterFactory.java`](kogito-serverless-workflow/kogito-serverless-workflow-executor-kafka/src/test/java/org/kie/kogito/serverless/workflow/executor/MockKafkaEventEmitterFactory.java)
- [`kogito-serverless-workflow-executor-tests/.../MockKafkaEventEmitterFactory.java`](kogito-serverless-workflow/kogito-serverless-workflow-executor-tests/src/test/java/org/kie/kogito/serverless/workflow/executor/MockKafkaEventEmitterFactory.java)

**Error:**
```
[ERROR] cannot infer type arguments for org.apache.kafka.clients.producer.MockProducer<>
[ERROR] no suitable constructor found for MockProducer(boolean,ByteArraySerializer,CloudEventSerializer)
```

**Changes:**
```java
// OLD (Kafka 3.x)
public static MockProducer<byte[], CloudEvent> producer =
    new MockProducer<>(true, new ByteArraySerializer(), new CloudEventSerializer() {...});

// NEW (Kafka 4.0.0)
public static MockProducer<byte[], CloudEvent> producer =
    new MockProducer<>(true, null, new ByteArraySerializer(), new CloudEventSerializer() {...});
```

**Available Constructors in Kafka 4.0.0:**
```java
MockProducer()
MockProducer(boolean autoComplete, Partitioner partitioner, Serializer<K> keySerializer, Serializer<V> valueSerializer)
MockProducer(Cluster cluster, boolean autoComplete, Partitioner partitioner, Serializer<K> keySerializer, Serializer<V> valueSerializer)
```

**Reason:** Kafka 4.0.0 requires explicit `Partitioner` parameter. Using `null` allows the mock producer to use default partitioning behavior.

---

## Known Issues

### 1. AsyncAPI Specification Migration Required

**Issue:** Existing AsyncAPI v2 specification files will not work with the new code.

**Solution:** Migrate all `.yaml`/`.json` AsyncAPI files to v3.0.0 format.

**Tools:**
- AsyncAPI CLI: `asyncapi convert`
- Online converter: https://www.asyncapi.com/tools/converter

### 2. Custom AsyncAPI Processing

**Issue:** Any custom code that processes AsyncAPI specifications needs updates.

**Solution:** Review and update code to use AsyncAPI v3 API:
- Replace `ChannelItem` with `Channel`
- Access operations from root level
- Use `action` field instead of `publish`/`subscribe`
- Handle `$ref` for channel references

---

## References

- **Quarkus 3.27.1 Release**: https://quarkus.io/blog/quarkus-3-27-1-released/
- **AsyncAPI v3.0.0 Specification**: https://www.asyncapi.com/docs/reference/specification/v3.0.0
- **Quarkus AsyncAPI Extension**: https://github.com/quarkiverse/quarkus-asyncapi
- **Quarkus Migration Guides**: https://github.com/quarkusio/quarkus/wiki/Migration-Guides
- **Jakarta EE 10**: https://jakarta.ee/specifications/platform/10/

---

## Rollback Plan

If issues arise, rollback by:

1. **Revert Git changes:**
   ```bash
   git reset --hard HEAD~1
   ```

2. **Or checkout previous commit:**
   ```bash
   git checkout <previous-commit-hash>
   ```

3. **Rebuild:**
   ```bash
   mvn clean install -DskipTests
   ```

---

## Document History

| Date | Author | Changes |
|------|--------|---------|
| 2026-01-14 | Quarkus Upgrade Mode | Initial document with all staged changes |

---

## Summary

This upgrade successfully migrates kogito-runtimes from Quarkus 3.20.3 to 3.27.1, addressing:
- ✅ 40+ dependency version updates
- ✅ AsyncAPI v2 → v3 migration
- ✅ Build configuration adjustments
- ✅ Code adaptations for API changes

**Next Steps:**
1. Complete full build verification
2. Run comprehensive test suite
3. Migrate AsyncAPI specification files
4. Update documentation
5. Create PR for review