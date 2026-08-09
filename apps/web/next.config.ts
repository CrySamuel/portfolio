import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // Remove o header X-Powered-By: nao ha ganho em anunciar o framework e a versao.
  poweredByHeader: false,
  // O design system e consumido do fonte, nao de um build intermediario. Isso
  // mantem um passo a menos no pipeline e preserva o tree-shaking do Next.
  transpilePackages: ['@portfolio/ui'],
};

export default nextConfig;
