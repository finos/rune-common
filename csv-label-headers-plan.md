# Plan: CSV serialisation with label column headers

---

## START HERE — read before doing anything

This document is the spec. Treat it as the source of truth. The hard investigation
is done **and the two previously-open assumptions are now VERIFIED** (see "Step 0",
which is marked DONE). You do not need to re-verify before coding — proceed to
Steps 1–4. (If you change the target function/type, re-run the Step 0 checks for the
new case.)

### Prerequisite: working directories (required)

This plan references files across three repositories. **All three must be added to
the session as working directories before starting** — otherwise the references
below cannot be resolved and you will re-investigate or guess:

1. `rune-common` — the repo being changed (contains this plan).
2. `rune-dsl` — the DSL/runtime: `LabelProvider`, `@RuneLabelProvider`,
   `GraphBasedLabelProvider`, and the generators that emit them.
3. The BNP model repo (`bnpp`) — the concrete `BnmCsvIRSType` / projection function
   and the pipeline config that this change is for.

If any are missing, stop and ask for them to be added before continuing.

### Order of work

1. **Step 0 — already DONE (both assumptions verified).** Read it for the evidence,
   but no action is required before coding.
2. Proceed to Steps 1–4 in order.

---

## Goal

Make `RosettaCsvMapper` write the **label** of an attribute as the CSV column
header when a `[label ...]` annotation is present, falling back to the
**attribute name** when it is not.

### Motivating example (validated against the BNP model)

`bnpp/rosetta-source/src/main/rosetta/projection-csv-bnm-trade-type.rosetta` defines
the projection output type, with a label on every flat top-level attribute:

```
type BnmCsvIRSType:
    csv001_Action string (0..1)
      [label "001 - Action"]
    csv002_AD_Ref_No string (0..1)
      [label "002 - Ad Ref No"]
    ...
    csv022_Approval_Code string (0..1)
      [label "022 - Approval Code"]
```

The projection function `Project_BnmTransactionReport_IrSwap_ToBnmCsv`
(`projection-csv-bnm-trade-func.rosetta`) outputs `BnmCsvIRSType` and is to be
switched from `[projection JSON]` to `[projection CSV]`.

Today CSV serialisation of a `BnmCsvIRSType` would produce headers from the raw
attribute names:

```
csv001_Action,csv002_AD_Ref_No,...
<value>,<value>,...
```

Target:

```
001 - Action,002 - Ad Ref No,...
<value>,<value>,...
```

### Scope (agreed)
- **Flat, top-level attributes only** — each column maps to a single top-level
  attribute, i.e. a `RosettaPath` of one element. This matches `BnmCsvIRSType`
  exactly (all attributes are simple `string`/`number`, no nesting, no meta
  wrapping, so JSON property name == attribute name == path element).
- **Write side only** for headers. Read-side implications are noted below.

### Configurability — opt-in, disabled by default (required)

Label headers must be **opt-in**, controlled by a new serialisation format
`CSV_LABELLED`. Plain `CSV` is **unchanged forever** (attribute-name headers, no
provider resolution); only `CSV_LABELLED` resolves the label provider and emits
label headers.

This is not merely a preference — it is a correctness guard. **Every projection and
report function gets a `@RuneLabelProvider` generated** (that is the only way labels
are exposed at runtime; see below). So an "auto-detect: use labels if a provider
exists" approach would silently flip essentially every existing CSV
projection/report pipeline from attribute-name headers to label headers — a silent
breaking change for downstream consumers. The opt-in format avoids that entirely.

The opt-in carrier is the value already persisted in the pipeline config and already
used as the serialiser dispatch key: `PipelineModel.Serialisation.Format`
(`transform/PipelineModel.java:207`).

---

## How labels are exposed at runtime (validated)

Labels are **not** stored on the generated data type. The Rune DSL generates a
`LabelProvider` per transform function:

- `com.rosetta.model.lib.functions.LabelProvider` —
  `String getLabel(RosettaPath path)`; returns `null` when it has no label.
  (`rune-dsl/rune-runtime/.../functions/LabelProvider.java`)
- Generated providers extend `GraphBasedLabelProvider`, walking a graph of
  `LabelNode`s keyed by path elements.
  (`rune-dsl/.../lib/labelprovider/{GraphBasedLabelProvider,LabelNode}.java`)
- `LabelProviderGenerator.xtend` builds the graph from the function's **output
  type**, registering every `[label ...]` annotation keyed by its `RosettaPath`
  (`buildLabelGraph` → `registerLabelAnnotation`). For a flat top-level attribute,
  the key is a single-element path equal to the attribute name.

### The provider is attached to the transform function class

The `@RuneLabelProvider(labelProvider=...)` annotation is generated **onto the
function class**, not the data type:
- Projections/ingests/enrichments: `FunctionGenerator.xtend:124-126`, gated by
  `LabelProviderGeneratorUtil.shouldGenerateLabelProvider` →
  `RosettaFunctionExtensions.getTransformAnnotations` which matches
  `ingest | enrich | projection` (`RosettaFunctionExtensions.java:154`).
- Reports: `ReportGenerator.xtend:40-41`.

So for `Project_BnmTransactionReport_IrSwap_ToBnmCsv`, the generated function
class carries `@RuneLabelProvider`, and its provider keys labels at the top-level
attribute paths of `BnmCsvIRSType`. The lookup we need is simply:

```java
labelProvider.getLabel(RosettaPath.valueOf("csv001_Action")) // -> "001 - Action" (or null)
```

`[projection JSON]` → `[projection CSV]` is orthogonal to provider generation —
both formats are flags on the same `projection` annotation
(`annotations.rosetta:31-35`), and provider generation triggers on the annotation's
presence, not its format. The `CSV` flag is valid syntax.

### Resolving data type → provider: no scanning needed

`rune-common` has **no** classpath-scanning library (Reflections/ClassGraph), and
`@RuneLabelProvider` lives on the function class, not the data type. But the
pipeline metadata already binds the two. Confirmed by the real config added in
BNP PR #4707
(`pipeline-projection-...-bnm-transaction-ir-swap-to-bnm-csv.json`):

```json
"transform" : {
    "type" : "PROJECTION",
    "function" : "bnpp.projection.csv.bnm.trade.functions.Project_BnmTransactionReport_IrSwap_ToBnmCsv",
    "inputType" : "bnpp.regulation.bnm.trade.BNMTransactionReport",
    "outputType" : "bnpp.projection.csv.bnm.trade.BnmCsvIRSType"
},
"outputSerialisation" : { "format" : "CSV_LABELLED" }
```

(To opt in, the BNP pipeline config must set `"format" : "CSV_LABELLED"`. With plain
`"format" : "CSV"` the output keeps attribute-name headers.)

- `PipelineModel.Transform` carries `function` (transform class FQN) and
  `outputType` (`BnmCsvIRSType` FQN). (`transform/PipelineModel.java:109-139`)
- `FunctionNameHelper.getOutputType(Class<? extends RosettaFunction>)` returns the
  `evaluate(...)` return type, confirming the output type from the function class.
  (`transform/FunctionNameHelper.java:47`)

So resolution is: **`transform.function` class → `@RuneLabelProvider` →
`LabelProvider` instance**, with the function FQN supplied by the pipeline. This
directly realises the user's intent ("look up the function, and from it the type")
without any classpath scan.

---

## The Jackson CSV gotcha (important)

`RosettaCsvMapper.RosettaCsvObjectWriter.writeValueAsString` currently does:

```java
CsvSchema schema = mapper.schemaFor(value.getClass()).withHeader();
```

In Jackson CSV a `CsvSchema` column name is used for **two** things:
1. the text in the header row, and
2. matching the POJO's serialised field name to place each value in its column.

So we **cannot** simply rename schema columns to labels — renaming a column to
`"001 - Action"` would stop Jackson matching the `csv001_Action` field and the
value cell would go blank.

### Chosen approach: keep attribute-named columns, rewrite the header line

1. Build the schema with attribute-named columns but **without** a header
   (`schema.withoutHeader()`) so values still bind.
2. Take the column list **from the schema in its own column order** (see ordering
   note below) — the rewrite is order-agnostic, it just maps whatever columns the
   schema has.
3. For each column attribute name, compute
   `label = provider == null ? null : provider.getLabel(RosettaPath.valueOf(name))`,
   falling back to the attribute name when `label == null`.
4. Emit the label header line, then the body.

**Quote/escape the header via Jackson, not by hand.** PR #4707's output shows the
current header already quotes some columns (e.g. `"csv003_ROMS_Transaction_ID"`)
and labels contain spaces, `-`, `/`, `()` (e.g. `009 - IRS/PRS/OIS`). To get
escaping right (and survive any future label containing a comma/quote/newline),
produce the header by writing the ordered list of label strings as a single CSV
record through the same `CsvMapper` (a header-only schema), rather than
`String.join(",", ...)`. Then concatenate `headerLine + body`.

This keeps value binding intact and is robust for the flat case. A custom
`PropertyNamingStrategy`/serializer was considered and rejected as it fights
Jackson's column/field coupling.

**Column ordering (corrected).** `CsvMapper.schemaFor(...)` orders columns by bean
introspection (alphabetical property name), **not** Rune declaration order. PR
#4707 confirms this: the header emits `csv012_Pay_Rate__OP_1_CP_` *before*
`csv012_Pay_Rate__OP_1_CP__Num`, although `_Num` is declared first in the
`.rosetta`. The header-rewrite approach is unaffected (it maps the schema's actual
columns), and it preserves the existing column order — only the header *text*
changes. If a specific column order is ever required, that is a separate concern
(e.g. an explicit ordered schema) and out of scope here.

Note on the BNM model: `csv012_Pay_Rate__OP_1_CP__Num` and
`csv012_Pay_Rate__OP_1_CP_` both carry label `"012 - Pay Rate (1)"` (likewise the
two `csv016_*`). Duplicate header text in the output is acceptable; the columns
remain distinct by position.

---

## Implementation steps

### Step 0 — verify the two open assumptions ✅ DONE (both PASS)

> **Status: VERIFIED — 2026-06-14.** Both assumptions hold. Verified
> authoritatively against the rune-dsl code generator and its integration-test
> fixtures (which compile *and run* the generated provider), rather than by
> building bnpp — the generator is the deterministic source of truth for what the
> generated class looks like, and bnpp itself is not buildable locally (it needs
> the private ISDA GCP Artifact Registry / auth). Implementer: you do **not** need
> to re-run this before Step 1. Evidence is recorded below.

Evidence (all in the `rune-dsl` working dir):
- Generator template:
  `rune-lang/.../generator/java/function/LabelProviderGenerator.xtend:94-122`.
- Integration test (compiles + runs generated provider):
  `rune-integration-tests/.../function/LabelProviderGeneratorTest.java`
  (`testFunctionWithIngestAnnotationGeneratesLabelProvider`, lines 133-159).
- Expected generated fixture:
  `rune-integration-tests/src/test/resources/label-annotations/func-ingest/MyFuncLabelProvider.java`.
- `@RuneLabelProvider` is attached to the function/report class:
  `FunctionGenerator.xtend:125-126`, `ReportGenerator.xtend:40-41`
  (annotation FQN `com.rosetta.model.lib.annotations.RuneLabelProvider`).
- `GraphBasedLabelProvider implements com.rosetta.model.lib.functions.LabelProvider`
  (`rune-runtime/.../labelprovider/GraphBasedLabelProvider.java:10`).

**Verification A — provider instantiation. ✅ PASS.**
The generated provider has a **public no-arg constructor**. The generator emits
exactly:
```java
public class MyFuncLabelProvider extends GraphBasedLabelProvider {
    public MyFuncLabelProvider() {
        super(new LabelNode());
        startNode.addLabel(Arrays.asList("attr"), "My attribute");
    }
}
```
So `labelProvider().getDeclaredConstructor().newInstance()` works. (Note: the DSL
test instantiates it via Guice `injector.getInstance(clazz)`, which also resolves
the public no-arg constructor — so either strategy is fine; reflection is simplest
for the resolver and needs no DI.) The function class carries
`@RuneLabelProvider(labelProvider = ...LabelProvider.class)` as expected.

**Verification B — flat top-level path lookup. ✅ PASS.**
A flat top-level `[label ...]` attribute is keyed at a **single-element**
`RosettaPath` equal to the attribute name, and `getLabel` returns it directly. The
generator emits `startNode.addLabel(Arrays.asList("attr"), "My attribute")` and the
integration test asserts:
- `getLabel(RosettaPath.valueOf("attr"))` → `"My attribute"`, and
- `getLabel(RosettaPath.valueOf("other"))` → `null` (unlabelled → per-column
  fallback applies).

This is exactly the `BnmCsvIRSType` shape. The `BnmCsvChoice` wrapper concern does
**not** apply to our function: `Project_BnmTransactionReport_IrSwap_ToBnmCsv` outputs
`BnmCsvIRSType` **directly** (not the choice), so the generator's `startNode` is
`BnmCsvIRSType` and labels are keyed at `csv001_Action`, `csv002_AD_Ref_No`, … with
no wrapper prefix. Therefore `getLabel(RosettaPath.valueOf("csv001_Action"))` returns
`"001 - Action"`. The resolver must use the **projection function** itself
(`...ToBnmCsv`), not the choice-returning `Project_BnmTransactionReportToBnmCsv`.

**Confirmed against the real bnpp 9.83.0 codegen.** A full bnpp build was run and
the actual generated provider inspected:
`rosetta-source/src/generated/java/bnpp/projection/csv/bnm/trade/labels/Project_BnmTransactionReport_IrSwap_ToBnmCsvLabelProvider.java`.
It matches the prediction exactly — public no-arg constructor, and all 24 flat
top-level labels keyed directly on `startNode`:
```java
public class Project_BnmTransactionReport_IrSwap_ToBnmCsvLabelProvider extends GraphBasedLabelProvider {
    public Project_BnmTransactionReport_IrSwap_ToBnmCsvLabelProvider() {
        super(new LabelNode());
        startNode.addLabel(Arrays.asList("csv001_Action"), "001 - Action");
        startNode.addLabel(Arrays.asList("csv002_AD_Ref_No"), "002 - Ad Ref No");
        ...
        startNode.addLabel(Arrays.asList("csv012_Pay_Rate__OP_1_CP__Num"), "012 - Pay Rate (1)");
        startNode.addLabel(Arrays.asList("csv012_Pay_Rate__OP_1_CP_"),     "012 - Pay Rate (1)"); // duplicate label, distinct key
        ...
        startNode.addLabel(Arrays.asList("csv022_Approval_Code"), "022 - Approval Code");
    }
}
```
The function class carries
`@RuneLabelProvider(labelProvider=Project_BnmTransactionReport_IrSwap_ToBnmCsvLabelProvider.class)`.
Provider package is `bnpp.projection.csv.bnm.trade.labels` (the `labels`
sub-package). No remaining open checks.

Only once both A and B hold, continue to Step 1.

### 1. Label resolution component (new)

`com.regnosys.rosetta.common.serialisation.csv.LabelProviderResolver` (name TBD):
- `static LabelProvider fromTransformFunction(Class<? extends RosettaFunction> fn)`:
  read `@RuneLabelProvider`; instantiate `labelProvider()` via its public no-arg
  constructor; return `null` (or a no-op provider) when the annotation is absent.
- Optional convenience: resolve from a `PipelineModel.Transform` by loading
  `getFunction()` via the supplied `ClassLoader`.
- Unit-testable in isolation (no Jackson).

### 2. Plumb the provider into the writer

- Keep the existing zero-arg `RosettaCsvMapper.createCsvObjectMapper()` behaviour
  **identical** (no provider → attribute-name headers), so the change is purely
  additive / backwards compatible.
- Add a factory/constructor overload taking either a `LabelProvider` or the
  transform function `Class`, e.g.
  `RosettaCsvMapper.createCsvObjectMapper(LabelProvider provider)`.
- Pass the provider into `RosettaCsvObjectWriter`.

### 3. Header rewriting in `RosettaCsvObjectWriter.writeValueAsString`

- Build schema columns as today but `withoutHeader()`.
- Resolve provider for `value.getClass()` (only top-level path lookups needed).
- Build `attributeName -> headerText` for the ordered columns.
- Emit `headerLine + "\n" + bodyWithoutHeader`.
- With no provider, output is byte-for-byte identical to today.

### 4. Wire into the transform/pipeline path (opt-in via `CSV_LABELLED`)

Key constraint: the label provider needs `transform.function`, but the existing
hook `TestPackUtils.getObjectMapper(PipelineModel.Serialisation)`
(`TestPackUtils.java:146`) only receives the `Serialisation` (which carries just
`format`/`configPath`). The `function` FQN lives on the sibling
`PipelineModel.Transform`. Also note `getObjectMapper`/`getObjectWriter` have **no
callers inside rune-common** — they are invoked by external projection-runner
tooling that holds the whole `PipelineModel`.

Plan:
- **New format.** Add `CSV_LABELLED("csv")` to
  `PipelineModel.Serialisation.Format` (`transform/PipelineModel.java:207-222`),
  alongside the existing `CSV("csv")` — same `.csv` file extension, distinct
  dispatch.
- **Mapper creators.** Keep the no-arg `forCSV()` untouched (plain, no provider).
  Add `RosettaObjectMapperCreator.forCSV(LabelProvider)` (and/or
  `forCSV(Class<? extends RosettaFunction>)`), used only by the `CSV_LABELLED` path.
- **Function-aware overload.** Add
  `TestPackUtils.getObjectMapper(PipelineModel, ClassLoader)` (and matching
  `getObjectWriter`) that:
  - `CSV_LABELLED` → resolve the provider from
    `pipelineModel.getTransform().getFunction()` via `LabelProviderResolver` and
    call `forCSV(provider)`;
  - `CSV` → plain `forCSV()` (unchanged);
  - all other formats → delegate to the existing per-`Serialisation` logic.
- **Legacy method.** In `getObjectMapper(PipelineModel.Serialisation)`:
  - `CSV` → plain `forCSV()` as today (unchanged);
  - `CSV_LABELLED` → **throw** `IllegalArgumentException` (or similar) stating the
    labelled format requires the transform function and that callers must use the
    `getObjectMapper(PipelineModel, ClassLoader)` overload. It must **not** silently
    fall back to plain CSV.
- External runners that want labelled headers switch to the `PipelineModel`
  overload **and** set `outputSerialisation.format = CSV_LABELLED`.

---

## Round-trip / deserialisation

`RosettaCsvMapper.readValue(...)` expects headers == attribute names. A
label-headed file will not bind on read (labels != property names). Per the
agreed write-only scope:
- Document that label-headed CSV is **not** round-trippable by the current reader.
- Optional follow-up: a reverse `label -> attributeName` remap on read, built from
  the same provider, behind an opt-in flag.
- The existing `RosettaCsvMapperTest` round-trip tests use the no-arg mapper (no
  provider) and must keep passing unchanged.

---

## Testing

Add tests under `common/src/test/java/.../serialisation/csv/`:

1. **Labels present → label headers.** Add a small `.rosetta` test model under
   `common/src/test/resources/rosetta/` modelled on `BnmCsvIRSType` (a projection
   func + a flat type with top-level `[label ...]`), compiled by
   `rune-maven-plugin` during `generate-test-sources`. Assert headers use labels
   and value cells are correct and correctly ordered.
2. **Label absent → attribute-name header** (per-column fallback).
3. **Mixed** labelled/unlabelled attributes.
4. **Plain `CSV` unchanged** → output byte-for-byte identical to current behaviour
   (regression guard; reuse the existing `User` fixture). The `CSV_LABELLED` path is
   the only one that emits labels.
5. **Duplicate labels** (mirror `csv012`/`csv016`) → both columns emit the same
   header text, values stay in their own columns.
6. **`LabelProviderResolver`** unit test: function class → `@RuneLabelProvider` →
   provider, plus the no-annotation case.
7. **Legacy method rejects `CSV_LABELLED`** →
   `getObjectMapper(PipelineModel.Serialisation)` throws for the labelled format
   (no function available), with the message pointing at the `PipelineModel`
   overload.
8. **`CSV_LABELLED` via the `PipelineModel` overload** → label headers emitted end
   to end (provider resolved from `transform.function`).

Per CLAUDE.md, run `mvn test-compile` first — generated types land in
`target/test-classes/serialisation/java/`.

---

### Concrete before/after (BNM, from PR #4707 output)

Current header (attribute names):

```
csv001_Action,csv002_AD_Ref_No,"csv003_ROMS_Transaction_ID",csv004_Trade_Date,...
CANCEL_F,452175062BF,,,...
```

Target header (labels, same column order, same value row):

```
001 - Action,002 - Ad Ref No,003 - Roms Transaction ID,004 - Trade Date,...
CANCEL_F,452175062BF,,,...
```

## Resolved assumptions (from the BNM model + PR #4707)

- Provider is generated for **projection** functions, not only reports (transform
  annotations include `projection`). ✔
- Labels declared directly on flat top-level type attributes are registered at the
  single-element `RosettaPath` equal to the attribute name. ✔
- `BnmCsvIRSType` attributes are simple `string`/`number` with no meta wrapping, so
  JSON property name == attribute name == path element (no name-mismatch handling
  needed for this case). ✔
- `[projection CSV]` is valid DSL syntax and does not change provider generation. ✔
- The pipeline config provides `transform.function`
  (`...functions.Project_BnmTransactionReport_IrSwap_ToBnmCsv`),
  `transform.outputType` (`...BnmCsvIRSType`), and `outputSerialisation.format=CSV`
  — exactly the inputs needed to resolve the provider. ✔
- `CsvMapper.schemaFor` column order is alphabetical, not declaration order
  (header-rewrite is order-agnostic, so this is fine). ✔

## Previously-open assumptions — now VERIFIED (see Step 0)

Both former open assumptions have been confirmed (2026-06-14). They are no longer
blockers; full evidence is in **Step 0** above.

1. ✅ The generated provider
   (`...Project_BnmTransactionReport_IrSwap_ToBnmCsvLabelProvider`) has a public
   no-arg constructor — confirmed from the generator template and fixture.
2. ✅ `getLabel(RosettaPath.valueOf("csv001_Action"))` keys flat top-level paths
   directly (single-element `RosettaPath` == attribute name); the `BnmCsvChoice`
   wrapper does not apply because the projection function outputs `BnmCsvIRSType`
   directly. Unlabelled attributes return `null` → per-column attribute-name
   fallback.

3. ✅ Confirmed against bnpp's exact DSL `9.83.0`: a full bnpp build was run and the
   real generated `Project_BnmTransactionReport_IrSwap_ToBnmCsvLabelProvider`
   matches exactly (all 24 labels keyed at single-element paths, public no-arg ctor,
   `@RuneLabelProvider` on the function class). **No remaining checks.**

---

## Files likely to change

- `common/.../transform/PipelineModel.java` — add `CSV_LABELLED("csv")` to the `Format` enum.
- `common/.../serialisation/RosettaCsvMapper.java` — provider plumbing + header rewrite.
- `common/.../serialisation/RosettaObjectMapperCreator.java` — `forCSV(LabelProvider)` overload.
- `common/.../serialisation/csv/LabelProviderResolver.java` — **new**.
- `common/.../transform/TestPackUtils.java` — new `getObjectMapper(PipelineModel, ClassLoader)` overload dispatching `CSV_LABELLED`; legacy `Serialisation`-only method throws for `CSV_LABELLED`.
- `common/.../transform/FunctionNameHelper.java` — reuse `getOutputType` (no change expected).
- Tests + a small `.rosetta` test model under `common/src/test/resources/rosetta/`.
