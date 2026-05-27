"use client";

import { useState } from "react";
import { Table, Button, Input, Tag, Modal, Form, Select, Popconfirm, Space, Typography, message, Card, Row, Col, Statistic } from "antd";
import { PlusOutlined, SearchOutlined, ToolOutlined, ApiOutlined, ThunderboltOutlined } from "@ant-design/icons";

const { Title } = Typography;

interface ToolItem {
  key: string;
  name: string;
  type: string;
  status: string;
  calls: number;
  avgLatency: string;
  description: string;
}

const MOCK_TOOLS: ToolItem[] = [
  { key: "1", name: "weather_api", type: "http", status: "active", calls: 2340, avgLatency: "120ms", description: "Weather query API" },
  { key: "2", name: "order_lookup", type: "http", status: "active", calls: 15200, avgLatency: "85ms", description: "Order lookup service" },
  { key: "3", name: "excel_parser", type: "sdk", status: "active", calls: 890, avgLatency: "450ms", description: "Excel/CSV parser" },
  { key: "4", name: "mcp_search", type: "mcp", status: "inactive", calls: 0, avgLatency: "-", description: "MCP search tool" },
];

export default function ToolsPage() {
  const [search, setSearch] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [messageApi, ctx] = message.useMessage();
  const [form] = Form.useForm();

  const filtered = MOCK_TOOLS.filter(
    (t) => !search || t.name.includes(search) || t.type.includes(search)
  );

  const columns = [
    { title: "Name", dataIndex: "name", key: "name", render: (v: string) => <strong>{v}</strong> },
    {
      title: "Type", dataIndex: "type", key: "type",
      render: (v: string) => {
        const colors: Record<string, string> = { http: "blue", sdk: "purple", mcp: "orange" };
        return <Tag color={colors[v] || "default"}>{v.toUpperCase()}</Tag>;
      },
    },
    {
      title: "Status", dataIndex: "status", key: "status",
      render: (v: string) => <Tag color={v === "active" ? "green" : "default"}>{v}</Tag>,
    },
    { title: "Calls", dataIndex: "calls", key: "calls", render: (v: number) => v.toLocaleString() },
    { title: "Avg Latency", dataIndex: "avgLatency", key: "avgLatency" },
    {
      title: "Actions", key: "actions",
      render: () => (
        <Space>
          <Popconfirm title="Delete?" onConfirm={() => messageApi.success("Deleted")}>
            <Button type="link" size="small" danger>Delete</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="p-6">
      {ctx}
      <div className="flex flex-wrap justify-between items-center mb-4 gap-3">
        <Title level={3} style={{ margin: 0 }}>Tool Registry</Title>
        <Space>
          <Input prefix={<SearchOutlined />} placeholder="Search tools..." value={search}
            onChange={(e) => setSearch(e.target.value)} style={{ width: 220 }} allowClear />
          <Button type="primary" icon={<PlusOutlined />}
            onClick={() => { form.resetFields(); setModalOpen(true); }}>
            Register Tool
          </Button>
        </Space>
      </div>

      <Row gutter={[16, 16]} className="mb-6">
        <Col xs={24} sm={8}><Card><Statistic title="Total Tools" value={4} prefix={<ToolOutlined />} styles={{ value: { color: "#4096FF" } }} /></Card></Col>
        <Col xs={24} sm={8}><Card><Statistic title="Total Calls" value={18430} prefix={<ApiOutlined />} styles={{ value: { color: "#4096FF" } }} /></Card></Col>
        <Col xs={24} sm={8}><Card><Statistic title="Active Tools" value={3} prefix={<ThunderboltOutlined />} styles={{ value: { color: "#4096FF" } }} /></Card></Col>
      </Row>

      <Table dataSource={filtered} columns={columns} pagination={{ pageSize: 10 }} size="middle"
        expandable={{
          expandedRowRender: (record: ToolItem) => (
            <p className="text-gray-500">{record.description}</p>
          ),
        }} />

      <Modal title="Register Tool" open={modalOpen} onCancel={() => setModalOpen(false)}
        onOk={() => { form.submit(); }}>
        <Form form={form} layout="vertical" onFinish={() => { messageApi.success("Saved"); setModalOpen(false); }}>
          <Form.Item name="name" label="Tool Name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="type" label="Tool Type" rules={[{ required: true }]}>
            <Select options={[
              { value: "http", label: "HTTP API" }, { value: "sdk", label: "SDK" }, { value: "mcp", label: "MCP" },
            ]} />
          </Form.Item>
          <Form.Item name="description" label="Description"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item name="status" label="Status">
            <Select options={[{ value: "active", label: "Active" }, { value: "inactive", label: "Inactive" }]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
