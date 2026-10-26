# Product voice and language

## Product identity

**Intelligent Financial Compliance.**

Built for regulated finance teams who know what they’re doing. Today, Comply brings customer and transaction data into one operational workspace so teams can assess risk, review alerts, investigate activity and prepare reports.

Compliance should make finance safer, sharper and easier to operate—not slow it down.

The longer-term design aims to support external-tool integrations, advanced precision mechanisms, compliance-analysis algorithms, transaction-fragmentation detection and native AI within compliance models. Treat these as planned capabilities, not current functionality, unless delivery is verified.

Security alignment with ISO/IEC 27001:2022 and SOC 2 Type II criteria is a roadmap objective. Do not claim current alignment, ISO certification or a SOC 2 Type II report.

## Voice

Comply speaks like software built for people who know what they are doing.

- Write in English: youthful, professional, clear, direct, confident and bold.
- Keep sentences short. Lead with the action or the state.
- Use active verbs and concrete words. Name the boundary.
- Be precise without sounding bureaucratic. Be technical without sounding academic. Be confident without sounding aggressive.
- Say what something is and, when useful, what it is not.
- Cut corporate filler, legalese and unnecessary abstraction.

Prefer:

- “Set the floor.”
- “Access is blocked.”
- “This policy applies here.”
- “The membership expired.”
- “You need step-up.”
- “This action is audited.”
- “No access across Organizations.”
- “CommercialClient is back-office metadata.”

Avoid:

- “The aforementioned entity”
- “Pursuant to the applicable provisions”
- “Hierarchically superior entity”
- “Administrative organizational structure”
- “The system shall enforce…”
- “In accordance with the aforementioned policy”

## Core vocabulary

| Term                 | Meaning                                                                                                                                                                                                       |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Account**          | A person's identity and security profile.                                                                                                                                                                     |
| **Organization**     | The tenant and client institution in the product. Memberships, policies and compliance data belong to an Organization.                                                                                        |
| **CommercialClient** | A back-office namespace for commercial control and reference. It stores an identifier, name, contract reference and status. It is not a tenant, does not manage Organizations and never drives authorization. |
| **Membership**       | An Account's access relationship with an Organization.                                                                                                                                                        |
| **Policy floor**     | The minimum security bar an Organization cannot weaken.                                                                                                                                                       |

Keep stable API, event and storage identifiers separate from display labels. Do not rename wire values to improve copy. Keep security boundaries and domain distinctions explicit. Never imply that CommercialClient grants access or governs an Organization.

## Interface principles

- Put the information needed for a decision where the operator needs it.
- Make the current state, next action and outcome obvious.
- Keep dense data readable with clear labels, stable hierarchy and predictable actions.
- Provide accessible loading, empty, success, error and denied states. Never leave an operational area blank without context.
- Use neutral colors for structure, brand colors for emphasis, and destructive colors only for destructive or urgent states.
- Keep tables and analytical views wide enough for real work.

## Review

For product-copy changes, use the `check-product-language` skill and review the surrounding workflow against this standard. Confirm that each message uses the established vocabulary, describes verified behavior, names the current state and next action, and remains accessible in context. Keep stable wire values and immutable historical migrations unchanged.
