"use client";

import { usePathname, useRouter } from "next/navigation";
import { Layout, Menu, Typography } from "antd";
import {
  AppstoreOutlined,
  ShopOutlined,
  ThunderboltOutlined,
  FileTextOutlined,
  ApartmentOutlined,
} from "@ant-design/icons";

const { Sider, Content, Header } = Layout;

const menuItems = [
  { key: "/agents", icon: <AppstoreOutlined />, label: "Agents" },
  { key: "/marketplace", icon: <ShopOutlined />, label: "Marketplace" },
  { key: "/skills", icon: <ThunderboltOutlined />, label: "Skills" },
  { key: "/prompts", icon: <FileTextOutlined />, label: "Prompts" },
  { key: "/multi-agent", icon: <ApartmentOutlined />, label: "Multi-Agent" },
];

export default function AgentStudioClientLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const selectedKey = menuItems.find((item) => pathname.startsWith(item.key))?.key || "/agents";

  return (
    <Layout className="min-h-screen">
      <Sider width={200} theme="light" className="border-r border-gray-200">
        <Header className="flex items-center px-4 bg-white border-b border-gray-200">
          <Typography.Title level={5} style={{ margin: 0, color: "#4096FF" }}>
            Agent Studio
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
