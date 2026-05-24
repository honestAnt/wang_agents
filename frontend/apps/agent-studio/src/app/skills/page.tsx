"use client";

import { useState } from "react";
import { Card, Button } from "@enterprise-ai/ui";

const MOCK_SKILLS = [
  { name: "data_analysis", category: "analysis", status: "published", downloads: 1234, rating: 4.5 },
  { name: "customer_service", category: "customer_service", status: "published", downloads: 5678, rating: 4.8 },
  { name: "legal_review", category: "legal", status: "test", downloads: 345, rating: 4.2 },
];

export default function SkillsPage() {
  const [category, setCategory] = useState("all");

  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Skills Marketplace</h1>
        <Button>Create Skill</Button>
      </div>

      <div className="flex gap-2 mb-6">
        {["all", "analysis", "customer_service", "legal", "engineering"].map((c) => (
          <button key={c} onClick={() => setCategory(c)}
            className={`px-3 py-1 rounded text-sm ${category === c ? "bg-blue-600 text-white" : "bg-gray-200"}`}>
            {c}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-3 gap-4">
        {MOCK_SKILLS.map((s) => (
          <Card key={s.name} title={s.name}>
            <p className="text-sm text-gray-500">Category: {s.category}</p>
            <div className="flex items-center gap-2 mt-2">
              <span className="text-yellow-500">{"★".repeat(Math.floor(s.rating))}</span>
              <span className="text-sm text-gray-400">{s.rating}</span>
              <span className="text-sm text-gray-400 ml-auto">{s.downloads} downloads</span>
            </div>
            <Button variant="secondary" className="w-full mt-3 text-sm">Install</Button>
          </Card>
        ))}
      </div>
    </div>
  );
}
