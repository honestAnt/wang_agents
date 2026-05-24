"use client";

import { createContext, useContext, useState, useCallback, type ReactNode } from "react";
import { jwtDecode } from "jwt-decode";
import type { JwtPayload } from "./types";

interface AuthContextType {
  isAuthenticated: boolean;
  user: { userId: string; username: string; tenantId: string; roles: string[] } | null;
  login: (token: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType>({
  isAuthenticated: false,
  user: null,
  login: () => {},
  logout: () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthContextType["user"]>(null);

  const login = useCallback((token: string) => {
    localStorage.setItem("access_token", token);
    const decoded = jwtDecode<JwtPayload>(token);
    setUser({
      userId: decoded.sub ?? "",
      username: decoded.preferred_username ?? "",
      tenantId: decoded.tenant_id ?? "",
      roles: decoded.realm_access?.roles ?? [],
    });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem("access_token");
    setUser(null);
    window.location.href = "/login";
  }, []);

  return (
    <AuthContext.Provider value={{ isAuthenticated: !!user, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
