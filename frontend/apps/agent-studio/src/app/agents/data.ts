export interface Agent {
  key: string;
  name: string;
  desc: string;
  status: "Published" | "Test" | "Draft";
  systemPrompt?: string;
  model?: string;
}

export const agents: Agent[] = [
  {
    key: "1",
    name: "Lab Assistant",
    desc: "Research & analysis agent with RAG",
    status: "Published",
    systemPrompt: "You are a research and analysis assistant. Help users with data analysis, literature review, and generating insights from complex information.",
    model: "gpt-4.1",
  },
  {
    key: "2",
    name: "Customer Service Bot",
    desc: "FAQ, order lookup, complaint handling",
    status: "Test",
    systemPrompt: "You are a customer service bot. Answer FAQs, look up orders, and handle complaints professionally and empathetically.",
    model: "claude-sonnet-4-6",
  },
  {
    key: "3",
    name: "Data Analyst",
    desc: "Excel/CSV analysis, chart generation",
    status: "Draft",
    systemPrompt: "You are a data analyst. Analyze spreadsheets, generate charts, and provide actionable insights from data.",
    model: "deepseek-v3",
  },
  {
    key: "4",
    name: "Code Reviewer",
    desc: "PR review, static analysis, security scan",
    status: "Draft",
    systemPrompt: "You are a code reviewer. Review pull requests for correctness, readability, security, and performance.",
    model: "gpt-4.1",
  },
];

export const statusColors: Record<string, string> = { Published: "green", Test: "blue", Draft: "default" };

export const modelOptions = [
  { value: "gpt-4.1", label: "GPT-4.1" },
  { value: "claude-sonnet-4-6", label: "Claude Sonnet 4.6" },
  { value: "deepseek-v3", label: "DeepSeek V3" },
  { value: "qwen-max", label: "Qwen Max" },
];
