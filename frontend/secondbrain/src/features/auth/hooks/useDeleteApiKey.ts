import { useMutation } from '@tanstack/react-query';
import { toast } from 'sonner';
import { deleteApiKey } from '@/features/auth/api/apiKey';

/**
 * API Key 삭제 React Query Hook
 *
 * @returns {UseMutationResult} mutate 함수 및 상태 (isPending, isError 등)
 *
 * @example
 * const { mutate, isPending } = useDeleteApiKey();
 * mutate(undefined, {
 *   onSuccess: () => console.log('삭제 완료')
 * });
 */
export function useDeleteApiKey() {
  return useMutation({
    mutationFn: deleteApiKey,
    onSuccess: () => {
      toast.success('API Key가 삭제되었습니다.');
    },
    onError: (error: Error) => {
      toast.error(error.message || 'API Key 삭제에 실패했습니다.');
    },
  });
}
