import { createRequire } from "node:module"
import { describe, expect, it } from "vitest"

// Resolve the application's dependencies: the shared test package does not own Next.js.
const requireWeb = createRequire(
  new URL("../../../../apps/ui/package.json", import.meta.url)
)
const { ImageResponse } = requireWeb("next/og")
const { createElement } = requireWeb("react")
const manifest = requireWeb("./package.json")

const size = { width: 64, height: 32 }

function rectangle(props: Record<string, unknown> = {}) {
  return createElement("rect", { ...size, fill: "blue", ...props })
}

function svg(children: unknown, props: Record<string, unknown> = {}) {
  return createElement("svg", { ...size, viewBox: "0 0 64 32", ...props }, children)
}

async function render(element: unknown) {
  const response = new ImageResponse(element, size)
  expect(response.status).toBe(200)
  expect(response.headers.get("content-type")).toBe("image/png")
  const png = Buffer.from(await response.arrayBuffer())
  expect([...png.subarray(0, 8)]).toEqual([137, 80, 78, 71, 13, 10, 26, 10])
  expect(png.toString("ascii", 12, 16)).toBe("IHDR")
  expect(png.readUInt32BE(16)).toBe(size.width)
  expect(png.readUInt32BE(20)).toBe(size.height)
  return png
}

describe("Next.js image generation dependency", () => {
  it("loads the Next.js version declared by the application", () => {
    expect(requireWeb("next/package.json").version).toBe(manifest.dependencies.next)
  })

  it("renders valid SVG shapes and preserves their fill", async () => {
    const blue = await render(svg(rectangle()))
    const red = await render(svg(rectangle({ fill: "red" })))
    expect(blue).not.toEqual(red)
  })

  // Regression coverage for GHSA-vcvr-r3jv-pc5j, fixed by the Next.js upgrade.
  // Compare renders within each test to avoid platform-dependent image snapshots.
  it.each(["", 'double " quote', "single ' quote", "A & B", "<tag>", "&#34;"])(
    "keeps the SVG attribute value %j as data",
    async (label) => {
      const baseline = await render(svg(rectangle()))
      const result = await render(svg(rectangle({ "data-label": label })))
      expect(result).toEqual(baseline)
    }
  )

  it("prevents an attribute value from injecting an SVG element", async () => {
    const baseline = await render(svg(rectangle()))
    const result = await render(
      svg(
        rectangle({
          "data-label": '"/><rect width="64" height="32" fill="red"/><rect data-label="',
        })
      )
    )
    expect(result).toEqual(baseline)
  })

  it("escapes special characters on the root SVG element", async () => {
    const baseline = await render(svg(rectangle()))
    const result = await render(svg(rectangle(), { "data-label": "\"<&'> &quot;" }))
    expect(result).toEqual(baseline)
  })

  it("prevents a style value from injecting an SVG element", async () => {
    const baseline = await render(svg(rectangle()))
    const result = await render(
      svg(
        rectangle({
          style: {
            fontFamily: '"/><rect width="64" height="32" fill="red"/><rect style="',
          },
        })
      )
    )
    expect(result).toEqual(baseline)
  })

  it("prevents title text from injecting an SVG element", async () => {
    const baseline = await render(
      svg([
        rectangle(),
        createElement("title", { style: { display: "none" } }, "Image title"),
      ])
    )
    const result = await render(
      svg([
        rectangle(),
        createElement(
          "title",
          { style: { display: "none" } },
          '</title><rect width="64" height="32" fill="red"/><title>'
        ),
      ])
    )
    expect(result).toEqual(baseline)
  })

  it.each(["rect invalid", 'rect"', "rect>"])(
    "rejects the malformed SVG element name %j",
    async (name) => {
      await expect(render(svg(createElement(name, size)))).rejects.toThrow(
        /Invalid XML element name/
      )
    }
  )

  it.each(["data invalid", 'data"', "data>"])(
    "rejects the malformed SVG attribute name %j",
    async (name) => {
      await expect(render(svg(rectangle({ [name]: "value" })))).rejects.toThrow(
        /Invalid XML attribute name/
      )
    }
  )

  it("rejects malformed attributes on the root SVG element", async () => {
    await expect(render(svg(rectangle(), { 'data"': "value" }))).rejects.toThrow(
      /Invalid XML attribute name/
    )
  })
})
