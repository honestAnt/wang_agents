"use client";

import { useState } from "react";
import { Card, Button, Input, Select, Form, Space, Typography, App } from "antd";
import { ArrowLeftOutlined } from "@ant-design/icons";
import { useRouter } from "next/navigation";

const { Title } = Typography;
const { TextArea } = Input;

export default function NewAgentPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  const { message } = App.useApp();

  const onFinish = (values: Record<string, unknown>) => {
    setLoading(true);
    setTimeout(() => {
      message.success("Agent saved as draft");
      setLoading(false);
      router.push("/");
    }, 500);
  };

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <div className="mb-6">
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => router.back()} className="mb-2">
          Back
        </Button>
        <Title level={3} style={{ margin: 0 }}>Create Agent</Title>
      </div>

      <Card>
        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
          initialValues={{ model: "gpt-4.1" }}
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
            <Select
              options={[
                { value: "gpt-4.1", label: "GPT-4.1" },
                { value: "claude-sonnet-4-6", label: "Claude Sonnet 4.6" },
                { value: "deepseek-v3", label: "DeepSeek V3" },
                { value: "qwen-max", label: "Qwen Max" },
              ]}
            />
          </Form.Item>

          <Form.Item name="description" label="Description">
            <TextArea rows={2} placeholder="Brief description of this agent..." />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" loading={loading}>
                Save Draft
              </Button>
              <Button onClick={() => router.back()}>Cancel</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
