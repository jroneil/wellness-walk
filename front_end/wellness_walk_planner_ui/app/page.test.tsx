// @vitest-environment jsdom
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import Home from "./page";

vi.mock("./lib/backend", () => ({
  fetchBackendStatus: vi.fn().mockResolvedValue({
    applicationName: "Wellness Window",
    status: "UP",
    backendTimestamp: "2026-07-15T12:00:00Z",
    developmentStage: "local",
  }),
}));

describe("Home", () => {
  it("renders a keyboard-accessible gear settings link in the dashboard header", async () => {
    render(await Home());

    const settingsLink = screen.getByRole("link", { name: /^settings$/i });
    expect(settingsLink).toHaveAttribute("href", "/settings");
    expect(settingsLink).toHaveAttribute("title", "Settings");
    expect(settingsLink.querySelector("svg")).toBeInTheDocument();

    settingsLink.focus();
    expect(settingsLink).toHaveFocus();
  });
});
