import type { ReactNode } from 'react';
import BaseLayout from '@/layouts/BaseLayout';

interface BaseLayoutProps {
  children: ReactNode;
}

const LandingLayout = ({ children }: BaseLayoutProps) => {
  return (
    <BaseLayout>
      <div className="flex h-screen items-center justify-center">{children}</div>
    </BaseLayout>
  );
};

export default LandingLayout;
