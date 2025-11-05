import { useState } from 'react';

/**
 * 사용자 프로필 아바타 컴포넌트
 * - 프로필 이미지 또는 폴백 아이콘 표시
 * - 이미지 로드 실패 시 자동 폴백
 * - 크기 조절 가능 (sm, md, lg)
 */

interface UserAvatarProps {
  src: string | null | undefined;
  alt: string;
  size?: 'sm' | 'md' | 'lg';
  fallbackIcon: React.ReactNode;
}

export function UserAvatar({ src, alt, size = 'md', fallbackIcon }: UserAvatarProps) {
  const [imageError, setImageError] = useState(false);

  const sizeClasses = {
    sm: 'size-8',
    md: 'size-10',
    lg: 'size-12',
  };

  // 이미지가 없거나 로드 실패 시 폴백 아이콘 표시
  if (!src || imageError) {
    return (
      <div className={`${sizeClasses[size]} flex items-center justify-center`}>{fallbackIcon}</div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      className={`${sizeClasses[size]} rounded-full object-cover`}
      onError={() => setImageError(true)}
    />
  );
}
