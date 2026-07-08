# Why location-annotated choice types get wrapped, not flattened

## Background

While investigating an "unexpected" serialization shape in a CDM-generated sample, we
found a JSON structure in the wild that our test suite didn't cover: a `[metadata location]`
field whose value resolves, one or more levels down, through another `[metadata location]`-annotated
choice option. The nesting looked surprising at first, so this note explains why the shape is
correct, reproduces it in `serialization`'s test suite, and pins down the actual mechanism (not
just a plausible-sounding one) behind it.

## The CDM example

The sample is `CAD-Long-Initial-Stub-versioned.json` in `common-domain-model`. The relevant
fragment:

```json
"observable" : {
  "@key:scoped" : "observable-1",
  "@data" : {
    "@type" : "cdm.observable.asset.InterestRateIndex",
    "@key:scoped" : "InterestRateIndex-1",
    "@data" : {
      "@type" : "cdm.observable.asset.FloatingRateIndex",
      "identifier" : [ { "identifier" : { "@data" : "CAD-BA-CDOR" }, "identifierType" : "Other" } ],
      "assetType" : "Other",
      "assetClass" : "InterestRate",
      "floatingRateIndex" : { "@data" : "CAD-BA-CDOR" },
      "indexTenor" : { "periodMultiplier" : 3, "period" : "M" }
    }
  }
}
```

Two separate `[metadata location]` annotations are stacked here, from the CDM Rosetta model
(`observable-asset-type.rosetta`):

```
type PriceQuantity:
    ...
    observable Observable (0..1)
        [metadata location]           // <-- field-level annotation
```

```
choice Index:
    CreditIndex
    EquityIndex
    InterestRateIndex
        [metadata location]           // <-- choice-option-level annotation
    ForeignExchangeRateIndex
    OtherIndex

choice InterestRateIndex:
    FloatingRateIndex                 // <-- no location; concrete leaf, flattens
    InflationIndex
```

`observable`'s declared type, `Observable`, is a choice of `Asset | Basket | Index`. The runtime
value resolves through `Index` down to `InterestRateIndex`, and separately down to the concrete
`FloatingRateIndex`. `Index` itself carries no location, so the walk from `Observable` to `Index`
to `InterestRateIndex` is transparent — nothing is emitted for those hops except the eventual
`@type`. Two things *do* carry `[metadata location]`, and each produces its own wrap:

1. The `observable` **field** — produces the outer `{"@key:scoped": "observable-1", "@data": ...}`.
2. The `InterestRateIndex` **choice option** — produces the inner
   `{"@type": "...InterestRateIndex", "@key:scoped": "InterestRateIndex-1", "@data": ...}`.

The field-level wrap has no `@type` next to it. The option-level wrap does. Both of those
observations are explained below, once the underlying mechanism is established.

## Reproducing it in `serialization`'s tests

The scenario wasn't covered anywhere in `serialization`'s test suite, so we extended the existing
`choicetype` round-trip group
(`serialization/src/test/resources/rune-serializer-round-trip-test/choicetype/choice.rosetta`),
reusing its pre-existing `ChoiceData` (a choice of `A | ExtA | B | C | D | F | G`) and `A` types:

```
choice ChoiceWithLocationNested:
  ChoiceData
    [metadata location]                    // choice-option-level location, like InterestRateIndex

type Root:
  ...
  choiceWithFieldAndOptionLocation ChoiceWithLocationNested (0..1)
    [metadata location]                    // field-level location, like PriceQuantity.observable
```

With the corresponding round-trip fixture,
`choice-data-nested-with-field-and-option-meta-location.json`:

```json
{
  "@model" : "serialization",
  "@type" : "serialization.test.passing.choicetype.Root",
  "@version" : "0.0.0",
  "choiceWithFieldAndOptionLocation" : {
    "@key:scoped" : "someOuterLocation",
    "@data" : {
      "@type" : "serialization.test.passing.choicetype.ChoiceData",
      "@key:scoped" : "someNestedLocation",
      "@data" : {
        "@type" : "serialization.test.passing.choicetype.A",
        "fieldA" : "foo"
      }
    }
  }
}
```

This is a structural match for the CDM fragment: an outer field-level wrap with no `@type`,
containing a choice-option-level wrap with `@type` + its own `@key:scoped`, containing the
flattened concrete leaf. It round-trips (deserialize → reserialize) byte-for-byte, confirming the
"new" `serialization` module handles this shape correctly.

For contrast, the group also has `choiceWithLocationNested`, the option-level-only version (no
field-level annotation), whose fixture (`choice-data-nested-with-meta-location.json`) shows the
same choice-option wrap but with nothing above it:

```json
"choiceWithLocationNested" : {
  "@type" : "serialization.test.passing.choicetype.ChoiceData",
  "@key:scoped" : "someNestedLocation",
  "@data" : {
    "@type" : "serialization.test.passing.choicetype.A",
    "fieldA" : "foo"
  }
}
```

Here `@type` sits right next to `@key:scoped`, because — unlike the field-level case — the field's
*own* declared type is the abstract `ChoiceWithLocationNested` choice, so serializing the field at
all requires announcing which option (`ChoiceData` vs. `string`) was selected, and that
announcement happens to coincide with the option that carries the location.

## The mechanism

Every other `[metadata location]`/`[metadata key]`/`[metadata scheme]`-annotated field in the
codebase **unwraps and flattens** rather than nesting under `@data`. For example
(`serialization/src/test/resources/rune-serializer-round-trip-test/metalocation/`):

```
type A:
  b B (1..1)
    [metadata location]

type B:
  fieldB string (1..1)
```

```json
"typeA" : {
  "b" : {
    "@key:scoped" : "someLocation",
    "fieldB" : "foo"
  }
}
```

`b`'s value (`B`) is merged flat alongside `@key:scoped` — no `@data` key anywhere. So why do
`observable`/`choiceWithFieldAndOptionLocation` behave differently? There are two things to
explain: (1) why the *wrapping* happens at all instead of flattening, and (2) why a *nested* choice
sometimes stays fully transparent (`choice-deep-nested.json`) and sometimes doesn't
(`choice-data-nested-with-meta-location.json`), even though both go through the same
`RuneChoiceTypeSerializer` the whole way down.

### 1. Why plain beans unwrap but choices/scalars don't

Unwrapping is implemented in
`serialization/src/main/java/org/finos/rune/mapper/introspector/RuneJsonAnnotationIntrospector.java`:

```java
@Override
public NameTransformer findUnwrappingNameTransformer(AnnotatedMember member) {
    RuneMetaType ann = _findAnnotation(member, RuneMetaType.class);
    if (ann == null) {
        return super.findUnwrappingNameTransformer(member);
    }
    return NameTransformer.NOP;
}
```

`NameTransformer.NOP` tells Jackson: take the nested value's serialized *properties* and merge
them straight into the parent object, dropping the wrapping key. This works for `B` because `B`
is an ordinary bean — Jackson's standard `BeanSerializer` writes it property-by-property
(`fieldB: "foo"`), and the unwrapping machinery can intercept that property stream and splice it
into the parent.

Choice types don't go through that pipeline at all. They're handled by a fully custom
`JsonSerializer`,
`serialization/src/main/java/org/finos/rune/mapper/serializer/RuneChoiceTypeSerializer.java`:

```java
private void writeChoiceValue(ChoiceValue choiceValue, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    Object selectedValue = choiceValue.value;
    validateDeclaredChoiceOptionType(choiceValue, serializers);
    if (selectedValue instanceof RosettaModelObject) {
        RosettaModelObject selectedRosettaValue = (RosettaModelObject) selectedValue;
        if (isChoiceType(selectedRosettaValue.getType())) {
            ChoiceValue nestedChoiceValue = getChoiceValue(selectedRosettaValue);
            if (nestedChoiceValue == null) {
                throw new IOException("Nested Rune choice has no selected value for " + selectedRosettaValue.getType().getName());
            }
            writeChoiceValue(nestedChoiceValue, gen, serializers);
            return;
        }

        gen.writeStringField(RuneJsonConfig.MetaProperties.TYPE, resolveChoiceTypeName(selectedRosettaValue));
        writeRosettaFields(selectedRosettaValue, gen, serializers);
    } else {
        gen.writeStringField(RuneJsonConfig.MetaProperties.TYPE, choiceValue.choiceOptionType);
        gen.writeFieldName(DATA);
        serializers.defaultSerializeValue(selectedValue, gen);
    }
}

private boolean isChoiceType(Class<?> type) {
    return type != null && type.isAnnotationPresent(RuneChoiceType.class);
}
```

This serializer talks to the `JsonGenerator` directly — `writeStartObject`/`writeStringField`/
`writeFieldName` — rather than exposing a set of named bean properties Jackson can enumerate and
splice. `NameTransformer`-based unwrapping has nothing to hook into: there's no property list to
intercept, just one opaque call that emits a complete object (or, for a basic value like
`string`/`number`, a bare scalar with even less structure to unwrap). Basic scalars fail to
unwrap for the same underlying reason from the other direction — a raw scalar has no named
properties *at all*, so "merge its properties into the parent" is meaningless.

### 2. Why nested choices sometimes stay flat, and sometimes don't

`RuneChoiceTypeSerializer` is the serializer for every hop in *both*
`choice-deep-nested.json` (`ChoiceDeepNested → MiddleChoiceA → ChoiceData → A`, no location
anywhere) and `choice-data-nested-with-meta-location.json`
(`ChoiceWithLocationNested → ChoiceData [metadata location] → A`). The difference between them
comes entirely from the `isChoiceType(...)` check in the code above, and what value that check is
actually run against at each hop.

**No location anywhere (`choice-deep-nested.json`):** each option's accessor
(`getMiddleChoiceA()`, `getChoiceData()`, ...) returns the raw choice-typed object directly,
because there's no metadata annotation forcing a wrapper. So at every hop, `selectedValue` really
is a plain `@RuneChoiceType`-annotated instance, `isChoiceType(...)` is true, and `writeChoiceValue`
calls **itself directly** — an ordinary private Java method call, not a new Jackson serializer
dispatch. No new JSON object is opened; everything happens inside the single `{ }` that the
top-level `serialize()` call started. The walk only stops recursing once it reaches `A`, which is
a concrete type (not `@RuneChoiceType`-annotated), so it falls to the `@type` + flattened-fields
branch — giving the fully flat:

```json
"choiceDeepNested" : {
  "@type" : "serialization.test.passing.choicetype.A",
  "fieldA" : "foo"
}
```

**Location on the `ChoiceData` option (`choice-data-nested-with-meta-location.json`):** because
that specific option carries `[metadata location]`, rune-dsl generates its accessor to return
`FieldWithMeta<ChoiceData>` instead of a raw `ChoiceData`. `FieldWithMetaChoiceData` is the same
generic wrapper class used for `B`, `F`, `dateField`, etc. — it is **never** itself
`@RuneChoiceType`-annotated. So at this hop, `isChoiceType(FieldWithMetaChoiceData.class)` is
**false**, the direct-recursion shortcut is skipped, and the code falls to:

```java
gen.writeStringField(TYPE, resolveChoiceTypeName(selectedRosettaValue));
writeRosettaFields(selectedRosettaValue, gen, serializers);
```

`resolveChoiceTypeName` detects the `FieldWithMeta` wrapping and substitutes the *wrapped* type's
name (`ChoiceData`) for `@type`, which is why the JSON says `"@type": "...ChoiceData"` rather than
`"@type": "...FieldWithMetaChoiceData"`. `writeRosettaFields` then serializes
`FieldWithMetaChoiceData` through **ordinary Jackson bean serialization** (buffered via
`TokenBuffer` + `defaultSerializeValue`), because to Jackson this wrapper is just a bean with
`meta`/`value` properties, not something `RuneChoiceTypeSerializer` treats specially. That normal
bean walk reaches the `value` property (renamed `@data`), still typed `ChoiceData`. Serializing
*that* property is a **fresh, independent Jackson serializer lookup** — not the private recursive
call — Jackson finds `@RuneChoiceType` on `ChoiceData` again, and invokes a **brand-new**
`RuneChoiceTypeSerializer.serialize()` call, which opens its own `{ }` object. That new object is
exactly the `"@data": {"@type": "...A", "fieldA": "foo"}` boundary we see.

So the serializer never asks "should I unwrap here?" There's one check,
`isChoiceType(selectedRosettaValue.getType())`. A `[metadata location]` annotation on a choice
option doesn't flip that check directly — it changes *what Java type shows up* at that hop, from a
raw choice object to a `FieldWithMeta` wrapper, and it's the wrapper failing that check (because it
is never `@RuneChoiceType`-annotated) that breaks the recursive short-circuit and forces a brand
new, independently-dispatched `serialize()` call — which is what creates the `@data` object
boundary.

### 3. It's the value type, not where the annotation sits

It's tempting to conclude the rule is "annotate a field → flatten; annotate a choice option →
wrap." That's not right. A field and a choice option are the same underlying mechanism — a
`[metadata ...]`-annotated attribute gets a `FieldWithMeta`-style wrapper regardless of whether
it's a type's own field or an option listed inside a `choice` declaration. What actually decides
flatten-vs-wrap is the *value type* held by that attribute, not which of the two syntactic
positions the annotation is written in:

| | value is a plain bean | value is itself a choice (or a scalar) |
|---|---|---|
| **annotation on a field** | `metalocation`'s `b B (1..1) [metadata location]` → flattens, no `@data` | `choiceWithFieldAndOptionLocation`: field typed `ChoiceWithLocationNested` (a choice) → wraps in `@data` |
| **annotation on a choice option** | `choicetype`'s `ChoiceData: ... F [metadata location] ...` (`F` is a plain bean) → flattens, no separate `@data` envelope:<br>`{"@type": "...F", "@key:scoped": "someLocation", "fieldF": "foo"}` | `ChoiceWithLocationNested: ChoiceData [metadata location], string` (`ChoiceData` is itself a choice) → wraps in `@data` |

Both rows behave identically depending on the column, not the row. So "field vs. choice option"
isn't the root cause — it's a red herring. The only thing that matters is whether Jackson can walk
the value's properties one by one (a plain bean) or not (a scalar, or another choice handled by
the custom `RuneChoiceTypeSerializer`).

Both families of `@data`-without-sibling-`@type` in the test suite trace back to the same root
cause — the wrapped value isn't produced by Jackson's ordinary bean-property pipeline — just via
two different value shapes:

| Wrapped value kind | Why it can't unwrap | Examples |
|---|---|---|
| Basic scalar (`String`, `date`, enum) | No properties exist to merge | `metakey/attribute-key-with-ref.json` (`dateField`), `metascheme/enum-single.json`, `choicetype/choice-data-with-meta-scheme.json` (`fieldC`), `overriding/overriding-foo2.json` (`stringAttr`) |
| Choice-typed value (`@RuneChoiceType`) | Serialized by a custom `JsonSerializer`, not a bean property stream | `choicetype/choice-data-nested-with-field-and-option-meta-location.json` (`choiceWithFieldAndOptionLocation`) — and CDM's `observable` |

## Where the key-clash *does* matter

Our working assumption going in was that this all exists to avoid a `@key:scoped` clash. That's a
real, demonstrable *consequence* in our specific test case — but it isn't the general mechanism
explained above, and treating it as the mechanism would predict wrapping only when a clash is
possible. We checked that directly: point a `[metadata location]` field at a choice with **no**
location anywhere inside it — no key to clash with:

```
choice ChoiceBasic:
  string
  number

type Root:
  [rootType]
  choiceBasicWithLocation ChoiceBasic (0..1)
    [metadata location]
```

It still wraps:

```json
"choiceBasicWithLocation" : {
  "@key:scoped" : "onlyLocation",
  "@data" : {
    "@type" : "string",
    "@data" : "hello"
  }
}
```

No clash was possible there, and it wrapped anyway — confirming the explanation has to be
structural (as above), not "would collide."

That said, the key-clash intuition isn't wrong — it's just downstream of the structural cause
rather than the cause itself. In our field-and-option test case, if unwrapping *had* somehow been
attempted, it would have produced this (invalid) object:

```json
"choiceWithFieldAndOptionLocation" : {
  "@key:scoped" : "someOuterLocation",
  "@type" : "serialization.test.passing.choicetype.ChoiceData",
  "@key:scoped" : "someNestedLocation",
  "fieldA" : "foo"
}
```

— two `@key:scoped` keys in the same JSON object, which is ambiguous/invalid. So for any case
where the wrapped choice option is *itself* independently location-annotated, flattening isn't
just architecturally unsupported, it would be semantically broken even if it were supported. The
`ChoiceBasic` experiment above shows this isn't a *necessary* condition for wrapping (wrapping
happens regardless), but it does mean that even a hypothetical future unwrap-capable choice
serializer would still have to fall back to nesting in exactly this scenario — a field-level
location wrapping a value that resolves through another location-annotated choice option.

## Summary

- The root cause is **not** "annotation on a field vs. annotation on a choice option" — both are
  the same `FieldWithMeta`-style mechanism, and either one flattens fine when it wraps a plain
  bean (`B`, `F`).
- `@data` appears without a sibling `@type` in exactly two situations, both keyed on the *value
  type*, not the annotation's position: a `[metadata ...]`-annotated basic/scalar value, or a
  `[metadata location]`-annotated value whose declared type is itself a choice.
- Both trace to the same cause: the wrapped value isn't serialized via Jackson's bean-property
  pipeline, so the `NameTransformer.NOP` unwrapping used everywhere else has nothing to attach to.
- At the choice level specifically, `RuneChoiceTypeSerializer` decides whether to recurse
  transparently (same JSON object) or open a new nested object purely via one check —
  `isChoiceType(selectedValue.getType())`. A `[metadata location]` on a choice option changes the
  Java type at that hop from a raw choice object to a `FieldWithMeta` wrapper, which fails that
  check and forces a brand-new, independently-dispatched serializer call — that's the mechanical
  origin of the `@data` boundary.
- A `@key:scoped` clash would indeed occur if the choice-typed case were ever flattened while the
  chosen option also carries its own location — but that's a consequence of the structural
  limitation, not the reason for it, since the same wrapping is observed even with no clash
  possible at all.
