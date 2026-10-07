import { createRequire } from "node:module"
import { resolve } from "node:path"
import { beforeAll, describe, expect, it } from "vitest"

// Exercise the web app's installed dependency through its public API. This is a
// small integration test: mocking the renderer would hide the security regression.
const requireFromWeb = createRequire(
  resolve(__dirname, "../../../../apps/ui/package.json")
)
const { ImageResponse } = requireFromWeb("next/og")
const { createElement } = requireFromWeb("react")
const manifest = requireFromWeb("./package.json")

const width = 32
const height = 24
const injectedRectangle = '<rect width="32" height="24" fill="red"/>'

function svg({
  title = "",
  rootAttributes = {},
  groupAttributes = {},
  rectangleAttributes = {},
  fill = "green",
}: {
  title?: string | number
  rootAttributes?: Record<string, unknown>
  groupAttributes?: Record<string, unknown>
  rectangleAttributes?: Record<string, unknown>
  fill?: string
} = {}) {
  return createElement(
    "svg",
    { width, height, viewBox: `0 0 ${width} ${height}`, ...rootAttributes },
    createElement("rect", { width: 16, height, fill, ...rectangleAttributes }),
    createElement("title", { style: { display: "none" } }, title),
    createElement("g", groupAttributes, createElement("title", null, ""))
  )
}

async function render(element: ReturnType<typeof svg>) {
  const response = new ImageResponse(element, { width, height })
  expect(response.status).toBe(200)
  expect(response.headers.get("content-type")).toBe("image/png")
  const png = Buffer.from(await response.arrayBuffer())
  expect(png.subarray(0, 8)).toEqual(
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])
  )
  expect(png.toString("ascii", 12, 16)).toBe("IHDR")
  expect(png.readUInt32BE(16)).toBe(width)
  expect(png.readUInt32BE(20)).toBe(height)
  return png
}

describe("Next.js ImageResponse SVG serialization (GHSA-vcvr-r3jv-pc5j)", () => {
  let baseline: Buffer

  beforeAll(async () => {
    baseline = await render(svg())
  })

  it("loads the Next.js version declared by the application", () => {
    expect(requireFromWeb("next/package.json").version).toBe(manifest.dependencies.next)
  })

  it("preserves visible SVG content", async () => {
    // Guard against a blank/ignored SVG making the escaping checks pass vacuously.
    expect(await render(svg({ fill: "blue" }))).not.toEqual(baseline)
  })

  it.each([
    ["empty text", ""],
    ["zero", 0],
    ["XML metacharacters", `A & B < C > D "quoted" 'text'`],
    ["Unicode", "Conformidade — Moçambique"],
    ["entity-like text", "&lt;rect/&gt; &#60;rect/&#62;"],
    ["closing-tag injection", `</title>${injectedRectangle}<title>`],
  ])("keeps %s inside SVG title content", async (_name, title) => {
    expect(await render(svg({ title }))).toEqual(baseline)
  })

  describe.each(["root", "nested"] as const)("%s SVG attributes", (location) => {
    it.each([
      ["empty value", ""],
      ["XML metacharacters", `A & B < C > D "quoted" 'text'`],
      ["entity-like value", "&quot;&gt;&lt;rect/&gt;"],
      ["attribute breakout", `">${injectedRectangle}<g data-label="`],
    ])("keeps %s inside its attribute", async (_name, value) => {
      const attributes = { "data-label": value }
      const element = svg(
        location === "root"
          ? { rootAttributes: attributes }
          : { groupAttributes: attributes }
      )
      expect(await render(element)).toEqual(baseline)
    })
  })

  it.each([
    ["quoted font name", '"Example Sans"'],
    ["XML metacharacters", `A & B < C > D "quoted" 'text'`],
    ["style attribute breakout", `sans-serif">${injectedRectangle}<g data-label="`],
  ])("keeps %s inside a nested SVG style", async (_name, fontFamily) => {
    expect(await render(svg({ groupAttributes: { style: { fontFamily } } }))).toEqual(
      baseline
    )
  })

  it.each(["", 'double " quote', "single ' quote", "A & B", "<tag>", "&#34;"])(
    "keeps the rectangle attribute value %j as data",
    async (label) => {
      expect(await render(svg({ rectangleAttributes: { "data-label": label } }))).toEqual(
        baseline
      )
    }
  )

  it.each(["attribute", "style"])(
    "prevents a rectangle %s value from injecting an SVG element",
    async (location) => {
      const payload = `"/>${injectedRectangle}<rect data-label="`
      const rectangleAttributes =
        location === "attribute"
          ? { "data-label": payload }
          : { style: { fontFamily: payload } }
      expect(await render(svg({ rectangleAttributes }))).toEqual(baseline)
    }
  )

  it.each(["rect invalid", 'rect"', "rect>"])(
    "rejects the malformed SVG element name %j",
    async (name) => {
      const element = createElement(
        "svg",
        { width, height },
        createElement(name, { width, height })
      )
      await expect(render(element)).rejects.toThrow(/Invalid XML element name/)
    }
  )

  it.each(["data invalid", 'data"', "data>"])(
    "rejects the malformed SVG attribute name %j",
    async (name) => {
      await expect(
        render(svg({ rectangleAttributes: { [name]: "value" } }))
      ).rejects.toThrow(/Invalid XML attribute name/)
    }
  )

  it("rejects malformed attributes on the root SVG element", async () => {
    await expect(render(svg({ rootAttributes: { 'data"': "value" } }))).rejects.toThrow(
      /Invalid XML attribute name/
    )
  })
})
