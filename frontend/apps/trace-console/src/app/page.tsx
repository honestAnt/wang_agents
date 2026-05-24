"use client";

import { useState } from "react";
import { Card } from "@enterprise-ai/ui";
import { formatCost, formatDate } from "@enterprise-ai/utils";

const MOCK_SESSIONS = [
  { id: "s1", user: "alice", model: "gpt-4.1", cost: 0.023, latency: 1240, status: "ok", time: new Date().toISOString() },
  { id: "s2", user: "bob", model: "claude-sonnet-4-6", cost: 0.015, latency: 890, status: "ok", time: new Date().toISOString() },
  { id: "s3", user: "carol", model: "deepseek-v3", cost: 0.002, latency: 3400, status: "error", time: new Date().toISOString() },
];

export default function TraceConsolePage() {
  const [selectedSession, setSelectedSession] = useState<string | null>(null);

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-6">Trace & Audit Console</h1>
      <div className="grid grid-cols-4 gap-4 mb-6">
        <Card title="Total Sessions"><p className="text-3xl font-bold">2,847</p></Card>
        <Card title="Total Cost"><p className="text-3xl font-bold">$156.32</p></Card>
        <Card title="Avg Latency"><p className="text-3xl font-bold">1.2s</p></Card>
        <Card title="Error Rate"><p className="text-3xl font-bold text-red-600">0.8%</p></Card>
      </div>
      <Card title="Recent Sessions">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left">
              <th className="py-2">Session ID</th>
              <th>User</th>
              <th>Model</th>
              <th>Cost</th>
              <th>Latency</th>
              <th>Status</th>
              <th>Time</th>
            </tr>
          </thead>
          <tbody>
            {MOCK_SESSIONS.map((s) => (
              <tr key={s.id} className="border-b hover:bg-gray-50 cursor-pointer" onClick={() => setSelectedSession(s.id)}>
                <td className="py-2 font-mono text-xs">{s.id}</td>
                <td>{s.user}</td>
                <td>{s.model}</td>
                <td>{formatCost(s.cost)}</td>
                <td>{s.latency}ms</td>
                <td><span className={`px-2 py-0.5 rounded text-xs ${s.status === "ok" ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"}`}>{s.status}</span></td>
                <td className="text-gray-400">{formatDate(s.time)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
