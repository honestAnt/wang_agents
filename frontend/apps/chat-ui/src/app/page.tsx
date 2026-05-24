"use client";

import { useAuth } from "@enterprise-ai/auth";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

export default function Home() {
  const { isAuthenticated } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/login");
    }
  }, [isAuthenticated, router]);

  return (
    <main className="flex h-screen">
      <aside className="w-64 bg-gray-900 text-white p-4">
        <h1 className="text-xl font-bold mb-6">Enterprise AI</h1>
        <nav className="space-y-2">
          <a href="#" className="block px-3 py-2 rounded hover:bg-gray-800">New Chat</a>
          <a href="#" className="block px-3 py-2 rounded hover:bg-gray-800">History</a>
          <a href="#" className="block px-3 py-2 rounded hover:bg-gray-800">Agents</a>
        </nav>
      </aside>
      <section className="flex-1 flex flex-col">
        <div className="flex-1 p-8 overflow-y-auto">
          <p className="text-gray-500 text-center mt-20">Select an agent to start a conversation.</p>
        </div>
        <div className="border-t p-4">
          <input
            type="text"
            placeholder="Type your message..."
            className="w-full border rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </section>
    </main>
  );
}
