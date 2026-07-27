# StAX Migration — Progress Report

Unified progress log for all steps of the StAX binder migration
(`xml-mapper-stax-migration-plan.md`). Living document — updated as sub-steps complete.

> **For agents updating this report:** Add each step's section in ascending order
> (Step 0 first, Step 6 last). When a step completes, update its status row and
> add implementation notes immediately below it — do not move completed sections
> to the top. Keep the `## Step N` headings so the document stays scannable top-to-bottom.

Legend: ✅ done · 🔄 in progress · ⬜ not started

---

## Step 0 — Spike & boundary proof — ✅ COMPLETE (2026-06-20)

| Sub-step | What | Owner | Status |
|---|---|---|---|
| 0.1 | Pin Woodstox as direct dep | Sonnet sub-agent | ✅ |
| 0.2 | Throwaway StAX read/write spike (one simple type) | Sonnet sub-agent | ✅ |
| 0.3 | Boundary proof: collision + namespace + document order | Opus (main) | ✅ |
| 0.4 | Eyeball generated config for completeness vs criteria 1–12 | Opus (main) | ✅ |
| 0.5 | Harvest named production types as acceptance fixtures | Opus (main) | ✅ |

**Step 0 exit status: COMPLETE.** Spike proves read+write+collision for one type; the two
hardest issues (namespace, order) proven at parser level; production fixtures captured;
config-completeness note written below. No findings block Step 1.

### Step 0.1 — Pin Woodstox ✅

- Parent `pom.xml`: added `woodstox.version=6.6.2`, `stax2-api.version=4.2.2` to
  `<properties>`; added `dependencyManagement` entries for
  `com.fasterxml.woodstox:woodstox-core` and `org.codehaus.woodstox:stax2-api`.
- `common/pom.xml`: added direct version-less `woodstox-core` dependency. `stax2-api`
  arrives transitively.
- Verified: `mvn -pl common dependency:tree | grep -i woodstox` →
  `woodstox-core:6.6.2`, `stax2-api:4.2.2` as direct/managed deps.

### Step 0.2 — StAX spike ✅

- New: `common/src/test/java/com/regnosys/rosetta/common/serialisation/xml/spike/StaxBinderSpikeTest.java`
- 3 JUnit5 tests, raw `javax.xml.stream` (no Jackson), all pass:
  `timeContainerRoundTrip`, `measureRoundTrip`, `attributeDistinction`.
- Verified: `mvn test -pl common -Dtest=StaxBinderSpikeTest` → `Tests run: 3, Failures: 0`;
  checkstyle clean.

#### Carry-forward notes from the spike
- **Enum form:** `.rosetta` uses `displayName` (`"Meter"`, `"Kilogram"`). Confirm whether the
  XML config serializes display names vs enum `name()` (criterion 8).
- **`getElementText()` footgun:** advances cursor past `END_ELEMENT`; reader loop must not call
  `next()` again or it skips the next sibling.
- **Attr/element decision** is clean at the StAX API level; the real work is the config layer
  (`AttributeXMLConfiguration`/`TypeXMLConfiguration`), reused as-is.
- **Meta-headers** (`@type`/`@model`/`@version`) need attribute-style handling for polymorphic roots.

### Step 0.3 — Boundary proof ✅

Added 3 boundary-proof tests to the spike (`StaxBinderSpikeTest`), all pass —
`mvn test -pl common -Dtest=StaxBinderSpikeTest` → **Tests run: 6, Failures: 0**:

1. `collisionSameLocalNameAttributeAndElement` — feeds raw
   `<RepoTransactionLeg id="ATTR_ID"><id>ELEM_ID</id></RepoTransactionLeg>`; reads attribute
   `id` via `getAttributeValue(null,"id")` and element `id` via `getElementText()`, asserting
   both survive **distinctly** (`assertNotEquals`). **This is the bug being fixed (issue 1 /
   criterion 13) and the whole justification for the migration — proven clean at parser level.**
2. `namespaceUriIsSurfacedPerElement` — two `commodityOption` elements in different namespaces;
   `getNamespaceURI()` returns each element's real namespace in order (issue 6). Confirms StAX
   feeds the already-namespace-aware `SubstitutionMap` natively — no `RoutingInput.UNKNOWN`
   fallback. (Spike uses synthetic namespace URIs purely to exercise the mechanism.)
3. `documentOrderPreservedForInterleavedRepeats` — `<a/><b/><a/><b/>` reads back in exact
   document order, not collapsed into a map (issues 2/5; required by the matcher's SEQUENCE
   handling).

**Friction:** project compiles to **Java 8 bytecode**, so `List.of(...)` is unavailable — used
`Arrays.asList(...)`. (Same constraint applies to all Step 1 code.) Checkstyle clean.

**Verdict: the two hardest issues and the collision are de-risked. No reason to stop —
Step 1 is viable.**

### Step 0.4 — Config completeness review ✅

Reviewed the **real production configs** (no regeneration needed — they already exist), in
`rosetta-models/bnpp/rosetta-source/src/main/resources/xml-config/`:
- `fpml-5-13-confirmation-xml-config.json` — **7,756 lines** (largest available; FpML 5.13)
- `fiml-5-4-xml-config.json` — **7,092 lines** (the BNPP FiML config the plan names)

#### Field coverage (occurrences — FpML / FiML)

| Config field | FpML | FiML | Covers criterion | Verdict |
|---|---|---|---|---|
| `xmlRepresentation` (ATTRIBUTE/ELEMENT/VALUE/VIRTUAL) | 857 | 835 | 5, 7 | ✅ present, pervasive |
| `xmlElementName` + `xmlElementFullyQualifiedName` (name **+ namespace**) | 292 | 220 | 1, 6, 10 | ✅ namespaces carried per element |
| `abstract` | 292 | — | type inference | ✅ |
| `substitutionGroup` | 174 | 146 | 3 | ✅ |
| `xmlAttributes` (constant attrs incl. schemaLocation/prefixes) | 122 | 78 | 6 | ✅ |
| `xmlName` (per-attribute name override) | 96 | 98 | 7 | ✅ |
| `namespace` | 24 | 24 | 6 | ✅ type/prefix-level only |
| `enumValues` | 4 | 8 | 8 | ⚠️ present but sparse |
| **`contentModel`** | **2** | **2** | 4 | ⚠️ **only conflicting types** |

#### Key findings
- **The Step 1/2 split assumption is empirically confirmed.** `contentModel` is emitted for
  exactly **2 types per config**: FiML → `tradeIdentifier`, `fxTargetKnockoutForward`; FpML →
  `fxTargetKnockoutForward` (+ 1). Everything else carries no content model, so the Step 1
  binder must take structure from bean declaration order (as the plan's design constraint
  states). Notably `tradeIdentifier` is itself the issue-3 fixture.
- **Namespaces for *elements* are fully present** (`xmlElementFullyQualifiedName`), so criterion
  6 (the namespace-aware substitution path) needs **no config change** — confirms the plan.
- Criteria **1, 3, 5–11** are expressible from the existing config. Criterion 4 disambiguation
  is only as complete as the 2 content models (fine for Step 1's scope).

#### Genuine gaps → Step 2 inputs (NOT Step 1 blockers)
- **Content models only for conflicting types** → full config-driven structure = **Section 2-A**
  (and the latent issue 2 / issue-3 cardinality clash).
- **No attribute-level namespace field** (the 24 `namespace` entries are type/prefix-level, not
  per-attribute) → **Section 2-B**.
- **No `default` / `fixed` / `nillable`** fields anywhere → **Section 2-C**.
- `enumValues` is sparse — verify XML enum mapping uses `displayName` vs enum `name()` during
  Step 2 (carry-forward from sub-step 2).

### Step 0.5 — Harvest production fixtures ✅

All located in **`rosetta-models/bnpp/`** (the BNPP repo under the working dirs). Type
definitions live under `rosetta-source/src/main/rosetta/`:

| Fixture | Criterion / issue | Location | Notes |
|---|---|---|---|
| `RepoTransactionLeg` | 13 / issue 1 | `fiml-repo-type.rosetta:573` | element `id RepoLegId (0..*)`; the colliding attribute `id` is the meta/key id attribute |
| `Transfer` / `SecurityTransfer` | 13 / issue 1 | `fiml-repo-type.rosetta:622` (`SecurityTransfer`); `regulation-sec-rewrite-trade-type.rosetta` | same attr+element `id` pattern |
| `TradeIdentifier` | 14 / issue 3 (routing) | `consolidated-fimlextension-type.rosetta:219` (extends `fpml.consolidated.doc.TradeIdentifier`) | has `tradeId`; **carries a `contentModel` in the FiML config** |
| `CommodityEuropeanExercise` | issue 3 cardinality (Section 2-A) | `consolidated-com-type.rosetta:2928` | `expirationDate AdjustableOrRelativeDate (0..*)` — the unbounded-across-layers case |
| `TradeUnderlyer2` | 15 / issue 5 | `consolidated-generic-type.rosetta` (used at `consolidated-reg-fpmlreporting-product-type.rosetta:298`) | `referenceEntity` collision with `underlyingAsset` substitute |
| `environmentalPhysicalLeg` / `commoditySwapLeg` | 16 / issue 6 | `mapping-fpml-contribution-synonym.rosetta`; `consolidated-com-type.rosetta` | namespace-aware substitution |
| `commodityOption` | 16 / issue 6 | `consolidated-com-type.rosetta` | FiML variant shadows FpML's |

#### Sample document (criterion 16 round-trip)
`rosetta-source/src/main/resources/ingest/input/bnpp-transactions-commodities-emissions-citadel/`
`fiml-emissions-forward-ukallowance-new-schema.xml` (216 lines).
- Root `<FiML>`; default ns `http://www.bnpparibas.com/2012/FiML-5`, also pulls in FpML
  `http://www.fpml.org/FpML-5/recordkeeping` in nested scope — exactly the mixed-namespace case.
- Contains `<fiml:environmentalPhysicalLeg>` (×1) **and** `<fiml:schedule>` (×1) — the property
  the issue-6 workaround loses today. This is the primary criterion-16 fixture.
- `commodityOption`/`commoditySwapLeg` are **not** in this particular sample; the sibling
  `fiml-emissions-forward-euallowance-new-schema.xml` and `fiml-trade-*-Environmental-Emissions-EUAE.xml`
  in the same dir are candidates for those.
- Sibling configs `fpml-5-13-confirmation` / `fpml-5-13-recordkeeping` xml-config.json sit
  alongside the FiML config for the FpML-typed fixtures.

**Note:** these are production types in a separate repo, not yet test fixtures in `rune-common`.
Step 6 (regression tests for criteria 13–17) will need to copy minimal cut-downs of
these `.xml` samples + the relevant generated types into `common/src/test/resources/`.

---

## Step 1 — Introspection layer — ✅ COMPLETE (2026-06-21)

| Sub-step | What | Owner | Status |
|---|---|---|---|
| 1.1 | `AttributeBinding` value class | Sonnet (main) | ✅ |
| 1.2 | `TypeBinding` value class | Sonnet (main) | ✅ |
| 1.3 | `RuneTypeIntrospector.introspect()` | Sonnet (main) | ✅ |
| 1.4 | `RuneTypeIntrospectorTest` (10 cases) | Sonnet (main) | ✅ |

**Step 1 exit status: COMPLETE.** All 10 tests pass; checkstyle clean; full build clean.

### Step 1.1–1.3 — Production files

All under `common/src/main/java/com/regnosys/rosetta/common/serialisation/xml/stax/introspect/`:

| File | Role |
|---|---|
| `AttributeBinding.java` | Immutable value: one attribute's XML binding plan (logical name, getter, setter/adder, cardinality, value type, XML name, representation, element ref) |
| `TypeBinding.java` | Immutable value: one type's complete XML binding plan (type + builder refs, attribute list, XML element name, namespace, constant attrs, content model, abstract flag) |
| `RuneTypeIntrospector.java` | Main class: `introspect(Class<?>, RosettaXMLConfiguration) → TypeBinding` |

### Step 1.4 — Test file

`common/src/test/java/com/regnosys/rosetta/common/serialisation/xml/stax/introspect/RuneTypeIntrospectorTest.java`
— 10 test cases covering:

| Test | What it covers |
|---|---|
| `basicAttributeOrder` | `Document`: attr order, XML name override from config |
| `measureAttributeOrderAndRepresentation` | `Measure`: VALUE + ATTRIBUTE representations |
| `multiCardinalityAttribute` | `MulticardinalityContainer`: `@Multi` → adder wired, no setter |
| `virtualRepresentation` | `Party`: both attrs are VIRTUAL |
| `inheritanceAttributeOrder` | `DocumentExtension extends Document`: parent attrs first, then child |
| `getTypeExclusion` | `TypeWithTypeElement`: `getType()` excluded; `_getType()` getter included |
| `typeLevelXmlMetadata` | `Camel`: `xmlElementName`, FQN-derived namespace |
| `abstractType` | `Fish`: `isAbstract()` is true |
| `noConfigDefaults` | `Animal`: no config entry → ELEMENT representation, logical name = XML name |
| `animalAttributeRepresentation` | `Animal.name`: ATTRIBUTE representation from config |

### Key design decisions

**Builder hierarchy traversal**: Uses `@RuneDataType.builder()` (falling back to `@RosettaDataType`) to get the concrete builder impl class; walks `getSuperclass()` collecting levels whose declaring class is a Rune type (`@RuneDataType` or `@RosettaDataType`). Reverses to root-to-leaf order. This avoids needing to navigate the type interface hierarchy (which `getSuperclass()` cannot traverse for interfaces).

**Attribute declaration order**: `getDeclaredMethods()` does not guarantee source order on Java 9+. `getDeclaredFields()` DOES (JVM spec preserves field order from the class file). We build a field-name → position map per builder level and sort the filtered getters by field position.

**Bridge method exclusion**: Java compiler generates synthetic bridge methods for covariant return-type overrides; these bridge methods carry the same annotations (`@RosettaAttribute`, `@Accessor`) as the real method. Added `m.isBridge()` check as the first guard in `isAttributeGetter`.

**Value-type unwrapping**: Builder getters return `Foo.FooBuilder` (a `RosettaModelObjectBuilder`). When `RosettaModelObjectBuilder.isAssignableFrom(returnType)`, the value type is `returnType.getDeclaringClass()` (i.e., `Foo.class`).

**Multi-cardinality**: `@Multi` on the getter → adder lookup (`add<Name>(ValueType)` via `getMethods()`), no setter; single → setter lookup (`set<Name>(ValueType)`). Setter is found using `isAssignableFrom` so the builder-typed parameter matches the unwrapped value type.

---

## Step 2 — Scalar & value conversion — ✅ COMPLETE (2026-06-24)

| Sub-step | What | Owner | Status |
|---|---|---|---|
| 2.1 | `StaxScalarConverter` production class | Sonnet (main) | ✅ |
| 2.2 | `StaxScalarConverterTest` (23 cases) | Sonnet (main) | ✅ |

**Step 2 exit status: COMPLETE.** All 23 tests pass; checkstyle clean.

### Step 2.1 — Production file

`common/src/main/java/com/regnosys/rosetta/common/serialisation/xml/stax/convert/StaxScalarConverter.java`

| Scalar type | `toXmlString` | `fromXmlString` |
|---|---|---|
| `String` | identity | identity |
| `BigDecimal` | `toPlainString()` (no sci notation) | `new BigDecimal(text)` |
| `Integer` / `int` | `toString()` | `Integer.parseInt` |
| `Boolean` / `boolean` | `toString()` | `Boolean.parseBoolean` |
| `LocalTime` | `ISO_TIME` + UTC offset (`HH:mm:ssZ`) | strip offset from `OffsetTime`, fallback `LocalTime.parse` |
| `ZonedDateTime` | `ISO_LOCAL_DATE_TIME` if Unknown zone; else `ISO_ZONED_DATE_TIME` | 5-format cascade (see below) |
| `Date` (Rune) | `date.toString()` → ISO date | `Date.parse(text)` |
| `Enum` | config override → `toDisplayString()` → `toString()` | config reverse → `fromDisplayName()` → `toString()` → `name()` |

**ZonedDateTime 5-format cascade (ported verbatim from `RosettaXMLModule`):**
1. `ISO_ZONED_DATE_TIME` (full, with zone ID)
2. `ISO_OFFSET_DATE_TIME` → `toZonedDateTime()`
3. `ISO_LOCAL_DATE_TIME` → `atZone(UNKNOWN_ZONE)`
4. Date + offset (3 offset patterns: `+01:00`, `+0100`, `+01`)
5. `ISO_LOCAL_DATE` → midnight `atStartOfDay(UNKNOWN_ZONE)`

**Enum serialization priority:**
1. Config `enumValues` map keyed by `RosettaEnumValue.value()` (logical enum name, e.g. `"METER"`)
2. `toDisplayString()` via reflection (generated enums always have this)
3. `toString()` fallback

**`UnknownZoneProvider` registration:** `static` block registers it once if not already present, exactly as in `RosettaXMLModule`.

### Step 2.2 — Test file

`common/src/test/java/com/regnosys/rosetta/common/serialisation/xml/stax/convert/StaxScalarConverterTest.java`
— 23 test cases covering all types. Time/ZonedDateTime cases mirror `XmlSerialisationTest.testTime*` / `testZonedDateTime*` exactly:

| Test group | Cases |
|---|---|
| String, BigDecimal, Integer, Boolean | round-trip + edge cases |
| Rune `Date` | `2026-05-09` round-trip |
| `LocalTime` | serialize, deserialize (no TZ, UTC Z, +02:00 offset) |
| `ZonedDateTime` | serialize Unknown-zone, + all 5 deserialize formats |
| `Enum` with config (`UnitEnum`) | serialize + deserialize with config override |
| `Enum` without config (`SnakeDeadlinessEnum`) | serialize + deserialize via `toDisplayString`/`fromDisplayName` |

---

## Step 3 — Serializer (writer) — ✅ COMPLETE (2026-06-24)

| Sub-step | What | Owner | Status |
|---|---|---|---|
| 3a | `StaxWriter` core + root handling | Sonnet sub-agent | ✅ |
| 3b | Substitution groups on write | Sonnet sub-agent | ✅ |
| 3c | VIRTUAL/unwrapped types + full suite green | Sonnet sub-agent | ✅ |

**Step 3 exit status: COMPLETE.** Full `common` module: **262 tests pass, 0 failures, 3 skipped** (pre-existing `@Disabled`). Checkstyle clean across all three sub-steps.

**Deliverables:**
- `StaxWriter.java` — pure StAX serializer with ELEMENT/ATTRIBUTE/VALUE/VIRTUAL/substitution support
- `StaxWriterTest.java` (4 tests), `StaxWriterSubstitutionTest.java` (5 tests), `StaxWriterVirtualTest.java` (2 tests)

**Key implementation notes for Step 4:**
- The writer uses `((RosettaModelObject) obj).getType()` to get the interface class for introspection (immutable impls don't carry `@RuneDataType`).
- Substitution group resolution: `attr.getElementRef().isPresent()` → look up concrete type's `TypeBinding.getXmlElementName()` via the introspector.
- VIRTUAL handling uses a `writeChildAttributes` helper that writes children at the parent's depth with no wrapper element — Step 4 will need the mirror-image read logic.

### Step 3a — Basic emission + root handling ✅

**Files created:**
- `common/src/main/java/com/regnosys/rosetta/common/serialisation/xml/stax/write/StaxWriter.java` — production StAX serializer
- `common/src/test/java/com/regnosys/rosetta/common/serialisation/xml/stax/write/StaxWriterTest.java` — 4 tests

**Tests (all pass):** `testDocumentSerialisation`, `testTopLevelExtensionSerialisation`, `testElementNamedTypeSerialisation`, `testTimeSerialisation`.

**Key implementation decisions:**

- **Type resolution:** Immutable impl classes (e.g. `TopLevel.TopLevelImpl`) don't carry `@RuneDataType` — only interfaces do. Fixed by calling `((RosettaModelObject) obj).getType()` to obtain the interface class before calling `introspector.introspect(...)`.
- **Getter invocation:** `AttributeBinding` getters come from the builder impl class. Serializing immutable impls (which share the same interface) required a try/catch fallback that re-looks up the method by name on the actual object's class.
- **Pretty-print algorithm:** `boolean[] hasChildElement` array indexed by depth. Before writing a child element at depth `d`, sets `hasChildElement[d] = true`. Before writing `</tag>`, checks `hasChildElement[depth]` to decide whether to emit `\n` + indent. A trailing `\n` is appended after the root element to match fixture files.
- **Namespace handling:** Constant `xmlAttributes` entries with key `"xmlns"` → `writeDefaultNamespace()`; `"xmlns:prefix"` → `writeNamespace(prefix, uri)` + record in local `prefixToNs` map. Extra root attrs with colon (e.g. `"xsi:schemaLocation"`) → look up namespace from `prefixToNs`, call `writeAttribute(namespaceUri, localName, value)`. Woodstox emits the registered prefix automatically.
- **VIRTUAL attributes:** Skipped (placeholder) — handled in 3c.
- All 4 test fixtures matched byte-for-byte on first run.

### Step 3b — Substitution groups on write ✅

**Files modified/created:**
- `StaxWriter.java` — added `resolveElementName(AttributeBinding, Object)` helper; applied in both single and multi-cardinality ELEMENT branches for `isRosettaModelObject()` values
- `common/src/test/java/com/regnosys/rosetta/common/serialisation/xml/stax/write/StaxWriterSubstitutionTest.java` — 5 tests

**Tests (all pass):** `testSubstitutionGroupSerialisation`, `testMultiCardinalitySubstitutionGroupSerialisation`, `testSubstitutionGroupLegacyV2Serialisation`, `testMultiCardinalitySubstitutionGroupLegacyV2Serialisation`, `testSubstitutionGroupLegacyV1Serialisation`.

**Key implementation decisions:**

- `resolveElementName(attr, value)` checks `attr.getElementRef().isPresent()` and `value instanceof RosettaModelObject`. If both true, calls `introspector.introspect(((RosettaModelObject) value).getType(), config).getXmlElementName()` to get the substituted element name. Otherwise returns `attr.getXmlName()`.
- `AttributeBinding.getElementRef()` already handles legacy V1/V2 config formats via the fallback to `getSubstitutionGroup()` in `RuneTypeIntrospector` — no extra logic needed in the writer for legacy.
- Legacy configs are built in the test class using the same transformation logic as `XmlSerialisationTest.getLegacyV1/V2RosettaXMLConfiguration()`.
- All 5 fixtures matched byte-for-byte on first run; 0 regressions in `StaxWriterTest`.

### Step 3c — VIRTUAL/unwrapped types + full suite green ✅

**Files modified/created:**
- `StaxWriter.java` — added step 6 in `writeObject` for VIRTUAL representation; added `writeChildAttributes(virtualValue, virtualTypeBinding, writer, depth, prettyPrint, hasChildElement)` helper (~85 lines)
- `common/src/test/java/com/regnosys/rosetta/common/serialisation/xml/stax/write/StaxWriterVirtualTest.java` — 2 tests

**Tests (all pass):** `testVirtualAttributes`, `testMultiCardinalitySerialisation`.

**Key implementation decisions:**

- `writeChildAttributes` accepts the parent's `depth` and `hasChildElement[]` array. No depth change occurs (no element is started for VIRTUAL), so all children are written at `depth+1` from the parent's perspective, and `hasChildElement[depth]` is set to `true` when any child element is written — ensuring the parent's closing tag gets its `\n` + indent.
- VIRTUAL recursion: `writeChildAttributes` handles `VIRTUAL` attributes by calling itself recursively at the same `depth`, supporting arbitrarily nested VIRTUAL wrappers.
- Loop separation: main `writeObject` processes ATTRIBUTE/VALUE/ELEMENT first; VIRTUAL last (step 6). `writeChildAttributes` uses a single switch over all four representations.
- Multi-cardinality inside VIRTUAL (e.g. `partyId` list inside `PartyModel` VIRTUAL): handled by the `ELEMENT + isMulti()` branch of `writeChildAttributes`.
- Both fixtures matched byte-for-byte on first run.

---

## Step 4 — Deserializer (reader) — ✅ COMPLETE (2026-07-27)

| Sub-step | What | Owner | Status |
|---|---|---|---|
| 4a | `StaxReader` core + root inference + VIRTUAL + pruning | Sonnet (main) | ✅ |
| 4b | Substitution groups + polymorphism on read | Sonnet (main) | ✅ |
| 4c | Content-model disambiguation | Opus (main) | ✅ |
| 4d | Multi-cardinality accumulation + full suite green | Sonnet (main) | ✅ |

### Step 4a — Basic stream → builder + attribute/element collision fix ✅ (2026-06-28)

**Files created:**
- `common/src/main/java/com/regnosys/rosetta/common/serialisation/xml/stax/read/StaxReader.java` — production StAX deserialiser
- `common/src/test/java/com/regnosys/rosetta/common/serialisation/xml/stax/read/StaxReaderTest.java` — 18 tests

**Tests (all pass):**
`testDocumentDeserialisation`, `testTopLevelExtensionDeserialisation`, `testPrunesEmptyNestedObject`,
`testDateAttribute`, `testMeasureValueAndAttribute`, `testTimeDeserialisation`,
`testTimeDeserialisationWithoutTimezone`, `testTimeDeserialisationWithTimeOffset`,
`testZonedDateTimeWithUnknownTimezone`, `testZonedDateTimeWithUnknownTimeAndUnknownTimezone`,
`testZonedDateTimeWithUnknownTimeAndZuluTimezone`,
`testZonedDateTimeWithUnknownTimeAndStandardOffsetTimezone`,
`testZonedDateTimeWithUnknownTimeAndCompactOffsetTimezone`,
`testZonedDateTimeWithUnknownTimeAndShortOffsetTimezone`,
`testElementNamedTypeDeserialisation`, `testMultiCardinalityDeserialisation`,
`testVirtualAttributeDeserialisation`, `testAttributeAndElementSameLocalNameAreDistinct`

**Full module: 280 tests pass, 0 failures, 3 skipped.**

**Key implementation decisions:**

- **Attribute/element distinction (criterion 13 fix):** XML attributes are read from
  the START_ELEMENT token via `reader.getAttributeCount()` / `getAttributeValue()`.
  Child elements are routed in the child-event loop (START_ELEMENT events). The two paths
  never collide — different StAX APIs.
- **Root-element type inference:** `inferTypeFromRootElement` scans `config.getTypeConfigMap()`
  for types whose `xmlElementName` matches the root local name, then checks
  `hintType.isAssignableFrom(candidate)` — ports `RosettaXmlMapper.getTypeFromRootElementName`.
- **Scalar types at root level:** detected via `isScalarType` (no `@RuneDataType`/
  `@RosettaDataType`); read via `getElementText()` + converter (handles `ZonedDateTime`).
- **VIRTUAL (one level deep):** child elements not matched by direct ELEMENT bindings are
  searched in VIRTUAL types' bindings. Virtual builders are created lazily via
  `getOrCreateVirtualBuilder`; applied after the child-event loop via
  `((RosettaModelObjectBuilder) vBuilder).build()` + parent setter.
- **Post-deserialisation pruning:** `pruneObject` calls `toBuilder().prune().build()`,
  porting `RosettaXmlMapper.pruneObject`.
- **`getElementText()` footgun:** after the call the reader is on END_ELEMENT of the
  element; the outer loop's `reader.next()` advances past it correctly. `readObject`
  has the same contract — returns on END_ELEMENT.
- **Builder instantiation:** `binding.getBuilderType().getDeclaredConstructor().newInstance()`
  (inner classes of interfaces are implicitly static; no enclosing-instance needed).
- **Builder invocation:** `((RosettaModelObjectBuilder) builder).build()` — avoids
  reflection method lookup; all generated builders implement `RosettaModelObjectBuilder`.
- Java 8 compatible: no `List.of`, no `var`.

**Not yet covered (Step 4b):** substitution groups, `@type`-driven polymorphism.
**Not yet covered (Step 4c):** content-model disambiguation.
**Not yet covered (Step 4d):** repeated unwrapped groups (issue 7).

### Step 4b — Substitution groups + polymorphism on read ✅ (2026-07-01)

**Files created:**
- `common/src/main/java/com/regnosys/rosetta/common/serialisation/xml/stax/read/SubstitutionResolver.java`
- `common/src/test/java/com/regnosys/rosetta/common/serialisation/xml/stax/read/StaxReaderSubstitutionTest.java` — 7 tests

**Files modified:**
- `StaxReader.java` — `handleChildElement` now delegates to a new `resolveElementMatch` helper
  that returns an `ElementMatch(AttributeBinding, Class<?> concreteType)` pair; `applyChildElement`
  takes the resolved `concreteType` instead of always using `attr.getValueType()`.

**Tests (all pass):** `testSubstitutionGroupDeserialisation`,
`testMultiCardinalitySubstitutionGroupDeserialisation`,
`testMultiCardinalitySubstitutionGroupLegacyV2Deserialisation`, `testPolymorphicDeserialisation`,
`testPolymorphicReplacementDeserialisation`,
`testPolymorphicReplacementDeserialisationThroughVirtualWrapper`,
`testNamespaceAwareSubstitutionResolvesLocalNameCollision`.

**Full module: 287 tests pass, 0 failures, 3 skipped.** Checkstyle clean.

**Key design: `SubstitutionResolver` is the read-side mirror of the Jackson-era mechanism.**
`RosettaXMLAnnotationIntrospector#findSubstitutionMap` built a Jackson `SubstitutionMap` keyed
by `JavaType`, consulted at deserialize time via `SubstitutedMethodProperty.getActualType()`
reaching into `FromXmlParser.getStaxReader().getName()` — a documented Jackson-era back door
into the StAX namespace the JSON-shaped property model had already discarded. `SubstitutionResolver`
builds the equivalent index directly from `RosettaXMLConfiguration` (no Jackson dependency) and
resolves plain `Class<?>` candidates, since the StAX reader already has the real namespace state
natively — no back door needed.

**Resolution order (mirrors `SubstitutedMethodProperty` exactly):**
1. Exact match on `(namespace URI, local name)` — the namespace-aware path (issue 6 / criterion 16).
2. Local-name-only fallback (single unambiguous candidate, or legacy configs with no namespace).

**Group-membership algorithm (mirrors `RosettaXMLAnnotationIntrospector.buildSubstitutionLogicIndexes`
+ `populateSubstitutionMapFor*`):**
- `elementIndex`: `Map<String fqn, ModelSymbolId>` — catches the case where the group head element
  itself is concrete.
- `substitutionGroupIndex`: `Map<String group, List<ModelSymbolId>>` — direct members.
- Transitive walk: each member's own `xmlElementFullyQualifiedName` becomes the next group key,
  so multi-level chains resolve (`fish` substitutes `animal`; `shark`/`salmon` substitute `fish`).
  Abstract types (e.g. `fish`) are excluded from the final candidate list but still relay the walk.
- Legacy V1 `substitutionFor` (deprecated direct `ModelSymbolId` back-reference) is also consulted,
  keyed off the attribute's statically declared head type (e.g. `Animal.class`), mirroring
  `lookupLegacySubstitutionsForType`.

**`StaxReader` integration:**
- `resolveElementMatch(childLocalName, childNamespaceURI, binding)` is the single chokepoint for
  child-element routing. It checks direct (non-`elementRef`) ELEMENT bindings first (by local
  name only — the config has no per-attribute namespace for direct elements, a Section 2-B gap),
  then `elementRef` bindings second, resolving the polymorphic concrete type via
  `SubstitutionResolver`.
- Used both for the root type's own bindings and (already-existing) one-level-deep VIRTUAL
  bindings, so substitution inside a VIRTUAL wrapper (e.g. `WrappedAnimalContainerModel.animal`)
  works with no extra code.
- `applyChildElement` now takes the resolved `concreteType` and recurses via
  `readObject(reader, concreteType)` for `RosettaModelObject` values — this is what makes
  `@type`-driven polymorphism work (e.g. `<snake>` resolves to `com.rosetta.test.Snake`,
  `<ext:snake>` resolves to `com.rosetta.extension.test.Snake`, purely from element name +
  namespace, no `@type` XML attribute needed — verified no such attribute mechanism exists
  in the codebase; the plan's "`@type`-driven polymorphism" bullet refers to this
  element-name-driven resolution, matching the `testPolymorphic*` test names it was written for).

**Test fixtures — all synthetic, no BNPP repo dependency:**
- Substitution round-trip: `AnimalContainer`/`Zoo` reading `expected/substitution-group.xml`,
  `expected/substitution-group-multi.xml` (goat/cow/shark/salmon — exercises the full transitive
  `fish` chain) and `expected/substitution-group-multi-legacy.xml` against a legacy V2 config
  (goat/cow only — the V2 chain-break is a faithful port of the Jackson-era limitation, not
  a regression: confirmed by comparing against the pre-existing write-side V2 test fixture).
- Polymorphism + criterion 16: reuses the *existing* Jackson-test input fixtures
  `input/polymorphic.xml`, `input/polymorphic-replacement.xml`,
  `input/polymorphic-replacement-token-buffer-parser.xml`,
  `input/polymorphic-replacement-ambiguous-choice.xml`. The last one is the key criterion-16
  proof: `com.rosetta.test.Camel` (`urn:my.schema/camel`) and `com.rosetta.extension.test.Camel`
  (`urn:my.extension/camel`) both substitute the same group with the same local name `camel` —
  exactly the issue-6 shape (FiML vs FpML `commodityOption`) — and the exact-namespace-match
  correctly resolves `<ext:camel>` to the extension type, not "first wins".

**Criterion 15 boundary (documented, not a bug):** the acceptance criterion's specific shape —
a *direct* element and a *substitution* candidate sharing the *same* local name in one type
(`TradeUnderlyer2.referenceEntity`) — needs content-model position to disambiguate when the
config carries no per-attribute namespace for the direct side. That is Step 4c's job; this step
delivers the substitution-resolution half of issue 5 (correct routing when names don't collide,
and correct namespace-based routing when they do and namespace is known on the substitution
side). Full closure + a real `TradeUnderlyer2` regression test lands in Step 6.

**Carry-forward for Step 4c:** `resolveElementMatch` in `StaxReader` is the intended integration
point — content-model routing should slot in as an additional resolution phase without changing
`handleChildElement`/`applyChildElement`'s call shape (`ElementMatch(AttributeBinding, Class<?>)`).

### Step 4c — Content-model disambiguation ✅ (2026-07-27)

**Files created:**

| File | Role |
|---|---|
| `.../xml/deserialization/ContentModelRouter.java` | Public, Jackson-free facade over the (unchanged) `XMLContentModelMatcher`, plus the ported lenient-recovery policy |
| `.../xml/stax/read/XmlCursor.java` | 10-method pull-parser interface the reader consumes |
| `.../xml/stax/read/StaxCursor.java` | `XmlCursor` over a live `XMLStreamReader` (pass-through) |
| `.../xml/stax/read/BufferedSubtree.java` | Captures one element subtree as a replayable event list + its replay cursor |
| `.../xml/stax/read/VirtualPathAssembler.java` | StAX-native port of `VirtualPathBuilderHelper` |
| `.../xml/stax/read/BuilderAccess.java` | Shared builder reflection (`newBuilder` / `apply` / `findByLogicalName`) |

**Files modified:** `StaxReader.java` (reads via `XmlCursor`; new routed read path).

**Test file:** `.../xml/stax/read/StaxReaderContentModelTest.java` — 15 tests.
**Test resource:** `serialisation/xml/xml-config/content-model-namespace-xml-config.json`.

**Full module: 325 tests pass, 0 failures, 3 skipped** (baseline before this step was 310;
the jump from the 287 recorded at Step 4b is the merge from `main`). Checkstyle clean;
`mvn clean install` green.

#### Reuse boundary: the matcher is untouched

`XMLContentModelMatcher` is reused **byte-for-byte unchanged**, exactly as the plan requires.
It is package-private, so rather than widening its visibility (or that of `RoutingInput` and
`RoutingResult`), a new public `ContentModelRouter` sits alongside it in the same package and
translates between the two worlds:

- **in:** `ContentModelRouter.Element(localName, namespaceUri)` per child, in document order;
- **out:** `Route.getPath(childIndex)` → Rosetta property path, and
  `Route.getOccurrenceKey(childIndex)` → opaque occurrence identity.

That keeps the Jackson-era `RoutingInput.Namespace.UNKNOWN` state off the StAX side entirely
(the StAX reader always knows the namespace) and gives Step 6 a class worth keeping when the
Jackson deserializers are deleted.

#### Read pipeline

1. `routerFor(type, binding)` — returns a cached `ContentModelRouter` for the type, or `null`
   when there is no `contentModel` or `requiresRouting` is false. Non-ambiguous types keep the
   Step 4a/4b streaming path with **zero buffering overhead**.
2. `readRoutedObject` buffers every child element (name + namespace URI + `BufferedSubtree`) in
   document order, accumulating VALUE text alongside.
3. The child sequence goes to `ContentModelRouter.route(...)`, which filters to the names the
   content model mentions (or all of them when the model contains an `ANY`), matches strictly,
   and falls back to lenient recovery on failure.
4. Each child is bound by its routed path: multi-segment → `VirtualPathAssembler`;
   single-segment → the named attribute directly (by **logical** name, which may differ from the
   XML name that got routed there); no path → plain name matching via the existing
   `handleChildElement`, so elements outside the content model (e.g. `barrier` on
   `FpmlFxTargetKnockoutForward`) still bind normally.

#### Key design decisions

**`XmlCursor` instead of implementing `XMLStreamReader`.** Routing cannot decide where a child
belongs until it has seen all of them, and a StAX cursor cannot rewind — so children must be
buffered and replayed. Replaying through a hand-written `XMLStreamReader` would have meant ~30
stub methods; instead `StaxReader` now reads through a 10-method `XmlCursor` with two
implementations. One read path serves live XML and buffered subtrees, so nested routing,
substitution, VIRTUAL handling and scalar conversion work inside a buffered subtree with no
duplicated logic. Only private signatures changed.

**Buffering keeps XML shape, unlike `TokenBuffer`.** `BufferedSubtree` is a flat event list
retaining element names, namespace URIs, attributes and document order. Jackson's `TokenBuffer`
flattens XML onto a JSON-shaped stream and drops the namespace context — the root cause of
issue 6 and of the `UNKNOWN` fallback.

**Occurrence identity by `equals`, not `toString`.** The Jackson path stringified the matcher's
`OccurrenceKey` to group leaves into one virtual object; since `OccurrenceFrame` has no
`toString`, that string was identity-hash-based. `Route` exposes the key as an opaque `Object`
and `VirtualPathAssembler` compares with `Objects.equals`, which is strictly more accurate.
The "same leaf twice in one occurrence → start a new occurrence" rule is preserved, using a
fresh sentinel key that no later assignment can join (equivalent to the old `key + "#" + n`).

**Introspector-driven virtual walk.** `VirtualPathAssembler` resolves each path segment through
`RuneTypeIntrospector` (logical name → `AttributeBinding` → setter/adder + value type) instead of
guessing `addX`/`setX` by reflection as `VirtualPathBuilderHelper` did. Leaf values are read back
via a `LeafValueReader` callback into `StaxReader`, so a routed leaf is deserialised by exactly
the same code as any other element.

**Lenient policy is duplicated, deliberately.** The reorder → drop-un-routable → give-up cascade
was ported into `ContentModelRouter` rather than refactored out of
`XMLContentModelDisambiguatingDeserializer`. Step 4c does not touch the Jackson engine (which
Step 6 deletes), and the warnings observed in the test run confirm the same three recovery paths
fire on the same documents.

#### Tests

The 12 deserialisation cases of `XmlContentModelDisambiguationTest` are ported one-for-one, so
the StAX binder is held to the Jackson engine's exact routing behaviour: `FpmlTradeIdentifier`
virtual/direct branches, both `FpmlFxTargetKnockoutForward` examples, multi-leaf occurrences,
ALL, ANY, multi-layer virtual paths, mixed nested choices, and the three lenient cases (missing
required element, genuinely ambiguous, misordered-but-valid).

Three namespace tests have **no Jackson counterpart** — they only pass because the reader feeds
the matcher real namespace state:

| Test | Proves |
|---|---|
| `testNamespaceQualifiedContentModelRoutes` | a namespace-qualified model matches and groups by occurrence normally |
| `testWrongNamespaceIsNotRoutedByLocalNameAlone` | a foreign-namespace element is **rejected** by the model (Jackson's `UNKNOWN` matched it permissively), falling back to name binding |
| `testSameLocalNameInDifferentNamespacesRoutesToDistinctSlots` | two model branches sharing local name `value`, differing only by namespace, route to **distinct** slots — the issue-6 shape that `XMLContentModelMatcherNamespaceTest` shows Jackson reports as AMBIGUOUS |

#### Criterion status

- **Criterion 4** (SEQUENCE/CHOICE/ALL/ANY, occurrence bounds, nested multi-layer virtual
  paths): green.
- **Criteria 15/16**: the missing mechanism now exists — same-local-name elements are separated
  by content-model position **and** by real namespace. Closing criterion 15 against the
  production `TradeUnderlyer2` still requires a `contentModel` to be emitted for that type; per
  Step 0, only ~2 types per production config carry one, so that fixture-level regression test
  stays a **Section 2-A** dependency, exactly as the traceability table states.

**Carry-forward for Step 4d:** routed multi-cardinality already accumulates (each occurrence
calls the adder, verified by the Fx and multi-leaf tests). Issue 7 concerns a repeated
**unwrapped group** on a type with *no* content model, which goes through the streaming path's
`getOrCreateVirtualBuilder` — that still creates a single virtual builder per attribute and so
collapses repeats to one. That is the remaining work.

### Step 4d — Multi-cardinality accumulation + full suite green ✅ (2026-07-27)

**Files modified:**

| File | Change |
|---|---|
| `.../xml/stax/read/StaxReader.java` | New `VirtualGroupState` accumulator + `beginVirtualChild`; `getOrCreateVirtualBuilder` and `applyVirtualBuilders` reworked to build on it |
| `.../xml/stax/write/StaxWriter.java` | VIRTUAL branch of `writeObject` now branches on `attr.isMulti()`; new `writeVirtualOccurrence` helper shared by both branches |

**Test files modified/created:**

| File | Change |
|---|---|
| `.../xml/stax/read/StaxReaderTest.java` | + `testRepeatedUnwrappedGroupAccumulatesAllInstances` |
| `.../xml/stax/write/StaxWriterVirtualTest.java` | + `testRepeatedUnwrappedGroupSerialisation` |
| `.../xml/stax/read/StaxReaderContentModelTest.java` | `testWrongNamespaceIsNotRoutedByLocalNameAlone` expectation corrected from 1 → 2 occurrences (see below) |

**Full module: 327 tests pass, 0 failures, 3 skipped** (pre-existing `@Disabled`, unchanged
from Step 4c's baseline of 325 — the delta is exactly the 2 new tests added this step).
Checkstyle clean; `mvn clean install` green across both modules.

#### The bug, confirmed empirically before fixing

A scratch read of the existing `NestedContainer` / `expected/nested-container.xml` fixture
(present since Step 0/1, previously exercised only by the Jackson-era
`@Disabled // TODO` test `XmlSerialisationTest.testNestedContainerSerialisation` — itself the
plan's issue-7 fixture) showed the repeated unwrapped group `nestedContainerSequence1`
(`1..*`, VIRTUAL, no wrapper element) collapsing two occurrences into **one**, holding the
**last** occurrence's values (`c=4, d=5`) rather than accumulating both. Root cause: the
pre-4d `getOrCreateVirtualBuilder` cached exactly one builder per `AttributeBinding`, shared
across the whole read of the parent element — every subsequent `<c>`/`<d>` just kept calling
setters on the same builder.

#### Read-side fix

`StaxReader.java`'s single shared virtual builder is replaced by a `VirtualGroupState`
accumulator per VIRTUAL attribute: a builder for the occurrence currently being filled, the
**logical names** of single-cardinality child attributes already filled in it, and every
already-completed occurrence. `beginVirtualChild(virtualAttr, childAttr, virtualBuilders)`
is the single decision point: for a **multi-cardinality** VIRTUAL attribute, if a
**single-cardinality** child attribute is about to be filled a second time in the current
occurrence, that signals a new occurrence has begun — the current builder is finalised
(`build()`) into the completed list and a fresh builder started. Multi-cardinality child
attributes never trigger a rollover; they keep accumulating into the same occurrence via the
adder exactly as before (e.g. `partyId` inside the single-cardinality `PartyModel` VIRTUAL
wrapper is untouched — `beginVirtualChild`'s rollover only ever fires when
`virtualAttr.isMulti()`). `applyVirtualBuilders` applies every completed occurrence through
the ordinary `BuilderAccess.apply` adder/setter, once per occurrence — no new
builder-invocation machinery.

**Gotcha that cost the first fix attempt:** `RuneTypeIntrospector.introspect()` builds a
**fresh** `AttributeBinding` on every call — there's no caching — and the streaming path
re-introspects the virtual type on every child element. So two `AttributeBinding` instances
for the same logical child attribute across two occurrences are never `==` or `.equals()`
(no `equals()`/`hashCode()` override on `AttributeBinding` at all — identity semantics). A
`Set<AttributeBinding>` for occurrence-boundary tracking therefore never detects a repeat.
Fixed by keying the tracking set on `childAttr.getLogicalName()` (a stable `String`) instead.
This was caught by the new test failing (still 1 occurrence after the first fix attempt), not
by code inspection — worth remembering for any future StaxReader logic that compares
`AttributeBinding` instances across separate `introspect()` calls.

XML-attribute routing into VIRTUAL types (`applyXmlAttribute`'s call to
`getOrCreateVirtualBuilder`) is untouched: attributes belong to the parent START_ELEMENT and
are read once, before any child-element-driven occurrence rollover can happen, so there is
exactly one occurrence to route into by construction.

#### Write-side fix (a parallel, previously untested gap)

`StaxWriter.java`'s VIRTUAL branch (`writeObject` step 6) had the mirror-image bug, undetected
because no test exercised a multi-cardinality VIRTUAL attribute: it called
`invoke(attr, object)` and checked `instanceof RosettaModelObject`, but for a **multi**
VIRTUAL attribute the getter returns a `List`, which silently fails that check — so **nothing
was written at all** for a repeated group, not even one occurrence. Fixed by branching on
`attr.isMulti()`: the multi branch calls the new `writeVirtualOccurrence` helper once per list
item, writing each occurrence's children inline back-to-back with no wrapper element
(`<c/><d/><c/><d/>` — matches `expected/nested-container.xml` exactly); the single branch
reuses the same helper for its one value, unchanged in behaviour from Step 3c.

#### Full-suite audit

A full `common` module run (no code changes yet) confirmed the baseline was still exactly
325 passing / 3 skipped — identical to Step 4c's exit — so issue 7 was the **only**
outstanding gap; no other latent failures existed across 4a–4c to clean up.

One pre-existing Step 4c test needed its expectation corrected, not weakened:
`StaxReaderContentModelTest.testWrongNamespaceIsNotRoutedByLocalNameAlone` feeds a
`MultiLeafContainer` document in the wrong namespace, which the content-model router
correctly rejects, falling back to plain name-based binding
(`StaxReader#handleChildElement`) — the same streaming path this step fixes. Before this
step, that fallback path could only produce one collapsed occurrence, and the test's
assertion (`assertEquals(1, ...)`) captured that as expected behaviour. Since the streaming
path now accumulates repeated unwrapped groups correctly, the same fallback document now
round-trips to the correct 2 occurrences — the test (and its docstring, which explicitly
described the old "single virtual object" outcome) was updated accordingly.

#### Scope note: issue 3's cardinality-clash half remains Section 2-A

The plan's Step 4d text also mentions honouring per-layer `maxOccurs` when the same element
name spans layers with different cardinalities (e.g. `CommodityEuropeanExercise.
expirationDate`, which must stay unbounded across layers). As the traceability table and
Step 4c's notes already established, disambiguating *which layer* a given occurrence belongs
to requires a per-type content model, and per Step 0 only ~2 types per production config
carry one. Nothing in this step's scope changes that; it remains a **Section 2-A**
dependency, unchanged from prior steps' documented position. Step 4d fully closes the
multi-cardinality-*accumulation* half of issue 7 (criterion 17), which needed no content
model at all.

**Criterion 17: green.** `XmlContentModelDisambiguationTest` (14 tests) and
`XMLContentModelMatcherNamespaceTest` (5 tests) — both exercising the old Jackson engine,
unaffected by this step — remain green, satisfying the plan's stated exit bar alongside the
new StAX-side tests.

**Section 1 exit note:** Steps 1–5 (introspection, scalar conversion, writer, reader, public
entry-point wiring) are now all complete. What remains for Section 1 is Step 6 (full test-package
pass, benchmark, delete the Jackson XML classes, drop the `jackson-dataformat-xml` dependency).

---

## Step 5 — Wire into the public entry point — ✅ COMPLETE (2026-07-27)

**Files created:**

| File | Role |
|---|---|
| `.../xml/stax/RuneXmlMapper.java` | Jackson-free native entry point: `readValue(String\|Reader\|InputStream, Class)`, `writeValueAsString(...)`, `writer()`/`writerWithDefaultPrettyPrinter()` |
| `.../xml/stax/RuneXmlWriter.java` | Immutable fluent writer config (`withAttribute(...).writeValueAsString(...)`); translates `"schemaLocation"` → `"xsi:schemaLocation"` for `StaxWriter`'s `extraRootAttrs` |
| `.../xml/StaxXmlObjectMapper.java` | `ObjectMapper` facade wrapping `RuneXmlMapper`; overrides only the entry points existing callers use |
| `.../xml/StaxObjectWriter.java` | `ObjectWriter` facade wrapping `RuneXmlWriter`; package-private, constructed only by `StaxXmlObjectMapper` |

**Files modified:**

| File | Change |
|---|---|
| `RosettaObjectMapperCreator.java` | `forXML(config, classLoader)` now builds `new StaxXmlObjectMapper(config, classLoader)` directly (no more `RosettaXmlMapper`/`RosettaXMLModule`/`RosettaSerialiserFactory`/`XmlMapper.Builder`). New private constructor + `prebuilt` flag lets `create()` return a fully-built mapper as-is, skipping the generic Guava/Joda/JavaTime/mixin pipeline that's JSON-oriented and irrelevant to the StAX-backed facade |
| `.../xml/stax/write/StaxWriter.java` | Added root-level scalar support (`isScalarType` + `writeScalarRoot`) — see gap below |
| `TransformObjectMapperFactoryTest.java` | Two `assertInstanceOf(XmlMapper.class, ...)` → `assertInstanceOf(StaxXmlObjectMapper.class, ...)` (asserted the old concrete Jackson type; updated to the new one) |

**Test file created:** `.../xml/stax/RuneXmlMapperTest.java` (3 tests) — exercises `RuneXmlMapper`
directly (write→read round-trip, `readValue(Reader, Class)`, and
`writerWithDefaultPrettyPrinter().withAttribute("schemaLocation", ...)` byte-for-byte against the
same `expected/document.xml` fixture `XmlSerialisationTest`/`StaxWriterTest` use).

**Full `common` module: 330 tests pass, 0 failures, 3 skipped** (327 pre-existing + 3 new). Checkstyle
clean; `mvn clean install` green across both modules.

### Both options from the plan were built, not chosen between

The plan's Step 5.1 posed a choice: keep an `ObjectMapper`-compatible facade, or introduce
`RuneXmlMapper` and adapt `forXML(...)`. Confirmed with the user beforehand that both are possible
and did both: `RuneXmlMapper` is the clean Jackson-free API for new consumers; `forXML(...)` wraps
that same instance behind `StaxXmlObjectMapper`/`StaxObjectWriter` for existing callers. One
implementation serves both audiences — the facade has no logic of its own beyond type/exception
adaptation.

### Facade scope: audited real call sites, not just the test file

Before writing the facade, audited every production and test call site against
`RosettaObjectMapperCreator.forXML(...)` (`TransformObjectMapperFactory`, `TestPackUtils`, and the
XML test suite) to find the actual `ObjectMapper`/`ObjectWriter` surface in use, rather than guessing
at what to support. It's narrow: `readValue(String|Reader|InputStream, Class)`,
`writeValueAsString(Object)`, and
`writerWithDefaultPrettyPrinter().withAttribute("schemaLocation", ...).writeValueAsString(...)`.
`ObjectMapper`/`ObjectWriter` are concrete but not `final`, and none of those specific methods are
`final` either (confirmed via `javap`), so the facades override only those entry points and leave
everything else inherited-but-unused. `StaxObjectWriter`'s constructor calls
`super(mapper, mapper.getSerializationConfig())` to satisfy `ObjectWriter`'s protected constructor
with a real (but never subsequently read) `SerializationConfig`.

**Checked-exception mapping:** `RuneXmlMapper`/`RuneXmlWriter` throw plain `IOException`. The two
facade methods whose Jackson signature only declares `JsonMappingException`/`JsonProcessingException`
(not plain `IOException`) — `readValue(String, Class)` and `writeValueAsString(Object)` — catch and
rewrap as `new JsonMappingException(message, cause)`; `readValue(Reader/InputStream, Class)` declare
plain `IOException` already, so it passes straight through.

### The wiring itself was the acceptance test

Repointing `forXML(...)` at the new engine meant `XmlSerialisationTest`,
`XmlContentModelDisambiguationTest`, and `XmlContentModelSerializationOrderTest` (43 tests total)
started running against the StAX binder through the *same, unchanged* public entry point, with no
test-code changes beyond the two `assertInstanceOf` updates above. Ran a baseline first (full suite
green, 327/0/3 before touching anything) to isolate any regression precisely.

**One genuine gap surfaced and fixed:** `testZonedDateTimeWithUnknownTimezoneSerialisation` writes a
bare `ZonedDateTime` as the document root (no wrapping Rune type) and failed with
`IllegalArgumentException: Type java.time.ZonedDateTime has neither @RuneDataType nor
@RosettaDataType` — `StaxWriter.write` unconditionally called `introspector.introspect(...)`, which
assumes a Rune type. `StaxReader.read` already had the mirror-image `isScalarType` check for reading
a root-level scalar (Step 4a); the writer had never grown the equivalent because no pre-Step-5 writer
test exercised a root-level scalar value. Fixed by adding the same `isScalarType` check (no
`@RuneDataType`/`@RosettaDataType`) to `StaxWriter.write`, branching to a new `writeScalarRoot`
that writes `<ClassSimpleName>` + `converter.toXmlString(value)` + close tag — matches the Jackson-era
output exactly (`<ZonedDateTime>2006-04-02T15:38:00</ZonedDateTime>`). Everything else in the 43-test
XML suite passed unchanged on the first run after the swap.

### Old Jackson-XML engine is now dead code, deliberately not deleted

`RosettaXmlMapper`, `RosettaXMLModule`, `RosettaSerialiserFactory`, and the rest of the Jackson-XML
serializer/deserializer/introspector stack are no longer reachable from any production path — the
only production reference was `RosettaObjectMapperCreator.forXML(...)`, now repointed. Confirmed no
test in `common` instantiates them directly (grepped `common/src/test/java`; the only hits were
through `forXML(...)` and one stale doc-comment mention in
`XmlContentModelSerializationOrderTest`). Deleting them — and dropping the `jackson-dataformat-xml`
dependency — is explicitly Step 6's job; left in place here to keep this step's diff scoped to
wiring, per the plan's step boundaries.

## Step 6 — Full test pass, performance, cleanup — ✅ COMPLETE (2026-07-27)

| Sub-step | What | Owner | Status |
|---|---|---|---|
| 6.0 | Port content-model write ordering into `StaxWriter` (gap found greening the suite) | Opus (main) | ✅ |
| 6.2 | Criteria 13–17 regression tests (anonymous in-repo fixtures) | Opus (main) | ✅ |
| 6.3 | Benchmark old vs new engine; fix the regression it exposed | Opus (main) | ✅ |
| 6.4 | Delete the dead Jackson XML classes (20 files) | Opus (main) | ✅ |
| 6.5 | Drop `jackson-dataformat-xml` | Opus (main) | ✅ |
| 6.6 | Restore `RosettaXmlMapper` as a deprecated alias (back-compat) | Opus (main) | ✅ |
| 6.1 | Full `mvn clean install` + checkstyle green | Opus (main) | ✅ |

**Step 6 exit status: COMPLETE — and with it, Section 1.** `mvn clean install` green across both
modules: `common` **334 tests pass, 0 failures, 3 skipped** (pre-existing `@Disabled`);
`serialization` **63 pass, 0 failures, 1 skipped**. Checkstyle clean.

Test-count arithmetic from Step 5's 330: **+7** new `XmlCriteriaRegressionTest` cases, **+1** new
`XmlContentModelSerializationOrderTest` case, **+2** new `RosettaXmlMapperCompatibilityTest` cases,
**−6** deleted `StaxBinderSpikeTest` cases (the Step 0 throwaway, retired exactly as Step 0 said it
would be) = **334**.

### The old Jackson-era test suites were kept, and did the heavy lifting

Worth stating plainly, because it drove the whole step: `XmlSerialisationTest` (incl. its legacy
v1/v2 config cases), `XmlContentModelDisambiguationTest` and `XmlContentModelSerializationOrderTest`
all construct their mapper via `RosettaObjectMapperCreator.forXML(...)`, which since Step 5 returns
the `StaxXmlObjectMapper` facade. They therefore already exercise the StAX binder through the
unchanged public entry point and were **kept verbatim** — no rewrite, no porting. They are the parity
harness for this step, and one of them is what exposed the write-ordering gap below. The only
test-side reference to a deleted class was a stale doc-comment, now corrected.

`XMLContentModelMatcherNamespaceTest` and `XMLContentModelOrdererTest` were also kept: both test
classes that survive the migration (the matcher, reused byte-for-byte since 4c; the orderer, now
driven by `StaxWriter` — see below).

### 6.0 — Content-model write ordering was missing from `StaxWriter` (real gap, fixed)

Greening the suite surfaced that `StaxWriter` had **no content-model-driven ordering at all**. It
wrote ATTRIBUTE/VALUE, then all ELEMENT bindings in declaration order, then all VIRTUAL bindings
last. The Jackson engine did have it: `RosettaBeanSerializerModifier` attached an
`XMLContentModelOrderer` to `RosettaBeanSerializer`, which permuted the content-model-participating
properties into model order. **Deleting the Jackson serializer without porting this would have been
a silent output-ordering regression.**

`XmlContentModelSerializationOrderTest` passed anyway, by luck: for `FpmlTradeIdentifier` the model
order (`partyReference, accountReference?, choice`) happens to coincide with "direct elements first,
VIRTUAL last". A new test proves the gap on `FpmlFxTargetKnockoutForward`, whose `barrier` property
is **absent from the content model** and declared after the VIRTUAL choice:

```
before fix:  <linearPayoffRegion/><barrier/><constantPayoffRegion/>   (choice flushed last)
after fix:   <linearPayoffRegion/><constantPayoffRegion/><barrier/>   (model order)
```

**Fix.** `XMLContentModelOrderer` is a pure algorithm over the config's content model with zero
Jackson dependency, so rather than reimplement it, it was **kept and widened to public** (class +
constructor + two methods; body untouched) and left in the `serialization` package. That mirrors
what Step 4c did with the matcher, and leaves a clean read/write duality in the two packages the
Jackson classes vacated:

| Direction | Kept algorithm | Jackson-free facade / driver |
|---|---|---|
| read | `deserialization/XMLContentModelMatcher` | `deserialization/ContentModelRouter` → `StaxReader` |
| write | `serialization/XMLContentModelOrderer` | `StaxWriter.orderChildAttributes` |

`StaxWriter.writeObject`'s two separate passes (ELEMENT, then VIRTUAL) are now **one pass** over
`orderChildAttributes(binding, object)`. That method reproduces the Jackson serializer's exact
permute-in-place contract: only properties that the content model mentions *and* that are populated
on this instance are reordered, among the slots they already occupy; everything else — notably a
property absent from the content model, like `barrier` — keeps its position. Ordering is at
Rosetta-property granularity, so a VIRTUAL group's leaves move as one contiguous block; leaf order
*within* a group still follows the group type's own declaration order, as before.

One deliberate divergence: the Jackson version also bailed out when the type had a text (VALUE)
property, because reordering interfered with its `_textPropertyIndex` bookkeeping. That is a
Jackson-mechanics constraint with no analogue here (`StaxWriter` writes VALUE text in a separate
step), so it was not ported.

### 6.3 — Benchmark: the StAX binder was 9–30× slower; fixed to parity

This is the sub-step that paid for itself. Harness (kept out-of-tree in the session scratchpad,
since it must instantiate the now-deleted Jackson engine): rebuild the pre-Step-5 `forXML(...)`
pipeline by hand, build the current one via `forXML(...)`, and time both on the same document with
interleaved rounds after warm-up.

**First run — a serious regression:**

| Case | Read (deser) | Write (ser) |
|---|---|---|
| Zoo, 20k substitution elements (419 KB) | 4.3 → 40.3 ms (**0.11×**) | 2.9 → 85.1 ms (**0.03×**) |

**Two root causes, neither visible from the tests:**

1. **`RuneTypeIntrospector.introspect()` had no cache.** It runs full reflection
   (`getDeclaredFields()`, plus `getMethods()` per attribute for setter/adder lookup) and the binder
   calls it **once per XML element** — on both read and write paths. Reflection, not parsing,
   dominated every document.
2. **`StaxWriter.invoke` used exception-driven control flow.** The stored getter comes from the
   *builder* impl class, but serialisation runs against *immutable* impls, so
   `getter.invoke(object)` threw `IllegalArgumentException` **every single time**, and the catch
   block then re-ran `Class.getMethod`. That is one exception construction plus one reflective
   lookup per property of per element written.

**After caching both** (`introspect` memoised per (config, type); getters cached per
(getter, concrete class), with an `isAssignableFrom` fast path that avoids the lookup entirely when
the stored getter already applies). Final-state figures are old-engine-mean ÷ new-engine-mean, as a
range over two consecutive runs — above 1.0× means the StAX binder is faster:

| Case | Read (deser) | Write (ser) |
|---|---|---|
| Zoo — 20k substitution-group elements (419 KB) | 0.97–0.99× | **1.56–1.60×** |
| MulticardinalityContainer — 80k elements (1.6 MB) | 0.91–0.92× | 0.85–0.92× |
| Party — VIRTUAL unwrapped groups (509 KB) | 1.09–1.14× | 0.99–1.02× |

On the Zoo case the absolute recovery was **read 40.3 → 5.1 ms (8×)** and
**write 85.1 → 2.0 ms (42×)**. Results were stable across repeated runs. **Serialised output is
byte-identical between the two engines on all three cases** — a parity signal stronger than any
assertion in the test suite.

**Honest reading of the result:** parity overall, not a clean sweep. Four of six measurements are at
or better than Jackson (write on the substitution-heavy case is 1.6–1.8× faster); the deep-nesting
1.6 MB case remains ~8–15% slower in both directions. Caching the per-type ELEMENT/VIRTUAL attribute
list (`childAttributesOf`) recovered part of that. The plan's bar — "should match or beat the
TokenBuffer path" — is met in aggregate, with that one shape called out rather than papered over.

**Production-scale sanity check.** The largest real document available (320 KB, ~4.1k lines, mixed
two-namespace FpML/vendor-extension) on the new engine: **4.85 ms read, 2.47 ms write** (mean),
producing 490 KB of output. The old engine **could not read it at all** — it failed with
`ClassNotFoundException` on a config-referenced type absent from the model jar, on **14 of 14**
documents tried. That is a local jar/config version skew rather than a genuine Jackson defect, but
it is a meaningful robustness difference: the old engine eagerly resolves classes across the config
while the StAX binder resolves lazily, driven by what the document actually contains. It is also why
the head-to-head table above uses in-repo synthetic documents — they are the only ones both engines
can load.

### Pre-existing thread-unsafety, found and fixed

Adding caches raised the question of sharing, and the audit found the problem predated this step.
`forXML(...)` returns an `ObjectMapper` — whose documented contract is thread-safe, and which
consumers (`TransformObjectMapperFactory`, `TestPackUtils`) do cache and share — and one mapper owns
exactly one `StaxReader` and one `StaxWriter`. Yet:

| Cache | Introduced | Was |
|---|---|---|
| `StaxReader.routerCache` | Step 4c | plain `HashMap`, mutated per read |
| `SubstitutionResolver.resolvedGroups` + `elementIndex`/`substitutionGroupIndex` | Step 4b | plain `HashMap` + non-volatile lazy init |
| `RuneTypeIntrospector` bindings, `StaxWriter` orderers/getters/child-lists | Step 6 | plain `HashMap` (new) |

All are now `ConcurrentHashMap` with `putIfAbsent` publication; `SubstitutionResolver`'s lazy index
build is a proper double-checked lock over `volatile` fields, with `elementIndex` assigned last as
the guard. Every cached value is immutable, so a lost race merely recomputes an entry.

Two implementation notes:
- `Optional` is used as the cache value wherever "absent" is a meaningful cached answer
  (`routerCache`, `orderers`), because `ConcurrentHashMap` cannot store `null` — previously
  `containsKey` + a `null` value carried that meaning.
- The introspector cache is keyed on **config identity**, not equality: `RosettaXMLConfiguration`
  overrides `equals`, so hashing it by value would deep-compare a structure with thousands of
  entries on every element. A first attempt used a two-level map with a per-call key wrapper and
  measurably *lost* throughput (read 0.98× → 0.77×); it was flattened to one map plus a `volatile`
  config reference, so the hot path is one volatile read and one lookup with no allocation. If a
  caller ever passes a different config instance, the cache is dropped and rebuilt rather than
  returning a stale binding.

### 6.2 — Criteria 13–17 regression tests (anonymous in-repo fixtures)

**Files created:**

| File | Role |
|---|---|
| `serialisation/xml/rosetta/rosetta-regression-type.rosetta` | Anonymous stand-in types reproducing each broken XML shape |
| `serialisation/xml/xml-config/regression-xml-config.json` | Their XML config, incl. two content models and two namespaces |
| `.../serialisation/xml/XmlCriteriaRegressionTest.java` | 7 tests, one or two per criterion |

**Fixtures are anonymous by design.** Step 0.5 harvested the named production types, but they live in
a **private** repo and `rune-common` is public, so nothing from it may land here. The stand-ins
reproduce each broken *shape* — that is what the binder actually has to get right — and depend on
nothing outside this repository. Where a criterion needs two schemas, the fixtures follow the
convention already established in this test model: `urn:my.schema` for the base schema and
`urn:my.extension` for the vendor extension. The private production model was used only out-of-tree,
to validate the benchmark.

Every test asserts **the data that used to be lost is present**, not merely that parsing succeeds,
and (where round-tripping is meaningful) re-serialises and re-reads to prove the write side preserves
the distinction the read side made.

| Test | Criterion / issue | What it proves |
|---|---|---|
| `criterion13_attributeAndElementIdBothSurviveMultiCardinality` | 13 / 1 | attribute `id` **and** two `<id>` elements (each with its own attribute) all survive on one type |
| `criterion13_attributeAndElementIdBothSurviveSingleCardinality` | 13 / 1 | same collision with a single-cardinality element |
| `criterion14_tradeIdRoutedToExactlyOneSlot` | 14 / 3 (routing) | `tradeId` lands in the choice group only — the direct property is asserted **null**, i.e. not deserialised twice |
| `criterion14_tradeIdRoutesToDirectSlotOnTheOtherBranch` | 14 / 3 | the same element name routes to the *direct* slot on the model's other branch |
| `criterion15_substitutedNameCollidingWithDirectElementPopulatesBothSlots` | 15 / 5 | direct `referenceEntity` **and** the same-named substitution member both populate, in distinct slots, with the member resolved to its concrete type |
| `criterion16_extensionLegsResolveByNamespaceAndRetainSchedule` | 16 / 6 | extension-namespace substitute parses at all; `<ext:commodityOption>` binds to the extension type (not the base type shadowing it by local name) and **retains `schedule`**; `<base:commodityOption>` still binds to the base type |
| `criterion17_repeatedUnwrappedGroupAccumulatesAllOccurrences` | 17 / 7 | all **three** occurrences of a repeated unwrapped group accumulate |

#### Criterion 15 is now genuinely closed — with a constraint worth recording

Steps 4b and 4c both documented criterion 15 as awaiting a content model for the production type
(Section 2-A). Supplying one in the test config closes it, and doing so surfaced a **new empirical
constraint on what 2-A must emit**:

The obvious content model — `SEQUENCE[referenceEntity(0..1) → [referenceEntity],
referenceEntity(0..*) → [underlyingAsset]]` — **cannot be routed at all**. The router rejected even a
single `<referenceEntity>`, logging "Cannot route XML content". The reason is correct behaviour, not
a bug: with `minOccurs: 0` on the first slot, one element has two valid complete matches (either
slot), so the matcher reports AMBIGUOUS and lenient recovery declines to guess.

Making the direct element `minOccurs: 1` makes the model unambiguous and everything routes:
`referenceEntity` → `[referenceEntity]`, the second → `[underlyingAsset]`. **This is exactly the
constraint XSD's Unique Particle Attribution imposes**, so a generated content model that satisfies
UPA will route, and one that does not, cannot. Section 2-A should therefore treat UPA-conformance of
emitted content models as a requirement, not an incidental property. (A third case verified in
passing: a substitution member whose element name is *not* in the content model — `equityAsset` —
gets no route and falls through to plain name binding, which resolves it via the substitution
resolver as before.)

Also confirmed: `StaxReader.applyRoutedDirectElement` already handled `elementRef` substitution on a
routed single-segment path, so no production code change was needed for this criterion.

### 6.4 — Deleted the dead Jackson XML classes (20 files)

**Deleted (19 main + 1 test):**

| Package | Files |
|---|---|
| `serialisation/xml/` | `RosettaXmlMapper`, `RosettaXMLModule`, `RosettaXMLAnnotationIntrospector`, `RosettaXMLTypeConfigLookup`, `SubstitutionMap`, `SubstitutionMapLoader`, `VirtualXMLAttribute` |
| `serialisation/xml/serialization/` | `RosettaBeanSerializer`, `RosettaBeanSerializerModifier`, `RosettaSerialiserFactory`, `SubstitutingBeanPropertyWriter`, `UnwrappableIndexedListSerializer`, `UnwrappingAsArraySerializerBase`, `UnwrappingIndexedListSerializer` |
| `serialisation/xml/deserialization/` | `RosettaBeanDeserializerModifier`, `RosettaModelObjectSizeEstimator`, `SubstitutedMethodProperty`, `VirtualPathBuilderHelper`, `XMLContentModelDisambiguatingDeserializer` |
| tests | `spike/StaxBinderSpikeTest` (the Step 0 throwaway, 6 tests) |

**Kept, deliberately:**

| Kept | Why |
|---|---|
| `config/*` (7 classes) | The config model is not Jackson scaffolding — it is the XML contract `model-import` emits. Never in scope. |
| `deserialization/XMLContentModelMatcher`, `RoutingInput` | The routing algorithm, reused byte-for-byte since 4c |
| `deserialization/ContentModelRouter` | Jackson-free facade over it (4c) |
| `serialization/XMLContentModelOrderer` | Pure ordering algorithm, now driven by `StaxWriter` (see 6.0) |
| `UnknownZoneProvider` | Used by `StaxScalarConverter` |
| `StaxXmlObjectMapper`, `StaxObjectWriter` | The `ObjectMapper` facade from Step 5 (jackson-**databind**, not dataformat-xml) |

Every reference was checked before deleting, rather than relying on Step 5's note. All cross-package
references from surviving code into the deleted set turned out to be **javadoc/comment only**
("ported from X", "mirrors Y"). Four needed editing, three of them `{@link}`s that would have
dangled:

- `RuneTypeIntrospector` — `{@link SubstitutionMap.XMLFullyQualifiedName}` → describes the config's
  `xmlElementFullyQualifiedName` convention directly.
- `StaxScalarConverter` — `{@link RosettaXMLModule}` → `{@code}`, noting the class is now deleted and
  the port was verbatim.
- `XMLContentModelMatcher` — `{@link XMLContentModelDisambiguatingDeserializer}` →
  `{@link ContentModelRouter}`, now its only caller. **This is the one edit to the
  otherwise-byte-for-byte-unchanged matcher, and it is a javadoc link only.**
- `XmlContentModelSerializationOrderTest` — a stale sentence describing the Jackson deserializer,
  rewritten for the StAX engine.

**API-compatibility note:** these were public classes, so their removal is a breaking change for any
downstream code importing them directly. Verified that the two model repos available in this session
(`common-domain-model`, `digital-regulatory-reporting`) contain **no references** to any deleted
class — they only ever went through `RosettaObjectMapperCreator.forXML`, which is unchanged. One
alias was nonetheless restored; see 6.6.

### 6.6 — `RosettaXmlMapper` restored as a deprecated alias

Of the 19 deleted main classes, exactly one was worth giving back: `RosettaXmlMapper` is the only
deleted type whose entire **public** surface was a single constructor,
`(RosettaXMLConfiguration, ClassLoader)` — everything else on it was `protected` or `private` Jackson
internals. `StaxXmlObjectMapper`'s public constructor has the *identical* signature, so the old
fully-qualified name can be re-offered as a three-line subclass:

```java
@Deprecated
public class RosettaXmlMapper extends StaxXmlObjectMapper {
    public RosettaXmlMapper(RosettaXMLConfiguration config, ClassLoader classLoader) {
        super(config, classLoader);
    }
}
```

A deprecated subclass was chosen over renaming `StaxXmlObjectMapper` itself: it restores the old
import and call shape while keeping a name that describes what the class now *is*, and the
`@Deprecated` marker points callers at `forXML(...)` / `RuneXmlMapper` instead of silently
re-establishing a Jackson-era name as the primary type.

**Two limits are documented on the class, because neither can be fixed:**
1. It **no longer extends `XmlMapper`** — that supertype needs `jackson-dataformat-xml`, removed in
   6.5. Code that assigned it to an `XmlMapper` variable, or passed it to
   `new XmlMapper.Builder(...)`, still breaks. It is also binary-incompatible for pre-compiled
   callers.
2. `ObjectMapper#_readValue` / `#_readMapAndClose` — the two methods the old class overrode — are
   bypassed entirely by the StAX path, so a downstream subclass overriding them still *compiles* but
   has **no effect**. This is the one silent-behaviour hazard of reusing the name, so it is called
   out explicitly in the javadoc.

**How much compatibility this actually buys is narrower than the name suggests**, and the javadoc says
so: the old `RosettaXmlMapper` was never usable standalone. It carried no `RosettaXMLModule`, so on
its own it did no Rosetta-aware XML at all — the working recipe was the four-step pipeline in
`forXML(...)` (construct → `setSerializerFactory(RosettaSerialiserFactory.INSTANCE)` → wrap in
`new XmlMapper.Builder(...)` → register `RosettaXMLModule`), and anyone who copied *that* breaks
regardless since two of those types are gone. The alias therefore helps only callers who used it as a
bare `ObjectMapper` — a usage that would not have worked correctly before and does now.

**Tested, not assumed.** `RosettaXmlMapperCompatibilityTest` (2 tests) pins the contract: the legacy
constructor shape compiles and yields a working `ObjectMapper`, and its output is asserted **identical**
to `forXML(...)`'s for both plain and pretty-printed-with-`schemaLocation` writes. The test deliberately
does *not* assert `XmlMapper` assignability, with a comment recording that as the known break.

The other 18 deleted classes get no shim: every one of them takes or returns Jackson types
(`Module`, `BeanSerializerFactory`, `BeanPropertyWriter`, `JavaType`, `SettableBeanProperty`, …), so a
shim would either have nothing to delegate to or would reintroduce the dependency 6.5 just removed.

### 6.5 — Dropped `jackson-dataformat-xml`

Removed from **both** `common/pom.xml` and `serialization/pom.xml`. Removing it from `common` alone
would have been cosmetic: `common` depends on the `serialization` module, which declared it too, so
it would still have arrived transitively. Neither module has a single remaining reference to
`com.fasterxml.jackson.dataformat.xml` (verified by grep over both `src/main` and `src/test`), and
`serialization` never used it.

Verified gone from the resolved tree, with Woodstox still pinned directly as Step 0.1 intended:

```
mvn -pl common dependency:tree | grep -iE "woodstox|stax2|dataformat"
  +- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:jar:2.17.1:compile
  +- com.fasterxml.woodstox:woodstox-core:jar:6.6.2:compile
  |  \- org.codehaus.woodstox:stax2-api:jar:4.2.2:compile
  +- com.fasterxml.jackson.dataformat:jackson-dataformat-csv:jar:2.17.1:compile
```

The parent pom's `dependencyManagement` entry and `${jackson.version}`-style pin for
`jackson-dataformat-xml` were **left in place** — harmless, and it keeps re-adding the dependency a
one-line change if some future consumer needs it.

---

## Section 1 — COMPLETE

All six steps done. The XML mapper is a purpose-built StAX binder; the Jackson XML engine and its
dependency are gone from `rune-common`; the same external `model-import` config drives the new
engine unchanged.

**Criteria status:**

| Criteria | Status |
|---|---|
| 1–12 (feature parity) | ✅ green — the pre-existing suites pass unchanged through `forXML(...)` |
| 13 (attr/element same local name — *the bug that motivated the migration*) | ✅ green, regression-tested |
| 14 (issue 3, routing half) | ✅ green, regression-tested |
| 15 (substituted name collides with direct element) | ✅ green, regression-tested (UPA constraint recorded above) |
| 16 (same local name across namespaces) | ✅ green, regression-tested |
| 17 (repeated unwrapped group) | ✅ green, regression-tested |
| Issue 2 (same name, different order in one type) | ⏭ **Section 2-A** — latent; needs content models for all types |
| Issue 3, cardinality-clash half | ⏭ **Section 2-A** — needs per-type content models |

**Carry-forward into Section 2-A**, beyond what the plan already records:
1. **Emitted content models must satisfy UPA** to be routable — see the criterion-15 finding above.
   An ambiguous model is correctly refused by the matcher, so this is a hard requirement on the
   generator, not a preference.
2. **Both engines' duplicated lenient-recovery policy is no longer duplicated** — Step 4c noted it
   was deliberately copied into `ContentModelRouter` because the Jackson deserializer still existed.
   That deserializer is now deleted, so `ContentModelRouter` is the single implementation.
3. **`XMLContentModelOrderer` is now the write-side entry point for ordering.** If 2-A makes content
   models universal, the orderer runs for *every* type rather than ~2 per config; its
   `MAX_CANDIDATES = 256` search bound and null-fallback behaviour should be re-measured at that
   scale.
4. The out-of-tree benchmark harness is disposable but the method is worth repeating after 2-A:
   interleaved rounds, byte-compare the output of both implementations, and check a real
   production-scale document as well as synthetic ones.
