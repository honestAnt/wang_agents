"use client";

import { useState } from "react";
import { Card, Button, Tag, Segmented, Input, Typography, Row, Col, Space } from "antd";
import { PlusOutlined, SearchOutlined, AppstoreOutlined, UnorderedListOutlined } from "@ant-design/icons";
import Link from "next/link";
import { agents, statusColors } from "./agents/data";

const { Title, Text } = Typography;

export default function AgentStudioPage() {
  const [view, setView] = useState("card");
  const [search, setSearch] = useState("");

  const filtered = agents.filter((a) => !search || a.name.toLowerCase().includes(search.toLowerCase()));

  return (
    <div className="p-6">
      <div className="flex flex-wrap justify-between items-center mb-4 gap-3">
        <Title level={3} style={{ margin: 0 }}>Agent Studio</Title>
        <Space>
          <Input prefix={<SearchOutlined />} placeholder="Search agents..." value={search}
            onChange={(e) => setSearch(e.target.value)} style={{ width: 220 }} allowClear />
          <Segmented value={view} onChange={(v) => setView(v as string)}
            options={[
              { value: "card", icon: <AppstoreOutlined /> },
              { value: "list", icon: <UnorderedListOutlined /> },
            ]} />
          <Link href="/agents/new">
            <Button type="primary" icon={<PlusOutlined />}>Create Agent</Button>
          </Link>
        </Space>
      </div>

      {view === "card" ? (
        <Row gutter={[16, 16]}>
          {filtered.map((agent) => (
            <Col xs={24} sm={12} lg={6} key={agent.key}>
              <Card hoverable title={agent.name} extra={<Tag color={statusColors[agent.status]}>{agent.status}</Tag>}>
                <Text type="secondary">{agent.desc}</Text>
                <div className="mt-3">
                  <Link href={`/agents/${agent.key}`}>
                    <Button size="small">Edit</Button>
                  </Link>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      ) : (
        <div className="space-y-2">
          {filtered.map((agent) => (
            <Card key={agent.key} size="small" hoverable>
              <div className="flex justify-between items-center">
                <div>
                  <Text strong>{agent.name}</Text>
                  <Text type="secondary" className="ml-3">{agent.desc}</Text>
                </div>
                <Space>
                  <Tag color={statusColors[agent.status]}>{agent.status}</Tag>
                  <Link href={`/agents/${agent.key}`}><Button size="small">Edit</Button></Link>
                </Space>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
