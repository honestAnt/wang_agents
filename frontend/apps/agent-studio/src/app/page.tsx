"use client";

import { Card, Button } from "@enterprise-ai/ui";
import Link from "next/link";

export default function AgentStudioPage() {
  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Agent Studio</h1>
        <Link href="/agents/new"><Button>Create Agent</Button></Link>
      </div>
      <div className="grid grid-cols-3 gap-4">
        <Card title="Lab Assistant" className="cursor-pointer hover:shadow-md">
          <p className="text-sm text-gray-500">Research & analysis agent with RAG</p>
          <span className="text-xs bg-green-100 text-green-800 px-2 py-1 rounded mt-2 inline-block">Published</span>
        </Card>
        <Card title="Customer Service Bot" className="cursor-pointer hover:shadow-md">
          <p className="text-sm text-gray-500">FAQ, order lookup, complaint handling</p>
          <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded mt-2 inline-block">Test</span>
        </Card>
        <Card title="Data Analyst" className="cursor-pointer hover:shadow-md">
          <p className="text-sm text-gray-500">Excel/CSV analysis, chart generation</p>
          <span className="text-xs bg-gray-100 text-gray-800 px-2 py-1 rounded mt-2 inline-block">Draft</span>
        </Card>
      </div>
    </div>
  );
}
