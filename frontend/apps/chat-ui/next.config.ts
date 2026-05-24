import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  transpilePackages: [
    "@enterprise-ai/auth",
    "@enterprise-ai/api-client",
    "@enterprise-ai/ui",
    "@enterprise-ai/utils",
  ],
};

export default nextConfig;
