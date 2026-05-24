"use client";

import { useState, useRef, useEffect } from "react";
import { useAuth } from "@enterprise-ai/auth";
import { apiClient } from "@enterprise-ai/api-client";
import { Button } from "@enterprise-ai/ui";

interface Message { role: "user" | "assistant"; content: string; }

export default function ChatPage() {
  const { user } = useAuth();
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [streaming, setStreaming] = useState(false);
  const [selectedModel, setSelectedModel] = useState("gpt-4.1");
  const messagesEnd = useRef<HTMLDivElement>(null);

  useEffect(() => { messagesEnd.current?.scrollIntoView({ behavior: "smooth" }); }, [messages]);

  const handleSend = async () => {
    if (!input.trim()) return;
    const userMsg: Message = { role: "user", content: input };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setStreaming(true);

    try {
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8000"}/api/v1/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-Tenant-Id": user?.tenantId ?? "" },
        body: JSON.stringify({ message: input, model: selectedModel }),
      });

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();
      let assistantContent = "";

      while (reader) {
        const { done, value } = await reader.read();
        if (done) break;
        const text = decoder.decode(value);
        try {
          const data = JSON.parse(text);
          if (data.event === "delta") {
            const delta = JSON.parse(data.data);
            assistantContent += delta.content;
            setMessages((prev) => {
              const updated = [...prev];
              const last = updated[updated.length - 1];
              if (last?.role === "assistant") {
                updated[updated.length - 1] = { ...last, content: assistantContent };
              } else {
                updated.push({ role: "assistant", content: assistantContent });
              }
              return updated;
            });
          }
        } catch { /* SSE chunk */ }
      }
    } finally {
      setStreaming(false);
    }
  };

  return (
    <main className="flex h-screen">
      <aside className="w-64 bg-gray-900 text-white p-4 flex flex-col">
        <h1 className="text-xl font-bold mb-4">Enterprise AI</h1>
        <select value={selectedModel} onChange={(e) => setSelectedModel(e.target.value)}
          className="bg-gray-800 text-white rounded px-2 py-1 mb-4 text-sm">
          <option value="gpt-4.1">GPT-4.1</option>
          <option value="claude-sonnet-4-6">Claude Sonnet 4.6</option>
          <option value="deepseek-v3">DeepSeek V3</option>
          <option value="qwen-max">Qwen Max</option>
        </select>
        <Button variant="secondary" onClick={() => setMessages([])} className="w-full text-sm">New Chat</Button>
      </aside>
      <section className="flex-1 flex flex-col">
        <div className="flex-1 p-6 overflow-y-auto space-y-4">
          {messages.map((m, i) => (
            <div key={i} className={`flex ${m.role === "user" ? "justify-end" : "justify-start"}`}>
              <div className={`max-w-[70%] rounded-lg px-4 py-2 ${m.role === "user" ? "bg-blue-600 text-white" : "bg-gray-200 text-gray-900"}`}>
                {m.content}
              </div>
            </div>
          ))}
          {streaming && <div className="text-gray-400 text-sm">AI is thinking...</div>}
          <div ref={messagesEnd} />
        </div>
        <div className="border-t p-4">
          <div className="flex gap-2">
            <input type="text" value={input} onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSend()}
              placeholder="Type your message..."
              className="flex-1 border rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500" />
            <Button onClick={handleSend} disabled={streaming}>Send</Button>
          </div>
        </div>
      </section>
    </main>
  );
}
