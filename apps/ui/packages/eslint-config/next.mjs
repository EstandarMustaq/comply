import pluginNext from "@next/eslint-plugin-next"

import { config as reactInternalConfig } from "./react-internal.mjs"

/**
 * Para a aplicação Next.js.
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

      // Aviso, não erro: as páginas ainda carregam dados com setState dentro de
      // useEffect. É orientação de desempenho, não de correcção, e a migração
      // dessas páginas está registada como dívida no AGENTS.md.
      "react-hooks/set-state-in-effect": "warn",
    },
  },
]

export default config
