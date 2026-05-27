"use client";

import { useState } from "react";
import { Card, Button, Input, Select, Typography, Steps, Form, App } from "antd";
import { UserOutlined, ApiOutlined, DatabaseOutlined } from "@ant-design/icons";

const { Title, Text } = Typography;

const STEPS = [
  { title: "Create Admin Account", icon: <UserOutlined /> },
  { title: "Configure AI Model", icon: <ApiOutlined /> },
  { title: "Setup Storage", icon: <DatabaseOutlined /> },
];

export default function SetupPage() {
  const [step, setStep] = useState(0);
  const [form] = Form.useForm();
  const { message } = App.useApp();

  const onFinish = () => {
    if (step < 2) {
      setStep(step + 1);
      form.resetFields();
    } else {
      message.success("Setup complete! Redirecting...");
      setTimeout(() => {
        window.location.href = "/";
      }, 1000);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center" style={{ background: "#F0F5FF" }}>
      <Card className="w-full max-w-lg shadow-md" styles={{ body: { padding: 32 } }}>
        <div className="text-center mb-6">
          <Title level={2} style={{ margin: 0 }}>First-Time Setup</Title>
          <Text type="secondary">Configure your enterprise AI platform</Text>
        </div>

        <Steps
          current={step}
          items={STEPS.map((s) => ({ title: s.title }))}
          size="small"
          className="mb-8"
        />

        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
          className="min-h-[200px]"
        >
          {step === 0 && (
            <>
              <Form.Item name="username" label="Admin Username" rules={[{ required: true }]}>
                <Input placeholder="admin" />
              </Form.Item>
              <Form.Item name="email" label="Admin Email" rules={[{ required: true, type: "email" }]}>
                <Input placeholder="admin@company.com" />
              </Form.Item>
              <Form.Item name="password" label="Password" rules={[{ required: true, min: 8 }]}>
                <Input.Password placeholder="Min 8 characters" />
              </Form.Item>
            </>
          )}

          {step === 1 && (
            <>
              <Form.Item name="provider" label="Model Provider" rules={[{ required: true }]}>
                <Select
                  options={[
                    { value: "openai", label: "OpenAI (GPT-4.1)" },
                    { value: "anthropic", label: "Anthropic (Claude Sonnet 4.6)" },
                    { value: "deepseek", label: "DeepSeek V3" },
                  ]}
                />
              </Form.Item>
              <Form.Item name="apiKey" label="API Key" rules={[{ required: true }]}>
                <Input.Password placeholder="sk-..." />
              </Form.Item>
              <Form.Item name="baseUrl" label="Base URL (optional)">
                <Input placeholder="https://api.openai.com/v1" />
              </Form.Item>
            </>
          )}

          {step === 2 && (
            <>
              <Form.Item name="minioEndpoint" label="MinIO Endpoint">
                <Input placeholder="http://localhost:9000" />
              </Form.Item>
              <Form.Item name="qdrantEndpoint" label="Qdrant Endpoint">
                <Input placeholder="http://localhost:6333" />
              </Form.Item>
              <Form.Item name="redisEndpoint" label="Redis Endpoint">
                <Input placeholder="localhost:6379" />
              </Form.Item>
            </>
          )}

          <div className="flex justify-between mt-6">
            <Button disabled={step === 0} onClick={() => setStep(step - 1)}>
              Back
            </Button>
            <Button type="primary" htmlType="submit">
              {step < 2 ? "Next" : "Finish Setup"}
            </Button>
          </div>
        </Form>
      </Card>
    </div>
  );
}
