"use client";

import { useState } from "react";
import { Table, Button, Input, Tag, Modal, Form, Select, Popconfirm, Space, Typography, message } from "antd";
import { PlusOutlined, SearchOutlined, EditOutlined, DeleteOutlined } from "@ant-design/icons";

const { Title } = Typography;

interface User {
  key: string;
  username: string;
  email: string;
  role: string;
  status: string;
}

const MOCK_USERS: User[] = [
  { key: "1", username: "alice", email: "alice@company.com", role: "TenantAdmin", status: "active" },
  { key: "2", username: "bob", email: "bob@company.com", role: "Developer", status: "active" },
  { key: "3", username: "carol", email: "carol@company.com", role: "User", status: "active" },
  { key: "4", username: "dave", email: "dave@company.com", role: "Operator", status: "inactive" },
];

export default function UsersPage() {
  const [search, setSearch] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [messageApi, ctx] = message.useMessage();
  const [form] = Form.useForm();

  const filtered = MOCK_USERS.filter(
    (u) => !search || u.username.includes(search) || u.email.includes(search)
  );

  const columns = [
    { title: "Username", dataIndex: "username", key: "username" },
    { title: "Email", dataIndex: "email", key: "email" },
    {
      title: "Role", dataIndex: "role", key: "role",
      render: (v: string) => <Tag color="blue">{v}</Tag>,
    },
    {
      title: "Status", dataIndex: "status", key: "status",
      render: (v: string) => <Tag color={v === "active" ? "green" : "default"}>{v}</Tag>,
    },
    {
      title: "Actions", key: "actions",
      render: (_: unknown, record: User) => (
        <Space>
          <Button
            type="link" size="small" icon={<EditOutlined />}
            onClick={() => { setEditingUser(record); form.setFieldsValue(record); setModalOpen(true); }}
          />
          <Popconfirm title="Delete this user?" onConfirm={() => messageApi.success("Deleted")}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="p-6">
      {ctx}
      <div className="flex flex-wrap justify-between items-center mb-4 gap-3">
        <Title level={3} style={{ margin: 0 }}>User & Permission Management</Title>
        <Space>
          <Input
            prefix={<SearchOutlined />} placeholder="Search users..."
            value={search} onChange={(e) => setSearch(e.target.value)}
            style={{ width: 220 }} allowClear
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingUser(null); form.resetFields(); setModalOpen(true); }}>
            Add User
          </Button>
        </Space>
      </div>

      <Table dataSource={filtered} columns={columns} pagination={{ pageSize: 10 }} size="middle" />

      <Modal
        title={editingUser ? "Edit User" : "Add User"}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => { form.submit(); }}
      >
        <Form form={form} layout="vertical" onFinish={() => { messageApi.success("Saved"); setModalOpen(false); }}>
          <Form.Item name="username" label="Username" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="email" label="Email" rules={[{ required: true, type: "email" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="role" label="Role" rules={[{ required: true }]}>
            <Select options={[
              { value: "TenantAdmin", label: "Tenant Admin" },
              { value: "Developer", label: "Developer" },
              { value: "Operator", label: "Operator" },
              { value: "User", label: "User" },
            ]} />
          </Form.Item>
          <Form.Item name="status" label="Status">
            <Select options={[
              { value: "active", label: "Active" },
              { value: "inactive", label: "Inactive" },
            ]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
