"use client";

import { useState } from "react";
import { Card, Button, Input, Tag, Rate, Typography, Row, Col, Segmented, Space } from "antd";
import { PlusOutlined, SearchOutlined, DownloadOutlined } from "@ant-design/icons";

const { Title, Text } = Typography;

const MOCK_SKILLS = [
  { key: "1", name: "data_analysis", category: "analysis", status: "published", downloads: 1234, rating: 4.5 },
  { key: "2", name: "customer_service", category: "customer_service", status: "published", downloads: 5678, rating: 4.8 },
  { key: "3", name: "legal_review", category: "legal", status: "test", downloads: 345, rating: 4.2 },
  { key: "4", name: "code_review", category: "engineering", status: "published", downloads: 2340, rating: 4.6 },
];

const categories = ["all", "analysis", "customer_service", "legal", "engineering"];

export default function SkillsPage() {
  const [category, setCategory] = useState("all");

  const filtered = category === "all"
    ? MOCK_SKILLS
    : MOCK_SKILLS.filter((s) => s.category === category);

  return (
    <div className="p-6">
      <div className="flex flex-wrap justify-between items-center mb-6 gap-3">
        <Title level={3} style={{ margin: 0 }}>Skills Marketplace</Title>
        <Space>
          <Input prefix={<SearchOutlined />} placeholder="Search skills..." style={{ width: 220 }} allowClear />
          <Button type="primary" icon={<PlusOutlined />}>Create Skill</Button>
        </Space>
      </div>

      <Segmented
        value={category}
        onChange={(v) => setCategory(v as string)}
        options={categories.map((c) => ({
          value: c,
          label: c === "all" ? "All" : c.replace("_", " ").replace(/\b\w/g, (l) => l.toUpperCase()),
        }))}
        className="mb-6"
      />

      <Row gutter={[16, 16]}>
        {filtered.map((s) => (
          <Col xs={24} sm={12} lg={6} key={s.key}>
            <Card
              hoverable
              title={s.name}
              extra={<Tag color={s.status === "published" ? "green" : "blue"}>{s.status}</Tag>}
            >
              <Text type="secondary" className="text-sm block mb-2">Category: {s.category}</Text>
              <div className="flex items-center gap-2 mb-3">
                <Rate disabled allowHalf value={s.rating} style={{ fontSize: 13 }} />
                <Text type="secondary" className="text-xs">({s.rating})</Text>
              </div>
              <Text type="secondary" className="text-xs block mb-3">
                {s.downloads.toLocaleString()} downloads
              </Text>
              <Button
                type="default"
                icon={<DownloadOutlined />}
                block
                size="small"
              >
                Install
              </Button>
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  );
}
