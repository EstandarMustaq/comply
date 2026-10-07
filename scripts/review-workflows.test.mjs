import assert from "node:assert/strict"
import { mkdtemp, mkdir, readFile, rm, writeFile } from "node:fs/promises"
import { tmpdir } from "node:os"
import { join } from "node:path"
import { execFileSync, spawnSync } from "node:child_process"
import { test } from "node:test"

// Execute the actual github-script block with synthetic API responses and no network.
async function workflow(name) {
  const source = await readFile(
    new URL(`../.github/workflows/${name}.yml`, import.meta.url),
    "utf8"
  )
  const script = source.split("          script: |\n")[1]
  assert.ok(script, "Expected a github-script block")
  const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor
  return new AsyncFunction(
    "github",
    "context",
    "core",
    "process",
    "fetch",
    script
      .split("\n")
      .map((line) => line.replace(/^ {12}/, ""))
      .join("\n")
  )
}

const bot = { login: "coderabbitai[bot]" }
const pullRequest = {
  number: 19,
  title: "Synthetic PR",
  html_url: "https://github.example/pr/19",
  head: { repo: { full_name: "contributor/fork" } },
  base: { repo: { default_branch: "main" } },
}
function review(body, state = "COMMENTED") {
  return {
    id: 42,
    body,
    state,
    user: bot,
    submitted_at: "2026-10-07T10:00:00Z",
    html_url: "https://github.example/review/42",
  }
}
function harness({ reviews = [], findings = [], issues = [], comments = [] } = {}) {
  const writes = []
  const reads = []
  const rest = {}
  for (const [group, names] of Object.entries({
    pulls: ["list", "listReviews", "listCommentsForReview"],
    issues: [
      "getLabel",
      "createLabel",
      "listForRepo",
      "listComments",
      "create",
      "update",
      "createComment",
    ],
  })) {
    rest[group] = Object.fromEntries(
      names.map((name) => [
        name,
        async (args) => {
          if (name === "getLabel") {
            reads.push({ ...args, name })
            return { data: {} }
          }
          writes.push({ name, ...args })
          return { data: { number: 100 } }
        },
      ])
    )
  }
  const pages = new Map([
    [rest.pulls.list, [pullRequest]],
    [rest.pulls.listReviews, reviews],
    [rest.pulls.listCommentsForReview, findings],
    [rest.issues.listForRepo, issues],
    [rest.issues.listComments, comments],
  ])
  return {
    writes,
    reads,
    github: {
      rest,
      paginate: async (method) => {
        assert.ok(pages.has(method))
        reads.push({ method })
        return pages.get(method)
      },
    },
    context: {
      repo: { owner: "example", repo: "complyz" },
      eventName: "schedule",
      payload: {},
    },
    core: {
      info() {},
      setFailed(message) {
        throw new Error(message)
      },
    },
  }
}
async function track(options) {
  const h = harness(options)
  await (
    await workflow("coderabbit-finding-issues")
  )(h.github, h.context, h.core)
  return h.writes
}

test("scheduled reconciliation tracks actionable fork reviews and section-specific clean statements", async () => {
  const writes = await track({
    reviews: [
      review(
        "## Findings\n- Tenant filter is missing.\n\nNo issues in the documentation."
      ),
    ],
  })
  assert.equal(writes.filter((w) => w.name === "create").length, 1)
  assert.match(
    writes.find((w) => w.name === "createComment").body,
    /Tenant filter is missing/
  )
})

test("a whole-review clean verdict does not create an issue", async () => {
  assert.deepEqual(
    await track({
      reviews: [review("**No actionable findings.**", "CHANGES_REQUESTED")],
    }),
    []
  )
})

test("inline findings remain actionable even when the summary says no findings", async () => {
  const writes = await track({
    reviews: [review("No findings")],
    findings: [
      {
        user: bot,
        body: "Scope the query",
        path: "service.java",
        line: 10,
        html_url: "https://github.example/comment/1",
      },
    ],
  })
  assert.match(writes.find((w) => w.name === "createComment").body, /Scope the query/)
})

test("human and dismissed reviews are ignored", async () => {
  assert.deepEqual(
    await track({
      reviews: [
        { ...review("## Findings\nBug"), user: { login: "someone" } },
        review("## Findings\nBug", "DISMISSED"),
      ],
    }),
    []
  )
})

test("reconciliation does not duplicate recorded reviews or reopen resolved findings", async () => {
  assert.deepEqual(
    await track({
      reviews: [review("## Findings\nBug")],
      issues: [{ number: 100, state: "closed", body: "<!-- coderabbit-pr: 19 -->" }],
      comments: [{ body: "<!-- coderabbit-review: 42 -->" }],
    }),
    []
  )
})

test("a new finding reopens the tracking issue", async () => {
  const writes = await track({
    reviews: [review("## Findings\nBug")],
    issues: [{ number: 100, state: "closed", body: "<!-- coderabbit-pr: 19 -->" }],
  })
  assert.equal(writes.find((w) => w.name === "update").state, "open")
  assert.equal(writes.filter((w) => w.name === "createComment").length, 1)
})

test("same-repository review events use the submitted review directly", async () => {
  const h = harness()
  h.context.eventName = "pull_request_review"
  h.context.payload = { pull_request: pullRequest, review: review("## Findings\nBug") }
  await (
    await workflow("coderabbit-finding-issues")
  )(h.github, h.context, h.core)
  assert.equal(h.writes.filter((w) => w.name === "create").length, 1)
})

test("Dependabot uses the Actions API URL, paginates, and reconciles alert states", async () => {
  const h = harness({
    issues: [
      { number: 101, state: "open", body: "<!-- dependabot-alert: 2 -->" },
      { number: 102, state: "closed", body: "<!-- dependabot-alert: 3 -->" },
    ],
  })
  const calls = []
  const fetch = async (url) => {
    calls.push(String(url))
    const first = calls.length === 1
    return {
      ok: true,
      json: async () =>
        first
          ? [{ number: 1, state: "open" }]
          : [
              { number: 2, state: "fixed" },
              { number: 3, state: "open" },
            ],
      headers: {
        get: () =>
          first
            ? '<https://github.example/api/v3/repos/example/complyz/dependabot/alerts?page=2>; rel="next"'
            : null,
      },
    }
  }
  await (
    await workflow("dependabot-alert-issues")
  )(
    h.github,
    h.context,
    h.core,
    {
      env: {
        GITHUB_API_URL: "https://github.example/api/v3/",
        DEPENDABOT_ALERTS_TOKEN: "synthetic-test-token",
      },
    },
    fetch
  )
  assert.equal(
    calls[0],
    "https://github.example/api/v3/repos/example/complyz/dependabot/alerts?per_page=100"
  )
  assert.equal(calls.length, 2)
  assert.equal(h.writes.filter((w) => w.name === "create").length, 1)
  assert.equal(
    h.writes.find((w) => w.name === "update" && w.issue_number === 101).state,
    "closed"
  )
  assert.equal(
    h.writes.find((w) => w.name === "update" && w.issue_number === 102).state,
    "open"
  )
})

test("missing Dependabot token fails before network access", async () => {
  const h = harness()
  await assert.rejects(
    () =>
      workflow("dependabot-alert-issues").then((run) =>
        run(h.github, h.context, h.core, { env: {} }, () =>
          assert.fail("unexpected network request")
        )
      ),
    /DEPENDABOT_ALERTS_TOKEN/
  )
  assert.deepEqual(h.writes, [])
})

test("failed Dependabot responses cannot close existing issues", async () => {
  const h = harness({
    issues: [{ number: 101, state: "open", body: "<!-- dependabot-alert: 2 -->" }],
  })
  await assert.rejects(
    () =>
      workflow("dependabot-alert-issues").then((run) =>
        run(
          h.github,
          h.context,
          h.core,
          {
            env: {
              GITHUB_API_URL: "https://api.github.com",
              DEPENDABOT_ALERTS_TOKEN: "synthetic-test-token",
            },
          },
          async () => ({ ok: false, status: 403, text: async () => "Forbidden" })
        )
      ),
    /returned 403/
  )
  assert.deepEqual(h.writes, [])
})

for (const summary of [
  "**⚠️ Outside diff range comments (1)**\n\nFix the API URL.",
  "**Actionable comments posted: 1**\n\nFix the API URL.",
]) {
  test(`tracks CodeRabbit summary format: ${summary.split("\n")[0]}`, async () => {
    const writes = await track({ reviews: [review(summary)] })
    assert.match(writes.find((w) => w.name === "createComment").body, /Fix the API URL/)
  })
}

test("reconciliation indexes issues and labels once and reuses newly created issues", async () => {
  const first = review("## Findings\nFirst issue")
  const second = { ...review("## Findings\nSecond issue"), id: 43 }
  const h = harness({ reviews: [first, second, first] })
  await (
    await workflow("coderabbit-finding-issues")
  )(h.github, h.context, h.core)
  assert.equal(
    h.reads.filter((r) => r.method === h.github.rest.issues.listForRepo).length,
    1
  )
  assert.equal(h.reads.filter((r) => r.name === "getLabel").length, 2)
  assert.equal(h.writes.filter((w) => w.name === "create").length, 1)
  assert.equal(h.writes.filter((w) => w.name === "createComment").length, 2)
  assert.equal(
    h.reads.filter((r) => r.method === h.github.rest.issues.listComments).length,
    0
  )
})

test("existing issue comments are fetched once across multiple reviews", async () => {
  const h = harness({
    reviews: [
      review("## Findings\nOld finding"),
      { ...review("## Findings\nNew finding"), id: 43 },
    ],
    issues: [{ number: 100, body: "<!-- coderabbit-pr: 19 -->" }],
    comments: [{ body: "<!-- coderabbit-review: 42 -->" }],
  })
  await (
    await workflow("coderabbit-finding-issues")
  )(h.github, h.context, h.core)
  assert.equal(
    h.reads.filter((r) => r.method === h.github.rest.issues.listComments).length,
    1
  )
  assert.equal(h.writes.filter((w) => w.name === "createComment").length, 1)
  assert.match(h.writes.find((w) => w.name === "createComment").body, /New finding/)
})

test("OpenAPI drift gate catches modified, deleted, staged and untracked contracts", async () => {
  const source = await readFile(
    new URL("../.github/workflows/quality-gates.yml", import.meta.url),
    "utf8"
  )
  const script = source
    .split(
      "      - name: Verify the checked-in documentation contract\n        run: |\n"
    )[1]
    .split("      - name:")[0]
  const root = await mkdtemp(join(tmpdir(), "complyz-contract-drift-"))
  const git = (...args) => execFileSync("git", args, { cwd: root, stdio: "pipe" })
  const status = () => spawnSync("bash", ["-c", script], { cwd: root }).status
  try {
    git("init")
    await mkdir(join(root, "docs"))
    const file = join(root, "docs/openapi.json")
    await writeFile(file, "{}\n")
    git("add", ".")
    git(
      "-c",
      "user.name=Test",
      "-c",
      "user.email=test@example.invalid",
      "commit",
      "-m",
      "fixture"
    )
    assert.equal(status(), 0)
    await writeFile(file, '{"changed":true}\n')
    assert.equal(status(), 1)
    git("add", ".")
    assert.equal(status(), 1)
    git("reset", "--hard", "HEAD")
    await rm(file)
    assert.equal(status(), 1)
    git("rm", "docs/openapi.json")
    git(
      "-c",
      "user.name=Test",
      "-c",
      "user.email=test@example.invalid",
      "commit",
      "-m",
      "remove contract"
    )
    await mkdir(join(root, "docs"), { recursive: true })
    await writeFile(file, "{}\n")
    assert.equal(status(), 1)
  } finally {
    await rm(root, { recursive: true, force: true })
  }
})
