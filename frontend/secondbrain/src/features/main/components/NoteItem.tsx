import GlassElement from '@/shared/components/GlassElement/GlassElement';
import CheckBoxIcon from '@/shared/components/icon/CheckBox.svg?react';

interface NoteItemProps {
  note: {
    noteId?: number; // RecentNote 타입
    id?: number; // Note 타입
    title: string;
    content?: string;
  };
  isSelected: boolean;
  onToggle: (id: number) => void;
}

export function NoteItem({ note, isSelected, onToggle }: NoteItemProps) {
  // noteId 또는 id를 사용 (RecentNote는 noteId, Note는 id 사용)
  const id = note.noteId ?? note.id ?? 0;
  const handleCheckboxChange = () => {
    onToggle(id);
  };

  return (
    <GlassElement
      as="div"
      className="flex h-14 cursor-pointer items-center gap-3 transition-all"
      onClick={handleCheckboxChange}
    >
      <button
        onClick={handleCheckboxChange}
        className={`flex size-5 shrink-0 items-center justify-center rounded border-2 transition-all ${
          isSelected
            ? 'border-white bg-white'
            : 'border-white/40 bg-transparent hover:border-white/60'
        }`}
        aria-label="노트 선택"
      >
        {isSelected && <CheckBoxIcon className="size-4 text-black" />}
      </button>
      <h3 className="flex-1 truncate text-sm font-medium text-white">{note.title}</h3>
    </GlassElement>
  );
}
