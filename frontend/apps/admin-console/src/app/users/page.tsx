"use client";

import { Card, Button } from "@enterprise-ai/ui";

const MOCK_USERS = [
  { username: "alice", email: "alice@company.com", role: "TenantAdmin", status: "active" },
  { username: "bob", email: "bob@company.com", role: "Developer", status: "active" },
  { username: "carol", email: "carol@company.com", role: "User", status: "active" },
];

export default function UsersPage() {
  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">User & Permission Management</h1>
        <Button>Add User</Button>
      </div>
      <Card>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left">
              <th className="py-2">Username</th><th>Email</th><th>Role</th><th>Status</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {MOCK_USERS.map((u) => (
              <tr key={u.username} className="border-b hover:bg-gray-50">
                <td className="py-2 font-medium">{u.username}</td>
                <td>{u.email}</td>
                <td><span className="bg-blue-100 text-blue-800 px-2 py-0.5 rounded text-xs">{u.role}</span></td>
                <td><span className="bg-green-100 text-green-800 px-2 py-0.5 rounded text-xs">{u.status}</span></td>
                <td><button className="text-blue-600 hover:underline text-xs">Edit</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
