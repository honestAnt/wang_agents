"use client";

import { Card } from "@enterprise-ai/ui";

const AGENTS = [
  { name: "Coordinator", status: "active", task: "Decompose user request into sub-tasks" },
  { name: "SearchAgent", status: "active", task: "Execute RAG retrieval + Tool queries" },
  { name: "AnalysisAgent", status: "active", task: "Data analysis + reasoning" },
  { name: "ReportAgent", status: "idle", task: "Aggregate results + generate report" },
];

export default function MultiAgentPage() {
  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-6">Multi-Agent Debug Console</h1>
      <div className="grid grid-cols-2 gap-4 mb-6">
        <Card title="Task Input">
          <input type="text" placeholder="Enter a complex task..."
            className="w-full border rounded px-3 py-2" />
        </Card>
        <Card title="Execution Flow">
          <div className="space-y-2">
            {AGENTS.map((a, i) => (
              <div key={a.name} className="flex items-center gap-2">
                <div className={`w-3 h-3 rounded-full ${a.status === "active" ? "bg-green-500" : "bg-gray-400"}`} />
                <span className="text-sm font-medium">{a.name}</span>
                <span className="text-xs text-gray-400 ml-auto">{a.task}</span>
                {i < AGENTS.length - 1 && <div className="w-full border-t border-dashed border-gray-300 mx-2" />}
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
