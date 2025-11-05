import type { ReactNode } from 'react';
import BaseLayout from '@/layouts/BaseLayout';
import GlassElement from '@/shared/components/GlassElement/GlassElement';
import UserIcon from '@/shared/components/icon/User.svg?react';
import PlusIcon from '@/shared/components/icon/Plus.svg?react';

interface MainLayoutProps {
  children: ReactNode;
}

const MainLayout = ({ children }: MainLayoutProps) => {
  return (
    <BaseLayout>
      <div className="fixed left-1/2 top-10 -translate-x-1/2">
        <GlassElement as="input" scale="md" />
      </div>

      <div className="fixed right-10 top-10">
        <GlassElement as="button" icon={<UserIcon />} />
      </div>

      <div className="fixed bottom-10 right-10">
        <GlassElement as="button" icon={<PlusIcon />} />
      </div>

      {children}
    </BaseLayout>
  );
};

export default MainLayout;
