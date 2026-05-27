"use client";

import { usePathname, useRouter } from "next/navigation";
import { Layout, Menu, Typography } from "antd";
import {
  DashboardOutlined,
  TeamOutlined,
  ApiOutlined,
  ToolOutlined,
  BarChartOutlined,
} from "@ant-design/icons";

const { Sider, Content, Header } = Layout;

const menuItems = [
  { key: "/", icon: <DashboardOutlined />, label: "Dashboard" },
  { key: "/users", icon: <TeamOutlined />, label: "Users" },
  { key: "/models", icon: <ApiOutlined />, label: "Models" },
  { key: "/tools", icon: <ToolOutlined />, label: "Tools" },
  { key: "/analytics", icon: <BarChartOutlined />, label: "Analytics" },
];

export default function AdminConsoleClientLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const selectedKey = menuItems.find((item) => {
    if (item.key === "/") return pathname === "/";
    return pathname.startsWith(item.key);
  })?.key || "/";

  return (
    <Layout className="min-h-screen">
      <Sider width={200} theme="light" className="border-r border-gray-200">
        <Header className="flex items-center px-4 bg-white border-b border-gray-200">
          <Typography.Title level={5} style={{ margin: 0, color: "#4096FF" }}>
            Admin Console
          </Typography.Title>
        </Header>
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => router.push(key)}
          style={{ borderRight: 0 }}
        />
      </Sider>
      <Content className="bg-gray-50 overflow-auto">
        {children}
      </Content>
    </Layout>
  );
}
