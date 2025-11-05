import { Crepe } from '@milkdown/crepe';
import '@milkdown/crepe/theme/common/style.css';
import '@milkdown/crepe/theme/frame.css';
import { Milkdown, MilkdownProvider, useEditor } from '@milkdown/react';
import React from 'react';

const CrepeEditor: React.FC = () => {
  useEditor((root) => {
    return new Crepe({
      root,
      defaultValue: 'Hello, Milkdown!',
    });
  });

  return <Milkdown />;
};

export function NoteEditor() {
  return (
    <MilkdownProvider>
      <CrepeEditor />
    </MilkdownProvider>
  );
}
