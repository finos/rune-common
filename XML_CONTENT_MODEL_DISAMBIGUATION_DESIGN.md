# XML Content-Model Disambiguation Runtime Design

## Purpose

`rune-common` needs a generic XML deserialization mechanism for Rosetta types where a flat XML object contains the same XML element name for more than one logical Rosetta property path. Jackson's XML module maps a field name to one property at a time, so name-only dispatch is not enough when the correct target depends on sibling order, cardinality, or a surrounding content model.

This document designs the `rune-common` runtime changes only. It treats `contentModel` as optional XML configuration supplied through the existing `RosettaObjectMapperCreator.forXML(...)` flow. The runtime must be able to interpret that configuration without depending on anything outside `rune-common`.

## Goals

- Add optional XML content-model metadata to `RosettaXMLConfiguration`.
- Route ambiguous XML child elements to the correct Rosetta property path before normal Jackson builder binding.
- Preserve the existing XML serializer behavior for the initial implementation.
- Preserve existing substitution-group, virtual unwrapping, XML attribute, XML text-value, enum, and date/time behavior.
- Support ordered content, alternatives, unordered content, wildcard content, cardinality, repeated virtual paths, and multi-layer virtual paths.
- Fail loudly when a configured content model cannot route the XML deterministically.

## Non-Goals

- No API or tooling for creating XML config files.
- No global replacement of Jackson XML deserialization.
- No tree-model conversion of XML objects to `JsonNode`.
- No serializer ordering implementation in the first runtime change.
- No silent fallback to default Jackson routing after a configured content-model mismatch.

## Runtime Config Contract

Add `contentModel` to `TypeXMLConfiguration`:

```java
private final Optional<XMLContentModel> contentModel;
```

The JSON config remains backward compatible because `contentModel` is optional and older configs do not include it.

### Node Type

Use an enum, not raw strings:

```java
public enum XMLContentModelNodeType {
    ELEMENT,
    SEQUENCE,
    CHOICE,
    ALL,
    ANY
}
```

The JSON representation should use enum strings:

```json
{
  "nodeType": "SEQUENCE",
  "children": []
}
```

### Content Model Shape

Add immutable config classes under:

```text
common/src/main/java/com/regnosys/rosetta/common/serialisation/xml/config
```

Suggested API:

```java
public final class XMLContentModel {
    private final XMLContentModelNodeType nodeType;
    private final Optional<String> xmlName;
    private final Optional<String> namespace;
    private final Optional<List<String>> path;
    private final Optional<Integer> minOccurs;
    private final Optional<OccursMax> maxOccurs;
    private final Optional<List<XMLContentModel>> children;
}
```

`children` is the runtime field name for nested nodes.

`path` is the logical Rosetta property path that receives the XML element. If the route crosses virtual Rosetta attributes, those virtual attribute names are included in the path:

```json
{
  "nodeType": "ELEMENT",
  "xmlName": "tradeId",
  "path": ["tradeIdentifierChoice", "tradeId"]
}
```

Do not add `virtualPath`. It is redundant:

- `path` already includes the virtual attribute segment.
- `TypeXMLConfiguration.attributes` already identifies virtual attributes with `xmlRepresentation: "VIRTUAL"`.
- Runtime grouping can derive virtual object boundaries from matched content-model occurrences and virtual path prefixes.

Do not add `routingOnly`. Partial content models are allowed by definition:

- XML names mentioned in the model participate in routing.
- XML names not mentioned in the model pass through unchanged.
- A mode flag would add ambiguity without adding information.

### Occurs Max

`maxOccurs` must support finite numbers and `"unbounded"`. Prefer a small value type over overloading raw strings throughout the matcher:

```java
public final class OccursMax {
    private final Optional<Integer> value;

    public int boundedValue(int inputRemaining) {
        return value.orElse(inputRemaining);
    }
}
```

Jackson JSON support should accept:

```json
{ "maxOccurs": 1 }
```

and:

```json
{ "maxOccurs": "unbounded" }
```

Default occurrence values:

- `minOccurs`: `1`
- `maxOccurs`: `1`

### Minimal Config Example

```json
{
  "com.rosetta.test.AmbiguousTradeIdentifier": {
    "attributes": {
      "tradeIdentifierChoice": {
        "xmlRepresentation": "VIRTUAL"
      }
    },
    "contentModel": {
      "nodeType": "CHOICE",
      "children": [
        {
          "nodeType": "SEQUENCE",
          "children": [
            {
              "nodeType": "ELEMENT",
              "xmlName": "issuer",
              "path": ["issuer"],
              "minOccurs": 0,
              "maxOccurs": 1
            },
            {
              "nodeType": "ELEMENT",
              "xmlName": "tradeId",
              "path": ["tradeId"],
              "minOccurs": 0,
              "maxOccurs": 1
            }
          ]
        },
        {
          "nodeType": "SEQUENCE",
          "children": [
            {
              "nodeType": "ELEMENT",
              "xmlName": "partyReference",
              "path": ["partyReference"],
              "minOccurs": 1,
              "maxOccurs": 1
            },
            {
              "nodeType": "CHOICE",
              "minOccurs": 0,
              "maxOccurs": "unbounded",
              "children": [
                {
                  "nodeType": "ELEMENT",
                  "xmlName": "tradeId",
                  "path": ["tradeIdentifierChoice", "tradeId"],
                  "minOccurs": 1,
                  "maxOccurs": 1
                },
                {
                  "nodeType": "ELEMENT",
                  "xmlName": "versionedTradeId",
                  "path": ["tradeIdentifierChoice", "versionedTradeId"],
                  "minOccurs": 1,
                  "maxOccurs": 1
                }
              ]
            }
          ]
        }
      ]
    }
  }
}
```

## Minimized FpML Runtime Examples

These examples are derived from the FpML `TradeIdentifier` and
`FxTargetKnockoutForward` conflicts, but are written as minimal runtime fixtures.
Unit tests in `rune-common` should not require real FpML classes. The important
part is that the fixture types reproduce the same XML shape, Rosetta property
paths, virtual attributes, occurrence bounds, and expected object graph.

Each example below includes enough information to create a deterministic unit
test:

- minimal Rosetta fixture types,
- XML config fragments,
- input XML,
- expected logical object,
- expected routing behavior,
- explicit assertions.

### Shared Test Helpers

The following small value and reference types are enough to test that routed
child subtrees keep XML attributes and XML text values intact while the parent
object field name is rewritten.

```rosetta
type FpmlTextValue:
  value string (1..1)
  scheme string (0..1)

type FpmlReference:
  href string (1..1)

type FpmlRegion:
  id string (0..1)

type FpmlPivot:
  id string (0..1)
```

XML config for the helpers:

```json
{
  "com.rosetta.test.FpmlTextValue": {
    "attributes": {
      "value": {
        "xmlRepresentation": "VALUE"
      },
      "scheme": {
        "xmlRepresentation": "ATTRIBUTE"
      }
    }
  },
  "com.rosetta.test.FpmlReference": {
    "attributes": {
      "href": {
        "xmlRepresentation": "ATTRIBUTE"
      }
    }
  },
  "com.rosetta.test.FpmlRegion": {
    "attributes": {
      "id": {
        "xmlRepresentation": "ATTRIBUTE"
      }
    }
  },
  "com.rosetta.test.FpmlPivot": {
    "attributes": {
      "id": {
        "xmlRepresentation": "ATTRIBUTE"
      }
    }
  }
}
```

For these examples, `FpmlTextValue.scheme` represents FpML attributes such as
`tradeIdScheme` or `issuerIdScheme`. A real fixture may use more specific
attribute names if desired. Routing behavior does not depend on the attribute
name.

The expected logical objects are the required end-to-end test assertions. The
expected routed token shapes are for lower-level matcher or rewrite-helper tests.
When a token shape needs duplicate field names, it is represented as an ordered
field-event array because strict JSON objects cannot represent duplicate keys.

### Example 1: TradeIdentifier `tradeId`

This fixture reproduces the FpML case where the XML element name `tradeId` can
mean either:

- direct path `tradeId`, or
- virtual path `tradeIdentifierChoice.tradeId`.

The `partyReference` element selects the second branch. The content-model order
is intentionally the XML content order, not the Rosetta type declaration order.
In the Rosetta type, `tradeId` appears before `partyReference`; in the content
model branch, `partyReference` appears before the repeated choice that contains
the virtual `tradeId`.

Fixture types:

```rosetta
type FpmlTradeIdentifier:
  id string (0..1)
  issuer FpmlTextValue (0..1)
  tradeId FpmlTextValue (0..1)
  partyReference FpmlReference (0..1)
  accountReference FpmlReference (0..1)
  tradeIdentifierChoice FpmlTradeIdentifierChoice (0..*)

type FpmlTradeIdentifierChoice:
  tradeId FpmlTextValue (0..1)
  versionedTradeId FpmlTextValue (0..1)
```

Type config:

```json
{
  "com.rosetta.test.FpmlTradeIdentifier": {
    "attributes": {
      "id": {
        "xmlRepresentation": "ATTRIBUTE"
      },
      "tradeIdentifierChoice": {
        "xmlRepresentation": "VIRTUAL"
      }
    },
    "contentModel": {
      "nodeType": "CHOICE",
      "children": [
        {
          "nodeType": "SEQUENCE",
          "children": [
            {
              "nodeType": "ELEMENT",
              "xmlName": "issuer",
              "path": ["issuer"],
              "minOccurs": 1,
              "maxOccurs": 1
            },
            {
              "nodeType": "ELEMENT",
              "xmlName": "tradeId",
              "path": ["tradeId"],
              "minOccurs": 1,
              "maxOccurs": 1
            }
          ]
        },
        {
          "nodeType": "SEQUENCE",
          "children": [
            {
              "nodeType": "ELEMENT",
              "xmlName": "partyReference",
              "path": ["partyReference"],
              "minOccurs": 1,
              "maxOccurs": 1
            },
            {
              "nodeType": "ELEMENT",
              "xmlName": "accountReference",
              "path": ["accountReference"],
              "minOccurs": 0,
              "maxOccurs": 1
            },
            {
              "nodeType": "CHOICE",
              "minOccurs": 0,
              "maxOccurs": "unbounded",
              "children": [
                {
                  "nodeType": "ELEMENT",
                  "xmlName": "tradeId",
                  "path": ["tradeIdentifierChoice", "tradeId"],
                  "minOccurs": 1,
                  "maxOccurs": 1
                },
                {
                  "nodeType": "ELEMENT",
                  "xmlName": "versionedTradeId",
                  "path": ["tradeIdentifierChoice", "versionedTradeId"],
                  "minOccurs": 1,
                  "maxOccurs": 1
                }
              ]
            }
          ]
        }
      ]
    }
  }
}
```

Input XML:

```xml
<FpmlTradeIdentifier id="ti-1">
  <partyReference href="party-1"/>
  <tradeId scheme="urn:trade-id">ABC-123</tradeId>
</FpmlTradeIdentifier>
```

Expected logical object:

```json
{
  "id": "ti-1",
  "partyReference": {
    "href": "party-1"
  },
  "tradeIdentifierChoice": [
    {
      "tradeId": {
        "value": "ABC-123",
        "scheme": "urn:trade-id"
      }
    }
  ]
}
```

The direct `tradeId` property on `FpmlTradeIdentifier` must be absent. The
virtual `tradeIdentifierChoice[0].tradeId` must be present.

Expected routed parent token shape if the aggregate synthetic property design is
used:

```json
{
  "id": "ti-1",
  "partyReference": {
    "href": "party-1"
  },
  "@xmlContentModel": [
    {
      "path": ["tradeIdentifierChoice", "tradeId"],
      "occurrence": "tradeIdentifierChoice[0]",
      "value": {
        "scheme": "urn:trade-id",
        "value": "ABC-123"
      }
    }
  ]
}
```

Minimum unit test assertions:

- `actual.getId()` is `ti-1`.
- `actual.getPartyReference().getHref()` is `party-1`.
- `actual.getTradeId()` is `null`.
- `actual.getTradeIdentifierChoice().size()` is `1`.
- `actual.getTradeIdentifierChoice().get(0).getTradeId().getValue()` is
  `ABC-123`.
- `actual.getTradeIdentifierChoice().get(0).getTradeId().getScheme()` is
  `urn:trade-id`.

Add a control input for the direct branch:

```xml
<FpmlTradeIdentifier id="ti-2">
  <issuer scheme="urn:issuer">BANK-A</issuer>
  <tradeId scheme="urn:trade-id">ABC-123</tradeId>
</FpmlTradeIdentifier>
```

The control assertion is the inverse: direct `tradeId` is present and
`tradeIdentifierChoice` is empty.

### Example 2: FxTargetKnockoutForward `constantPayoffRegion`

This fixture reproduces the FpML case where `constantPayoffRegion` exists both
as a direct repeated property and as a repeated virtual choice property.

The minimized content model keeps only the ambiguous payoff-region window. Other
fields are omitted from `contentModel` and therefore pass through unchanged.
The routing rule is:

1. leading `constantPayoffRegion` elements before the first
   `linearPayoffRegion` route to direct `constantPayoffRegion`,
2. the first `linearPayoffRegion` routes to direct `linearPayoffRegion`,
3. later `constantPayoffRegion` and `linearPayoffRegion` elements route to
   `fxTargetKnockoutForwardChoice`.

Fixture types:

```rosetta
type FpmlFxTargetKnockoutForward:
  pivot FpmlPivot (0..1)
  constantPayoffRegion FpmlRegion (0..*)
  linearPayoffRegion FpmlRegion (0..1)
  fxTargetKnockoutForwardChoice FpmlFxTargetKnockoutForwardChoice (0..*)
  barrier FpmlRegion (0..*)

type FpmlFxTargetKnockoutForwardChoice:
  constantPayoffRegion FpmlRegion (0..1)
  linearPayoffRegion FpmlRegion (0..1)
```

Type config:

```json
{
  "com.rosetta.test.FpmlFxTargetKnockoutForward": {
    "attributes": {
      "fxTargetKnockoutForwardChoice": {
        "xmlRepresentation": "VIRTUAL"
      }
    },
    "contentModel": {
      "nodeType": "SEQUENCE",
      "children": [
        {
          "nodeType": "ELEMENT",
          "xmlName": "pivot",
          "path": ["pivot"],
          "minOccurs": 0,
          "maxOccurs": 1
        },
        {
          "nodeType": "ELEMENT",
          "xmlName": "constantPayoffRegion",
          "path": ["constantPayoffRegion"],
          "minOccurs": 0,
          "maxOccurs": "unbounded"
        },
        {
          "nodeType": "ELEMENT",
          "xmlName": "linearPayoffRegion",
          "path": ["linearPayoffRegion"],
          "minOccurs": 1,
          "maxOccurs": 1
        },
        {
          "nodeType": "CHOICE",
          "minOccurs": 0,
          "maxOccurs": "unbounded",
          "children": [
            {
              "nodeType": "ELEMENT",
              "xmlName": "constantPayoffRegion",
              "path": ["fxTargetKnockoutForwardChoice", "constantPayoffRegion"],
              "minOccurs": 1,
              "maxOccurs": 1
            },
            {
              "nodeType": "ELEMENT",
              "xmlName": "linearPayoffRegion",
              "path": ["fxTargetKnockoutForwardChoice", "linearPayoffRegion"],
              "minOccurs": 1,
              "maxOccurs": 1
            }
          ]
        }
      ]
    }
  }
}
```

Input XML:

```xml
<FpmlFxTargetKnockoutForward>
  <constantPayoffRegion id="base-constant"/>
  <linearPayoffRegion id="base-linear"/>
  <constantPayoffRegion id="extra-constant"/>
  <barrier id="barrier-1"/>
</FpmlFxTargetKnockoutForward>
```

Expected logical object:

```json
{
  "constantPayoffRegion": [
    {
      "id": "base-constant"
    }
  ],
  "linearPayoffRegion": {
    "id": "base-linear"
  },
  "fxTargetKnockoutForwardChoice": [
    {
      "constantPayoffRegion": {
        "id": "extra-constant"
      }
    }
  ],
  "barrier": [
    {
      "id": "barrier-1"
    }
  ]
}
```

Expected routed parent token shape:

```json
{
  "constantPayoffRegion": {
    "id": "base-constant"
  },
  "linearPayoffRegion": {
    "id": "base-linear"
  },
  "@xmlContentModel": [
    {
      "path": ["fxTargetKnockoutForwardChoice", "constantPayoffRegion"],
      "occurrence": "fxTargetKnockoutForwardChoice[0]",
      "value": {
        "id": "extra-constant"
      }
    }
  ],
  "barrier": {
    "id": "barrier-1"
  }
}
```

Minimum unit test assertions:

- `actual.getConstantPayoffRegion().size()` is `1`.
- `actual.getConstantPayoffRegion().get(0).getId()` is `base-constant`.
- `actual.getLinearPayoffRegion().getId()` is `base-linear`.
- `actual.getFxTargetKnockoutForwardChoice().size()` is `1`.
- `actual.getFxTargetKnockoutForwardChoice().get(0)
  .getConstantPayoffRegion().getId()` is `extra-constant`.
- `actual.getFxTargetKnockoutForwardChoice().get(0)
  .getLinearPayoffRegion()` is `null`.
- `actual.getBarrier().get(0).getId()` is `barrier-1`.

The `barrier` assertion is important because `barrier` is not mentioned in the
content model. It proves that unmentioned XML children pass through unchanged
while mentioned ambiguous children are routed.

### Example 3: FxTargetKnockoutForward `linearPayoffRegion`

This fixture uses the same types and config as Example 2, but focuses on the
second conflict: `linearPayoffRegion` exists both as a direct singleton property
and as a repeated virtual choice property.

Input XML:

```xml
<FpmlFxTargetKnockoutForward>
  <constantPayoffRegion id="base-constant-1"/>
  <constantPayoffRegion id="base-constant-2"/>
  <linearPayoffRegion id="base-linear"/>
  <linearPayoffRegion id="extra-linear-1"/>
  <constantPayoffRegion id="extra-constant"/>
  <linearPayoffRegion id="extra-linear-2"/>
</FpmlFxTargetKnockoutForward>
```

Expected logical object:

```json
{
  "constantPayoffRegion": [
    {
      "id": "base-constant-1"
    },
    {
      "id": "base-constant-2"
    }
  ],
  "linearPayoffRegion": {
    "id": "base-linear"
  },
  "fxTargetKnockoutForwardChoice": [
    {
      "linearPayoffRegion": {
        "id": "extra-linear-1"
      }
    },
    {
      "constantPayoffRegion": {
        "id": "extra-constant"
      }
    },
    {
      "linearPayoffRegion": {
        "id": "extra-linear-2"
      }
    }
  ]
}
```

Expected routed parent token shape, shown as ordered field events because the
rewritten stream contains duplicate direct field names:

```json
[
  {
    "field": "constantPayoffRegion",
    "value": {
      "id": "base-constant-1"
    }
  },
  {
    "field": "constantPayoffRegion",
    "value": {
      "id": "base-constant-2"
    }
  },
  {
    "field": "linearPayoffRegion",
    "value": {
      "id": "base-linear"
    }
  },
  {
    "field": "@xmlContentModel",
    "value": [
      {
        "path": ["fxTargetKnockoutForwardChoice", "linearPayoffRegion"],
        "occurrence": "fxTargetKnockoutForwardChoice[0]",
        "value": {
          "id": "extra-linear-1"
        }
      },
      {
        "path": ["fxTargetKnockoutForwardChoice", "constantPayoffRegion"],
        "occurrence": "fxTargetKnockoutForwardChoice[1]",
        "value": {
          "id": "extra-constant"
        }
      },
      {
        "path": ["fxTargetKnockoutForwardChoice", "linearPayoffRegion"],
        "occurrence": "fxTargetKnockoutForwardChoice[2]",
        "value": {
          "id": "extra-linear-2"
        }
      }
    ]
  }
]
```

The equivalent Jackson object token stream is:

```text
START_OBJECT
FIELD_NAME constantPayoffRegion
START_OBJECT FIELD_NAME id VALUE_STRING base-constant-1 END_OBJECT
FIELD_NAME constantPayoffRegion
START_OBJECT FIELD_NAME id VALUE_STRING base-constant-2 END_OBJECT
FIELD_NAME linearPayoffRegion
START_OBJECT FIELD_NAME id VALUE_STRING base-linear END_OBJECT
FIELD_NAME @xmlContentModel
START_ARRAY
  START_OBJECT path=[fxTargetKnockoutForwardChoice, linearPayoffRegion]
    occurrence=fxTargetKnockoutForwardChoice[0]
    value={id=extra-linear-1}
  END_OBJECT
  START_OBJECT path=[fxTargetKnockoutForwardChoice, constantPayoffRegion]
    occurrence=fxTargetKnockoutForwardChoice[1]
    value={id=extra-constant}
  END_OBJECT
  START_OBJECT path=[fxTargetKnockoutForwardChoice, linearPayoffRegion]
    occurrence=fxTargetKnockoutForwardChoice[2]
    value={id=extra-linear-2}
  END_OBJECT
END_ARRAY
END_OBJECT
```

Minimum unit test assertions:

- direct `constantPayoffRegion` has exactly two entries with ids
  `base-constant-1` and `base-constant-2`, in that order,
- direct `linearPayoffRegion.id` is `base-linear`,
- virtual choice has exactly three entries,
- virtual choice entry 0 has only `linearPayoffRegion.id = extra-linear-1`,
- virtual choice entry 1 has only `constantPayoffRegion.id = extra-constant`,
- virtual choice entry 2 has only `linearPayoffRegion.id = extra-linear-2`.

This test proves that repeated occurrences of the virtual choice produce
separate virtual objects and that duplicate peer XML names retain their original
order.

### Example 4: Multi-Leaf Virtual Occurrence

This is not one of the original FpML warnings, but it is the smallest fixture
that proves the aggregate synthetic property design is necessary. It should be
implemented after the three FpML-derived tests above.

Fixture types:

```rosetta
type MultiLeafContainer:
  entry MultiLeafEntry (0..*)

type MultiLeafEntry:
  first FpmlTextValue (0..1)
  second FpmlTextValue (0..1)
```

Type config:

```json
{
  "com.rosetta.test.MultiLeafContainer": {
    "attributes": {
      "entry": {
        "xmlRepresentation": "VIRTUAL"
      }
    },
    "contentModel": {
      "nodeType": "SEQUENCE",
      "minOccurs": 0,
      "maxOccurs": "unbounded",
      "children": [
        {
          "nodeType": "ELEMENT",
          "xmlName": "first",
          "path": ["entry", "first"],
          "minOccurs": 1,
          "maxOccurs": 1
        },
        {
          "nodeType": "ELEMENT",
          "xmlName": "second",
          "path": ["entry", "second"],
          "minOccurs": 1,
          "maxOccurs": 1
        }
      ]
    }
  }
}
```

Input XML:

```xml
<MultiLeafContainer>
  <first>A1</first>
  <second>A2</second>
  <first>B1</first>
  <second>B2</second>
</MultiLeafContainer>
```

Expected logical object:

```json
{
  "entry": [
    {
      "first": {
        "value": "A1"
      },
      "second": {
        "value": "A2"
      }
    },
    {
      "first": {
        "value": "B1"
      },
      "second": {
        "value": "B2"
      }
    }
  ]
}
```

Minimum unit test assertions:

- `actual.getEntry().size()` is `2`.
- entry 0 has `first.value = A1` and `second.value = A2`.
- entry 1 has `first.value = B1` and `second.value = B2`.
- The implementation must not create four one-leaf `entry` objects.

This fixture catches the main failure mode of independent leaf synthetic
properties: without an occurrence-aware aggregate, `first` and `second` cannot be
merged into the same virtual object.

### Failure Cases For The Examples

Add at least two negative tests using the same fixtures:

1. Missing required direct `linearPayoffRegion` in the FX fixture:

```xml
<FpmlFxTargetKnockoutForward>
  <constantPayoffRegion id="base-constant"/>
</FpmlFxTargetKnockoutForward>
```

The matcher must throw `JsonMappingException` because the content model cannot
consume all mentioned child elements and satisfy the required direct
`linearPayoffRegion`.

2. Ambiguous TradeIdentifier branch:

```xml
<FpmlTradeIdentifier id="ti-ambiguous">
  <issuer scheme="urn:issuer">BANK-A</issuer>
  <partyReference href="party-1"/>
  <tradeId scheme="urn:trade-id">ABC-123</tradeId>
</FpmlTradeIdentifier>
```

The matcher must throw `JsonMappingException` because neither configured branch
matches the complete mentioned child sequence. It must not silently route
`tradeId` to either direct or virtual path.

For all positive and negative tests, assert the exception type and include the
target type name in the message. Where multiple complete route candidates exist,
the error message should also include the candidate paths.

## Jackson Integration

### Module Wiring

`RosettaXMLModule.setupModule(...)` currently creates one `SubstitutionMapLoader` and registers serializer/deserializer modifiers. Change the deserializer modifier constructor so it receives the XML configuration:

```java
context.addBeanDeserializerModifier(
    new RosettaBeanDeserializerModifier(
        substitutionMapLoader,
        rosettaXMLConfiguration
    )
);
```

Keep existing serializer modifier registration unchanged.

### Type Config Resolution

The content-model wrapper must be installed for both Rosetta interfaces and generated builder implementation classes. Add a resolver:

```java
final class RosettaXMLTypeConfigResolver {
    Optional<TypeXMLConfiguration> getConfigForBean(BeanDescription beanDesc);
    Optional<ModelSymbolId> getModelSymbolIdForBean(Class<?> beanClass);
}
```

Resolution order:

1. If `beanClass` has `@RosettaDataType`, use that annotation.
2. Inspect implemented interfaces for `@RosettaDataType`.
3. Walk declaring classes for generated nested builders and implementations.
4. For an annotated Rosetta type, build `ModelSymbolId` from package name and annotation value.
5. Read `TypeXMLConfiguration` from `RosettaXMLConfiguration`.

Keep this resolver in `rune-common`; do not hard-code any specific model package or generated class name suffix.

### Deserializer Modifier

`RosettaBeanDeserializerModifier` should keep its existing substitution-group behavior in `updateBuilder`. Extend it in two ways:

1. Add synthetic properties for nested routed paths to the bean builder.
2. Wrap the final bean deserializer when content-model routing is required.

Skeleton:

```java
public final class RosettaBeanDeserializerModifier extends BeanDeserializerModifier {
    private final SubstitutionMapLoader substitutionMapLoader;
    private final RosettaXMLTypeConfigResolver typeConfigResolver;

    @Override
    public BeanDeserializerBuilder updateBuilder(
            DeserializationConfig config,
            BeanDescription beanDesc,
            BeanDeserializerBuilder builder) {
        addSubstitutionProperties(config, builder);
        addContentModelSyntheticProperties(config, beanDesc, builder);
        return builder;
    }

    @Override
    public JsonDeserializer<?> modifyDeserializer(
            DeserializationConfig config,
            BeanDescription beanDesc,
            JsonDeserializer<?> deserializer) {
        Optional<TypeXMLConfiguration> typeConfig =
                typeConfigResolver.getConfigForBean(beanDesc);
        Optional<XMLContentModel> contentModel =
                typeConfig.flatMap(TypeXMLConfiguration::getContentModel);

        if (!contentModel.isPresent()) {
            return deserializer;
        }
        if (!requiresRouting(contentModel.get())) {
            return deserializer;
        }

        return new XMLContentModelDisambiguatingDeserializer(
                deserializer,
                beanDesc.getBeanClass(),
                typeConfig.get(),
                contentModel.get(),
                routingMetadataFactory.create(beanDesc, typeConfig.get(), contentModel.get()));
    }
}
```

`requiresRouting(...)` should return true when at least one XML name in the content model can map to more than one logical path, or when at least one path has more than one segment. The latter is needed because a nested route needs synthetic property handling even if the XML name is not duplicated in a small test fixture.

## Deserializer Wrapper

Add:

```text
common/src/main/java/com/regnosys/rosetta/common/serialisation/xml/deserialization/XMLContentModelDisambiguatingDeserializer.java
```

It should extend `DelegatingDeserializer`.

Responsibilities:

- Only process object-shaped XML values.
- Buffer exactly the current object, not more.
- Match configured content-model fields.
- Rewrite routed field names.
- Delegate to the original deserializer captured by Jackson.

Skeleton:

```java
final class XMLContentModelDisambiguatingDeserializer extends DelegatingDeserializer {
    private final Class<?> beanClass;
    private final XMLContentModel contentModel;
    private final TypeRoutingMetadata routingMetadata;

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        if (!p.hasToken(JsonToken.START_OBJECT)) {
            return _delegatee.deserialize(p, ctxt);
        }

        List<BufferedField> fields = readCurrentObject(p, ctxt);
        RoutingResult routing = matcher.route(fields);
        TokenBuffer rewritten = rewrite(fields, routing, ctxt, p);

        try (JsonParser rewrittenParser = rewritten.asParserOnFirstToken()) {
            return _delegatee.deserialize(rewrittenParser, ctxt);
        }
    }
}
```

Do not call `ctxt.findRootValueDeserializer(...)` inside this wrapper for the same bean type; that can re-enter the wrapper. Use `_delegatee`.

### Buffered Field Model

Store fields as a list, not a map:

```java
final class BufferedField {
    private final String xmlName;
    private final Optional<String> namespace;
    private final boolean xmlAttribute;
    private final TokenBuffer value;
    private final int originalIndex;
}
```

Use config to identify XML attributes:

- If a field matches an attribute whose `AttributeXMLConfiguration.xmlRepresentation` is `ATTRIBUTE`, pass it through and exclude it from content-model matching.
- XML text-value fields are also pass-through unless explicitly mentioned by content model, which should not be necessary in the initial implementation.
- Fields whose names are not accepted by the content model are pass-through.

### TokenBuffer Rules

The wrapper should be conservative:

- Copy each child value with Jackson's standard copy APIs.
- Treat child subtrees opaquely.
- Rewrite only the parent object's field names.
- Do not mutate Jackson's internal unwrapped-property buffers.
- Do not convert to `JsonNode`.
- Preserve duplicate XML element order.

`TokenBuffer` parsers are not `FromXmlParser`. Existing substitution logic has fallback paths that work for local names. Add regression tests for substitution-group child values inside a routed object. If same-local-name different-namespace substitutions fail, add namespace metadata to `BufferedField` and `RoutingInput`; do not guess.

## Content-Model Matcher

Add a matcher class under:

```text
common/src/main/java/com/regnosys/rosetta/common/serialisation/xml/deserialization
```

Suggested name:

```java
final class XMLContentModelMatcher
```

The matcher receives a filtered ordered input list:

```java
final class RoutingInput {
    private final int fieldIndex;
    private final String xmlName;
    private final Optional<String> namespace;
}
```

It returns assignments:

```java
final class RoutingAssignment {
    private final int inputIndex;
    private final List<String> path;
    private final OccurrenceKey occurrenceKey;
}

final class RoutingResult {
    private final Map<Integer, List<String>> fieldPathByOriginalFieldIndex;
    private final Map<Integer, OccurrenceKey> occurrenceByOriginalFieldIndex;
}
```

`OccurrenceKey` groups assignments produced by the same repeated content-model occurrence. It is required for multi-leaf and multi-layer virtual objects.

### Matching Contract

```text
match(node, inputIndex, occurrenceContext) -> List<MatchResult>

MatchResult:
  nextIndex
  assignments
```

All node matching must account for `minOccurs` and `maxOccurs`.

High-level implementation:

```java
private List<MatchResult> match(XMLContentModel node, int inputIndex, OccurrenceContext context) {
    int min = node.minOccursOrDefault();
    int max = Math.min(node.maxOccursOrDefault(inputRemaining), inputRemaining);

    List<MatchResult> results = new ArrayList<>();
    repeat(node, inputIndex, 0, min, max, context, emptyAssignments, results);
    return results;
}
```

Avoid Java APIs newer than the module source level. Use Java 8-compatible code.

### ELEMENT

Consumes one input item if:

- `xmlName` equals the configured `xmlName`, or
- namespace matching also passes when namespace constraints are configured.

If `path` is present, emit an assignment. If `path` is absent, consume as a sentinel.

### SEQUENCE

Matches `children` in order:

```java
current = singleton(start)
for child in children:
    next = []
    for result in current:
        next.addAll(match(child, result.nextIndex, result.context))
    current = next
return current
```

### CHOICE

Matches each child from the same input index and returns the union of results. Ambiguity is resolved only after complete top-level matching. Do not choose the first branch.

### ALL

Matches child nodes in any order. For a first robust implementation:

1. Expand each child according to occurrence bounds.
2. Try unmatched children recursively against the current input.
3. Stop when no child can consume the next input.
4. Accept only if all required children have matched.
5. Return all complete candidates; ambiguity is handled by top-level tie-breaking.

Because `ALL` is unordered, keep occurrence information in `OccurrenceKey` rather than relying on input order alone.

### ANY

Initial support:

- `ANY` with no namespace constraint consumes exactly one element not otherwise constrained by an `ELEMENT` at that position.
- If `path` is absent, it is a sentinel and the field remains pass-through.
- If `path` is present, assign to that path.

Later namespace support can add fields such as:

```java
Optional<String> namespaceMode; // ANY, OTHER, LOCAL, TARGET_NAMESPACE, LIST
Optional<List<String>> namespaces;
```

Do not add these fields until tests require them.

### Top-Level Tie-Breaking

After matching the root node:

1. Keep only matches that consume all routed input elements.
2. Keep only matches satisfying all required nodes.
3. If one match remains, use it.
4. If no match remains, throw `JsonMappingException`.
5. If several matches remain, throw `JsonMappingException` with candidate paths.

Do not silently fall back to default Jackson routing.

Generated validators can be added later as an optional secondary tie-breaker, but they must not be required for deterministic XML content.

## Synthetic Routed Properties

Nested paths are not normal Jackson field names. Add synthetic internal properties during `updateBuilder`.

Suggested class:

```text
VirtualPathSettableBeanProperty.java
```

Synthetic field name format:

```text
@xmlContentModel:outer.inner.leaf
```

The synthetic property must:

- Deserialize the leaf value using Jackson's normal deserializer for the leaf setter parameter.
- Build the required virtual object graph.
- Attach it to the current parent builder through generated setters or adders.
- Return the original builder instance unless the generated method returns a replacement.

### Multi-Layer Virtual Paths

Single-layer virtual paths are not enough:

```text
virtual.leaf
```

The implementation must support:

```text
outerVirtual.innerVirtual.leaf
```

and multiple leaves in one occurrence:

```text
outerVirtual.innerVirtual.leaf
outerVirtual.innerVirtual.otherLeaf
```

Do not create one virtual object per leaf when several assignments share the same occurrence and prefix. Instead, accumulate fields in a virtual object tree:

```java
final class VirtualObjectNode {
    private final String attributeName;
    private final Map<String, Object> leafValues;
    private final Map<String, List<VirtualObjectNode>> childrenByAttribute;
}
```

Implementation approach:

1. During routing, group assignments by `OccurrenceKey`.
2. For each occurrence group, build a tree from paths.
3. Build objects bottom-up:
   - call static `builder()` on the Rosetta type,
   - apply leaf setters,
   - attach child virtual objects through setter or adder,
   - call `build()`.
4. Attach the outermost object to the parent builder through setter or adder.

Reflection helper rules:

- Prefer generated single-value adders for list attributes: `addX(X value)`.
- Avoid overloads whose parameter is `List`.
- For non-list virtual attributes, use `setX(X value)`.
- Determine whether an attribute is virtual and whether it is multi-valued from available generated methods and, where needed, `@Multi`.
- Cache resolved reflection plans per parent builder class and path prefix.

### Synthetic Property Granularity

Two options are possible:

1. One synthetic property per leaf path.
2. One synthetic property per virtual root occurrence group.

Use option 1 for minimal Jackson integration, but the implementation must share an occurrence accumulator across all synthetic properties for one deserialized object. Otherwise multi-leaf virtual objects cannot be merged correctly.

Practical way to do this:

- The deserializer wrapper can rewrite all nested assignments to a synthetic aggregate field, not individual leaf fields.
- The aggregate value can be an object containing path/value entries.
- A single synthetic property can build the whole virtual graph.

This is more robust than independent leaf synthetic setters. If keeping leaf synthetic setters, add an object-local accumulator in the parent builder path, which is harder and more invasive.

Recommended production design: synthetic aggregate property.

Example routed token:

```json
{
  "@xmlContentModel": [
    {
      "path": ["tradeIdentifierChoice", "tradeId"],
      "occurrence": "choice:0",
      "value": { "value": "T-1" }
    },
    {
      "path": ["tradeIdentifierChoice", "versionedTradeId"],
      "occurrence": "choice:1",
      "value": { "value": "V-2" }
    }
  ]
}
```

The implementation must not stop at independent leaf setters. Multi-leaf virtual objects require shared occurrence grouping.

## Rewriting Output Tokens

The wrapper emits a new object:

- Pass-through fields keep original order and names.
- Direct routed fields keep original names.
- Nested routed fields are omitted from their original names and emitted through the synthetic aggregate property or synthetic leaf properties.
- Child values are copied unchanged.

Example input:

```json
{
  "partyReference": { "href": "party1" },
  "tradeId": { "value": "T-1" }
}
```

Example synthetic-leaf output:

```json
{
  "partyReference": { "href": "party1" },
  "@xmlContentModel:tradeIdentifierChoice.tradeId": { "value": "T-1" }
}
```

Example aggregate-style output:

```json
{
  "partyReference": { "href": "party1" },
  "@xmlContentModel": [
    {
      "path": ["tradeIdentifierChoice", "tradeId"],
      "occurrence": "branch-1.0",
      "value": { "value": "T-1" }
    }
  ]
}
```

Prefer aggregate-style for full multi-layer support.

## Error Handling

Use `JsonMappingException.from(parser, message)` so callers get normal Jackson context.

No-match example:

```text
Cannot route XML content for com.rosetta.test.AmbiguousTradeIdentifier.
XML child sequence: partyReference, tradeId
Configured content model did not match all routed child elements.
```

Ambiguous-match example:

```text
Ambiguous XML content for com.rosetta.test.FxStyleRouting.
XML child sequence: constantPayoffRegion, linearPayoffRegion, constantPayoffRegion
Remaining candidate paths:
- constantPayoffRegion, linearPayoffRegion, fxTargetKnockoutForwardChoice.constantPayoffRegion
- constantPayoffRegion, fxTargetKnockoutForwardChoice.linearPayoffRegion, fxTargetKnockoutForwardChoice.constantPayoffRegion
```

Unsupported config example:

```text
Unsupported XML content-model node type ANY with namespace mode OTHER for com.rosetta.test.WildcardContainer.
```

## Serialization

No serializer change is required for the first runtime implementation.

Rationale:

- Serialization starts from a logical Rosetta object, so route inference is unnecessary.
- Existing `VIRTUAL` unwrapping already writes virtual child fields as peer XML elements.
- Existing tests should continue to cover the serializer.

Later, if content-model field order must be enforced, add a `BeanSerializerModifier` for types with `contentModel`. That modifier should order existing `BeanPropertyWriter`s; it should not replace child serializers.

## Test Plan

Add tests under:

```text
common/src/test/java/com/regnosys/rosetta/common/serialisation/xml
```

Add or extend test Rosetta models under:

```text
common/src/test/resources/xml-serialisation/rosetta
```

Add manual XML config under:

```text
common/src/test/resources/xml-serialisation/xml-config
```

### Config Loading Tests

- `nodeType` binds to `XMLContentModelNodeType`.
- `children` binds to nested content-model nodes.
- Numeric `maxOccurs` binds.
- `"unbounded"` `maxOccurs` binds.
- Unknown older fields are ignored only because config loading already ignores unknown properties; new tests should not rely on legacy field names or removed routing flags.

### Basic Routing Tests

- Direct branch routes `tradeId` to direct property.
- Nested branch routes `tradeId` to virtual child property.
- Single `tradeId` without required nested discriminator routes direct.
- Repeated nested choices create multiple virtual objects.
- Mixed nested choices, such as `tradeId` plus `versionedTradeId`, create separate objects when the content model says they are separate occurrences.
- Invalid sequence fails with `JsonMappingException`.
- Structurally ambiguous sequence fails with `JsonMappingException`.

### Existing XML Feature Compatibility

Inside a buffered routed object, include child values that exercise:

- XML attributes.
- XML text value via `xmlRepresentation: VALUE`.
- Multi-cardinality properties.
- Substitution-group properties.
- Existing virtual unwrapping on unrelated properties.

Assertions should compare full Rosetta objects, not just "no exception".

### ALL Tests

- Reordered XML fields both deserialize to the same object.
- Missing required child fails.
- Duplicate child where max is one fails.
- Optional child absent succeeds.
- Ambiguous unordered assignment fails.

### ANY Tests

- `ANY` without `path` consumes an unknown element as a sentinel and leaves it pass-through if Jackson can otherwise ignore it.
- `ANY` with `path` routes a wildcard field to a Rosetta property when a suitable test property exists.
- Namespace constraints are either unsupported with a clear exception or covered by tests once implemented.

### Multi-Layer Virtual Tests

Use a fixture with:

```text
root.outer.inner.leaf
root.outer.inner.otherLeaf
```

Cases:

- one outer, one inner, one leaf,
- one outer, one inner, two leaves,
- multiple outer occurrences,
- one outer with multiple inner occurrences,
- direct and nested paths sharing the same XML name at different depths.

### Regression Tests

Run the existing XML suite:

```bash
mvn -pl common -Dtest=XmlSerialisationTest test
```

Run the new suite:

```bash
mvn -pl common -Dtest=XmlContentModelDisambiguationTest test
```

Compile with generated test sources:

```bash
mvn -pl common -DskipTests compile test-compile
```

The license plugin currently emits existing header scan warnings in this repository. Those warnings are not part of this feature and should not be treated as content-model failures unless the Maven build itself fails.

## Implementation Checklist

1. Add `XMLContentModelNodeType`.
2. Add `OccursMax`.
3. Use `XMLContentModel.nodeType`.
4. Use `children` for nested content-model nodes.
5. Remove `virtualPath` from config classes.
6. Remove `routingOnly` from config classes.
7. Update `TypeXMLConfiguration` constructors, getters, equality, and hash code.
8. Add `RosettaXMLTypeConfigResolver`.
9. Update `RosettaXMLModule` to pass config to `RosettaBeanDeserializerModifier`.
10. Keep existing substitution property behavior unchanged.
11. Add synthetic property support for nested routed paths.
12. Add `XMLContentModelMatcher` with `ELEMENT`, `SEQUENCE`, `CHOICE`, `ALL`, and `ANY`.
13. Add object buffering and routed token rewriting.
14. Replace leaf-only virtual setter logic with aggregate or occurrence-aware virtual graph construction.
15. Add error messages for no match, ambiguous match, and unsupported config.
16. Add tests listed above.
17. Run compile and XML test suites.

## Java Compatibility

The module currently compiles with Java 8 source/target settings. Do not use:

- records,
- pattern matching for `instanceof`,
- `List.of`,
- `Map.of`,
- `List.copyOf`,
- `Stream.toList`,
- `Optional.isEmpty`.

Use Java 8-compatible alternatives.

## Open Questions

- Whether to implement aggregate synthetic property first or extend the leaf synthetic property with an occurrence accumulator. Aggregate is cleaner for multi-layer support.
- Whether namespace metadata from XML parsers is enough for all substitution-group cases when values are copied through `TokenBuffer`.
- Whether serializer field ordering should be added later for content-model types.
- Whether validator-based tie-breaking should be added after deterministic content matching is complete.
