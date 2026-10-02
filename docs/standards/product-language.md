# Product language and interface principles

## Product identity and purpose

Complyz helps financial teams turn customer and transaction information into clear, explainable, and traceable operational decisions.

The product should feel dependable, focused, and respectful of the operator's expertise. Every workflow should make the current state, available action, and resulting outcome easy to understand.

## Core vocabulary

| Domain concept                   | Product wording            | Meaning                                                                |
| -------------------------------- | -------------------------- | ---------------------------------------------------------------------- |
| Customer due diligence           | Customer profile           | Identity, risk, and relationship information for a customer.           |
| Suspicious activity workflow     | Investigation case         | A review workflow created from an identified risk signal.              |
| Sanctions screening              | Restricted-party screening | Controlled matching against configured screening data.                 |
| Threshold/structuring monitoring | Transaction monitoring     | Rules that identify unusual value, frequency, or aggregation patterns. |
| STR/CTR records                  | Compliance reports         | Structured reports prepared and submitted by authorized operators.     |
| Audit log                        | Activity history           | Immutable record of important user and system actions.                 |

## Interface principles

- Present the information needed for a decision without forcing operators through unnecessary steps.
- Keep dense data readable with stable visual hierarchy, clear labels, and predictable actions.
- Provide accessible loading, empty, success, and error states; never leave a blank operational area without context.
- Use neutral colors for structure, brand colors for emphasis, and destructive colors only for destructive or urgent states.
- Keep data tables and analytical views wide enough to support real operational work.

## Product wording

- Use short, direct explanations focused on the action the user can take.
- Write product UI, user-facing API messages, and public documentation in direct Mozambican Portuguese (`pt-MZ`).
- Keep technical documentation and engineering instructions in concise technical English.
- Keep stable machine identifiers separate from display labels.
- Do not expose internal implementation details, machine enums, or external citations in product-facing content.
- Explain a blocked action through its current state and next step.
