import { AuthProvider } from "@enterprise-ai/auth";
import { ThemeProvider } from "@enterprise-ai/ui";
import { AntdRegistry } from "@ant-design/nextjs-registry";
import type { Metadata } from "next";
import AgentStudioClientLayout from "./AgentStudioClientLayout";

export const metadata: Metadata = { title: "Agent Studio" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body className="min-h-screen">
        <AntdRegistry>
          <ThemeProvider>
            <AuthProvider>
              <AgentStudioClientLayout>{children}</AgentStudioClientLayout>
            </AuthProvider>
          </ThemeProvider>
        </AntdRegistry>
      </body>
    </html>
  );
}
