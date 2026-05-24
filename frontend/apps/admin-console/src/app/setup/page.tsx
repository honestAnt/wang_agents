"use client";

import { useState } from "react";
import { Button, Card } from "@enterprise-ai/ui";

const STEPS = ["Create Admin Account", "Configure AI Model", "Setup Storage"];

export default function SetupPage() {
  const [step, setStep] = useState(0);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <Card className="w-full max-w-lg">
        <h1 className="text-2xl font-bold mb-2">First-Time Setup</h1>
        <p className="text-gray-500 mb-6">Configure your enterprise AI platform</p>

        <div className="flex mb-6">
          {STEPS.map((s, i) => (
            <div key={s} className={`flex-1 text-center text-sm ${i <= step ? "text-blue-600" : "text-gray-400"}`}>
              <div className={`w-8 h-8 mx-auto rounded-full flex items-center justify-center text-white text-sm mb-1 ${i <= step ? "bg-blue-600" : "bg-gray-300"}`}>
                {i + 1}
              </div>
              {s}
            </div>
          ))}
        </div>

        <div className="min-h-[200px]">
          {step === 0 && (
            <div className="space-y-3">
              <input type="text" placeholder="Admin username" className="w-full border rounded px-3 py-2" />
              <input type="email" placeholder="Admin email" className="w-full border rounded px-3 py-2" />
              <input type="password" placeholder="Password" className="w-full border rounded px-3 py-2" />
            </div>
          )}
          {step === 1 && (
            <div className="space-y-3">
              <select className="w-full border rounded px-3 py-2">
                <option>OpenAI (GPT-4.1)</option>
                <option>Anthropic (Claude Sonnet 4.6)</option>
                <option>DeepSeek V3</option>
              </select>
              <input type="text" placeholder="API Key" className="w-full border rounded px-3 py-2" />
            </div>
          )}
          {step === 2 && (
            <div className="space-y-3">
              <input type="text" placeholder="MinIO Endpoint (http://localhost:9000)" className="w-full border rounded px-3 py-2" />
              <input type="text" placeholder="Qdrant Endpoint (http://localhost:6333)" className="w-full border rounded px-3 py-2" />
            </div>
          )}
        </div>

        <div className="flex justify-between mt-6">
          <Button variant="secondary" disabled={step === 0} onClick={() => setStep(step - 1)}>Back</Button>
          <Button onClick={() => step < 2 ? setStep(step + 1) : window.location.href = "/"}>
            {step < 2 ? "Next" : "Finish Setup"}
          </Button>
        </div>
      </Card>
    </div>
  );
}
