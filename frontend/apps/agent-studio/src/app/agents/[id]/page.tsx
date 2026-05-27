"use client";

import { useState, useEffect } from "react";
import { Card, Button, Input, Select, Form, Space, Typography, App, Tag, Spin } from "antd";
import { ArrowLeftOutlined } from "@ant-design/icons";
import { useRouter, useParams } from "next/navigation";
import { agents, statusColors, modelOptions, type Agent } from "../data";

const { Title } = Typography;
const { TextArea } = Input;

export default function AgentDetailPage() {
  const router = useRouter();
  const params = useParams();
  const id = params.id as string;
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [agent, setAgent] = useState<Agent | null>(null);
  const [form] = Form.useForm();
  const { message } = App.useApp();

  useEffect(() => {
    const found = agents.find((a) => a.key === id);
    if (found) {
      setAgent(found);
      form.setFieldsValue(found);
    }
    setLoading(false);
  }, [id, form]);

  const onFinish = (values: Record<string, unknown>) => {
    setSaving(true);
    setTimeout(() => {
      if (agent) {
        Object.assign(agent, values);
      }
      message.success("Agent updated successfully");
      setSaving(false);
      router.push("/");
    }, 500);
  };

  if (loading) {
    return <Spin className="flex justify-center mt-20" size="large" />;
  }

  if (!agent) {
    return (
      <div className="p-6 max-w-3xl mx-auto text-center mt-20">
        <Title level={4} type="secondary">Agent not found</Title>
        <Button type="primary" onClick={() => router.push("/")} className="mt-4">
          Back to Agent Studio
        </Button>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <div className="mb-6">
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => router.back()} className="mb-2">
          Back
        </Button>
        <div className="flex items-center gap-3">
          <Title level={3} style={{ margin: 0 }}>{agent.name}</Title>
          <Tag color={statusColors[agent.status]}>{agent.status}</Tag>
        </div>
      </div>

      <Card>
        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
          initialValues={agent}
        >
          <Form.Item
            name="name"
            label="Agent Name"
            rules={[{ required: true, message: "Please enter agent name" }]}
          >
            <Input placeholder="e.g. Lab Assistant" size="large" />
          </Form.Item>

          <Form.Item
            name="systemPrompt"
            label="System Prompt"
            rules={[{ required: true, message: "Please enter system prompt" }]}
          >
            <TextArea rows={6} placeholder="You are an enterprise AI assistant..." />
          </Form.Item>

          <Form.Item name="model" label="Default Model">
            <Select options={modelOptions} />
          </Form.Item>

          <Form.Item name="desc" label="Description">
            <TextArea rows={2} placeholder="Brief description of this agent..." />
          </Form.Item>

          <Form.Item name="status" label="Status">
            <Select
              options={[
                { value: "Draft", label: "Draft" },
                { value: "Test", label: "Test" },
                { value: "Published", label: "Published" },
              ]}
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={saving}>
                Save Changes
              </Button>
              <Button onClick={() => router.back()}>Cancel</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
