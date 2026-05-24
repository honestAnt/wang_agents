"use client";

import { Card } from "@enterprise-ai/ui";

const MOCK_MODELS = [
  { name: "GPT-4.1", provider: "openai", status: "active", quota: "1M tokens" },
  { name: "Claude Sonnet 4.6", provider: "anthropic", status: "active", quota: "500K tokens" },
  { name: "DeepSeek V3", provider: "deepseek", status: "active", quota: "2M tokens" },
];

export default function ModelsPage() {
  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-6">Model Management</h1>
      <div className="grid grid-cols-3 gap-4">
        {MOCK_MODELS.map((m) => (
          <Card key={m.name} title={m.name}>
            <p className="text-sm text-gray-500">Provider: {m.provider}</p>
            <p className="text-sm text-gray-500">Status: {m.status}</p>
            <p className="text-sm text-gray-500">Quota: {m.quota}</p>
          </Card>
        ))}
      </div>
    </div>
  );
}
