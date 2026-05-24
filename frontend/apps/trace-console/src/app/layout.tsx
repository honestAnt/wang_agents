import { AuthProvider } from "@enterprise-ai/auth";
export const metadata = { title: "Trace Console" };
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return <html lang="zh-CN"><body className="min-h-screen bg-gray-50"><AuthProvider>{children}</AuthProvider></body></html>;
}
