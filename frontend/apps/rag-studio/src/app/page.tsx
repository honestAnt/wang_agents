"use client";

import { useState } from "react";
import { Card, Button } from "@enterprise-ai/ui";

export default function RagStudioPage() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<{ chunk: string; score: number; doc: string }[]>([]);

  const handleSearch = () => {
    setResults([
      { chunk: "Enterprise AI Platform supports hybrid search with BM25 + vector.", score: 0.95, doc: "product_overview.pdf" },
      { chunk: "RAG retrieval uses BGE reranker for improved accuracy.", score: 0.89, doc: "architecture.pdf" },
      { chunk: "Knowledge bases support multiple chunk strategies.", score: 0.82, doc: "user_guide.pdf" },
    ]);
  };

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-6">RAG Studio — Knowledge Base Debug</h1>
      <div className="flex gap-2 mb-6">
        <input type="text" value={query} onChange={(e) => setQuery(e.target.value)}
          placeholder="Enter search query..."
          className="flex-1 border rounded-lg px-4 py-2" />
        <Button onClick={handleSearch}>Search</Button>
      </div>
      <div className="space-y-4">
        {results.map((r, i) => (
          <Card key={i} title={`Chunk ${i + 1} (score: ${r.score.toFixed(3)})`}>
            <p>{r.chunk}</p>
            <p className="text-xs text-gray-400 mt-2">Source: {r.doc}</p>
          </Card>
        ))}
      </div>
    </div>
  );
}
