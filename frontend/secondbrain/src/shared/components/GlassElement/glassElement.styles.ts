// 스타일 설정 객체 - 엘리먼트 타입별 스타일 중앙 관리
export const STYLE_CONFIG = {
  button: {
    scale: 'p-0 text-xl',
    size: 'w-14 h-14',
    borderRadius: 'rounded-full',
    elementSpecific: 'flex cursor-pointer items-center justify-center',
  },
  input: {
    sm: {
      scale: 'p-0 text-sm',
      size: 'w-14 h-14',
      borderRadius: 'rounded-full',
      type: 'checkbox' as const,
    },
    md: {
      scale: 'px-6 text-base',
      size: 'w-[22rem] h-14',
      borderRadius: 'rounded-3xl',
      type: 'text' as const,
    },
    elementSpecific: 'block appearance-none outline-none focus:outline-none',
  },
  div: {
    scale: 'p-4 px-8 text-xl',
    size: 'w-[25rem]',
    borderRadius: 'rounded-3xl',
    elementSpecific: 'flex flex-row',
  },
} as const;
