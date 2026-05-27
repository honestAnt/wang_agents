"use client";

import { useState } from "react";
import { Card, Button, Input, Tag, Typography, Row, Col, Space, Empty } from "antd";
import { PlusOutlined, EditOutlined, ExperimentOutlined } from "@ant-design/icons";

const { Title, Text } = Typography;
const { TextArea } = Input;

const MOCK_PROMPTS = [
  { key: "1", name: "System Prompt v2", version: "2.1", tokens: 1200, category: "system" },
  { key: "2", name: "Code Review Prompt", version: "1.4", tokens: 850, category: "code" },
  { key: "3", name: "Data Analysis Prompt", version: "3.0", tokens: 2100, category: "analysis" },
  { key: "4", name: "Customer Service", version: "1.2", tokens: 650, category: "service" },
];

export default function PromptCenterPage() {
  const [selectedPrompt, setSelectedPrompt] = useState<string | null>(null);
  const [editContent, setEditContent] = useState("");

  const selected = MOCK_PROMPTS.find((p) => p.key === selectedPrompt);

  return (
    <div className="p-6">
      <div className="flex flex-wrap justify-between items-center mb-6 gap-3">
        <Title level={3} style={{ margin: 0 }}>Prompt Center</Title>
        <Button type="primary" icon={<PlusOutlined />}>New Prompt</Button>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={10}>
          <Card title="Prompt Templates" styles={{ body: { padding: 0 } }}>
            {MOCK_PROMPTS.map((p) => (
              <div
                key={p.key}
                onClick={() => {
                  setSelectedPrompt(p.key);
                  setEditContent(`You are an enterprise AI assistant for ${p.category}. Use the following context to answer questions accurately.`);
                }}
                className="cursor-pointer px-4 py-3 border-b border-gray-50 last:border-b-0 hover:bg-blue-50 transition-colors"
                style={{
                  background: selectedPrompt === p.key ? "#EFF6FF" : undefined,
                  borderLeft: selectedPrompt === p.key ? "3px solid #4096FF" : "3px solid transparent",
                }}
              >
                <div className="flex items-center justify-between">
                  <Text strong>{p.name}</Text>
                  <Tag color="blue">v{p.version}</Tag>
                </div>
                <div className="flex items-center gap-3 mt-1">
                  <Tag>{p.category}</Tag>
                  <Text type="secondary" className="text-xs">{p.tokens.toLocaleString()} tokens</Text>
                </div>
              </div>
            ))}
          </Card>
        </Col>

        <Col xs={24} lg={14}>
          {selected ? (
            <Card
              title={`Edit: ${selected.name}`}
              extra={
                <Space>
                  <Tag color="blue">v{selected.version}</Tag>
                  <Text type="secondary" className="text-xs">{selected.tokens.toLocaleString()} tokens</Text>
                </Space>
              }
            >
              <TextArea
                rows={10}
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                className="font-mono text-sm"
              />
              <div className="flex gap-2 mt-4">
                <Button type="primary" icon={<EditOutlined />}>Save</Button>
                <Button icon={<ExperimentOutlined />}>Test with Models</Button>
              </div>
            </Card>
          ) : (
            <Card title="Prompt Editor">
              <Empty description="Select a prompt template to edit" />
            </Card>
          )}
        </Col>
      </Row>
    </div>
  );
}
