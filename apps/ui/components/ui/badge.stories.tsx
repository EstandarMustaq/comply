import type { Meta, StoryObj } from "@storybook/react"
import { Badge } from "./badge"

const meta: Meta<typeof Badge> = {
  title: "UI/Badge",
  component: Badge,
  parameters: {
    layout: "centered",
  },
  tags: ["autodocs"],
  argTypes: {
    variant: {
      control: "select",
      options: [
        "default",
        "secondary",
        "destructive",
        "outline",
        "warning",
        "success",
        "urgent",
      ],
    },
  },
}

export default meta
type Story = StoryObj<typeof Badge>

export const Default: Story = {
  args: {
    children: "Default Badge",
    variant: "default",
  },
}

export const Success: Story = {
  args: {
    children: "Verified",
    variant: "success",
  },
}

export const Warning: Story = {
  args: {
    children: "Under Review",
    variant: "warning",
  },
}

export const Urgent: Story = {
  args: {
    children: "Critical Alert",
    variant: "urgent",
  },
}

export const Destructive: Story = {
  args: {
    children: "High Risk",
    variant: "destructive",
  },
}

export const Outline: Story = {
  args: {
    children: "Outline",
    variant: "outline",
  },
}
