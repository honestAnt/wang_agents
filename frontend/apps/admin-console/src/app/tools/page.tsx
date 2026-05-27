"use client";

import { useState, useEffect, useCallback } from "react";
import {
  Table, Button, Input, Tag, Modal, Form, Select, Popconfirm, Space, Typography,
  message, Card, Row, Col, Statistic, InputNumber, Drawer
} from "antd";
import {
  PlusOutlined, SearchOutlined, ToolOutlined, ApiOutlined,
  ThunderboltOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined, EyeOutlined
} from "@ant-design/icons";
import { apiClient } from "@enterprise-ai/api-client";

const { Title, Text, Paragraph } = Typography;

interface Tool {
  id: string;
  tenantId: string;
  name: string;
  displayName: string;
  description: string;
  toolType: string;
  schemaJson: string;
  endpointUrl: string;
  method: string;
  timeoutMs: number;
  retryCount: number;
  retryBackoff: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

interface ToolStats {
  total: number;
  active: number;
  inactive: number;
}

const TYPE_COLORS: Record<string, string> = { http: "blue", sdk: "purple", mcp: "orange" };

export default function ToolsPage() {
  const [tools, setTools] = useState<Tool[]>([]);
  const [stats, setStats] = useState<ToolStats>({ total: 0, active: 0, inactive: 0 });
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTool, setEditingTool] = useState<Tool | null>(null);
  const [detailTool, setDetailTool] = useState<Tool | null>(null);
  const [testOpen, setTestOpen] = useState(false);
  const [testParams, setTestParams] = useState("");
  const [testResult, setTestResult] = useState<string>("");
  const [testing, setTesting] = useState(false);
  const [messageApi, ctx] = message.useMessage();
  const [form] = Form.useForm();

  const tenantId = typeof window !== "undefined" ? localStorage.getItem("tenant_id") || "default" : "default";

  const fetchTools = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiClient.get("/api/tools", { params: { tenantId } });
      setTools(res.data?.data || res.data || []);
    } catch {
      messageApi.error("Failed to load tools");
    } finally {
      setLoading(false);
    }
  }, [tenantId, messageApi]);

  const fetchStats = useCallback(async () => {
    try {
      const res = await apiClient.get("/api/tools/stats", { params: { tenantId } });
      setStats(res.data?.data || res.data || { total: 0, active: 0, inactive: 0 });
    } catch {
      // non-critical
    }
  }, [tenantId]);

  useEffect(() => {
    fetchTools();
    fetchStats();
  }, [fetchTools, fetchStats]);

  const filtered = tools.filter(
    (t) => !search
      || t.name.toLowerCase().includes(search.toLowerCase())
      || (t.displayName || "").toLowerCase().includes(search.toLowerCase())
      || t.toolType.toLowerCase().includes(search.toLowerCase())
  );

  const openCreate = () => {
    setEditingTool(null);
    form.resetFields();
    form.setFieldsValue({ method: "POST", timeoutMs: 30000, retryCount: 0, retryBackoff: "fixed", toolType: "http", status: "active" });
    setModalOpen(true);
  };

  const openEdit = (tool: Tool) => {
    setEditingTool(tool);
    form.setFieldsValue({
      name: tool.name,
      displayName: tool.displayName,
      description: tool.description,
      toolType: tool.toolType,
      schemaJson: tool.schemaJson,
      endpointUrl: tool.endpointUrl,
      method: tool.method,
      timeoutMs: tool.timeoutMs,
      retryCount: tool.retryCount,
      retryBackoff: tool.retryBackoff,
      status: tool.status,
    });
    setModalOpen(true);
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      if (editingTool) {
        await apiClient.put(`/api/tools/${editingTool.id}`, values);
        messageApi.success("Tool updated");
      } else {
        await apiClient.post("/api/tools", { ...values, tenantId });
        messageApi.success("Tool registered");
      }
      setModalOpen(false);
      fetchTools();
      fetchStats();
    } catch {
      messageApi.error("Failed to save tool");
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await apiClient.delete(`/api/tools/${id}`);
      messageApi.success("Tool deleted");
      fetchTools();
      fetchStats();
    } catch {
      messageApi.error("Failed to delete tool");
    }
  };

  const openTest = (tool: Tool) => {
    setDetailTool(tool);
    setTestParams("{}");
    setTestResult("");
    setTestOpen(true);
  };

  const handleTest = async () => {
    if (!detailTool) return;
    setTesting(true);
    try {
      let params: Record<string, unknown> = {};
      try { params = JSON.parse(testParams); } catch { /* use empty */ }
      const res = await apiClient.post("/api/tools/execute", {
        toolId: detailTool.id,
        params,
      });
      setTestResult(JSON.stringify(res.data?.data || res.data, null, 2));
    } catch {
      setTestResult("Execution failed");
    } finally {
      setTesting(false);
    }
  };

  const columns = [
    {
      title: "Name", dataIndex: "name", key: "name",
      render: (v: string, r: Tool) => <strong>{r.displayName || v}</strong>,
    },
    {
      title: "Type", dataIndex: "toolType", key: "toolType",
      render: (v: string) => <Tag color={TYPE_COLORS[v] || "default"}>{(v || "").toUpperCase()}</Tag>,
    },
    {
      title: "Endpoint", dataIndex: "endpointUrl", key: "endpointUrl",
      render: (v: string, r: Tool) => (
        <Text code className="text-xs">{r.method} {v || "-"}</Text>
      ),
    },
    {
      title: "Status", dataIndex: "status", key: "status",
      render: (v: string) => <Tag color={v === "active" ? "green" : "default"}>{v}</Tag>,
    },
    {
      title: "Timeout", dataIndex: "timeoutMs", key: "timeoutMs",
      render: (v: number) => v ? `${v}ms` : "-",
    },
    {
      title: "Retry", dataIndex: "retryCount", key: "retryCount",
      render: (v: number, r: Tool) => v > 0 ? `${v} (${r.retryBackoff})` : "-",
    },
    {
      title: "Actions", key: "actions",
      render: (_: unknown, record: Tool) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => setDetailTool(record)} />
          <Button type="link" size="small" icon={<PlayCircleOutlined />} onClick={() => openTest(record)}>
            Test
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          <Popconfirm title="Delete this tool?" onConfirm={() => handleDelete(record.id)}>
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
        <Title level={3} style={{ margin: 0 }}>Tool Registry</Title>
        <Space>
          <Input
            prefix={<SearchOutlined />}
            placeholder="Search tools..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ width: 220 }}
            allowClear
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Register Tool
          </Button>
        </Space>
      </div>

      <Row gutter={[16, 16]} className="mb-6">
        <Col xs={24} sm={8}>
          <Card><Statistic title="Total Tools" value={stats.total} prefix={<ToolOutlined />} styles={{ value: { color: "#4096FF" } }} /></Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card><Statistic title="Active Tools" value={stats.active} prefix={<ThunderboltOutlined />} styles={{ value: { color: "#52c41a" } }} /></Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card><Statistic title="Inactive Tools" value={stats.inactive} prefix={<ApiOutlined />} styles={{ value: { color: "#8c8c8c" } }} /></Card>
        </Col>
      </Row>

      <Table
        dataSource={filtered}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10 }}
        size="middle"
        expandable={{
          expandedRowRender: (record: Tool) => (
            <div className="space-y-2 py-2">
              {record.description && <p className="text-gray-500">{record.description}</p>}
              {record.schemaJson && (
                <div>
                  <Text strong className="text-xs">Schema:</Text>
                  <pre className="bg-gray-50 p-2 rounded text-xs mt-1 whitespace-pre-wrap">{record.schemaJson}</pre>
                </div>
              )}
            </div>
          ),
        }}
      />

      {/* Create / Edit Modal */}
      <Modal
        title={editingTool ? "Edit Tool" : "Register Tool"}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSave}
        width={720}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="name" label="Tool Name" rules={[{ required: true }]}>
                <Input placeholder="e.g. weather_api" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="displayName" label="Display Name">
                <Input placeholder="e.g. Weather API" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={2} placeholder="Describe what this tool does..." />
          </Form.Item>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="toolType" label="Tool Type" rules={[{ required: true }]}>
                <Select options={[
                  { value: "http", label: "HTTP API" },
                  { value: "sdk", label: "SDK" },
                  { value: "mcp", label: "MCP" },
                ]} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="method" label="HTTP Method">
                <Select options={[
                  { value: "GET", label: "GET" },
                  { value: "POST", label: "POST" },
                  { value: "PUT", label: "PUT" },
                  { value: "DELETE", label: "DELETE" },
                ]} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="status" label="Status">
                <Select options={[
                  { value: "active", label: "Active" },
                  { value: "inactive", label: "Inactive" },
                ]} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="endpointUrl" label="Endpoint URL">
            <Input placeholder="https://api.example.com/v1/weather" />
          </Form.Item>
          <Form.Item name="schemaJson" label="Schema (JSON Schema)">
            <Input.TextArea rows={3} placeholder='{"type": "object", "properties": {"city": {"type": "string"}}}' />
          </Form.Item>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="timeoutMs" label="Timeout (ms)">
                <InputNumber min={100} max={300000} style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="retryCount" label="Retry Count">
                <InputNumber min={0} max={10} style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="retryBackoff" label="Backoff Strategy">
                <Select options={[
                  { value: "fixed", label: "Fixed" },
                  { value: "exponential", label: "Exponential" },
                ]} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* Detail Drawer */}
      <Drawer
        title={detailTool?.displayName || detailTool?.name}
        open={!!detailTool && !testOpen}
        onClose={() => setDetailTool(null)}
        width={480}
      >
        {detailTool && (
          <div className="space-y-4">
            <div>
              <Text strong>Name</Text><br /><Text code>{detailTool.name}</Text>
            </div>
            <div>
              <Text strong>Type</Text><br />
              <Tag color={TYPE_COLORS[detailTool.toolType] || "default"}>{(detailTool.toolType || "").toUpperCase()}</Tag>
            </div>
            <div>
              <Text strong>Status</Text><br />
              <Tag color={detailTool.status === "active" ? "green" : "default"}>{detailTool.status}</Tag>
            </div>
            <div>
              <Text strong>Endpoint</Text><br />
              <Text code>{detailTool.method} {detailTool.endpointUrl || "-"}</Text>
            </div>
            <div>
              <Text strong>Timeout / Retry</Text><br />
              <Text>{detailTool.timeoutMs}ms / {detailTool.retryCount} retries ({detailTool.retryBackoff})</Text>
            </div>
            {detailTool.description && (
              <div>
                <Text strong>Description</Text>
                <Paragraph>{detailTool.description}</Paragraph>
              </div>
            )}
            {detailTool.schemaJson && (
              <div>
                <Text strong>Schema</Text>
                <pre className="bg-gray-50 p-3 rounded text-xs mt-1 whitespace-pre-wrap">{detailTool.schemaJson}</pre>
              </div>
            )}
            <div>
              <Text strong>Created</Text><br />
              <Text type="secondary">{new Date(detailTool.createdAt).toLocaleString()}</Text>
            </div>
            <div>
              <Text strong>Updated</Text><br />
              <Text type="secondary">{new Date(detailTool.updatedAt).toLocaleString()}</Text>
            </div>
          </div>
        )}
      </Drawer>

      {/* Test Modal */}
      <Modal
        title={`Test: ${detailTool?.displayName || detailTool?.name || ""}`}
        open={testOpen}
        onCancel={() => { setTestOpen(false); setDetailTool(null); }}
        footer={[
          <Button key="close" onClick={() => { setTestOpen(false); setDetailTool(null); }}>Close</Button>,
          <Button key="run" type="primary" loading={testing} icon={<PlayCircleOutlined />} onClick={handleTest}>
            Execute
          </Button>,
        ]}
        width={640}
      >
        <div className="space-y-4">
          <div>
            <Text strong>Parameters (JSON)</Text>
            <Input.TextArea
              rows={6}
              value={testParams}
              onChange={(e) => setTestParams(e.target.value)}
              placeholder='{"city": "Beijing"}'
              className="font-mono text-sm"
            />
          </div>
          {testResult && (
            <div>
              <Text strong>Result</Text>
              <pre className="bg-gray-900 text-green-400 p-4 rounded text-sm mt-1 whitespace-pre-wrap overflow-auto max-h-80">
                {testResult}
              </pre>
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}
