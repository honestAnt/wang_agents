"use client";

import { useAuth } from "@enterprise-ai/auth";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { Card } from "@enterprise-ai/ui";

export default function Dashboard() {
  const { isAuthenticated } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isAuthenticated) router.push("/login");
  }, [isAuthenticated, router]);

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-6">Dashboard</h1>
      <div className="grid grid-cols-4 gap-4 mb-8">
        <Card title="Today Requests"><p className="text-3xl font-bold">1,234</p></Card>
        <Card title="Token Usage"><p className="text-3xl font-bold">5.2M</p></Card>
        <Card title="Cost Today"><p className="text-3xl font-bold">$12.45</p></Card>
        <Card title="Active Users"><p className="text-3xl font-bold">89</p></Card>
      </div>
    </div>
  );
}
