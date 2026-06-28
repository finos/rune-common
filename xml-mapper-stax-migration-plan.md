# Plan: Migrate the XML mapper off Jackson to a StAX binder (Option C)

---

## START HERE — read before doing anything

This document is the spec for replacing the Jackson-based XML mapper with a
purpose-built StAX binder ("Option C"), and a follow-up set of improvements to the
`model-import` XSD importer.

It is split into **two independent sections**:

- **Section 1 — Option C standalone.** All work lands in `rune-common`. No changes
  to `rune-dsl` or `model-import`. This delivers feature parity, fixes the
  attribute/element name-collision bug, and removes the Jackson token-rewriting
  workarounds. **Ship this first; it justifies itself.**
- **Section 2 — model-import follow-ups.** Optional, additive, version-coupled
  enhancements that buy deeper XSD fidelity (full config-driven structure, namespace
  fidelity, defaults). Each is its own self-contained increment, prioritised against
  real schema requirements **after** Section 1 ships.

### Why this migration (context, already established)

`jackson-dataformat-xml` adapts XML onto a JSON-shaped token stream: XML attributes
and child elements both surface to `jackson-databind` as indistinguishable
"properties". When an attribute and a child element share a local name in one type
(`<X type="..."><type>...</type></X>`), they collide and one clobbers the other —
the bug that motivates this work. The same impedance mismatch is *why* the current
content-model disambiguation machinery has to buffer each element into a
`TokenBuffer`, re-read the StAX reader for namespaces, run a routing matcher, and
rewrite the token stream. A StAX binder where attribute/element/namespace/order are
first-class removes that whole class of workaround.

### The seven Jackson XML issues (root cause + traceability)

The motivating defects are catalogued in the "Summary of Jackson XML issues" note
(Simon, 2026-01-26). They all reduce to **one root cause**: Jackson stores each type's
properties as a map from simple (local) name to deserializer, blind to XML namespace
and document order, and `@JsonUnwrapped` further breaks XML-specific handling. Option C
removes that root cause by making attribute-vs-element, namespace, and order first-class
in the reader/writer. The seven issues, their real FpML/FiML occurrences, and where each
is closed:

| # | Jackson issue | Live FpML/FiML impact today | Closed by |
|---|---|---|---|
| 1 | Same-name element + attribute throws (`<foo id><id>`) | FiML `RepoTransactionLeg`, `Transfer` (attr `id` + element `id`) — workaround picks one | **Section 1** (criterion 13) |
| 2 | Same name, different order in one type, throws | *No occurrences yet* — only because the XSD importer does **not** flatten sequences/choices; becomes live the moment we flatten | **Section 2-A** (needs full content models) |
| 3 | Same name across unwrapped layers → deserialised twice; on cardinality clash only first kept | **5+ occurrences.** FpML `TradeIdentifier.tradeId`; `CommodityEuropeanExercise.expirationDate` should be unbounded but only first kept — **data loss** | **Section 1** for routing; **2-A** for the cardinality-clash half |
| 4 | Unwrapping types + unwrapping lists, throws (#676, #762) | Heavily used in FpML; *already* patched into Jackson in rune-common (and still spawning new bugs — see #7) | **Section 1** |
| 5 | Substitution name overlaps an existing element in the type, throws | FpML `TradeUnderlyer2` (`referenceEntity` as both regular element and `underlyingAsset` substitute) — workaround leaves `referenceEntity` **never populated** | **Section 1** (substitution + content model) |
| 6 | Same local name, different namespace → always picks first (#65) | FiML `environmentalPhysicalLeg` (substitutes FpML `commoditySwapLeg`, parse **failure**); FiML `commodityOption` shadows FpML's, **losing** `schedule` | **Section 1** via the **namespace-aware substitution map** — *verified end-to-end against the real BNPP FiML config* (no content model involved) |
| 7 | Unwrapped unbounded group keeps only first instance | Repeated `<group ref maxOccurs="unbounded">` collapses to a single value — **data loss** | **Section 1** (virtual/unwrapped + multi-cardinality on read) |

**Two facts from this catalogue drive the plan and must not be lost:**

1. **The "keep patching Jackson" path is visibly failing.** Issue 4 is *"we already
   extended Jackson to support this,"* and issue 7 is a brand-new bug *inside that very
   extension.* This is direct evidence for replacing the engine rather than extending it
   further.

2. **The flatten-vs-nest dilemma is the strategic prize, and Option C dissolves it.**
   Issue 3 happens *because* `model-import` keeps sequences/choices as nested virtual
   types; issue 2 is dormant *only because* we don't flatten — and flattening to fix
   issue 3 *causes* issue 2 (the note states this explicitly). Jackson is trapped because
   it disambiguates by Java property-name uniqueness. A content-model-driven StAX binder
   routes by position/order/occurrence against the model, so it handles flattened **and**
   nested structures and never depends on unique Java names. This both closes issues 2/3
   and frees the XSD importer to flatten or nest at will — but realising it for *all*
   types requires Section 2-A (content models are emitted today **only** for
   name-conflicting types: `XmlConfigurationGenerator` passes only
   `NameConflictDetector` output to `XMLContentModelBuilder.buildContentModels`, which
   returns `Map.of()` for everything else — verified in `rosetta-components`).

**Code evidence the engine is already shaped for this migration** (two independent
namespace-aware mechanisms that only fail because Jackson starves them of namespace):

1. **Content-model matcher.** `deserialization/RoutingInput.Namespace` carries an explicit
   `UNKNOWN` state that exists *only because* Jackson's `TokenBuffer` has discarded the
   StAX namespace context before the matcher runs, forcing a degraded local-name fallback
   (issue 6). A StAX-native reader feeds the same — reused-unchanged —
   `XMLContentModelMatcher` real `PRESENT`/`ABSENT` namespace states, so `UNKNOWN` never
   fires.
2. **Substitution map.** `SubstitutionMap.fullyQualifiedNameToTypeMap` is already keyed by
   name **+** namespace (`XMLFullyQualifiedName`, `equals`/`hashCode` over both), with a
   local-name `Multimap` only as a "pick first" fallback. The config supplies the data:
   `xmlElementFullyQualifiedName` carries distinct namespaces per element (verified in the
   real `fiml-5-4-xml-config.json` — both `commodityOption` variants and
   `environmentalPhysicalLeg` resolve to their FiML/FpML namespaces). The read path,
   `SubstitutedMethodProperty.getActualType`, *already* casts to `FromXmlParser` and
   reaches **under Jackson into `getStaxReader().getName()`** to recover the namespace,
   using `getTypeByFullyQualifiedName` when present and the local-name fallback otherwise.
   That back-door StAX lookup — a Jackson-era workaround for exactly the namespace loss
   Option C removes — is direct proof the design already wants StAX as the substrate. This
   is also why issue 6 for the FiML commodity types needs **no content model** (the real
   config emits none for them) and has **no Section 2 dependency**.

### What this migration is NOT

The external XML config produced by `model-import` is **not** Jackson scaffolding and
is **not** being removed. A Rune type is a lossy, more-abstract projection of an XSD;
the config records the XML facts the Rune model cannot hold (element names,
namespaces, attribute-vs-element-vs-text, substitution groups, enum value mappings,
content-model ordering for disambiguation). Proof: the generated annotations
(`@RuneAttribute`, `@RuneDataType` in `rune-dsl/rune-runtime`) carry only the logical
Rune name/builder/model/version — zero XML information. Any mapper, Jackson or
otherwise, consumes this config. Option C reduces **runtime complexity** in
`rune-common`; it still reads the same config.

### Prerequisite: working directories

- **Section 1** needs only `rune-common` (this repo). The binder consumes the same
  two inputs Jackson does today — the generated config and the generated POJOs — so
  no other repo changes.
- **Section 2** additionally needs:
  - `rosetta-components` — the `model-import/xsd` and `model-import/common` modules
    (the XSD importer that emits the config).
  - `rune-dsl` — only as reference for the `@Rune*` annotation contract and the
    `RosettaModelObject.process()` / builder runtime the binder binds against. No
    code changes land here in either section.

If a referenced repo is missing when you reach a step that needs it, stop and ask
for it to be added rather than guessing.

---

## The feature-parity bar (acceptance criteria for Section 1)

The new binder MUST support all of the following. The existing test suites are the
executable spec — Section 1 is "done" when they all pass against the new binder
(plus a new test for the collision bug). Do not regress any of these:

1. Root-element → type inference & substitution (pick concrete type from root tag).
2. Post-deserialization pruning (`toBuilder().prune().build()`).
3. Substitution groups — FpML-style, transitive, namespace-aware, plus legacy v1/v2
   config formats.
4. XML content-model disambiguation — SEQUENCE/CHOICE/ALL/ANY, minOccurs/maxOccurs
   (incl. unbounded), routing ambiguous element names into virtual choice/group
   properties, including nested multi-layer virtual paths.
5. Virtual/unwrapped attributes — flattening Rune-only grouping types into the parent
   element.
6. Constant XML attributes + `schemaLocation` injection on the root.
7. Per-field representation: ATTRIBUTE vs ELEMENT vs VALUE (text), config-driven.
8. XML-specific enum value mapping.
9. Custom date/time handling — `LocalTime`, and `ZonedDateTime` across 5 formats incl.
   the "Unknown" zone.
10. Polymorphism via `@type`, multi-cardinality lists (wrapper suppression), CDATA,
    namespaces.
11. Builder-pattern integration; ignore non-`@RuneAttribute` members; handle the
    `getType()` name clash.
12. XSD round-trip fidelity — output validates against the source XSD.
13. **NEW (issue 1):** an attribute and a child element sharing the same local name in
    one type both round-trip correctly (the bug being fixed). Fixture: FiML
    `RepoTransactionLeg` / `Transfer`.
14. **NEW (issue 3, routing):** the same element name appearing across unwrapped layers
    is routed to exactly one slot, not deserialised twice. Fixture: FpML
    `TradeIdentifier.tradeId`.
15. **NEW (issue 5):** a substitution-group member whose substituted name collides with
    an existing element in the same complex type round-trips both as distinct slots — the
    previously-lost property is now populated. Fixture: FpML `TradeUnderlyer2`
    (`referenceEntity`).
16. **NEW (issue 6):** elements sharing a local name across different namespaces resolve
    to the correct type by namespace, not "first wins." Mechanism is the **namespace-aware
    substitution map** (`getTypeByFullyQualifiedName`), *not* the content-model matcher —
    the static chain (config `xmlElementFullyQualifiedName` → namespace-keyed
    `SubstitutionMap` → `SubstitutedMethodProperty`) is already verified end-to-end; the
    binder must feed it the StAX element namespace natively instead of via the current
    `FromXmlParser.getStaxReader()` back-door. Fixture: the BNPP FiML schema set + its
    generated `fiml-5-4-xml-config.json` (`commodityOption`, `commoditySwap`,
    `environmentalPhysicalLeg`). Round-trip the real sample
    `fiml-emissions-forward-ukallowance-new-schema.xml`, asserting `<fiml:…>` elements bind
    to the FiML types with `schedule` retained.
17. **NEW (issue 7):** a repeated unwrapped group (`<group ref maxOccurs="unbounded">`)
    accumulates all instances on read instead of collapsing to the first.

Issues 2 and the cardinality-clash half of 3 (`CommodityEuropeanExercise.expirationDate`
must stay unbounded across layers) require complete per-type content models and are
**Section 2-A** acceptance criteria, not Section 1 — see the traceability table above.
Each of criteria 13–17 should assert **no data loss** against the named production type,
proving the corresponding "pick the first" workaround is removed.

### Reference: the code being replaced / reused

In `common/src/main/java/com/regnosys/rosetta/common/serialisation/xml/`:

- Entry / wiring: `RosettaXmlMapper`, `RosettaXMLModule`, `RosettaXMLAnnotationIntrospector`,
  `RosettaXMLTypeConfigLookup`; created via `RosettaObjectMapperCreator.forXML(...)`.
- Config model (**reused as-is**): `config/RosettaXMLConfiguration`, `TypeXMLConfiguration`,
  `AttributeXMLConfiguration`, `AttributeXMLRepresentation`, `XMLContentModel`,
  `XMLContentModelNodeType`, `OccursMax`.
- Disambiguation (**matcher reused, Jackson wrapper replaced**):
  `deserialization/XMLContentModelMatcher` (reuse), `XMLContentModelDisambiguatingDeserializer`
  + `VirtualPathBuilderHelper` + `RoutingInput` (replace the Jackson-coupled parts),
  `RosettaBeanDeserializerModifier`, `SubstitutedMethodProperty`.
- Serialization (replace): `serialization/RosettaBeanSerializer`, `SubstitutingBeanPropertyWriter`,
  `Unwrapping*`, `RosettaBeanSerializerModifier`.
- Substitution: `SubstitutionMap`, `SubstitutionMapLoader`.
- Date logic (**port largely as-is**): the `LocalTime` / `ZonedDateTime` handlers in
  `RosettaXMLModule`, `UnknownZoneProvider`.

Tests: `common/src/test/java/.../serialisation/xml/XmlSerialisationTest`,
`XmlContentModelDisambiguationTest`, `deserialization/XMLContentModelMatcherNamespaceTest`,
plus resources under `common/src/test/resources/serialisation/xml/`.

---

# SECTION 1 — Option C standalone (rune-common only)

Estimated effort: **XL, ~10–16 weeks for one engineer.** Risk medium-high, mitigated
by the existing test suites. Cross-repo work: none.

> **Model guidance for sessions:** Steps 2, 3a–3c, 4a–4b, 4d, 5, and 6 are suitable for
> Sonnet. **Step 4c (content-model disambiguation)** is the one step that benefits from
> Opus — it requires designing the StAX-native replacement for
> `XMLContentModelDisambiguatingDeserializer` + `VirtualPathBuilderHelper` from scratch,
> coordinating three interacting mechanisms (routing, virtual-path assignment, namespace
> state) simultaneously. Steps 3 and 4 are intentionally split into sub-steps (3a/3b/3c
> and 4a/4b/4c/4d) because each is large enough to overflow a single session's context;
> start a fresh session at each sub-step boundary.

### Design constraint (decide up front, applies to every step)

The current config carries element **ordering/occurrence only for ambiguous types**
(`contentModel` is emitted by `model-import` only for name-conflicting types). For all
other types, Jackson takes element order from the generated bean's property
declaration order. **The new binder MUST do the same:** derive structural order and
occurrence from the generated bean (`@RuneAttribute` declaration order), and use the
config only for XML-specific overrides + disambiguation of the ambiguous types. This
keeps Section 1 free of any `model-import` change. (Making it fully config-driven is a
Section 2 item.) Target the `@Rune*` annotation set; keep reading the deprecated
`@Rosetta*` only as a fallback for older generated models.

## Step 0 — Spike & boundary proof (1 week) — ✅ COMPLETE (2026-06-20)

> **STATUS: DONE — Section 1 is cleared to proceed.** Detailed report, config field-coverage
> table, and exact fixture locations are in `xml-mapper-stax-migration-step0-progress.md`
> (same folder). Summary of what was proven / found:
> - **Where this lives:** committed on branch **`spike/stax-step0`** (off `main`, **not merged**).
>   Two commits: (1) the Woodstox pom pin — a real Section 1 foundation, keep; (2) the spike test
>   — throwaway, in its own commit so it can be dropped/reverted once Steps 3–4 land real
>   serializer/deserializer tests. The `.claude/plans/*` docs are git-ignored, so they are not in
>   the commits — they sit on disk in the working tree regardless of branch.
> - **Spike green:** `common/src/test/java/com/regnosys/rosetta/common/serialisation/xml/spike/StaxBinderSpikeTest.java`,
>   6 tests pass via raw Woodstox StAX (no Jackson). Proves read+write round-trip for a simple
>   type **and** the three hardest issues at parser level: same-local-name **attribute vs element**
>   collision (issue 1 / criterion 13 — the whole point), **namespace URI surfacing** (issue 6),
>   and **document order** for interleaved repeats (issues 2/5).
> - **Woodstox pinned** directly in `common/pom.xml` (`woodstox-core:6.6.2`, managing
>   `stax2-api:4.2.2`) — no longer relying on Jackson to drag it in.
> - **Config split CONFIRMED empirically:** in the real production configs (FpML 5.13 / FiML 5.4,
>   ~7.1–7.8k lines each), `contentModel` is emitted for only ~2 types each (FiML:
>   `tradeIdentifier`, `fxTargetKnockoutForward`). So the Section 1 binder MUST derive structure
>   from bean declaration order (per the design constraint above); full content models are
>   Section 2-A. Element namespaces are fully present in the config, so criterion 6 needs **no**
>   config change.
> - **Gaps → Section 2 (not Section 1 blockers):** content-models-for-all-types (2-A),
>   attribute-level namespaces (2-B), defaults/fixed/nillable (2-C). `enumValues` is sparse —
>   confirm displayName-vs-`name()` during Step 2.
> - **Fixtures located** in `rosetta-models/bnpp` (separate repo): `RepoTransactionLeg`,
>   `Transfer`, `TradeIdentifier`, `CommodityEuropeanExercise`, `TradeUnderlyer2`,
>   `environmentalPhysicalLeg`/`commoditySwapLeg`, `commodityOption`, and the round-trip sample
>   `fiml-emissions-forward-ukallowance-new-schema.xml` (confirmed to contain
>   `<fiml:environmentalPhysicalLeg>` + the `<fiml:schedule>` lost today). Step 6 will need
>   cut-down copies under `common/src/test/resources/`.
> - **Gotcha for all Section 1 code:** module compiles to **Java 8 bytecode** — no `List.of`,
>   `var`, etc. Also `XMLStreamReader.getElementText()` advances past `END_ELEMENT` (reader-loop
>   footgun).
>
> **Next: Step 2 — Scalar & value conversion.**

Goal: de-risk before committing.

1. Pick the StAX implementation. Default: **Woodstox** (already a transitive dep via
   `jackson-dataformat-xml`); confirm the exact coordinate and pin it directly in
   `common/pom.xml` so the binder doesn't rely on Jackson dragging it in. Aalto is an
   alternative if async/perf becomes a concern — note but don't adopt yet.
2. Write a throwaway StAX reader + writer for one simple Rune type (e.g. `TimeContainer`
   or `Measure`) end-to-end: element/attribute/text, builder population, round-trip.
3. Prove the collision case (criterion 13) round-trips on the spike — this is the whole
   point; if StAX can't cleanly distinguish `@type` from `<type>`, stop and re-evaluate.
   At the parser level (not full binding), also confirm Woodstox surfaces the namespace
   URI (issue 6) and preserves document order for interleaved repeats (issue 5/2) — the
   two hardest issues — before committing the full effort.
4. Generate a config + types from your **largest production XSD** (FpML if available)
   and eyeball the config for completeness against criteria 1–12 — especially
   namespaces. Record any genuine gaps here; they become Section 2 inputs, not Section 1
   blockers.
5. **Harvest the named production types as acceptance fixtures** (criteria 13–17):
   `RepoTransactionLeg`, `Transfer`, `TradeIdentifier`, `CommodityEuropeanExercise`,
   `TradeUnderlyer2`, `environmentalPhysicalLeg`/`commoditySwapLeg`, `commodityOption`.
   These beat synthetic fixtures — each has a documented "pick the first" workaround and
   real data loss to prove removed.

Note: the Section 1/2 split assumes content models are emitted **only** for
name-conflicting types. Verified in `rosetta-components`: `XmlConfigurationGenerator`
passes only `NameConflictDetector` output into
`XMLContentModelBuilder.buildContentModels`, which returns `Map.of()` for all other
types. Re-confirm if `model-import` changes before locking the split.

Exit: a runnable spike proving read+write+collision for one type, the production fixtures
captured, and a written note on config completeness.

## Step 1 — Introspection layer over generated beans (1–2 weeks) — ✅ COMPLETE (2026-06-21)

Build the read-only model the binder drives off, replacing Jackson's `BeanDescription`
/ introspector with our own:

1. A `RuneTypeIntrospector` that, given a generated class, returns its attributes in
   declaration order with: logical name, getter, builder setter/adder, cardinality
   (`@Multi`), value type, and whether it's a `RosettaModelObject`, enum, or scalar.
   Source: `@RuneAttribute` / `@RuneDataType` / `@RuneMetaType` / `@RuneChoiceType`,
   the `Accessor` annotation, and the builder class from `@RuneDataType.builder()`.
2. Port the include/ignore rules from `RosettaXMLAnnotationIntrospector._isIgnorable`
   / `findPropertyIgnoralByName` (only `@RuneAttribute` members; handle the `getType()`
   clash; respect `@RuneIgnore`).
3. Layer the config on top: resolve per-attribute `xmlName` + `xmlRepresentation`
   (ATTRIBUTE/ELEMENT/VALUE/VIRTUAL), per-type `xmlElementName` /
   `xmlElementFullyQualifiedName`, `xmlAttributes`, `enumValues`. Reuse
   `RosettaXMLTypeConfigLookup` logic.

Exit: given any generated type + config, you can enumerate its XML binding plan
(name, namespace, representation, order, cardinality) without Jackson.

## Step 2 — Scalar & value conversion (3–4 days) — ✅ COMPLETE (2026-06-24)

1. Port the date/time handling from `RosettaXMLModule` verbatim: the `LocalTime`
   serializer/deserializer and the 5-format `ZonedDateTime` logic, plus
   `UnknownZoneProvider` registration.
2. Build a small converter registry for the remaining scalar types Rune uses
   (`BigDecimal`, `Integer`, `Boolean`, `Date`, `String`, enums via `enumValues`).
3. Unit-test against the date/time cases in `XmlSerialisationTest`
   (`testTime*`, `testZonedDateTime*`).

> **STATUS: DONE.** `StaxScalarConverter` in
> `common/.../serialisation/xml/stax/convert/StaxScalarConverter.java` handles all scalar
> types. `StaxScalarConverterTest` (23 tests, all green) mirrors every `testTime*` and
> `testZonedDateTime*` case from `XmlSerialisationTest`. Checkstyle clean.
>
> **Key implementation notes:**
> - `UnknownZoneProvider` is registered in a `static` block, identical to `RosettaXMLModule`.
> - `LocalTime` serializes as `"HH:mm:ssZ"` (UTC offset appended via `OffsetTime.of(t, UTC)`);
>   deserializes by stripping the offset from `OffsetTime`, or plain `LocalTime.parse` as fallback.
> - `ZonedDateTime` uses the identical 5-format cascade from `RosettaXMLModule`.
> - `BigDecimal.toPlainString()` avoids scientific-notation output.
> - Enum priority: (1) config `enumValues` map override → (2) `toDisplayString()` via reflection
>   → (3) `toString()` → (4) `name()` fallback. Reverse lookup for deserialization uses
>   `fromDisplayName()` static when no config override is present.
> - Java 8 compatible: `Arrays.asList(...)` instead of `List.of(...)`.
>
> **Next: Step 3a — Basic emission + root handling (fresh session).**

## Step 3 — Serializer (writer) (2–3 weeks) — ✅ COMPLETE (2026-06-24)

> **STATUS: DONE.** All three sub-steps complete. Full `common` module: **262 tests pass,
> 0 failures, 3 skipped** (pre-existing `@Disabled`). Checkstyle clean. Detailed notes in
> `xml-mapper-stax-migration-progress.md`.
>
> **Deliverables:**
> - `StaxWriter.java` — pure StAX serializer with ELEMENT/ATTRIBUTE/VALUE/VIRTUAL/substitution support
> - `StaxWriterTest.java` (4 tests), `StaxWriterSubstitutionTest.java` (5 tests), `StaxWriterVirtualTest.java` (2 tests)
>
> **Key implementation notes for Step 4:**
> - The writer uses `((RosettaModelObject) obj).getType()` to get the interface class for introspection (immutable impls don't carry `@RuneDataType`).
> - Substitution group resolution: `attr.getElementRef().isPresent()` → look up concrete type's `TypeBinding.getXmlElementName()` via the introspector.
> - VIRTUAL handling uses a `writeChildAttributes` helper that writes children at the parent's depth with no wrapper element — Step 4 will need the mirror-image read logic.
>
> **Next: Step 4a — Basic stream → builder + attribute/element collision fix.**

Write the StAX writer driven by Steps 1–2. Split across three sessions (3a → 3b → 3c);
each session picks up from the passing exit criteria of the previous one.

### Step 3a — Basic emission + root handling (fresh session) — ✅ COMPLETE

### Step 3b — Substitution groups on write (fresh session) — ✅ COMPLETE

### Step 3c — Virtual/unwrapped types, multi-cardinality, pretty-print (fresh session) — ✅ COMPLETE

## Step 4 — Deserializer (reader) (3–4 weeks)

Write the StAX reader driven by Steps 1–2. Split across four sessions (4a → 4b → 4c →
4d). Step 4c carries the highest design complexity; use Opus for that session if
available (see model note at the top of this file).

### Step 4a — Basic stream → builder + attribute/element collision fix (fresh session) — ✅ COMPLETE (2026-06-28)

1. Stream elements, attributes, and text content into the builder via the introspection
   plan from Step 1; distinguish attribute vs element natively at the StAX token level —
   **this is where criterion 13 is fixed**.
2. Root-element → type inference: port `RosettaXmlMapper.getTypeFromRootElementName`
   + subtype check.
3. Post-deserialization pruning: `toBuilder().prune().build()` (port
   `RosettaXmlMapper.pruneObject`).

> **STATUS: DONE.** `StaxReader.java` in
> `common/.../serialisation/xml/stax/read/StaxReader.java`. 18 tests pass, 0 failures.
> Full `common` module: **280 tests pass, 0 failures, 3 skipped** (pre-existing
> `@Disabled`). Checkstyle clean.
>
> **Deliverables:**
> - `StaxReader.java` — StAX deserialiser with ELEMENT/ATTRIBUTE/VALUE/VIRTUAL support,
>   root-element type inference, and post-deserialisation pruning
> - `StaxReaderTest.java` (18 tests) — covers basic scalars, nested objects,
>   multi-cardinality, VIRTUAL, time/date types, ZonedDateTime (6 formats), pruning,
>   root-element type inference (TopLevelExtension as Document), and criterion-13 style
>   attribute/element distinction
>
> **Key implementation notes for Step 4b:**
> - `read(String, Class<T>)` is the entry point; it advances past comments/PIs to the
>   first START_ELEMENT, infers the concrete type via `inferTypeFromRootElement`, then
>   calls `readObject` and prunes.
> - `readObject` reads XML attributes from the START_ELEMENT token (via
>   `getAttributeCount()`/`getAttributeLocalName`/`getAttributeValue`), then loops
>   child events until END_ELEMENT, routing each START_ELEMENT child to the matching
>   AttributeBinding (by `xmlName`). Attribute/element collision is structurally
>   impossible — different StAX API paths.
> - VIRTUAL: child elements not matched by direct ELEMENT bindings are searched one level
>   deep into VIRTUAL types. Virtual builders are created lazily and applied after the
>   child loop. Multi-cardinality inside VIRTUAL works (e.g. `Party.partyModel.partyId`).
> - Scalar types at root level (e.g. `ZonedDateTime`) are detected via `isScalarType`
>   (no `@RuneDataType`/`@RosettaDataType`) and read via `getElementText()` + converter.
> - `getElementText()` footgun: after the call the reader is on END_ELEMENT; the outer
>   loop's `reader.next()` advances past it correctly. Same contract for `readObject`.
> - `skipElement` skips unknown child elements by tracking nesting depth.
> - Java 8 compatible: no `List.of`, no `var`.
>
> **Not yet covered (Step 4b):** substitution groups, `@type`-driven polymorphism.
> **Not yet covered (Step 4c):** content-model disambiguation.
> **Not yet covered (Step 4d):** repeated unwrapped groups (issue 7).
>
> **Next: Step 4b — Polymorphism + substitution-group resolution on read.**

Exit: simple scalar and nested-object round-trips work. Criterion 13 (same-local-name
attribute vs element, e.g. `RepoTransactionLeg`) passes with no data loss. Basic
`XmlSerialisationTest` deserialization cases are green.

### Step 4b — Polymorphism + substitution-group resolution on read (fresh session)

Port the `SubstitutedMethodProperty` routing logic so the reader resolves substituted
element names directly from the StAX element name + namespace, replacing the old
`TokenBuffer`-rewriting path. Specifics:

- `@type`-driven polymorphism.
- Substitution-group member type resolved by namespace-aware lookup
  (`getTypeByFullyQualifiedName` first, local-name fallback second).
- **Issue 5:** a substituted name that collides with an existing element in the same type
  must resolve to a *distinct* slot (by namespace), not be dropped — assert
  `TradeUnderlyer2.referenceEntity` populates (criterion 15).

Exit: substitution-group deserialization tests pass. Criteria 15 and 16 (namespace-aware
substitution) are green.

### Step 4c — Content-model disambiguation (fresh session — Opus recommended)

Build the StAX-native replacement for `XMLContentModelDisambiguatingDeserializer` +
`VirtualPathBuilderHelper`. **Reuse `XMLContentModelMatcher` unchanged** — it is a pure
routing algorithm over `RoutingInput` and has no Jackson dependency.

The new pipeline:
1. Detect types that `requiresRouting` (same gate as today).
2. Collect all child elements of the current element **in document order, with position**
   (name + namespace URI + buffered raw StAX subtree). Preserve order — the matcher's
   SEQUENCE handling depends on it.
3. Feed the collected sequence to `XMLContentModelMatcher`, passing real
   `RoutingInput.Namespace.PRESENT` / `ABSENT` states — **never** the `UNKNOWN` fallback
   the Jackson path was forced into (issue 6). The matcher already handles this correctly.
4. Translate each matcher assignment into either a direct builder property set or a
   nested virtual-path walk (the "VirtualPathBuilderHelper" logic), then replay the
   buffered subtrees through the recursive reader for each assigned slot.
5. Keep `requiresRouting` gating so non-ambiguous types skip the buffering overhead.

Exit: `XmlContentModelDisambiguationTest` (all examples + ALL/ANY + multi-layer +
failure cases) and `XMLContentModelMatcherNamespaceTest` both pass fully.

### Step 4d — Multi-cardinality accumulation + full suite green (fresh session)

**Issue 7 / multi-cardinality on read:** a repeated unwrapped group
(`maxOccurs="unbounded"`) must *accumulate* every instance into the builder list, not
overwrite to the first. Likewise, when the same element name spans layers with different
`maxOccurs` (issue 3 cardinality clash), honour each matched layer's cardinality rather
than collapsing to the most restrictive.

Fix any remaining test failures across the full deserialization suite identified once
4a–4c are in place.

Exit: all deserialization assertions pass, including `XmlContentModelDisambiguationTest`,
`XMLContentModelMatcherNamespaceTest`, and criterion 17 (issue 7, repeated unwrapped
group accumulates all instances).

## Step 5 — Wire into the public entry point (3–4 days)

1. Provide the binder behind the existing `RosettaObjectMapperCreator.forXML(...)`
   surface so callers don't change. Decide the return type: either keep returning an
   `ObjectMapper`-compatible facade, or introduce a `RuneXmlMapper` and adapt
   `forXML(...)` — ask for user preference when implementing.
2. Preserve the `withAttribute("schemaLocation", ...)` and
   `writerWithDefaultPrettyPrinter()` behaviours the tests rely on (or provide
   equivalents and update tests minimally).
3. Keep `RosettaXMLConfiguration.load(...)` and the legacy-config construction paths
   working (the legacy v1/v2 tests build configs on the fly).

## Step 6 — Full test pass, performance, cleanup (1–2 weeks)

1. Green the entire `serialisation/xml` test package + `mvn clean install` (checkstyle:
   no `com.google.inject.*`, use `jakarta.inject`).
2. Add explicit regression tests for criteria 13–17 against the harvested production
   fixtures (issues 1, 3-routing, 5, 6, 7), each asserting no data loss vs the old
   "pick the first" workaround. (Issue 2 and issue-3 cardinality land with Section 2-A.)
3. Benchmark against the old mapper on a large document; confirm no regression
   (StAX streaming should match or beat the TokenBuffer path).
4. Delete the now-dead Jackson XML classes (serializers/deserializers/modifiers/
   introspector) once nothing references them. Keep the config model classes.
5. Remove the `jackson-dataformat-xml` dependency from `common/pom.xml` if nothing else
   needs it; keep the pinned Woodstox/StAX dep.

Exit: Section 1 complete — parity + collision fix + Jackson XML removed, all in
`rune-common`.

---

# SECTION 2 — model-import follow-up improvements

Additive, independent increments. Each requires lockstep new fields in the
`rune-common` config classes AND emission logic in `model-import`, then binder support.
Do these **after** Section 1, prioritised against real schema needs. The two repos are
version-coupled through the shared config classes
(`com.regnosys.rosetta.common.serialisation.xml.config.*`).

## Improvement A — Full config-driven structure (highest value-for-effort)

**Today:** `model-import`'s `XMLContentModelBuilder.buildContentModels(...)` returns
`Map.of()` unless `targetTypeFqns` is non-empty, and those are only the name-conflicting
types from `NameConflictDetector`. So most types carry no content model, and the binder
(Section 1) takes structure from bean declaration order.

**Goal:** emit ordered content models (with occurrence) for **all** complex types, so
the binder is driven by the model rather than Java field order — enabling strict
validation-on-parse (reject out-of-order / missing-required elements with precise
errors) and decoupling the binder from generator field-ordering.

**This is also what fully closes issues 2 and 3 and dissolves the flatten-vs-nest
dilemma** (see the issue traceability table in Section 1). Today the importer cannot
flatten sequences/choices without triggering issue 2, yet keeping them nested is what
causes issue 3's cross-layer duplication and the `expirationDate` cardinality clash. Once
every type carries an ordered, occurrence-aware content model, the binder routes by model
position regardless of flattening, so `model-import` becomes free to flatten or nest —
and both issue 2 (latent) and the cardinality-clash half of issue 3 are resolved. This
elevates 2-A from "nice strict-mode validation" to "completes the production data-loss
remediation Section 1 starts."

Effort: **S–M** (the hard generation logic already exists; this is mostly removing the
gate and absorbing config size/perf).

Steps:
1. In `model-import` (`XmlConfigurationGenerator` / `XMLContentModelBuilder`), make
   content-model emission apply to every complex type, not just `conflictedTypeFqns`.
   Keep the conflict detector for *validation*, not for *gating* emission.
2. Measure config-size/perf impact on a large schema; if needed, add a config option to
   opt in per import target (`ImportConfig` in `model-import/common`).
3. In `rune-common`, extend the binder to prefer the config's content model for ordering
   when present, falling back to bean order when absent (so old configs still work).
4. Add strict-mode validation in the reader using the now-complete min/maxOccurs and
   ordering; surface clear errors. Gate behind a flag to preserve lenient behaviour.
5. Tests: regenerate a representative config, assert ordered/occurrence-aware
   round-trips and that strict-mode rejects malformed documents with useful messages.

## Improvement B — Namespace fidelity (attribute namespaces + prefix modeling)

**Today:** `AttributeXMLConfiguration` has no namespace field; `XsdAttributeHandler`
emits only name + `ATTRIBUTE`. Child-element namespaces exist only inside content models
(i.e. only for ambiguous types). Prefixes (`xmlns:fpml`) are handled ad-hoc via constant
`xmlAttributes`.

**Goal:** faithful round-trip of qualified attributes and controlled namespace prefixes.

**Scope clarification:** element-namespace *routing* — same local name in different
namespaces, issue 6, e.g. FiML vs FpML `commoditySwapLeg`/`commodityOption` — is **not**
here; it is fixed in Section 1, because the matcher is already namespace-aware and a
StAX reader feeds it real namespace state (the `RoutingInput.Namespace.UNKNOWN` fallback
disappears). 2-B is strictly the gaps the config still cannot express: **attribute**
namespaces and a systematic prefix model.

Effort: **M.**

Steps:
1. In `rune-common` config classes: add namespace to `AttributeXMLConfiguration`, and a
   namespace-prefix model (e.g. prefix↔URI bindings) to `RosettaXMLConfiguration` /
   `TypeXMLConfiguration`. Bump config compatibility carefully (these are deserialized
   from JSON with `@JsonCreator`; keep new fields optional).
2. In `model-import`: emit attribute namespaces from `XsdAttributeHandler` /
   `XsdElementHandler`, and a systematic prefix map from the schema target namespaces
   instead of ad-hoc constant attributes.
3. In `rune-common` binder: honour attribute namespaces on read/write and emit declared
   prefixes deterministically.
4. Tests: a schema with qualified attributes and multiple namespaces round-trips with
   stable prefixes and validates against its XSD.

## Improvement C — Defaults / fixed / xsi:nil

**Today:** XSD default/fixed values and nillability are not captured.

**Goal:** emit/honour schema defaults and `xsi:nil` for nillable elements.

Effort: **S–M.**

Steps:
1. In `rune-common` config classes: add optional `default` / `fixed` / `nillable` to
   `AttributeXMLConfiguration` (keep optional for back-compat).
2. In `model-import`: populate them from the XSD attribute/element declarations.
3. In `rune-common` binder: apply defaults when absent, honour `fixed`, and emit/parse
   `xsi:nil="true"` for nillable elements.
4. Tests: defaults applied on read, `xsi:nil` round-trips, fixed values enforced.

---

## Sequencing summary

1. **Section 1 (Option C standalone)** — parity + collision-bug fix + Jackson XML
   removed, entirely within `rune-common`. Ship this first.
2. **Section 2** — layer in, in priority order: **A** (full config-driven structure,
   best value), then **B** (namespace fidelity), then **C** (defaults/nil), each as an
   independent version-coupled increment driven by real schema requirements.

This decouples the risky engine work (Section 1) from the cross-repo config work
(Section 2): the bug is fixed and the architecture cleaned without touching
`model-import`, and deeper XSD fidelity is bought later, incrementally, only where it
pays.
