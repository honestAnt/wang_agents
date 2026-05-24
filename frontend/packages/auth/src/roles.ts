export function hasRole(roles: string[], role: string): boolean {
  return roles.includes(role);
}

export function hasAnyRole(roles: string[], ...required: string[]): boolean {
  return required.some((r) => roles.includes(r));
}
