import type { ReactNode } from 'react';
import BaseLayout from '@/layouts/BaseLayout';

interface LandingLayoutProps {
  children: ReactNode;
}

const LandingLayout = ({ children }: LandingLayoutProps) => {
  return (
    <BaseLayout>
      <div className="flex h-screen items-center justify-center">{children}</div>
    </BaseLayout>
  );
};

export default LandingLayout;
