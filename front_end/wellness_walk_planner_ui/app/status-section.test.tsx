import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import StatusSection from "./status-section";

describe("StatusSection", () => {
  it("renders backend status details", () => {
    render(
      <StatusSection
        status={{
          applicationName: "walk-planer",
          status: "UP",
          backendTimestamp: "2026-07-14T00:00:00Z",
          developmentStage: "phase-1",
        }}
        error={null}
      />,
    );

    expect(screen.getByText(/application:/i)).toBeInTheDocument();
    expect(screen.getByText(/walk-planer/i)).toBeInTheDocument();
    expect(screen.getAllByText("UP").length).toBeGreaterThan(0);
  });

  it("renders a friendly unavailable state", () => {
    render(<StatusSection status={null} error="The backend is unavailable." />);

    expect(screen.getByText("Unavailable")).toBeInTheDocument();
    expect(screen.getByText("The backend is unavailable.")).toBeInTheDocument();
  });
});
