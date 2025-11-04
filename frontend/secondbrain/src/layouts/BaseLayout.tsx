import type { ReactNode } from 'react';

interface BaseLayoutProps {
  children: ReactNode;
}

const BaseLayout = ({ children }: BaseLayoutProps) => {
  return <div className="bg-[#192030] p-10 text-white">{children}</div>;
};

export default BaseLayout;
