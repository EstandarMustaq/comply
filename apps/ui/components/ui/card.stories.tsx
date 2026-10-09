import type { Meta, StoryObj } from "@storybook/react"
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
} from "./card"
import { Button } from "./button"

const meta: Meta<typeof Card> = {
  title: "UI/Card",
  component: Card,
  parameters: {
    layout: "centered",
  },
  tags: ["autodocs"],
}

export default meta
type Story = StoryObj<typeof Card>

export const Default: Story = {
  render: () => (
    <Card className="w-[350px]">
      <CardHeader>
        <CardTitle>Compliance Review</CardTitle>
        <CardDescription>Automated KYC & PEP verification checklist.</CardDescription>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">
          Entity has 0 active sanctions matches and 1 low-priority PEP check.
        </p>
      </CardContent>
      <CardFooter className="flex justify-between">
        <Button variant="outline">Dismiss</Button>
        <Button>Approve</Button>
      </CardFooter>
    </Card>
  ),
}
