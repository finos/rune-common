# StAX Migration — Progress Report

Unified progress log for all steps of the StAX binder migration
(`xml-mapper-stax-migration-plan.md`). Living document — updated as sub-steps complete.

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

## Step 3 — Serializer (writer) — ✅ COMPLETE (2026-06-24)

| Sub-step | What | Owner | Status |
|---|---|---|---|
| 3a | `StaxWriter` core + root handling | Sonnet sub-agent | ✅ |
| 3b | Substitution groups on write | Sonnet sub-agent | ✅ |
| 3c | VIRTUAL/unwrapped types + full suite green | Sonnet sub-agent | ✅ |

**Step 3 exit status: COMPLETE.** Full `common` module: **262 tests pass, 0 failures, 3 skipped** (pre-existing `@Disabled`). Checkstyle clean across all three sub-steps.

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
