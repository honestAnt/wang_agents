"use client";

import { Card, Button } from "@enterprise-ai/ui";

const MOCK_TOOLS = [
  { name: "weather_api", type: "http", status: "active", calls: 2340, avgLatency: "120ms" },
  { name: "order_lookup", type: "http", status: "active", calls: 15200, avgLatency: "85ms" },
  { name: "excel_parser", type: "sdk", status: "active", calls: 890, avgLatency: "450ms" },
];

export default function ToolsPage() {
  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-6">Tool Registry</h1>
      <Card>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left">
              <th className="py-2">Name</th><th>Type</th><th>Status</th><th>Calls</th><th>Avg Latency</th>
            </tr>
          </thead>
          <tbody>
            {MOCK_TOOLS.map((t) => (
              <tr key={t.name} className="border-b hover:bg-gray-50">
                <td className="py-2 font-medium">{t.name}</td>
                <td><span className="bg-gray-100 px-2 py-0.5 rounded text-xs">{t.type}</span></td>
                <td><span className="bg-green-100 text-green-800 px-2 py-0.5 rounded text-xs">{t.status}</span></td>
                <td>{t.calls.toLocaleString()}</td>
                <td>{t.avgLatency}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
