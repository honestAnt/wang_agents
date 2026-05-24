"use client";

import { useState } from "react";
import { useAuth } from "@enterprise-ai/auth";
import { useRouter } from "next/navigation";
import { Button, Card } from "@enterprise-ai/ui";
import { apiClient } from "@enterprise-ai/api-client";

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const response = await apiClient.post("/api/auth/login", { username, password });
      login(response.data.data.access_token);
      router.push("/");
    } catch {
      setError("Invalid username or password.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <Card className="w-full max-w-md">
        <h1 className="text-2xl font-bold text-center mb-6">Admin Console</h1>
        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Username</label>
            <input type="text" value={username} onChange={(e) => setUsername(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500" required />
          </div>
          {error && <p className="text-red-600 text-sm">{error}</p>}
          <Button type="submit" loading={loading} className="w-full">Sign In</Button>
        </form>
      </Card>
    </div>
  );
}
