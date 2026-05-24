import type { Metadata } from "next";
import { AuthProvider } from "@enterprise-ai/auth";
import "./globals.css";

export const metadata: Metadata = {
  title: "Enterprise AI — Admin Console",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body className="min-h-screen bg-gray-50">
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
