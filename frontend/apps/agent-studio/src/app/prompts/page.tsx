"use client";

import { useState } from "react";
import { Card, Button } from "@enterprise-ai/ui";

export default function PromptCenterPage() {
  const [selectedPrompt, setSelectedPrompt] = useState<string | null>(null);

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-6">Prompt Center</h1>
      <div className="grid grid-cols-2 gap-6">
        <div>
          <Card title="Prompt Templates">
            <div className="space-y-2">
              {["System Prompt v2", "Code Review Prompt", "Data Analysis Prompt"].map((p) => (
                <div key={p} onClick={() => setSelectedPrompt(p)}
                  className={`p-3 rounded cursor-pointer ${selectedPrompt === p ? "bg-blue-50 border border-blue-200" : "bg-gray-50 hover:bg-gray-100"}`}>
                  <p className="font-medium text-sm">{p}</p>
                  <p className="text-xs text-gray-400">v2.1 · 1.2K tokens</p>
                </div>
              ))}
            </div>
          </Card>
        </div>
        <div>
          {selectedPrompt ? (
            <Card title={`Edit: ${selectedPrompt}`}>
              <textarea className="w-full border rounded p-3 font-mono text-sm h-64"
                defaultValue="You are an enterprise AI assistant. Use the following context to answer questions accurately." />
              <div className="flex gap-2 mt-3">
                <Button>Save</Button>
                <Button variant="secondary">Test with Models</Button>
              </div>
            </Card>
          ) : (
            <Card title="Prompt Editor"><p className="text-gray-400">Select a prompt to edit</p></Card>
          )}
        </div>
      </div>
    </div>
  );
}
