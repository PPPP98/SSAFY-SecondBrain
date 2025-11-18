import { useMutation } from '@tanstack/react-query';
import { toast } from 'sonner';
import { generateApiKey } from '@/features/auth/api/apiKey';

/**
 * API Key 발급 React Query Hook
 *
 * @returns {UseMutationResult} mutate 함수 및 상태 (isPending, isError 등)
 *
 * @example
 * const { mutate, isPending } = useGenerateApiKey();
 * mutate(undefined, {
 *   onSuccess: (apiKey) => console.log(apiKey)
 * });
 */
export function useGenerateApiKey() {
  return useMutation({
    mutationFn: generateApiKey,
    onError: (error: Error) => {
      toast.error(error.message || 'API Key 발급에 실패했습니다.');
    },
  });
}
