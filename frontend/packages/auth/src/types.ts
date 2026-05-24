export interface UserProfile {
  userId: string;
  username: string;
  email: string;
  displayName: string;
  tenantId: string;
  roles: string[];
}

export interface JwtPayload {
  sub?: string;
  preferred_username?: string;
  email?: string;
  name?: string;
  tenant_id?: string;
  realm_access?: {
    roles: string[];
  };
  exp?: number;
  iat?: number;
}
