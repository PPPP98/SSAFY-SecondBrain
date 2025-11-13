import type { ReactNode } from 'react';

interface BaseLayoutProps {
  children: ReactNode;
}

const BaseLayout = ({ children }: BaseLayoutProps) => {
  return (
    <div className="h-screen w-screen overflow-hidden bg-[#10131A] text-white">{children}</div>
  );
};

export { BaseLayout };
