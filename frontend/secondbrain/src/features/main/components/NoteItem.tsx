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
  const handleCheckboxClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onToggle(id);
  };

  return (
    <GlassElement as="div" className="w-full gap-6 transition-all" innerClassName="!pl-5">
      <div className="flex items-center">
        <button
          onClick={handleCheckboxClick}
          className={`mr-2 flex size-4 shrink-0 items-center justify-center rounded-sm border-2 transition-all ${
            isSelected
              ? 'border-white bg-white'
              : 'border-white/80 bg-transparent hover:border-white'
          }`}
          aria-label="노트 선택"
        >
          {isSelected && (
            <div className="size-full bg-white">
              <CheckBoxIcon className="size-full text-black" />
            </div>
          )}
        </button>
        <h3 className="flex-1 truncate text-base font-normal text-white/90">{note.title}</h3>
      </div>
    </GlassElement>
  );
}
