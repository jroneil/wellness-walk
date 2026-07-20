import type { Metadata, Viewport } from "next";
import "./globals.css";
import { ServiceWorkerRegistration } from "./pwa-registration";

export const metadata: Metadata = {
  title: "Wellness Window",
  description: "Explainable weather and availability-aware walking recommendations",
  manifest: "/manifest.webmanifest",
  applicationName: "Wellness Window",
};
export const viewport: Viewport = { themeColor: "#047857" };

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="min-h-full flex flex-col"><ServiceWorkerRegistration />{children}</body>
    </html>
  );
}
