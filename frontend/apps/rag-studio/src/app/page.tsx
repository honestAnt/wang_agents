"use client";

import { useState } from "react";
import { Card, Input, Button, Tag, Typography, Empty, Space, Spin, Row, Col, Statistic, Progress } from "antd";
import { SearchOutlined, FileTextOutlined, StarOutlined, ThunderboltOutlined } from "@ant-design/icons";

const { Title, Text } = Typography;

interface ChunkResult {
  chunk: string;
  score: number;
  doc: string;
  method: string;
}

export default function RagStudioPage() {
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<ChunkResult[]>([]);

  const handleSearch = () => {
    if (!query.trim()) return;
    setLoading(true);
    setTimeout(() => {
      setResults([
        { chunk: "Enterprise AI Platform supports hybrid search with BM25 + vector retrieval for optimal accuracy.", score: 0.95, doc: "product_overview.pdf", method: "vector" },
        { chunk: "RAG retrieval pipeline uses BGE reranker to improve result accuracy by up to 15%.", score: 0.89, doc: "architecture.pdf", method: "rerank" },
        { chunk: "Knowledge bases support multiple chunk strategies: fixed-size, semantic, recursive, and markdown.", score: 0.82, doc: "user_guide.pdf", method: "bm25" },
        { chunk: "Permission-aware RAG injects tenant_id, department, and role filters automatically.", score: 0.76, doc: "security.pdf", method: "vector" },
        { chunk: "Embedding config supports multiple models: BGE, OpenAI, Cohere, with configurable dimensions.", score: 0.71, doc: "api_reference.pdf", method: "bm25" },
      ]);
      setLoading(false);
    }, 800);
  };

  const methodColors: Record<string, string> = { vector: "blue", bm25: "purple", rerank: "green" };

  return (
    <div className="p-6">
      <div className="mb-4">
        <Title level={3} style={{ margin: 0 }}>RAG Studio — Knowledge Base Debug</Title>
        <Text type="secondary">Search and debug knowledge base retrieval results</Text>
      </div>

      <Card className="mb-6">
        <Space.Compact style={{ width: "100%" }}>
          <Input
            size="large"
            prefix={<SearchOutlined />}
            placeholder="Enter search query to test retrieval..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            allowClear
          />
          <Button type="primary" size="large" icon={<SearchOutlined />} onClick={handleSearch} loading={loading}>
            Search
          </Button>
        </Space.Compact>
      </Card>

      {results.length > 0 && (
        <Row gutter={[16, 16]} className="mb-6">
          <Col xs={24} sm={8}>
            <Card><Statistic title="Results" value={results.length} prefix={<FileTextOutlined />} styles={{ value: { color: "#4096FF" } }} /></Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card><Statistic title="Top Score" value={results[0].score} prefix={<StarOutlined />} precision={3} styles={{ value: { color: "#4096FF" } }} /></Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card><Statistic title="Avg Score" value={results.reduce((s, r) => s + r.score, 0) / results.length} prefix={<ThunderboltOutlined />} precision={3} styles={{ value: { color: "#4096FF" } }} /></Card>
          </Col>
        </Row>
      )}

      {loading ? (
        <div className="text-center py-12"><Spin size="large" /></div>
      ) : results.length > 0 ? (
        <div className="space-y-3">
          {results.map((r, i) => (
            <Card key={i} hoverable size="small">
              <div className="flex gap-4">
                <div className="flex-shrink-0 text-center" style={{ width: 60 }}>
                  <Progress type="circle" percent={Math.round(r.score * 100)} size={56}
                    strokeColor={{ "0%": "#4096FF", "100%": "#1677FF" }} format={(p) => `${p}%`} />
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <Text strong>Chunk {i + 1}</Text>
                    <Tag color={methodColors[r.method]}>{r.method.toUpperCase()}</Tag>
                    <Text type="secondary" className="text-xs">Source: {r.doc}</Text>
                  </div>
                  <Text className="text-sm leading-relaxed">{r.chunk}</Text>
                </div>
              </div>
            </Card>
          ))}
        </div>
      ) : query ? (
        <Empty description="No results found. Try a different query." />
      ) : (
        <Empty description="Enter a query above to test knowledge base retrieval." />
      )}
    </div>
  );
}
