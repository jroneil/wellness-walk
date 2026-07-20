// @vitest-environment jsdom
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import SettingsPage from "./page";
import { settingsStorageKey } from "./settings";

describe("SettingsPage", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it("renders defaults and saves valid settings to localStorage", () => {
    render(<SettingsPage />);

    expect(screen.getByLabelText(/default zip code/i)).toHaveValue("01830");
    expect(screen.getByLabelText(/preferred walk duration/i)).toHaveValue("30");
    expect(screen.getByLabelText(/preferred time/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/temperature preference/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/rain tolerance/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/wind tolerance/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/minimum acceptable recommendation score/i)).toHaveValue(60);
    expect(screen.getByLabelText(/measurement units/i)).toBeInTheDocument();
    const dashboardLink = screen.getByRole("link", { name: /dashboard/i });
    expect(dashboardLink).toHaveAttribute("href", "/");

    dashboardLink.focus();
    expect(dashboardLink).toHaveFocus();

    fireEvent.change(screen.getByLabelText(/default zip code/i), { target: { value: "02108" } });
    fireEvent.change(screen.getByLabelText(/preferred walk duration/i), { target: { value: "45" } });
    fireEvent.change(screen.getByLabelText(/preferred time/i), { target: { value: "AFTERNOON" } });
    fireEvent.change(screen.getByLabelText(/measurement units/i), { target: { value: "METRIC" } });
    fireEvent.click(screen.getByRole("button", { name: /save settings/i }));

    expect(screen.getByText(/settings saved/i)).toBeInTheDocument();
    expect(JSON.parse(window.localStorage.getItem(settingsStorageKey) ?? "{}")).toMatchObject({
      defaultZipCode: "02108",
      walkDurationMinutes: 45,
      preferredTimeOfDay: "AFTERNOON",
      unitSystem: "METRIC",
    });
  });

  it("validates ZIP and minimum score before saving", () => {
    render(<SettingsPage />);

    fireEvent.change(screen.getByLabelText(/default zip code/i), { target: { value: "abc" } });
    fireEvent.change(screen.getByLabelText(/minimum acceptable recommendation score/i), { target: { value: "101" } });
    fireEvent.click(screen.getByRole("button", { name: /save settings/i }));

    expect(screen.getByText(/valid 5-digit zip code/i)).toBeInTheDocument();
    expect(screen.getByText(/between 0 and 100/i)).toBeInTheDocument();
    expect(window.localStorage.getItem(settingsStorageKey)).toBeNull();
  });

  it("resets saved preferences to defaults", () => {
    window.localStorage.setItem(settingsStorageKey, JSON.stringify({ defaultZipCode: "90210" }));
    render(<SettingsPage />);

    fireEvent.click(screen.getByRole("button", { name: /reset defaults/i }));

    expect(screen.getByLabelText(/default zip code/i)).toHaveValue("01830");
    expect(window.localStorage.getItem(settingsStorageKey)).toBeNull();
  });
});
