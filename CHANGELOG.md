# Quarkus 3.27.1 LTS Upgrade Changelog

## Automated Changes (Commit: 169c421a27)

### Version Updates
- **Quarkus**: 3.20.3 → 3.27.1 (LTS)

### Dependency Updates

#### Major Updates
- **Apache Kafka**: 3.9.1 → 4.0.0
- **Jackson**: 2.18.4 → 2.19.2
- **Fabric8 Kubernetes Client**: 7.1.0 → 7.3.1

#### Jakarta EE APIs
- **jakarta.annotation-api**: 2.1.1 → 3.0.0
- **jakarta.validation-api**: 3.0.2 → 3.1.1
- **jakarta.persistence-api**: 3.1.0 → 3.2.0
- **jakarta.xml.bind-api**: 4.0.4 → 4.0.2

#### Testing & Build Tools
- **JUnit Jupiter**: 5.12.2 → 5.13.4
- **JUnit Platform**: 1.12.2 → 1.13.4
- **Mockito**: 5.17.0 → 5.18.0
- **Maven**: 3.9.6 → 3.9.9
- **Maven Plugin API**: 3.7.1 → 3.13.1
- **ByteBuddy**: 1.15.11 → 1.17.6
- **Awaitility**: 4.2.2 → 4.3.0

#### Other Dependencies
- **gRPC**: 1.76.0 → 1.69.1
- **Guava**: 33.0.0-jre → 33.4.8-jre
- **Gson**: 2.10.1 → 2.13.2
- **BouncyCastle**: 1.81 → 1.82
- **SmallRye Config**: 3.11.4 → 3.13.4
- **SmallRye Mutiny Vert.x Web Client**: 3.18.1 → 3.19.2
- **Micrometer**: 1.14.12 → 1.14.7
- **Flyway**: 11.14.1 → 11.11.2
- **PostgreSQL Driver**: 42.7.8 → 42.7.7
- **H2 Database**: 2.3.232 → 2.3.230
- **Infinispan**: 15.0.21.Final → 15.0.19.Final
- **Commons IO**: 2.19.0 → 2.20.0
- **Commons Compress**: 1.27.1 → 1.28.0
- **Glassfish JAXB**: 4.0.6 → 4.0.5
- **Angus Mail**: 2.0.5 → 2.0.4
- **Sun Activation**: 2.0.1 → 2.0.2
- **LZ4 Java**: 1.8.1 → 1.8.0

### Files Modified
- `kogito-build/kogito-dependencies-bom/pom.xml`
- `quarkus/bom/pom.xml`

---

## Manual Fixes

### Issue 1: Quarkiverse Jackson JQ Config Mapping (Build Failure)

**Error:**
```
Failed to initialize application configuration: The configuration class
io.quarkiverse.jackson.jq.deployment.JacksonJqConfig must be an interface
annotated with @ConfigRoot and @ConfigMapping
```

**Root Cause:**
Quarkus 3.27.1 requires Quarkiverse Jackson JQ extension to use the new `@ConfigMapping`
interface-based configuration (introduced in Quarkus 3.x). Version 2.2.0 uses the old
class-based configuration.

**Fix:**
Updated `io.quarkiverse.jackson-jq` from 2.2.0 to 2.4.0 which supports Quarkus 3.27.x
config mapping requirements.

**Files Modified:**
- `kogito-build/kogito-dependencies-bom/pom.xml`

**Commit:** (pending)

### Issue 2: Quarkiverse AsyncAPI Config Mapping (Build Failure)

**Error:**
```
Failed to initialize application configuration: The configuration class
io.quarkiverse.asyncapi.generator.AsyncApiConfigGroup must be an interface
annotated with @ConfigRoot and @ConfigMapping
```

**Root Cause:**
Similar to Jackson JQ, Quarkus 3.27.1 requires Quarkiverse AsyncAPI extension to use
the new `@ConfigMapping` interface-based configuration. Version 0.3.0 uses the old
class-based configuration.

**Fix:**
Updated `io.quarkiverse.asyncapi` from 0.3.0 to 1.0.5 which supports Quarkus 3.27.x
config mapping requirements.

**Files Modified:**
- `kogito-build/kogito-dependencies-bom/pom.xml`


---

### Issue 3: AsyncAPI v3.0.0 API Migration (Build Failure)

**Error:**
```
incompatible types: invalid method reference
incompatible types: com.asyncapi.v3._0_0.model.AsyncAPI cannot be converted to com.asyncapi.v2._6_0.model.AsyncAPI
```

**Root Cause:**
AsyncAPI Quarkiverse extension 1.0.5 uses AsyncAPI v3.0.0 model which has breaking changes from v2.6.0:
- In v2.6: channels contain operations (publish/subscribe)
- In v3.0: operations are top-level and reference channels

**Fix:**
Migrated `AsyncAPIInfoConverter` to use AsyncAPI v3.0.0 API:
- Updated imports from `com.asyncapi.v2._6_0.*` to `com.asyncapi.v3._0_0.*`
- Adapted logic to iterate over top-level operations and extract channel references
- Preserved `addChannel` method for minimal code changes

**Files Modified:**
- `quarkus/extensions/kogito-quarkus-serverless-workflow-extension/kogito-quarkus-serverless-workflow-deployment/src/main/java/org/kie/kogito/quarkus/serverless/workflow/asyncapi/AsyncAPIInfoConverter.java`

**Commit:** 1080f8395c

---

### Issue 4: Fabric8 Kubernetes Client 7.3.1 API Migration (Build Failure)

**Error:**
```
cannot find symbol: class KubernetesServer
cannot find symbol: class OpenShiftServer
cannot access org.junit.rules.ExternalResource
```

**Root Cause:**
Fabric8 kubernetes-client 7.x renamed mock server classes and changed lifecycle methods:
- `KubernetesServer` → `KubernetesMockServer`
- `OpenShiftServer` → `OpenShiftMockServer`
- `before()`/`after()` → `init()`/`destroy()`

**Fix:**
Updated Kubernetes test utilities to use new API:
- Replaced `KubernetesServer` with `KubernetesMockServer`
- Replaced `OpenShiftServer` with `OpenShiftMockServer`
- Updated lifecycle methods and added try-catch for error handling

**Files Modified:**
- `quarkus/addons/kubernetes/test-utils/src/main/java/org/kie/kogito/addons/quarkus/k8s/test/utils/KubernetesMockServerTestResource.java`
- `quarkus/addons/kubernetes/test-utils/src/main/java/org/kie/kogito/addons/quarkus/k8s/test/utils/OpenShiftMockServerTestResource.java`

**Commit:** 1080f8395c
**Commit:** (pending)
