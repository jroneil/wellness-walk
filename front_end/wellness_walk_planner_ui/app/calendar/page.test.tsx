// @vitest-environment jsdom
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CalendarClient } from "./calendar-client";

vi.mock("./calendar-connections", () => ({ CalendarConnections: () => <div>Calendar Connections</div> }));

describe("CalendarPage", () => {
  beforeEach(() => window.localStorage.clear());

  it("supports calendar CRUD and busy indicators", () => {
    render(<CalendarClient initialProviders={[]} />);
    fireEvent.change(screen.getByLabelText("Title"), { target: { value: "Design review" } });
    fireEvent.click(screen.getByRole("button", { name: "Add event" }));

    expect(screen.getByText("Design review")).toBeInTheDocument();
    expect(screen.getAllByText(/Busy/).length).toBeGreaterThan(0);

    fireEvent.click(screen.getByRole("button", { name: "Edit" }));
    fireEvent.click(screen.getByLabelText(/mark this event busy/i));
    fireEvent.click(screen.getByRole("button", { name: "Save changes" }));
    expect(screen.getByText(/Free/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Delete" }));
    expect(screen.getByText(/No calendar events yet/)).toBeInTheDocument();
  });
});
