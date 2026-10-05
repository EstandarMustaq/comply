---
name: check-product-language
description: Write and validate Complyz product copy and public documentation in clear, direct, confident English. Use when changing UI text, user-facing API messages, exports, or customer docs.
---

# Check product language

1. Read `docs/standards/product-language.md` and use the established Complyz vocabulary.
2. Write UI text, user-facing API errors, generated exports and public Mintlify docs in clear, direct, confident English. Keep sentences short and lead with the action or state.
3. Keep internal identifiers stable and separate from localized labels. Do not expose internal acronyms, implementation details, laws, citations, or legal deadlines in product-facing text.
4. Run `pnpm product-language:check`; fix findings at their source and rerun it. Preserve stable wire values and immutable historical migrations.
5. Internal engineering guidance and code comments use concise technical English.
