import type { ReactNode } from 'react';
import { BaseLayout } from '@/layouts/BaseLayout';
import { GlassElement } from '@/shared/components/GlassElement/GlassElement';
import UserIcon from '@/shared/components/icon/User.svg?react';
import PlusIcon from '@/shared/components/icon/Plus.svg?react';

interface MainLayoutProps {
  children: ReactNode;
}

const MainLayout = ({ children }: MainLayoutProps) => {
  return (
    <BaseLayout>
      {/* 배경 레이어: children (Graph 등) */}
      <div className="relative size-full">{children}</div>

      {/* UI 레이어: 개별 요소만 z-index로 위에 배치 */}
      <div className="absolute left-1/2 top-10 z-50 -translate-x-1/2">
        <GlassElement as="input" scale="md" />
      </div>

      <div className="absolute right-10 top-10 z-50">
        <GlassElement as="button" icon={<UserIcon />} />
      </div>

      <div className="absolute bottom-10 right-10 z-50">
        <GlassElement as="button" icon={<PlusIcon />} />
      </div>
    </BaseLayout>
  );
};

export { MainLayout };
