import { AuthProvider } from "@enterprise-ai/auth";
import { ThemeProvider } from "@enterprise-ai/ui";
import { AntdRegistry } from "@ant-design/nextjs-registry";

export const metadata = { title: "RAG Studio" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body className="min-h-screen">
        <AntdRegistry>
          <ThemeProvider>
            <AuthProvider>{children}</AuthProvider>
          </ThemeProvider>
        </AntdRegistry>
      </body>
    </html>
  );
}
