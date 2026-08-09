import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // Remove o header X-Powered-By: nao ha ganho em anunciar o framework e a versao.
  poweredByHeader: false,
};

export default nextConfig;
