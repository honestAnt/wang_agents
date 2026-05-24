import { AuthProvider } from "@enterprise-ai/auth";
import type { Metadata } from "next";

export const metadata: Metadata = { title: "Agent Studio" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body className="min-h-screen bg-gray-50">
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
