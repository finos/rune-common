# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test

Requires Java 21 (enforced by Maven Enforcer) and Maven. Compiles to Java 8 bytecode.

```sh
# Full build (compile + test + checkstyle)
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run tests for a single module
mvn test -pl serialization
mvn test -pl common

# Run a single test class
mvn test -pl serialization -Dtest=RuneJsonSerializerRoundTripTest
mvn test -pl common -Dtest=PathTest

# Run checkstyle only
mvn checkstyle:check
```

## Architecture

The project is a two-module Maven library for serializing/deserializing Rosetta Model Objects (generated from the [Rune DSL](https://github.com/finos/rune-dsl)).

### Module: `serialization` (`org.finos.rune.mapper`)

The **new** Rune-specific serialization layer, still under active development. Not yet production-ready — consumers should still use `RosettaObjectMapper` from `common` for now.

- `RuneJsonObjectMapper` — Jackson `ObjectMapper` pre-configured for Rune DSL objects; the main entry point
- `RuneJsonConfig` — constants for meta-property names (`@type`, `@model`, `@version`)
- `serializer/PreProcessingSerializer` — wraps the root-level serializer; triggers `SerializationPreProcessor` before writing JSON, and adds top-level `@type`/`@model`/`@version` headers
- `serializer/PruningDeserializer` — wraps the root-level deserializer; calls `.toBuilder().prune().build()` on the fully-deserialized object to remove empty/redundant nodes
- `serializer/RuneChoiceTypeSerializer` / `RuneChoiceTypeDeserializer` — handles Rune choice types (union types)
- `processor/SerializationPreProcessor` — pre-serialization pipeline: (1) collects all key types, (2) prunes duplicate references by precedence (address → external → global), (3) collects global references, (4) prunes global keys that no longer have references
- `introspector/RuneJsonModule` — Jackson module that wires together `RuneJsonAnnotationIntrospector`, `RuneEnumBuilderIntrospector`, `PreProcessingSerializerModifier`, and `PruningDeserializerModifier`
- `introspector/RuneStdTypeResolverBuilder` — custom polymorphic type resolver using `@type` property
- `date/RuneDateModule` — date serialization/deserialization module

### Module: `common` (`com.regnosys.rosetta.common`)

The stable, production-ready library built on top of `serialization`.

**Serialization** (`serialisation/`):
- `RosettaObjectMapper` — production entry point; delegates to `RosettaObjectMapperCreator.forJSON().create()`
- `RosettaObjectMapperCreator` — factory with format-specific builders (JSON, XML, CSV, YAML)
- `xml/` — full XML serialization support with substitution groups, content model disambiguation, and Rosetta-specific bean serializers/deserializers
- `mixin/legacy/` — backwards-compatible mixins for older Rosetta model versions

**Hashing & References** (`hashing/`): Process steps for computing global keys, resolving references, and re-keying objects. Uses the `RosettaModelObject.process(RosettaPath, Processor)` visitor pattern.

**Translation** (`translation/`): Mapping framework used during ingestion — `MappingProcessor`, `MappingContext`, `Path`. `Path` represents a dot-separated path within a source document.

**Transform models** (`transform/`): `TestPackModel` and `PipelineModel` — JSON-serializable descriptors for test packs and transformation pipelines used across REGnosys tooling.

**Validation** (`validation/`): `RosettaTypeValidator` validates model objects; `ValidationReport` collects results.

**Other packages**: `compile/` (dynamic Java compilation), `merging/`, `postprocess/`, `projection/`, `reports/`, `testing/`, `util/`.

### Code Generation in Tests

The `common` module uses `rune-maven-plugin` during `generate-test-sources` to compile `.rosetta` files under `src/test/resources/rosetta/` into Java classes, which are output to `target/test-classes/serialisation/java/` and added as test sources. This means `mvn test-compile` must run before tests that depend on generated types.

## Key Conventions

**Dependency Injection**: Guice is used for DI. The `checkstyle-for-deprecated-guice.xml` rule (active on all builds) **forbids** `com.google.inject.Inject`, `com.google.inject.name.Named`, `com.google.inject.Provider`, and `com.google.inject.Singleton`. Use `jakarta.inject` equivalents instead.

**Rosetta Model Object pattern**: All model types implement `RosettaModelObject` from `rune-runtime`. The standard pattern is immutable objects with a mutable builder: `obj.toBuilder().prune().build()`. Processing (hashing, key collection, etc.) uses the `.process(RosettaPath, Processor)` visitor.

**Two serialization paths**:
- Legacy/stable: `RosettaObjectMapper` / `RosettaObjectMapperCreator` (in `common`) — use for all current production code
- New Rune-specific: `RuneJsonObjectMapper` (in `serialization`) — actively developed, not production-ready
