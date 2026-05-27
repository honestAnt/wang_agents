"use client";

import { useState, useCallback } from "react";
import { Card, Button, Input, Tag, Rate, Typography, Row, Col, Space, Spin, App, Segmented } from "antd";
import { SearchOutlined, DownloadOutlined, PlusOutlined } from "@ant-design/icons";

const { Title, Text } = Typography;

interface MarketplaceAgent {
  id: string;
  agentId: string;
  publisherId: string;
  category: string;
  tags: string;
  readme?: string;
  version: number;
  installCount: number;
  ratingAvg: number;
  ratingCount: number;
  status: string;
}

const MOCK_CATEGORIES = ["all", "general", "analytics", "customer_service", "legal", "engineering", "finance", "hr"];
const MOCK_DATA: MarketplaceAgent[] = [
  { id: "mp1", agentId: "a1", publisherId: "u1", category: "analytics", tags: "data,reports", version: 2, installCount: 1250, ratingAvg: 4.5, ratingCount: 89, status: "published" },
  { id: "mp2", agentId: "a2", publisherId: "u2", category: "customer_service", tags: "faq,orders", version: 1, installCount: 3400, ratingAvg: 4.8, ratingCount: 210, status: "published" },
  { id: "mp3", agentId: "a3", publisherId: "u3", category: "legal", tags: "contract,review", version: 3, installCount: 890, ratingAvg: 4.2, ratingCount: 45, status: "published" },
  { id: "mp4", agentId: "a4", publisherId: "u4", category: "engineering", tags: "code,review", version: 2, installCount: 2100, ratingAvg: 4.6, ratingCount: 132, status: "published" },
  { id: "mp5", agentId: "a5", publisherId: "u5", category: "finance", tags: "reports,forecast", version: 1, installCount: 670, ratingAvg: 4.0, ratingCount: 34, status: "published" },
  { id: "mp6", agentId: "a6", publisherId: "u6", category: "hr", tags: "onboarding,faq", version: 2, installCount: 1560, ratingAvg: 4.3, ratingCount: 78, status: "published" },
];

const MARKETPLACE_API = process.env.NEXT_PUBLIC_MARKETPLACE_API_URL || "/api/marketplace";

export default function MarketplacePage() {
  const [category, setCategory] = useState("all");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState<string | null>(null);
  const { message } = App.useApp();

  const filtered = MOCK_DATA.filter((agent) => {
    if (category !== "all" && agent.category !== category) return false;
    if (search && !agent.tags.toLowerCase().includes(search.toLowerCase())
              && !agent.category.toLowerCase().includes(search.toLowerCase())) return false;
    return true;
  });

  const handleInstall = useCallback(async (id: string) => {
    setLoading(id);
    try {
      const response = await fetch(`${MARKETPLACE_API}/${id}/install`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
      });

      if (!response.ok) {
        const error = await response.json().catch(() => ({ message: "Installation failed" }));
        message.error((error as Record<string, unknown>).message as string || "Failed to install agent");
        return;
      }

      message.success("Agent installed successfully");
    } catch {
      message.error("Network error — please try again");
    } finally {
      setLoading(null);
    }
  }, [message]);

  const renderAgentCard = (agent: MarketplaceAgent) => (
    <Col xs={24} sm={12} lg={8} key={agent.id}>
      <Card
        hoverable
        title={agent.tags.split(",")[0]}
        extra={<Tag color="blue">v{agent.version}</Tag>}
      >
        <Text type="secondary" className="text-sm">Category: {agent.category}</Text>
        <div className="flex flex-wrap gap-1 my-2">
          {agent.tags.split(",").map((tag) => (
            <Tag key={tag} color="blue">{tag.trim()}</Tag>
          ))}
        </div>
        <div className="flex items-center gap-2 mb-3">
          <Rate disabled allowHalf value={agent.ratingAvg} style={{ fontSize: 14 }} />
          <Text type="secondary" className="text-xs">({agent.ratingCount})</Text>
        </div>
        <div className="flex items-center justify-between">
          <Text type="secondary" className="text-xs">{agent.installCount.toLocaleString()} installs</Text>
          <Button
            type="default"
            size="small"
            icon={loading === agent.id ? <Spin size="small" /> : <DownloadOutlined />}
            onClick={() => handleInstall(agent.id)}
            disabled={loading === agent.id}
          >
            {loading === agent.id ? "Installing..." : "Install"}
          </Button>
        </div>
      </Card>
    </Col>
  );

  return (
    <div className="p-6">
      <div className="flex flex-wrap justify-between items-center mb-6 gap-3">
        <Title level={3} style={{ margin: 0 }}>Agent Marketplace</Title>
        <Button type="primary" icon={<PlusOutlined />}>Publish Agent</Button>
      </div>

      <Space direction="vertical" size="middle" style={{ width: "100%" }} className="mb-6">
        <Input
          prefix={<SearchOutlined />}
          placeholder="Search agents by keyword or category..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          size="large"
          allowClear
        />

        <Segmented
          value={category}
          onChange={(v) => setCategory(v as string)}
          options={MOCK_CATEGORIES.map((c) => ({
            value: c,
            label: c.replace("_", " ").replace(/\b\w/g, (l) => l.toUpperCase()),
          }))}
        />
      </Space>

      <section className="mb-8">
        <Title level={4} className="mb-4">Most Popular</Title>
        <Row gutter={[16, 16]}>
          {[...MOCK_DATA]
            .sort((a, b) => b.installCount - a.installCount)
            .slice(0, 3)
            .map(renderAgentCard)}
        </Row>
      </section>

      <Title level={4} className="mb-4">All Agents</Title>
      <Row gutter={[16, 16]}>
        {filtered.map(renderAgentCard)}
      </Row>

      {filtered.length === 0 && (
        <div className="text-center py-12">
          <Text type="secondary">No agents found matching your criteria.</Text>
        </div>
      )}
    </div>
  );
}
