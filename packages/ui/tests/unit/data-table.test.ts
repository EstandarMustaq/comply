import { createRequire } from "node:module"
import { resolve } from "node:path"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { DataTable } from "@/components/data-table"

const requireWeb = createRequire(resolve(__dirname, "../../../../apps/ui/package.json"))
const { act, createElement } = requireWeb("react")
const { createRoot } = requireWeb("react-dom/client")

const rows = Array.from({ length: 12 }, (_, index) => ({
  id: index + 1,
  header: `Document ${index + 1}`,
  type: "Narrative",
  status: "Done",
  target: "10",
  limit: "20",
  reviewer: "Reviewer",
}))

let container: HTMLDivElement
let root: ReturnType<typeof createRoot>

beforeEach(() => {
  vi.stubGlobal("IS_REACT_ACT_ENVIRONMENT", true)
  vi.stubGlobal("matchMedia", () => ({
    matches: false,
    addEventListener() {},
    removeEventListener() {},
  }))
  container = document.createElement("div")
  document.body.appendChild(container)
  root = createRoot(container)
})

afterEach(async () => {
  await act(async () => root.unmount())
  container.remove()
  vi.unstubAllGlobals()
})

function button(label: string) {
  const result = Array.from(container.querySelectorAll("button")).find((element) =>
    element.textContent?.includes(label)
  )
  expect(result, label).toBeDefined()
  return result!
}

describe("DataTable with TanStack v9 compatibility API", () => {
  it("paginates and preserves row selection across pages", async () => {
    await act(async () => root.render(createElement(DataTable, { data: rows })))
    expect(container.querySelectorAll("tbody tr")).toHaveLength(10)
    expect(container.textContent).toContain("Page 1 of 2")

    const checkbox = container.querySelector<HTMLButtonElement>(
      'tbody [aria-label="Select row"]'
    )!
    await act(async () => checkbox.click())
    expect(checkbox.getAttribute("aria-checked")).toBe("true")
    expect(container.textContent).toContain("1 of 12 row(s) selected.")

    await act(async () => button("Go to next page").click())
    expect(container.querySelectorAll("tbody tr")).toHaveLength(2)
    expect(container.textContent).toContain("Page 2 of 2")
    expect(container.querySelector("tbody")?.textContent).toContain("Document 11")
    expect(button("Go to next page").disabled).toBe(true)

    await act(async () => button("Go to previous page").click())
    expect(container.querySelectorAll("tbody tr")).toHaveLength(10)
    expect(
      container
        .querySelector('tbody [aria-label="Select row"]')
        ?.getAttribute("aria-checked")
    ).toBe("true")
  })

  it("renders the empty state and disables pagination for no rows", async () => {
    await act(async () => root.render(createElement(DataTable, { data: [] })))
    expect(container.textContent).toContain("No results found.")
    expect(button("Go to next page").disabled).toBe(true)
    expect(button("Go to previous page").disabled).toBe(true)
  })
})
