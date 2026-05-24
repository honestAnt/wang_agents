"use client";

import { useState } from "react";
import { Button, Card } from "@enterprise-ai/ui";
import { useRouter } from "next/navigation";

export default function NewAgentPage() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [systemPrompt, setSystemPrompt] = useState("");
  const [model, setModel] = useState("gpt-4.1");

  return (
    <div className="p-8 max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">Create Agent</h1>
      <Card>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-1">Name</label>
            <input type="text" value={name} onChange={(e) => setName(e.target.value)}
              className="w-full border rounded px-3 py-2" />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">System Prompt</label>
            <textarea rows={6} value={systemPrompt} onChange={(e) => setSystemPrompt(e.target.value)}
              className="w-full border rounded px-3 py-2 font-mono text-sm" />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Model</label>
            <select value={model} onChange={(e) => setModel(e.target.value)}
              className="w-full border rounded px-3 py-2">
              <option value="gpt-4.1">GPT-4.1</option>
              <option value="claude-sonnet-4-6">Claude Sonnet 4.6</option>
              <option value="deepseek-v3">DeepSeek V3</option>
            </select>
          </div>
          <div className="flex gap-2">
            <Button onClick={() => router.back()} variant="secondary">Cancel</Button>
            <Button onClick={() => router.push("/")}>Save Draft</Button>
          </div>
        </div>
      </Card>
    </div>
  );
}
