<div align="center">

<h1 align="center">Complyz</h1>

<p align="center">Customer risk, transaction monitoring, and investigations in one workspace.</p>

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

Complyz helps financial teams understand customer risk, review unusual
transactions, and keep decisions clear and traceable. One workspace brings
customer records, transaction alerts, investigations, reports, and activity
history together.

## What teams can do

- Keep customer information, ownership details, and risk assessments in one
  place.
- Review transaction and restricted-party alerts with the context needed to
  decide what happens next.
- Import transaction files and check the results before processing them.
- Prepare reports, follow each case through review, and retain a record of
  important decisions.
- See current workload and activity without switching between separate tools.

## Try Complyz

Visit [trycomplyz.com](https://trycomplyz.com) to learn more about the product.

## Run the workspace locally

The repository contains the web app, API, shared packages, and local service
configuration. To run the application for development, install Node.js 26.5.0,
pnpm 12.8.1, Java 21, and Docker Compose v2, then run:

```bash
corepack enable
cp .env.example .env
pnpm install --frozen-lockfile
pnpm dev
```

The web app is available at `http://localhost:3000`; the API is available at
`http://localhost:8080`. See the [web app guide](apps/ui/README.md),
[API guide](apps/api/README.md), and [documentation index](docs/index.mdx) for
more.

To enable the repository's local commit and push checks, run
`pnpm hooks:install` once in your worktree. The hooks validate staged changes,
commit message format, and tests affected by a push. See the internal
[engineering workflow guide](docs/runbooks/engineering-workflow.md).
