"use client";

import { useState } from "react";
import { Table, Button, Input, Tag, Modal, Form, Select, Popconfirm, Space, Typography, message, Card, Row, Col } from "antd";
import { PlusOutlined, SearchOutlined, EditOutlined, DeleteOutlined, ApiOutlined } from "@ant-design/icons";

const { Title } = Typography;

interface ModelItem {
  key: string;
  name: string;
  provider: string;
  status: string;
  quota: string;
  tps: number;
}

const MOCK_MODELS: ModelItem[] = [
  { key: "1", name: "GPT-4.1", provider: "openai", status: "active", quota: "1M tokens", tps: 120 },
  { key: "2", name: "Claude Sonnet 4.6", provider: "anthropic", status: "active", quota: "500K tokens", tps: 85 },
  { key: "3", name: "DeepSeek V3", provider: "deepseek", status: "active", quota: "2M tokens", tps: 200 },
  { key: "4", name: "Qwen Max", provider: "alibaba", status: "inactive", quota: "300K tokens", tps: 0 },
];

export default function ModelsPage() {
  const [search, setSearch] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<ModelItem | null>(null);
  const [messageApi, ctx] = message.useMessage();
  const [form] = Form.useForm();

  const filtered = MOCK_MODELS.filter(
    (m) => !search || m.name.toLowerCase().includes(search.toLowerCase()) || m.provider.includes(search)
  );

  const columns = [
    { title: "Name", dataIndex: "name", key: "name", render: (v: string) => <strong>{v}</strong> },
    {
      title: "Provider", dataIndex: "provider", key: "provider",
      render: (v: string) => <Tag>{v}</Tag>,
    },
    {
      title: "Status", dataIndex: "status", key: "status",
      render: (v: string) => <Tag color={v === "active" ? "green" : "default"}>{v}</Tag>,
    },
    { title: "Quota", dataIndex: "quota", key: "quota" },
    { title: "TPS", dataIndex: "tps", key: "tps", render: (v: number) => v > 0 ? v : "-" },
    {
      title: "Actions", key: "actions",
      render: (_: unknown, record: ModelItem) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />}
            onClick={() => { setEditingModel(record); form.setFieldsValue(record); setModalOpen(true); }} />
          <Popconfirm title="Delete?" onConfirm={() => messageApi.success("Deleted")}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="p-6">
      {ctx}
      <div className="flex flex-wrap justify-between items-center mb-4 gap-3">
        <Title level={3} style={{ margin: 0 }}>Model Management</Title>
        <Space>
          <Input prefix={<SearchOutlined />} placeholder="Search models..."
            value={search} onChange={(e) => setSearch(e.target.value)} style={{ width: 220 }} allowClear />
          <Button type="primary" icon={<PlusOutlined />}
            onClick={() => { setEditingModel(null); form.resetFields(); setModalOpen(true); }}>
            Register Model
          </Button>
        </Space>
      </div>

      <Row gutter={[16, 16]} className="mb-6">
        {filtered.map((m) => (
          <Col xs={24} sm={12} lg={6} key={m.key}>
            <Card hoverable>
              <div className="flex items-center gap-3 mb-3">
                <ApiOutlined className="text-2xl" style={{ color: "#4096FF" }} />
                <div>
                  <div className="font-semibold">{m.name}</div>
                  <Tag color={m.status === "active" ? "green" : "default"} className="text-xs">{m.status}</Tag>
                </div>
              </div>
              <div className="text-sm text-gray-500 space-y-1">
                <div>Provider: <Tag>{m.provider}</Tag></div>
                <div>Quota: {m.quota}</div>
                <div>TPS: {m.tps > 0 ? m.tps : "-"}</div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      <Card title="All Models" className="mt-4">
        <Table dataSource={filtered} columns={columns} pagination={false} size="small" />
      </Card>

      <Modal title={editingModel ? "Edit Model" : "Register Model"} open={modalOpen}
        onCancel={() => setModalOpen(false)} onOk={() => { form.submit(); }}>
        <Form form={form} layout="vertical" onFinish={() => { messageApi.success("Saved"); setModalOpen(false); }}>
          <Form.Item name="name" label="Model Name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="provider" label="Provider" rules={[{ required: true }]}>
            <Select options={[
              { value: "openai", label: "OpenAI" }, { value: "anthropic", label: "Anthropic" },
              { value: "deepseek", label: "DeepSeek" }, { value: "alibaba", label: "Alibaba" },
            ]} />
          </Form.Item>
          <Form.Item name="quota" label="Quota"><Input /></Form.Item>
          <Form.Item name="status" label="Status">
            <Select options={[{ value: "active", label: "Active" }, { value: "inactive", label: "Inactive" }]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
