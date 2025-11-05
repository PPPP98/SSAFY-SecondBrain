import type { ReactNode } from 'react';

interface BaseLayoutProps {
  children: ReactNode;
}

const BaseLayout = ({ children }: BaseLayoutProps) => {
  return (
    <div className="h-screen w-screen overflow-hidden bg-[#192030] text-white">{children}</div>
  );
};

export default BaseLayout;
