import { forwardRef, useEffect, useRef } from 'react';

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
 * - 자동 높이 조절 (줄바꿈 지원)
 */
export const NoteTitleInput = forwardRef<HTMLTextAreaElement, NoteTitleInputProps>(
  ({ value, onChange, placeholder = '제목을 입력해주세요...' }, ref) => {
    const internalRef = useRef<HTMLTextAreaElement>(null);
    const textareaRef = (ref as React.RefObject<HTMLTextAreaElement>) || internalRef;

    // 자동 높이 조절
    useEffect(() => {
      const textarea = textareaRef.current;
      if (textarea) {
        textarea.style.height = 'auto';
        textarea.style.height = `${textarea.scrollHeight}px`;
      }
    }, [value, textareaRef]);

    return (
      <textarea
        ref={textareaRef}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        rows={1}
        className="mb-6 w-full resize-none overflow-hidden border-0 bg-transparent font-bold text-white outline-none ring-0 transition-all duration-200 placeholder:text-white/30 focus:border-0 focus:outline-none focus:ring-0"
        style={{ fontSize: '42px', lineHeight: '1.2' }}
        aria-label="노트 제목"
      />
    );
  },
);

NoteTitleInput.displayName = 'NoteTitleInput';
