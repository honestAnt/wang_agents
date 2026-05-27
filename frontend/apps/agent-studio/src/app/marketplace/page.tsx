"use client";

import { useState, useCallback } from "react";
import { Card, Button, Spinner, notification } from "@enterprise-ai/ui";

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

function StarRating({ rating }: { rating: number }) {
  const stars = [];
  for (let i = 1; i <= 5; i++) {
    stars.push(
      <span key={i} className={i <= Math.floor(rating) ? "text-yellow-500" : "text-gray-300"}>★</span>
    );
  }
  return <span className="flex items-center gap-0.5">{stars}</span>;
}

const MARKETPLACE_API = process.env.NEXT_PUBLIC_MARKETPLACE_API_URL || "/api/marketplace";

export default function MarketplacePage() {
  const [category, setCategory] = useState("all");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState<string | null>(null);

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
        notification.error({ message: (error as any).message || "Failed to install agent" });
        return;
      }

      notification.success({ message: "Agent installed successfully" });
    } catch (err) {
      notification.error({ message: "Network error — please try again" });
    } finally {
      setLoading(null);
    }
  }, []);

  const renderAgentCard = (agent: MarketplaceAgent) => (
    <Card key={agent.id} title={agent.tags.split(",")[0]} className="hover:shadow-md">
      <p className="text-sm text-gray-500 mb-1">Category: {agent.category}</p>
      <div className="flex flex-wrap gap-1 mb-2">
        {agent.tags.split(",").map((tag) => (
          <span key={tag} className="text-xs bg-blue-50 text-blue-700 px-2 py-0.5 rounded">{tag.trim()}</span>
        ))}
      </div>
      <div className="flex items-center gap-2 mb-3">
        <StarRating rating={agent.ratingAvg} />
        <span className="text-sm text-gray-400">({agent.ratingCount})</span>
      </div>
      <div className="flex items-center justify-between">
        <span className="text-sm text-gray-400">{agent.installCount.toLocaleString()} installs</span>
        <Button
          variant="secondary"
          size="small"
          onClick={() => handleInstall(agent.id)}
          disabled={loading === agent.id}
        >
          {loading === agent.id ? <Spinner /> : "Install"}
        </Button>
      </div>
    </Card>
  );

  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Agent Marketplace</h1>
        <Button>Publish Agent</Button>
      </div>

      {/* Search */}
      <div className="mb-4">
        <input
          type="text"
          placeholder="Search agents by keyword or category..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      {/* Category filter */}
      <div className="flex gap-2 mb-6 flex-wrap">
        {MOCK_CATEGORIES.map((c) => (
          <button
            key={c}
            onClick={() => setCategory(c)}
            className={`px-3 py-1 rounded text-sm capitalize ${
              category === c ? "bg-blue-600 text-white" : "bg-gray-200 hover:bg-gray-300"
            }`}
          >
            {c.replace("_", " ")}
          </button>
        ))}
      </div>

      {/* Most Popular */}
      <section className="mb-8">
        <h2 className="text-lg font-semibold mb-3">Most Popular</h2>
        <div className="grid grid-cols-3 gap-4">
          {[...MOCK_DATA]
            .sort((a, b) => b.installCount - a.installCount)
            .slice(0, 3)
            .map(renderAgentCard)}
        </div>
      </section>

      {/* All Agents */}
      <h2 className="text-lg font-semibold mb-3">All Agents</h2>
      <div className="grid grid-cols-3 gap-4">
        {filtered.map(renderAgentCard)}
      </div>

      {filtered.length === 0 && (
        <p className="text-center text-gray-400 mt-8">No agents found matching your criteria.</p>
      )}
    </div>
  );
}
