"use client";

import { useState, useEffect, useCallback } from "react";
import {
  Card, Button, Input, Tag, Rate, Typography, Row, Col, Segmented, Space,
  Modal, Form, Select, Drawer, Popconfirm, message, Spin, Empty
} from "antd";
import {
  PlusOutlined, SearchOutlined, DownloadOutlined,
  EditOutlined, DeleteOutlined, SendOutlined, EyeOutlined
} from "@ant-design/icons";
import { apiClient } from "@enterprise-ai/api-client";

const { Title, Text, Paragraph } = Typography;

interface Skill {
  id: string;
  tenantId: string;
  name: string;
  displayName: string;
  description: string;
  category: string;
  iconUrl: string;
  status: string;
  version: number;
  promptTemplate: string;
  inputSchema: string;
  outputSchema: string;
  downloadCount: number;
  rating: number;
  createdAt: string;
  updatedAt: string;
}

const CATEGORY_LABELS: Record<string, string> = {
  general: "General",
  analysis: "Analysis",
  customer_service: "Customer Service",
  legal: "Legal",
  engineering: "Engineering",
  finance: "Finance",
};

export default function SkillsPage() {
  const [skills, setSkills] = useState<Skill[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [category, setCategory] = useState("all");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingSkill, setEditingSkill] = useState<Skill | null>(null);
  const [detailSkill, setDetailSkill] = useState<Skill | null>(null);
  const [messageApi, ctx] = message.useMessage();
  const [form] = Form.useForm();

  const tenantId = typeof window !== "undefined" ? localStorage.getItem("tenant_id") || "default" : "default";

  const fetchSkills = useCallback(async () => {
    setLoading(true);
    try {
      const res = await apiClient.get("/api/skills", { params: { tenantId } });
      setSkills(res.data?.data || res.data || []);
    } catch {
      messageApi.error("Failed to load skills");
    } finally {
      setLoading(false);
    }
  }, [tenantId, messageApi]);

  const fetchCategories = useCallback(async () => {
    try {
      const res = await apiClient.get("/api/skills/categories", { params: { tenantId } });
      setCategories(res.data?.data || res.data || []);
    } catch {
      // non-critical
    }
  }, [tenantId]);

  useEffect(() => {
    fetchSkills();
    fetchCategories();
  }, [fetchSkills, fetchCategories]);

  const filtered = skills.filter((s) => {
    const matchCat = category === "all" || s.category === category;
    const matchSearch = !search
      || s.name.toLowerCase().includes(search.toLowerCase())
      || (s.displayName || "").toLowerCase().includes(search.toLowerCase())
      || (s.description || "").toLowerCase().includes(search.toLowerCase());
    return matchCat && matchSearch;
  });

  const openCreate = () => {
    setEditingSkill(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = (skill: Skill) => {
    setEditingSkill(skill);
    form.setFieldsValue({
      name: skill.name,
      displayName: skill.displayName,
      description: skill.description,
      category: skill.category,
      promptTemplate: skill.promptTemplate,
      inputSchema: skill.inputSchema,
      outputSchema: skill.outputSchema,
      iconUrl: skill.iconUrl,
      status: skill.status,
    });
    setModalOpen(true);
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      if (editingSkill) {
        await apiClient.put(`/api/skills/${editingSkill.id}`, values);
        messageApi.success("Skill updated");
      } else {
        await apiClient.post("/api/skills", { ...values, tenantId });
        messageApi.success("Skill created");
      }
      setModalOpen(false);
      fetchSkills();
      fetchCategories();
    } catch {
      messageApi.error("Failed to save skill");
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await apiClient.delete(`/api/skills/${id}`);
      messageApi.success("Skill deleted");
      fetchSkills();
      fetchCategories();
    } catch {
      messageApi.error("Failed to delete skill");
    }
  };

  const handlePublish = async (id: string) => {
    try {
      await apiClient.post(`/api/skills/${id}/publish`);
      messageApi.success("Skill published");
      fetchSkills();
    } catch {
      messageApi.error("Failed to publish skill");
    }
  };

  const handleInstall = async (id: string) => {
    try {
      await apiClient.post(`/api/skills/${id}/install`);
      messageApi.success("Skill installed");
      fetchSkills();
    } catch {
      messageApi.error("Failed to install skill");
    }
  };

  const segOptions = [
    { value: "all", label: "All" },
    ...categories.map((c) => ({ value: c, label: CATEGORY_LABELS[c] || c })),
  ];

  return (
    <div className="p-6">
      {ctx}
      <div className="flex flex-wrap justify-between items-center mb-6 gap-3">
        <Title level={3} style={{ margin: 0 }}>Skills Marketplace</Title>
        <Space>
          <Input
            prefix={<SearchOutlined />}
            placeholder="Search skills..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ width: 220 }}
            allowClear
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Create Skill
          </Button>
        </Space>
      </div>

      <Segmented
        value={category}
        onChange={(v) => setCategory(v as string)}
        options={segOptions}
        className="mb-6"
      />

      {loading ? (
        <div className="flex justify-center py-20">
          <Spin size="large" />
        </div>
      ) : filtered.length === 0 ? (
        <Empty description="No skills found" className="py-20" />
      ) : (
        <Row gutter={[16, 16]}>
          {filtered.map((s) => (
            <Col xs={24} sm={12} lg={6} key={s.id}>
              <Card
                hoverable
                title={s.displayName || s.name}
                extra={<Tag color={s.status === "published" ? "green" : s.status === "test" ? "blue" : "default"}>{s.status}</Tag>}
                actions={[
                  <EyeOutlined key="view" onClick={() => setDetailSkill(s)} />,
                  <EditOutlined key="edit" onClick={() => openEdit(s)} />,
                  s.status !== "published" && (
                    <SendOutlined key="publish" onClick={() => handlePublish(s.id)} />
                  ),
                  <Popconfirm
                    key="delete"
                    title="Delete this skill?"
                    onConfirm={() => handleDelete(s.id)}
                  >
                    <DeleteOutlined />
                  </Popconfirm>,
                ].filter(Boolean)}
              >
                <Text type="secondary" className="text-sm block mb-2">
                  Category: {CATEGORY_LABELS[s.category] || s.category}
                </Text>
                {s.description && (
                  <Paragraph ellipsis={{ rows: 2 }} type="secondary" className="text-sm mb-3">
                    {s.description}
                  </Paragraph>
                )}
                <div className="flex items-center gap-2 mb-3">
                  <Rate disabled allowHalf value={s.rating} style={{ fontSize: 13 }} />
                  <Text type="secondary" className="text-xs">({s.rating})</Text>
                </div>
                <div className="flex justify-between items-center">
                  <Text type="secondary" className="text-xs">
                    v{s.version} · {(s.downloadCount || 0).toLocaleString()} downloads
                  </Text>
                  <Button
                    type="default"
                    icon={<DownloadOutlined />}
                    size="small"
                    onClick={() => handleInstall(s.id)}
                  >
                    Install
                  </Button>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      {/* Create / Edit Modal */}
      <Modal
        title={editingSkill ? "Edit Skill" : "Create Skill"}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSave}
        width={640}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="name" label="Name" rules={[{ required: true }]}>
                <Input placeholder="e.g. data_analysis" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="displayName" label="Display Name">
                <Input placeholder="e.g. Data Analysis" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={2} placeholder="Describe what this skill does..." />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="category" label="Category">
                <Select
                  placeholder="Select category"
                  options={categories.map((c) => ({ value: c, label: CATEGORY_LABELS[c] || c }))}
                />
              </Form.Item>
            </Col>
            {editingSkill && (
              <Col span={12}>
                <Form.Item name="status" label="Status">
                  <Select
                    options={[
                      { value: "draft", label: "Draft" },
                      { value: "test", label: "Test" },
                      { value: "published", label: "Published" },
                      { value: "archived", label: "Archived" },
                    ]}
                  />
                </Form.Item>
              </Col>
            )}
          </Row>
          <Form.Item name="promptTemplate" label="Prompt Template">
            <Input.TextArea rows={4} placeholder="System prompt template with {{variables}}..." />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="inputSchema" label="Input Schema (JSON)">
                <Input.TextArea rows={3} placeholder='{"type": "object", "properties": {...}}' />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="outputSchema" label="Output Schema (JSON)">
                <Input.TextArea rows={3} placeholder='{"type": "object", "properties": {...}}' />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="iconUrl" label="Icon URL">
            <Input placeholder="https://..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Detail Drawer */}
      <Drawer
        title={detailSkill?.displayName || detailSkill?.name}
        open={!!detailSkill}
        onClose={() => setDetailSkill(null)}
        width={480}
      >
        {detailSkill && (
          <div className="space-y-4">
            <div>
              <Text strong>Name</Text>
              <br /><Text code>{detailSkill.name}</Text>
            </div>
            <div>
              <Text strong>Status</Text>
              <br /><Tag color={detailSkill.status === "published" ? "green" : "default"}>{detailSkill.status}</Tag>
            </div>
            <div>
              <Text strong>Category</Text>
              <br /><Text>{CATEGORY_LABELS[detailSkill.category] || detailSkill.category}</Text>
            </div>
            <div>
              <Text strong>Version</Text>
              <br /><Text>{detailSkill.version}</Text>
            </div>
            <div>
              <Text strong>Downloads</Text>
              <br /><Text>{detailSkill.downloadCount.toLocaleString()}</Text>
            </div>
            <div>
              <Text strong>Rating</Text>
              <br /><Rate disabled allowHalf value={detailSkill.rating} />
            </div>
            {detailSkill.description && (
              <div>
                <Text strong>Description</Text>
                <Paragraph>{detailSkill.description}</Paragraph>
              </div>
            )}
            {detailSkill.promptTemplate && (
              <div>
                <Text strong>Prompt Template</Text>
                <pre className="bg-gray-50 p-3 rounded text-xs mt-1 whitespace-pre-wrap">
                  {detailSkill.promptTemplate}
                </pre>
              </div>
            )}
            {detailSkill.inputSchema && (
              <div>
                <Text strong>Input Schema</Text>
                <pre className="bg-gray-50 p-3 rounded text-xs mt-1 whitespace-pre-wrap">
                  {detailSkill.inputSchema}
                </pre>
              </div>
            )}
            {detailSkill.outputSchema && (
              <div>
                <Text strong>Output Schema</Text>
                <pre className="bg-gray-50 p-3 rounded text-xs mt-1 whitespace-pre-wrap">
                  {detailSkill.outputSchema}
                </pre>
              </div>
            )}
            <div>
              <Text strong>Created</Text>
              <br /><Text type="secondary">{new Date(detailSkill.createdAt).toLocaleString()}</Text>
            </div>
          </div>
        )}
      </Drawer>
    </div>
  );
}
