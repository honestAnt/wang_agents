"use client";

import { useState } from "react";
import { useAuth } from "@enterprise-ai/auth";
import { useRouter } from "next/navigation";
import { apiClient } from "@enterprise-ai/api-client";
import { Card, Form, Input, Button, message, Typography } from "antd";
import { LockOutlined, UserOutlined } from "@ant-design/icons";

const { Title, Text } = Typography;

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const response = await apiClient.post("/api/auth/login", values);
      const data = response.data.data;
      login(data.access_token, data.tenantId);
      router.push("/");
    } catch {
      messageApi.error("Invalid username or password.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-primary-50">
      {contextHolder}
      <Card className="w-full max-w-md shadow-md" styles={{ body: { padding: 32 } }}>
        <div className="text-center mb-8">
          <div className="text-4xl mb-3">🏢</div>
          <Title level={3} style={{ margin: 0 }}>Enterprise AI</Title>
          <Text type="secondary">Admin Console</Text>
        </div>

        <Form
          name="login"
          onFinish={onFinish}
          layout="vertical"
          size="large"
          initialValues={{ username: "", password: "" }}
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: "Please enter your username" }]}
          >
            <Input prefix={<UserOutlined />} placeholder="Username" autoFocus />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: "Please enter your password" }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="Password" />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" loading={loading} block>
              Sign In
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
