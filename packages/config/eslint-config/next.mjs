import pluginNext from "@next/eslint-plugin-next"

import { config as reactInternalConfig } from "./react-internal.mjs"

export const config = [
  ...reactInternalConfig,
  {
    plugins: {
      "@next/next": pluginNext,
    },
    rules: {
      ...pluginNext.configs.recommended.rules,
      ...pluginNext.configs["core-web-vitals"].rules,
      "react-hooks/set-state-in-effect": "warn",
    },
  },
]

export default config
