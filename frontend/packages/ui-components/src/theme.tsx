"use client";

import React, { useState, useEffect } from "react";
import { ConfigProvider, theme as antTheme, App as AntApp } from "antd";
import zhCN from "antd/locale/zh_CN";

const themeConfig = {
  token: {
    colorPrimary: "#4096FF",
    borderRadius: 6,
    colorBgLayout: "#F0F5FF",
    colorBgContainer: "#FFFFFF",
    fontFamily:
      "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
  },
  components: {
    Layout: {
      siderBg: "#FFFFFF",
      headerBg: "#FFFFFF",
      bodyBg: "#F0F5FF",
      triggerBg: "#FFFFFF",
    },
    Menu: {
      itemBg: "transparent",
      itemSelectedBg: "#EFF6FF",
      itemSelectedColor: "#4096FF",
      itemActiveBg: "#DBEAFE",
    },
    Card: {
      borderRadiusLG: 8,
    },
    Button: {
      borderRadius: 6,
    },
    Table: {
      headerBg: "#FAFAFA",
    },
  },
};

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);

  return (
    <ConfigProvider theme={themeConfig} locale={zhCN}>
      <AntApp>
        {mounted ? children : <div style={{ visibility: "hidden" }}>{children}</div>}
      </AntApp>
    </ConfigProvider>
  );
}

export { themeConfig };
