<div align="center">

<h1 align="center">Complyz</h1>

<p align="center"><strong>Intelligent Financial Compliance</strong></p>

<p align="center">
  <a href="https://github.com/EstandarMustaq/complyz/actions/workflows/quality-gates.yml"><img src="https://github.com/EstandarMustaq/complyz/actions/workflows/quality-gates.yml/badge.svg?branch=main" alt="Quality gates"></a>
  <a href="https://github.com/EstandarMustaq/complyz/actions/workflows/publish-containers.yml"><img src="https://github.com/EstandarMustaq/complyz/actions/workflows/publish-containers.yml/badge.svg?branch=main" alt="Container publishing"></a>
  <a href="https://github.com/EstandarMustaq/complyz/actions/workflows/release-ui.yml"><img src="https://github.com/EstandarMustaq/complyz/actions/workflows/release-ui.yml/badge.svg?branch=main" alt="Web release"></a>
  <a href="https://github.com/EstandarMustaq/complyz/commits/main/"><img src="https://img.shields.io/github/last-commit/EstandarMustaq/complyz?branch=main&label=last%20updated" alt="Last updated"></a>
  <img src="https://img.shields.io/badge/Node.js-26.5.0-339933?logo=node.js&logoColor=white" alt="Node.js 26.5.0">
  <img src="https://img.shields.io/badge/pnpm-12.8.1-F69220?logo=pnpm&logoColor=white" alt="pnpm 12.8.1">
  <a href="https://trycomplyz.com"><img src="https://img.shields.io/badge/Explore%20Complyz-trycomplyz.com-536760" alt="Explore Complyz"></a>
</p>

</div>

Built for regulated finance teams who know what they’re doing. Today, Complyz brings customer and transaction data into one operational workspace so teams can assess risk, review alerts, investigate activity and prepare reports.

## What Complyz does today

- Keep customer profiles, ownership details and risk assessments together.
- Review transaction and sanctions alerts with the context to decide what comes next.
- Import transaction files and check the data before processing it.
- Prepare reports, move each case through review and keep a clear record of decisions.
- See workload and activity without switching tools.

## Designed to grow

The longer-term design aims to support external-tool integrations, advanced precision mechanisms, compliance-analysis algorithms, transaction-fragmentation detection and native AI within compliance models. These are design goals, not claims that every capability is available today.

## Security alignment

Aligning security practices with ISO/IEC 27001:2022 and SOC 2 Type II criteria is a roadmap objective, not an implemented program. Complyz does not claim ISO certification or a SOC 2 Type II report.

## Licensing and trademarks

Source code is licensed under [AGPL-3.0-only](LICENSE-AGPL-3.0). Documentation is licensed under [CC BY 4.0](LICENSE-CC-BY-4.0). The Complyz™ name, logo and trademarks are reserved. These licenses do not grant trademark rights.

## Explore Complyz

Visit [trycomplyz.com](https://trycomplyz.com) to learn more about the product.

## Run the workspace locally

The repository includes the web app, API, shared packages and local services.
Install Node.js 26.5.0, pnpm 12.8.1, Java 21 and Docker Compose v2, then run:

```bash
corepack enable
cp .env.example .env
pnpm install --frozen-lockfile
pnpm dev
```

The web app is available at `http://localhost:3000`; the API is available at
`http://localhost:8080`. See the [web app guide](apps/ui/README.md),
[API guide](apps/api/README.md), and [documentation index](docs/index.mdx) for
setup details.

To enable the repository's local commit and push checks, run
`pnpm hooks:install` once in your worktree. The hooks validate staged changes,
commit message format, and tests affected by a push. See the internal
[engineering workflow guide](docs/runbooks/engineering-workflow.md).
