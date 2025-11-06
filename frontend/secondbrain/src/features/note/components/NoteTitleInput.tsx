import { forwardRef } from 'react';

interface NoteTitleInputProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

/**
 * 노트 제목 입력 컴포넌트
 * - NotePage용 큰 제목 스타일 (투명 배경, 흰색 텍스트)
 * - 스크린샷 "Meeting with the team" 스타일 적용
 * - Glass UI 배경과 통합되는 디자인
 */
export const NoteTitleInput = forwardRef<HTMLInputElement, NoteTitleInputProps>(
  ({ value, onChange, placeholder = '제목을 입력해주세요...' }, ref) => {
    return (
      <input
        ref={ref}
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full border-0 bg-transparent px-16 font-bold text-white outline-none ring-0 transition-all duration-200 placeholder:text-white/30 focus:border-0 focus:outline-none focus:ring-0"
        style={{ fontSize: '42px' }}
        aria-label="노트 제목"
      />
    );
  },
);

NoteTitleInput.displayName = 'NoteTitleInput';
