import pluginNext from "@next/eslint-plugin-next"

import { config as reactInternalConfig } from "./react-internal.mjs"

/**
 * ESLint rules for the Next.js application.
 *
 * @type {import("eslint").Linter.Config[]}
 */
export const config = [
  ...reactInternalConfig,
  {
    plugins: {
      "@next/next": pluginNext,
    },
    rules: {
      ...pluginNext.configs.recommended.rules,
      ...pluginNext.configs["core-web-vitals"].rules,

      // Warn while existing pages are migrated away from state updates in effects.
      "react-hooks/set-state-in-effect": "warn",
    },
  },
]

export default config
