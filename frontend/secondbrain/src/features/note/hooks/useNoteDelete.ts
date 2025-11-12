import { useMutation, useQueryClient } from '@tanstack/react-query';
import { noteAPI } from '@/features/note/services/noteService';
import type { NoteDeleteRequest, NoteDeleteResponse } from '@/features/note/types/note';

/**
 * 노트를 삭제하는 React Query Mutation 훅
 */
export function useNoteDelete() {
  const queryClient = useQueryClient();

  return useMutation<NoteDeleteResponse, Error, NoteDeleteRequest>({
    mutationFn: async (requestData: NoteDeleteRequest) => {
      return await noteAPI.deleteNotes(requestData);
    },
    onSuccess: async (_data, variables) => {
      // 삭제된 노트들의 쿼리 캐시 무효화
      await Promise.all(
        variables.noteIds.map((noteId) =>
          queryClient.invalidateQueries({ queryKey: ['note', String(noteId)] }),
        ),
      );

      // 노트 목록 쿼리도 무효화 (목록이 있다면)
      await queryClient.invalidateQueries({ queryKey: ['notes'] });

      // 그래프 쿼리 무효화 (삭제된 노트가 그래프에서 사라지도록)
      await queryClient.invalidateQueries({ queryKey: ['graphs', 'visualization'] });
    },
  });
}
