import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("server-only", () => ({}));
import { fetchCalendarProviderSnapshot } from "./backend";

describe("calendar backend response normalization", () => {
  beforeEach(() => vi.restoreAllMocks());

  it.each([
    ["an empty response", new Response("", { status: 200 })],
    ["malformed JSON", new Response("not-json", { status: 200, headers: { "Content-Type": "application/json" } })],
  ])("turns %s into a safe unavailable snapshot", async (_label, response) => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
    await expect(fetchCalendarProviderSnapshot()).resolves.toEqual({ providers: [], available: false, message: "Calendar connections are temporarily unavailable. Manual events can still be used." });
  });

  it("turns a network failure into a safe unavailable snapshot", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("connect ECONNREFUSED backend:9090")));
    const snapshot = await fetchCalendarProviderSnapshot();
    expect(snapshot.available).toBe(false);
    expect(snapshot.message).not.toContain("backend:9090");
  });
});
