"use client";

import { useState, useRef, useEffect } from "react";
import { useAuth } from "@enterprise-ai/auth";
import { apiClient } from "@enterprise-ai/api-client";
import { Button, Select, Input, Avatar, Spin, Typography, Space } from "antd";
import { SendOutlined, PlusOutlined, RobotOutlined, UserOutlined } from "@ant-design/icons";

const { Text, Title } = Typography;

interface Message { role: "user" | "assistant"; content: string; }

interface ModelOption { name: string; provider: string; inputPrice: number; outputPrice: number }

const FALLBACK_MODELS: ModelOption[] = [
  { name: "gpt-4.1", provider: "openai", inputPrice: 0.003, outputPrice: 0.006 },
  { name: "claude-sonnet-4-6", provider: "anthropic", inputPrice: 0.003, outputPrice: 0.015 },
  { name: "deepseek-v3", provider: "openai_compatible", inputPrice: 0.00027, outputPrice: 0.0011 },
  { name: "qwen-max", provider: "openai_compatible", inputPrice: 0.002, outputPrice: 0.006 },
];

export default function ChatPage() {
  const { user } = useAuth();
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [streaming, setStreaming] = useState(false);
  const [selectedModel, setSelectedModel] = useState("deepseek-v3");
  const [models, setModels] = useState<ModelOption[]>(FALLBACK_MODELS);
  const messagesEnd = useRef<HTMLDivElement>(null);

  useEffect(() => { messagesEnd.current?.scrollIntoView({ behavior: "smooth" }); }, [messages]);

  useEffect(() => {
    fetch(`${process.env.NEXT_PUBLIC_AGENT_URL ?? "http://localhost:8000"}/api/v1/models`)
      .then(res => res.json())
      .then(data => { if (data?.models?.length) setModels(data.models); })
      .catch(() => {});
  }, []);

  const handleSend = async () => {
    if (!input.trim()) return;
    const userMsg: Message = { role: "user", content: input };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setStreaming(true);

    try {
      const token = localStorage.getItem("access_token");
      const response = await fetch(`${process.env.NEXT_PUBLIC_AGENT_URL ?? "http://localhost:8000"}/api/v1/chat`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Tenant-Id": user?.tenantId ?? "",
          "Authorization": `Bearer ${token ?? ""}`,
        },
        body: JSON.stringify({ message: input, model: selectedModel }),
      });

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();
      let assistantContent = "";
      let buffer = "";

      while (reader) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const normalized = buffer.replace(/\r\n/g, "\n");
        const frames = normalized.split("\n\n");
        buffer = frames.pop() ?? "";
        for (const frame of frames) {
          if (!frame.trim()) continue;
          const lines = frame.split("\n");
          let eventType = "";
          let eventData = "";
          for (const line of lines) {
            if (line.startsWith("event: ")) eventType = line.slice(7).trim();
            else if (line.startsWith("data: ")) eventData = line.slice(6);
          }
          if (eventType === "delta" && eventData) {
            try {
              const delta = JSON.parse(eventData);
              assistantContent += delta.content ?? "";
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
            } catch { /* skip */ }
          }
        }
      }
    } finally {
      setStreaming(false);
    }
  };

  const modelOptions = models.map(m => ({ value: m.name, label: m.name }));

  return (
    <main className="flex h-screen">
      <aside className="w-64 flex flex-col" style={{ background: "#001529" }}>
        <div className="px-4 py-4">
          <Title level={5} style={{ color: "#fff", margin: 0 }}>Enterprise AI</Title>
          <Text style={{ color: "rgba(255,255,255,0.45)", fontSize: 12 }}>Chat Platform</Text>
        </div>

        <div className="px-3 mb-3">
          <Select
            value={selectedModel}
            onChange={setSelectedModel}
            options={modelOptions}
            style={{ width: "100%" }}
            size="small"
            popupMatchSelectWidth={false}
          />
        </div>

        <div className="px-3">
          <Button
            type="default"
            ghost
            icon={<PlusOutlined />}
            block
            onClick={() => setMessages([])}
          >
            New Chat
          </Button>
        </div>
      </aside>

      <section className="flex-1 flex flex-col" style={{ background: "#F0F5FF" }}>
        <div className="flex-1 p-6 overflow-y-auto">
          <div className="max-w-3xl mx-auto space-y-4">
            {messages.length === 0 && (
              <div className="text-center py-20">
                <RobotOutlined className="text-6xl mb-4" style={{ color: "#BFDBFE" }} />
                <Title level={4} style={{ color: "#BFBFBF" }}>Select a model and start a conversation.</Title>
              </div>
            )}

            {messages.map((m, i) => (
              <div key={i} className={`flex ${m.role === "user" ? "justify-end" : "justify-start"}`}>
                <div className="flex gap-3 max-w-[80%]">
                  {m.role === "assistant" && (
                    <Avatar icon={<RobotOutlined />} style={{ backgroundColor: "#4096FF", flexShrink: 0 }} />
                  )}
                  <div
                    className="rounded-2xl px-4 py-3 text-sm leading-relaxed"
                    style={
                      m.role === "user"
                        ? { background: "linear-gradient(135deg, #4096FF, #1677FF)", color: "#fff" }
                        : { background: "#fff", border: "1px solid #f0f0f0", boxShadow: "0 1px 2px rgba(0,0,0,0.03)" }
                    }
                  >
                    {m.content}
                  </div>
                  {m.role === "user" && (
                    <Avatar icon={<UserOutlined />} style={{ backgroundColor: "#1677FF", flexShrink: 0 }} />
                  )}
                </div>
              </div>
            ))}
            {streaming && (
              <div className="flex justify-start">
                <div className="flex gap-3 items-center">
                  <Avatar icon={<RobotOutlined />} style={{ backgroundColor: "#4096FF" }} />
                  <Spin size="small" />
                  <Text type="secondary" className="text-sm">AI is thinking...</Text>
                </div>
              </div>
            )}
            <div ref={messagesEnd} />
          </div>
        </div>

        <div style={{ background: "#fff", borderTop: "1px solid #f0f0f0", padding: "16px 24px" }}>
          <div className="max-w-3xl mx-auto">
            <Space.Compact style={{ width: "100%" }}>
              <Input.TextArea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.shiftKey) {
                    e.preventDefault();
                    handleSend();
                  }
                }}
                placeholder="Type your message... (Enter to send, Shift+Enter to new line)"
                autoSize={{ minRows: 1, maxRows: 5 }}
                style={{ resize: "none" }}
              />
              <Button
                type="primary"
                icon={<SendOutlined />}
                onClick={handleSend}
                loading={streaming}
                disabled={!input.trim()}
              >
                Send
              </Button>
            </Space.Compact>
          </div>
        </div>
      </section>
    </main>
  );
}
