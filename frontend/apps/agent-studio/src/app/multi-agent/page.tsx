"use client";

import { Card, Input, Tag, Typography, Row, Col } from "antd";
import { NodeIndexOutlined, SearchOutlined, BarChartOutlined, FileTextOutlined } from "@ant-design/icons";

const { Title, Text } = Typography;

const AGENTS = [
  { name: "Coordinator", status: "active", task: "Decompose user request into sub-tasks", icon: <NodeIndexOutlined /> },
  { name: "SearchAgent", status: "active", task: "Execute RAG retrieval + Tool queries", icon: <SearchOutlined /> },
  { name: "AnalysisAgent", status: "active", task: "Data analysis + reasoning", icon: <BarChartOutlined /> },
  { name: "ReportAgent", status: "idle", task: "Aggregate results + generate report", icon: <FileTextOutlined /> },
];

export default function MultiAgentPage() {
  return (
    <div className="p-6">
      <Title level={3} className="mb-6">Multi-Agent Debug Console</Title>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Task Input">
            <Input.TextArea
              rows={3}
              placeholder="Enter a complex task to decompose..."
            />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Execution Flow">
            <div className="space-y-3">
              {AGENTS.map((a, i) => (
                <div key={a.name}>
                  <div className="flex items-center gap-2 p-2 rounded" style={{ background: "#FAFAFA" }}>
                    <div
                      className="w-3 h-3 rounded-full flex-shrink-0"
                      style={{ background: a.status === "active" ? "#52C41A" : "#D9D9D9" }}
                    />
                    <span style={{ color: "#4096FF", fontSize: 16 }}>{a.icon}</span>
                    <div className="flex-1 min-w-0">
                      <Text strong className="text-sm">{a.name}</Text>
                      <Text type="secondary" className="text-xs block truncate">{a.task}</Text>
                    </div>
                    <Tag color={a.status === "active" ? "green" : "default"}>{a.status}</Tag>
                  </div>
                  {i < AGENTS.length - 1 && (
                    <div className="flex justify-center py-1">
                      <div style={{ width: 2, height: 16, background: "#E8E8E8" }} />
                    </div>
                  )}
                </div>
              ))}
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
