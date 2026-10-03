---
name: check-product-language
description: Review and validate Complyz product-facing language in Mozambican Portuguese (`pt-MZ`). Use when writing or changing UI copy, user-facing API messages, exports, or public documentation.
---

# Check product language

1. Read `docs/standards/product-language.md` and use the established Complyz vocabulary.
2. Keep UI text, user-facing API errors, generated user-facing exports, and public Mintlify docs direct and operational in `pt-MZ`.
3. Keep internal identifiers stable and separate from localized labels. Do not expose internal acronyms, implementation details, laws, citations, or legal deadlines in product-facing text.
4. Run `pnpm product-language:check`; fix findings at their source and rerun it.
5. Internal engineering guidance and code comments use concise technical English.
