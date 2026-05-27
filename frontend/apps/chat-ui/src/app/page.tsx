"use client";

import { useAuth } from "@enterprise-ai/auth";
import { useRouter } from "next/navigation";
import { useEffect, useState, useRef } from "react";
import { apiClient } from "@enterprise-ai/api-client";
import { Layout, Menu, Select, Button, Input, Upload, Collapse, Typography, Tag, Space, Spin, Avatar } from "antd";
import {
  PlusOutlined, SendOutlined, UserOutlined, RobotOutlined, PaperClipOutlined,
  HistoryOutlined, AppstoreOutlined,
} from "@ant-design/icons";

const { Sider, Content, Header } = Layout;
const { TextArea } = Input;
const { Text, Title } = Typography;

interface Message { role: "user" | "assistant"; content: string; toolCall?: { name: string; status: string } }

interface ModelOption { name: string; provider: string; inputPrice: number; outputPrice: number }

const FALLBACK_MODELS: ModelOption[] = [
  { name: "gpt-4.1", provider: "openai", inputPrice: 0.003, outputPrice: 0.006 },
  { name: "claude-sonnet-4-6", provider: "anthropic", inputPrice: 0.003, outputPrice: 0.015 },
  { name: "deepseek-v3", provider: "openai_compatible", inputPrice: 0.00027, outputPrice: 0.0011 },
  { name: "qwen-max", provider: "openai_compatible", inputPrice: 0.002, outputPrice: 0.006 },
];

export default function ChatPage() {
  const { isAuthenticated, user } = useAuth();
  const router = useRouter();
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [streaming, setStreaming] = useState(false);
  const [selectedModel, setSelectedModel] = useState("deepseek-v3");
  const [selectedAgent, setSelectedAgent] = useState("lab-assistant");
  const [collapsed, setCollapsed] = useState(false);
  const [models, setModels] = useState<ModelOption[]>(FALLBACK_MODELS);
  const messagesEnd = useRef<HTMLDivElement>(null);

  useEffect(() => { if (!isAuthenticated) router.push("/login"); }, [isAuthenticated, router]);
  useEffect(() => { messagesEnd.current?.scrollIntoView({ behavior: "smooth" }); }, [messages]);

  useEffect(() => {
    fetch(`${process.env.NEXT_PUBLIC_AGENT_URL ?? "http://localhost:8000"}/api/v1/models`)
      .then(res => res.json())
      .then(data => { if (data?.models?.length) setModels(data.models); })
      .catch(() => {}); // keep fallback models
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
      if (response.body) {
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let assistantContent = "";
        let buffer = "";
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          // Parse SSE frames: normalize \r\n → \n then split on \n\n
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
                  if (last?.role === "assistant") updated[updated.length - 1] = { ...last, content: assistantContent };
                  else updated.push({ role: "assistant", content: assistantContent });
                  return updated;
                });
              } catch { /* skip malformed frames */ }
            }
          }
        }
      }
    } finally { setStreaming(false); }
  };

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sider
        collapsible collapsed={collapsed} onCollapse={setCollapsed}
        theme="light" width={260}
        className="border-r border-gray-100"
      >
        <div className="px-4 py-4">
          <Title level={5} style={{ margin: 0, color: "#4096FF" }}>
            {collapsed ? "AI" : "Enterprise AI"}
          </Title>
        </div>

        <div className="px-3 mb-3">
          <Button type="primary" icon={<PlusOutlined />} block onClick={() => setMessages([])}>
            {!collapsed && "New Chat"}
          </Button>
        </div>

        <Menu
          mode="inline" defaultSelectedKeys={["1"]}
          items={[
            { key: "1", icon: <PlusOutlined />, label: "New Chat" },
            { key: "2", icon: <HistoryOutlined />, label: "History" },
            { key: "3", icon: <AppstoreOutlined />, label: "Agents" },
          ]}
          style={{ borderRight: 0 }}
        />
      </Sider>

      <Layout>
        <Header style={{ background: "#fff", borderBottom: "1px solid #f0f0f0", padding: "0 24px", display: "flex", alignItems: "center", gap: 12, height: 56 }}>
          <Select value={selectedAgent} onChange={setSelectedAgent} style={{ width: 180 }}
            options={[
              { value: "lab-assistant", label: "Lab Assistant" },
              { value: "customer-service", label: "Customer Service" },
              { value: "data-analyst", label: "Data Analyst" },
            ]} />
          <Select value={selectedModel} onChange={setSelectedModel} style={{ width: 200 }}
            loading={models.length === 0}
            options={models.map(m => ({ value: m.name, label: m.name }))} />
        </Header>

        <Content style={{ padding: 24, overflow: "auto", background: "#F0F5FF" }}>
          <div className="max-w-3xl mx-auto space-y-4">
            {messages.length === 0 && (
              <div className="text-center py-20">
                <RobotOutlined className="text-6xl mb-4" style={{ color: "#BFDBFE" }} />
                <Title level={4} className="text-gray-400">Select an agent to start a conversation.</Title>
              </div>
            )}

            {messages.map((m, i) => (
              <div key={i} className={`flex ${m.role === "user" ? "justify-end" : "justify-start"}`}>
                <div className="flex gap-3 max-w-[80%]">
                  {m.role === "assistant" && (
                    <Avatar icon={<RobotOutlined />} style={{ backgroundColor: "#4096FF", flexShrink: 0 }} />
                  )}
                  <div className={`rounded-2xl px-4 py-3 text-sm leading-relaxed ${
                    m.role === "user"
                      ? "text-white" : "bg-white border border-gray-100 shadow-sm text-gray-800"
                  }`}
                  style={m.role === "user" ? { background: "linear-gradient(135deg, #4096FF, #1677FF)" } : undefined}
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
        </Content>

        <div style={{ background: "#fff", borderTop: "1px solid #f0f0f0", padding: "16px 24px" }}>
          <div className="max-w-3xl mx-auto">
            <Space.Compact style={{ width: "100%" }}>
              <Upload showUploadList={false}>
                <Button icon={<PaperClipOutlined />} />
              </Upload>
              <TextArea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); handleSend(); } }}
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
      </Layout>
    </Layout>
  );
}
