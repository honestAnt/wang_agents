"use client";

import { useAuth } from "@enterprise-ai/auth";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { Card, Col, Row, Statistic, Table, Select, Typography, Tag } from "antd";
import {
  BarChartOutlined,
  TeamOutlined,
  ThunderboltOutlined,
  DollarOutlined,
} from "@ant-design/icons";

const { Title } = Typography;

const recentSessions = [
  { key: "s1", user: "alice", agent: "Lab Assistant", model: "gpt-4.1", latency: "1240ms", cost: "$0.023", status: "ok" },
  { key: "s2", user: "bob", agent: "Customer Service", model: "claude-sonnet-4-6", latency: "890ms", cost: "$0.015", status: "ok" },
  { key: "s3", user: "carol", agent: "Data Analyst", model: "deepseek-v3", latency: "3400ms", cost: "$0.002", status: "error" },
  { key: "s4", user: "dave", agent: "Lab Assistant", model: "qwen-max", latency: "620ms", cost: "$0.008", status: "ok" },
  { key: "s5", user: "eve", agent: "Customer Service", model: "gpt-4.1", latency: "2100ms", cost: "$0.031", status: "ok" },
];

const columns = [
  { title: "User", dataIndex: "user", key: "user" },
  { title: "Agent", dataIndex: "agent", key: "agent" },
  { title: "Model", dataIndex: "model", key: "model", render: (v: string) => <Tag color="blue">{v}</Tag> },
  { title: "Latency", dataIndex: "latency", key: "latency" },
  { title: "Cost", dataIndex: "cost", key: "cost" },
  {
    title: "Status", dataIndex: "status", key: "status",
    render: (v: string) => <Tag color={v === "ok" ? "green" : "red"}>{v}</Tag>,
  },
];

export default function Dashboard() {
  const { isAuthenticated } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isAuthenticated) router.push("/login");
  }, [isAuthenticated, router]);

  return (
    <div className="p-6">
      <Title level={3} className="mb-6">Dashboard</Title>

      <Row gutter={[16, 16]} className="mb-8">
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Today Requests"
              value={1234}
              prefix={<ThunderboltOutlined />}
              styles={{ value: { color: "#4096FF" } }}
              suffix={<span className="text-green-500 text-sm">↑12%</span>}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Token Usage"
              value={5.2}
              precision={1}
              prefix={<BarChartOutlined />}
              styles={{ value: { color: "#4096FF" } }}
              suffix={<span className="text-green-500 text-sm ml-2">M ↑8%</span>}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Cost Today"
              value={12.45}
              precision={2}
              prefix={<DollarOutlined />}
              styles={{ value: { color: "#4096FF" } }}
              suffix={<span className="text-red-500 text-sm">↓3%</span>}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Active Users"
              value={89}
              prefix={<TeamOutlined />}
              styles={{ value: { color: "#4096FF" } }}
              suffix={<span className="text-green-500 text-sm">↑5%</span>}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} className="mb-6">
        <Col xs={24} lg={14}>
          <Card
            title="Recent Sessions"
            extra={
              <Select
                defaultValue="today"
                size="small"
                style={{ width: 100 }}
                options={[
                  { value: "today", label: "Today" },
                  { value: "week", label: "This Week" },
                  { value: "month", label: "This Month" },
                ]}
              />
            }
          >
            <Table
              dataSource={recentSessions}
              columns={columns}
              pagination={false}
              size="small"
            />
          </Card>
        </Col>
        <Col xs={24} lg={10}>
          <Card title="Model Usage" className="h-full">
            <div className="space-y-3 py-2">
              {[
                { model: "GPT-4.1", pct: 45, color: "#4096FF" },
                { model: "Claude Sonnet 4.6", pct: 30, color: "#69B1FF" },
                { model: "DeepSeek V3", pct: 15, color: "#91CAFF" },
                { model: "Qwen Max", pct: 10, color: "#BAE0FF" },
              ].map(({ model, pct, color }) => (
                <div key={model} className="flex items-center gap-3">
                  <span className="w-32 text-sm text-gray-600 truncate">{model}</span>
                  <div className="flex-1 h-5 bg-gray-100 rounded-full overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all"
                      style={{ width: `${pct}%`, backgroundColor: color }}
                    />
                  </div>
                  <span className="text-sm font-medium w-10 text-right">{pct}%</span>
                </div>
              ))}
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
