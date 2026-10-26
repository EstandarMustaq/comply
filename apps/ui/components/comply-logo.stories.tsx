import type { Meta, StoryObj } from "@storybook/react"
import { ComplyLogo } from "./comply-logo"

const meta: Meta<typeof ComplyLogo> = {
  title: "Brand/ComplyLogo",
  component: ComplyLogo,
  parameters: {
    layout: "centered",
  },
  tags: ["autodocs"],
  argTypes: {
    variant: {
      control: "select",
      options: ["auto", "light-bg", "dark-bg"],
    },
    size: {
      control: "select",
      options: ["sm", "md", "lg", "xl"],
    },
  },
}

export default meta
type Story = StoryObj<typeof ComplyLogo>

export const Default: Story = {
  args: {
    variant: "auto",
    size: "md",
  },
}

export const Small: Story = {
  args: {
    variant: "auto",
    size: "sm",
  },
}

export const Large: Story = {
  args: {
    variant: "auto",
    size: "lg",
  },
}

export const ExtraLarge: Story = {
  args: {
    variant: "auto",
    size: "xl",
  },
}

export const DarkBackground: Story = {
  args: {
    variant: "dark-bg",
    size: "md",
  },
  parameters: {
    backgrounds: { default: "dark" },
  },
}
