import type { Metadata } from "next";
import { Syne, Outfit } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "../components/auth/AuthProvider";
import Header from "../components/Header";

const syne = Syne({ subsets: ["latin"], variable: "--font-syne" });
const outfit = Outfit({ subsets: ["latin"], variable: "--font-outfit" });

export const metadata: Metadata = {
  title: "ALGOCORE",
  description: "Advanced Algorithmic Game Suite.",
  icons: {
    icon: "/ALGOCORE-Logo.png",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body
        className={`${syne.variable} ${outfit.variable} font-sans antialiased bg-[#050505] text-white selection:bg-white selection:text-black overflow-x-hidden`}
      >
        {/* Global Noise Texture Overlay */}
        <div className="bg-noise" />

        <AuthProvider>
          <Header />
          <main className="relative z-10 min-h-screen">{children}</main>
        </AuthProvider>
      </body>
    </html>
  );
}
