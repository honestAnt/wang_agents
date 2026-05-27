-- ============================================================
-- Seed demo data: default tenant and admin user
-- ============================================================

INSERT INTO tenants (id, name, domain, plan, status, max_users, max_agents, quota_limit)
VALUES ('tenant-demo-001', 'Demo Enterprise', 'demo.enterprise.ai', 'enterprise', 'active', 100, 50, 10000000)
ON CONFLICT (id) DO NOTHING;

-- admin@123.com matches the Keycloak user (sub: 1ae50488-75f9-48ff-8560-1589fddef8a6)
INSERT INTO users (id, tenant_id, username, email, display_name, status)
VALUES ('1ae50488-75f9-48ff-8560-1589fddef8a6', 'tenant-demo-001', 'admin@123.com', 'admin@123.com', 'Admin', 'active')
ON CONFLICT (tenant_id, username) DO NOTHING;
