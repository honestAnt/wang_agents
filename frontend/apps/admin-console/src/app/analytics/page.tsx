"use client";

import { useState } from "react";
import { Card, Row, Col, Statistic, Table, Select, Tag, Typography, Segmented } from "antd";
import {
  TeamOutlined, ThunderboltOutlined, DollarOutlined, ClockCircleOutlined,
  SmileOutlined, CheckCircleOutlined,
} from "@ant-design/icons";

const { Title } = Typography;

export default function AnalyticsPage() {
  const [period, setPeriod] = useState("week");

  const summaryCards = [
    { title: "Active Users", value: 1450, prefix: <TeamOutlined />, trend: "+12%", trendUp: true },
    { title: "Total Sessions", value: 8920, prefix: <ThunderboltOutlined />, trend: "+8%", trendUp: true },
    { title: "Total Tokens", value: "45.6M", prefix: <ThunderboltOutlined />, trend: "+15%", trendUp: true },
    { title: "Total Cost", value: "$234.50", prefix: <DollarOutlined />, trend: "-5%", trendUp: false },
    { title: "Avg Latency", value: "320ms", prefix: <ClockCircleOutlined />, trend: "-8%", trendUp: false },
    { title: "Satisfaction", value: "4.2/5", prefix: <SmileOutlined />, trend: "0%", trendUp: true },
  ];

  const costByModel = [
    { key: "1", model: "GPT-4.1", calls: 45200, cost: "$156.80", pct: 67 },
    { key: "2", model: "Claude Sonnet", calls: 32100, cost: "$45.20", pct: 19 },
    { key: "3", model: "DeepSeek V3", calls: 28700, cost: "$12.30", pct: 5 },
    { key: "4", model: "Others", calls: 24500, cost: "$20.20", pct: 9 },
  ];

  const qualityMetrics = [
    { label: "Tool Success Rate", value: "94%", color: "#52C41A" },
    { label: "RAG Hit Rate", value: "82%", color: "#4096FF" },
    { label: "Hallucination Rate", value: "3%", color: "#FAAD14" },
    { label: "Error Rate", value: "2%", color: "#FF4D4F" },
  ];

  return (
    <div className="p-6">
      <div className="flex flex-wrap justify-between items-center mb-4 gap-3">
        <Title level={3} style={{ margin: 0 }}>AI Operations Analytics</Title>
        <Segmented
          value={period}
          onChange={(v) => setPeriod(v as string)}
          options={[
            { value: "today", label: "Today" },
            { value: "week", label: "Week" },
            { value: "month", label: "Month" },
            { value: "quarter", label: "Quarter" },
          ]}
        />
      </div>

      <Row gutter={[16, 16]} className="mb-6">
        {summaryCards.map((card) => (
          <Col xs={24} sm={12} lg={8} xl={4} key={card.title}>
            <Card>
              <Statistic
                title={card.title}
                value={card.value}
                prefix={card.prefix}
                styles={{ value: { color: "#4096FF", fontSize: 20 } }}
                suffix={
                  <span className={`text-xs ml-2 ${card.trendUp ? "text-green-500" : "text-red-500"}`}>
                    {card.trend}
                  </span>
                }
              />
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]} className="mb-6">
        <Col xs={24} lg={14}>
          <Card title="Cost by Model">
            <Table
              dataSource={costByModel}
              columns={[
                { title: "Model", dataIndex: "model", key: "model", render: (v: string) => <Tag color="blue">{v}</Tag> },
                { title: "Calls", dataIndex: "calls", key: "calls", render: (v: number) => v.toLocaleString() },
                { title: "Cost", dataIndex: "cost", key: "cost" },
                {
                  title: "Share", dataIndex: "pct", key: "pct",
                  render: (v: number) => (
                    <div className="flex items-center gap-2">
                      <div className="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden">
                        <div className="h-full rounded-full" style={{ width: `${v}%`, backgroundColor: "#4096FF" }} />
                      </div>
                      <span className="text-xs w-8">{v}%</span>
                    </div>
                  ),
                },
              ]}
              pagination={false}
              size="small"
            />
            <p className="text-xs text-green-600 mt-3">
              <CheckCircleOutlined className="mr-1" />Routing saved $57.90 this period
            </p>
          </Card>
        </Col>
        <Col xs={24} lg={10}>
          <Card title="Quality Metrics">
            <div className="space-y-4 py-2">
              {qualityMetrics.map((m) => (
                <div key={m.label}>
                  <div className="flex justify-between text-sm mb-1">
                    <span>{m.label}</span>
                    <span className="font-medium">{m.value}</span>
                  </div>
                  <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                    <div className="h-full rounded-full" style={{
                      width: m.value,
                      backgroundColor: m.color,
                    }} />
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </Col>
      </Row>

      <Card title="Weekly Report Highlights">
        <ul className="list-disc list-inside space-y-1 text-sm text-gray-600">
          <li>Token usage increased 15% week-over-week — driven by new engineering team onboarding</li>
          <li>Claude Haiku usage grew 30%, reducing overall costs by 12%</li>
          <li>RAG hit rate improved from 78% to 82% after chunk strategy optimization</li>
          <li>No critical incidents — tool failure rate at all-time low of 2%</li>
          <li>Top departments by AI usage: Engineering (38%), Product (24%), Marketing (19%)</li>
        </ul>
      </Card>
    </div>
  );
}
