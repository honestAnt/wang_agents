"use client";

import { useState, useEffect } from "react";
import { Card } from "@enterprise-ai/ui";

interface MetricsCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  trend?: "up" | "down" | "flat";
  trendValue?: string;
}

function MetricsCard({ title, value, subtitle, trend, trendValue }: MetricsCardProps) {
  return (
    <Card className="p-4">
      <p className="text-sm text-gray-500 mb-1">{title}</p>
      <p className="text-2xl font-bold mb-1">{value}</p>
      {subtitle && <p className="text-xs text-gray-400">{subtitle}</p>}
      {trend && (
        <span className={`text-xs ${trend === "up" ? "text-green-600" : trend === "down" ? "text-red-600" : "text-gray-500"}`}>
          {trend === "up" ? "↑" : trend === "down" ? "↓" : "→"} {trendValue}
        </span>
      )}
    </Card>
  );
}

interface TrendChartProps {
  title: string;
  data: { date: string; value: number }[];
}

function TrendChart({ title, data }: TrendChartProps) {
  const max = Math.max(...data.map((d) => d.value), 1);
  return (
    <Card className="p-4">
      <h3 className="text-sm font-medium mb-3">{title}</h3>
      <div className="flex items-end gap-1 h-32">
        {data.map((point, i) => (
          <div key={i} className="flex-1 flex flex-col items-center">
            <div
              className="w-full bg-blue-500 rounded-t"
              style={{ height: `${(point.value / max) * 100}%` }}
            />
          </div>
        ))}
      </div>
      <div className="flex justify-between mt-2 text-xs text-gray-400">
        <span>{data[0]?.date}</span>
        <span>{data[data.length - 1]?.date}</span>
      </div>
    </Card>
  );
}

export default function AnalyticsPage() {
  const [period, setPeriod] = useState("week");

  // Mock data for the dashboard
  const summaryCards = [
    { title: "Active Users", value: "1,450", trend: "up" as const, trendValue: "12%", subtitle: "Today" },
    { title: "Total Sessions", value: "8,920", trend: "up" as const, trendValue: "8%", subtitle: "This week" },
    { title: "Total Tokens", value: "45.6M", trend: "up" as const, trendValue: "15%", subtitle: "This week" },
    { title: "Total Cost", value: "$234.50", trend: "down" as const, trendValue: "5%", subtitle: "Saved $12.30 via routing" },
    { title: "Avg Latency", value: "320ms", trend: "down" as const, trendValue: "8%", subtitle: "p95: 1,200ms" },
    { title: "Satisfaction", value: "4.2/5", trend: "flat" as const, trendValue: "0%", subtitle: "Based on 234 reviews" },
  ];

  const trendData = [
    { date: "Mon", value: 120 }, { date: "Tue", value: 145 },
    { date: "Wed", value: 160 }, { date: "Thu", value: 135 },
    { date: "Fri", value: 170 }, { date: "Sat", value: 98 },
    { date: "Sun", value: 85 },
  ];

  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">AI Operations Analytics</h1>
        <div className="flex gap-2">
          {["today", "week", "month", "quarter"].map((p) => (
            <button
              key={p}
              onClick={() => setPeriod(p)}
              className={`px-3 py-1 rounded text-sm capitalize ${
                period === p ? "bg-blue-600 text-white" : "bg-gray-200 hover:bg-gray-300"
              }`}
            >
              {p}
            </button>
          ))}
        </div>
      </div>

      {/* Metrics Cards */}
      <div className="grid grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
        {summaryCards.map((card) => (
          <MetricsCard key={card.title} {...card} />
        ))}
      </div>

      {/* Trend Charts */}
      <div className="grid grid-cols-2 gap-4 mb-8">
        <TrendChart title="Requests Trend" data={trendData} />
        <TrendChart title="Token Usage Trend" data={trendData.map((d) => ({ ...d, value: d.value * 1000 }))} />
      </div>

      {/* Cost & Quality Panels */}
      <div className="grid grid-cols-3 gap-4 mb-8">
        {/* Cost by Model */}
        <Card title="Cost by Model" className="p-4">
          <div className="space-y-2">
            {[
              { name: "GPT-4.1", cost: 156.80, pct: 67 },
              { name: "Claude Sonnet", cost: 45.20, pct: 19 },
              { name: "DeepSeek V3", cost: 12.30, pct: 5 },
              { name: "Others", cost: 20.20, pct: 9 },
            ].map((item) => (
              <div key={item.name} className="flex items-center gap-2">
                <span className="text-sm w-28">{item.name}</span>
                <div className="flex-1 bg-gray-200 rounded-full h-3">
                  <div className="bg-blue-600 h-3 rounded-full" style={{ width: `${item.pct}%` }} />
                </div>
                <span className="text-sm text-gray-500 w-16 text-right">${item.cost}</span>
              </div>
            ))}
          </div>
          <p className="text-xs text-green-600 mt-3">Routing saved $57.90 this period</p>
        </Card>

        {/* Quality Metrics */}
        <Card title="Quality Metrics" className="p-4">
          <div className="space-y-3">
            {[
              { label: "Tool Success Rate", value: "94%", color: "bg-green-500" },
              { label: "RAG Hit Rate", value: "82%", color: "bg-blue-500" },
              { label: "Hallucination Rate", value: "3%", color: "bg-yellow-500" },
              { label: "Error Rate", value: "2%", color: "bg-red-500" },
            ].map((metric) => (
              <div key={metric.label}>
                <div className="flex justify-between text-sm mb-1">
                  <span>{metric.label}</span>
                  <span>{metric.value}</span>
                </div>
                <div className="bg-gray-200 rounded-full h-2">
                  <div className={`${metric.color} h-2 rounded-full`} style={{ width: metric.value }} />
                </div>
              </div>
            ))}
          </div>
        </Card>

        {/* Top Rankings */}
        <Card title="Top Models" className="p-4">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-gray-500 border-b">
                <th className="text-left py-1">Model</th>
                <th className="text-right py-1">Calls</th>
                <th className="text-right py-1">Cost</th>
              </tr>
            </thead>
            <tbody>
              {[
                { name: "gpt-4.1", calls: 45200, cost: "$156.80" },
                { name: "claude-sonnet-4-6", calls: 32100, cost: "$45.20" },
                { name: "deepseek-v3", calls: 28700, cost: "$12.30" },
                { name: "haiku-4-5", calls: 15600, cost: "$8.40" },
                { name: "qwen-max", calls: 8900, cost: "$4.50" },
              ].map((m) => (
                <tr key={m.name} className="border-b border-gray-100">
                  <td className="py-1">{m.name}</td>
                  <td className="text-right">{m.calls.toLocaleString()}</td>
                  <td className="text-right">{m.cost}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      </div>

      {/* Report Section */}
      <Card title="Weekly Report Highlights" className="p-4 mb-8">
        <ul className="list-disc list-inside space-y-1 text-sm text-gray-700">
          <li>Token usage increased 15% week-over-week — driven by new engineering team onboarding</li>
          <li>claude-haiku-4-5-20251001 usage grew 30%, reducing overall costs by 12%</li>
          <li>RAG hit rate improved from 78% to 82% after chunk strategy optimization</li>
          <li>No critical incidents — tool failure rate at all-time low of 2%</li>
          <li>Top departments by AI usage: Engineering (38%), Product (24%), Marketing (19%)</li>
        </ul>
        <div className="flex gap-2 mt-4">
          <button className="px-3 py-1 bg-blue-600 text-white rounded text-sm">Export PDF</button>
          <button className="px-3 py-1 bg-gray-200 rounded text-sm">Export CSV</button>
          <button className="px-3 py-1 bg-gray-200 rounded text-sm">Send to Lark</button>
        </div>
      </Card>
    </div>
  );
}
