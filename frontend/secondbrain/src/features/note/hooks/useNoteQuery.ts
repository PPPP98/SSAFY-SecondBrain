import { useQuery } from '@tanstack/react-query';
import { noteAPI } from '@/features/note/services/noteService';
import type { NoteData } from '@/features/note/types/note';

/**
 * 노트 데이터를 가져오는 React Query 훅
 */
export function useNoteQuery(noteId: string) {
  return useQuery<NoteData>({
    queryKey: ['note', noteId],
    queryFn: async () => {
      const response = await noteAPI.getNote({ noteId: Number(noteId) });
      return response.data;
    },
    enabled: !!noteId,
  });
}
