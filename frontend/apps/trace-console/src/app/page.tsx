"use client";

import { useState } from "react";
import { Card, Row, Col, Statistic, Table, Tag, Input, Collapse, Typography, Space, Button, Descriptions } from "antd";
import {
  BarChartOutlined, DollarOutlined, ClockCircleOutlined, WarningOutlined,
  SearchOutlined, ReloadOutlined,
} from "@ant-design/icons";

const { Title, Text } = Typography;

interface SessionItem {
  key: string;
  id: string;
  user: string;
  model: string;
  cost: number;
  latency: number;
  status: string;
  time: string;
  spans: SpanItem[];
}

interface SpanItem {
  key: string;
  type: string;
  name: string;
  latency: number;
  cost: number;
  status: string;
  detail: string;
}

const MOCK_SESSIONS: SessionItem[] = [
  { key: "s1", id: "s1", user: "alice", model: "gpt-4.1", cost: 0.023, latency: 1240, status: "ok", time: "2026-05-27 12:43",
    spans: [
      { key: "sp1", type: "llm", name: "GPT-4.1 Chat", latency: 980, cost: 0.018, status: "ok", detail: "Prompt: 234 tokens, Completion: 156 tokens" },
      { key: "sp2", type: "rag", name: "RAG Retrieval", latency: 120, cost: 0.002, status: "ok", detail: "3 chunks retrieved, top score: 0.95" },
      { key: "sp3", type: "tool", name: "Weather API", latency: 140, cost: 0.003, status: "ok", detail: "Query: Beijing, Response: 22°C" },
    ],
  },
  { key: "s2", id: "s2", user: "bob", model: "claude-sonnet-4-6", cost: 0.015, latency: 890, status: "ok", time: "2026-05-27 12:43",
    spans: [
      { key: "sp4", type: "llm", name: "Claude Chat", latency: 720, cost: 0.012, status: "ok", detail: "Prompt: 180 tokens, Completion: 95 tokens" },
      { key: "sp5", type: "tool", name: "Order Lookup", latency: 170, cost: 0.003, status: "ok", detail: "Order #12345 found, status: shipped" },
    ],
  },
];

export default function TraceConsolePage() {
  const [search, setSearch] = useState("");
  const [selectedSession, setSelectedSession] = useState<string | null>(null);

  const filtered = MOCK_SESSIONS.filter(
    (s) => !search || s.id.includes(search) || s.user.includes(search) || s.model.includes(search)
  );

  const columns = [
    { title: "Session ID", dataIndex: "id", key: "id", render: (v: string) => <Text code className="text-xs">{v}</Text> },
    { title: "User", dataIndex: "user", key: "user" },
    {
      title: "Model", dataIndex: "model", key: "model",
      render: (v: string) => <Tag color="blue">{v}</Tag>,
    },
    { title: "Cost", dataIndex: "cost", key: "cost", render: (v: number) => `$${v.toFixed(4)}` },
    { title: "Latency", dataIndex: "latency", key: "latency", render: (v: number) => `${v}ms` },
    {
      title: "Status", dataIndex: "status", key: "status",
      render: (v: string) => <Tag color={v === "ok" ? "green" : "red"}>{v}</Tag>,
    },
    { title: "Time", dataIndex: "time", key: "time" },
  ];

  const selectedData = MOCK_SESSIONS.find((s) => s.id === selectedSession);

  return (
    <div className="p-6">
      <div className="flex flex-wrap justify-between items-center mb-4 gap-3">
        <Title level={3} style={{ margin: 0 }}>Trace & Audit Console</Title>
        <Space>
          <Input prefix={<SearchOutlined />} placeholder="Search sessions..." value={search}
            onChange={(e) => setSearch(e.target.value)} style={{ width: 240 }} allowClear />
          <Button icon={<ReloadOutlined />}>Refresh</Button>
        </Space>
      </div>

      <Row gutter={[16, 16]} className="mb-6">
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="Total Sessions" value={2847} prefix={<BarChartOutlined />} styles={{ value: { color: "#4096FF" } }} /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="Total Cost" value={156.32} precision={2} prefix={<DollarOutlined />} styles={{ value: { color: "#4096FF" } }} suffix="$" /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="Avg Latency" value={1.2} precision={1} prefix={<ClockCircleOutlined />} styles={{ value: { color: "#4096FF" } }} suffix="s" /></Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card><Statistic title="Error Rate" value={0.8} precision={1} prefix={<WarningOutlined />} styles={{ value: { color: "#FF4D4F" } }} suffix="%" /></Card>
        </Col>
      </Row>

      <Card title="Recent Sessions">
        <Table
          dataSource={filtered}
          columns={columns}
          pagination={{ pageSize: 10 }}
          size="middle"
          onRow={(record) => ({
            onClick: () => setSelectedSession(record.id === selectedSession ? null : record.id),
            style: { cursor: "pointer", background: record.id === selectedSession ? "#EFF6FF" : undefined },
          })}
        />
      </Card>

      {selectedData && (
        <Card title={`Session ${selectedData.id} — Trace Details`} className="mt-4">
          <Descriptions size="small" column={4} className="mb-4">
            <Descriptions.Item label="User">{selectedData.user}</Descriptions.Item>
            <Descriptions.Item label="Model"><Tag color="blue">{selectedData.model}</Tag></Descriptions.Item>
            <Descriptions.Item label="Cost">${selectedData.cost.toFixed(4)}</Descriptions.Item>
            <Descriptions.Item label="Latency">{selectedData.latency}ms</Descriptions.Item>
          </Descriptions>

          <Collapse
            items={selectedData.spans.map((span) => ({
              key: span.key,
              label: (
                <Space>
                  <Tag color={span.type === "llm" ? "purple" : span.type === "rag" ? "green" : "orange"}>
                    {span.type.toUpperCase()}
                  </Tag>
                  <Text strong>{span.name}</Text>
                  <Text type="secondary">{span.latency}ms / ${span.cost.toFixed(4)}</Text>
                  <Tag color={span.status === "ok" ? "green" : "red"}>{span.status}</Tag>
                </Space>
              ),
              children: <Text type="secondary">{span.detail}</Text>,
            }))}
          />
        </Card>
      )}
    </div>
  );
}
