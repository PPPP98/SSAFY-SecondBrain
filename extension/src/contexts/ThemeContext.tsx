import { createContext, useEffect, useState, useRef } from 'react';
import { cn } from '@/lib/utils/utils';

type Theme = 'light' | 'dark' | 'system';
type ResolvedTheme = 'light' | 'dark';

interface ThemeContextType {
  theme: Theme;
  resolvedTheme: ResolvedTheme;
  setTheme: (theme: Theme) => void;
}

const STORAGE_KEY = 'secondbrain-theme';

const initialState: ThemeContextType = {
  theme: 'system',
  resolvedTheme: 'light',
  setTheme: () => null,
};

export const ThemeContext = createContext<ThemeContextType>(initialState);

interface ThemeProviderProps {
  children: React.ReactNode;
  defaultTheme?: Theme;
}

export function ThemeProvider({ children, defaultTheme = 'system' }: ThemeProviderProps) {
  const [theme, setTheme] = useState<Theme>(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    return (saved as Theme) || defaultTheme;
  });

  const containerRef = useRef<HTMLDivElement | null>(null);

  // Resolve system preference
  const getSystemTheme = (): ResolvedTheme => {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  };

  const [resolvedTheme, setResolvedTheme] = useState<ResolvedTheme>(() => {
    return theme === 'system' ? getSystemTheme() : theme;
  });

  // Apply theme to container ref (works in Shadow DOM)
  useEffect(() => {
    const applyTheme = (resolved: ResolvedTheme) => {
      if (containerRef.current) {
        if (resolved === 'dark') {
          containerRef.current.classList.add('dark');
        } else {
          containerRef.current.classList.remove('dark');
        }
      }
    };

    const newResolvedTheme = theme === 'system' ? getSystemTheme() : theme;
    setResolvedTheme(newResolvedTheme);
    applyTheme(newResolvedTheme);
  }, [theme]);

  // Listen to system preference changes
  useEffect(() => {
    if (theme !== 'system') return;

    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

    const handleChange = (e: MediaQueryListEvent) => {
      const newResolvedTheme = e.matches ? 'dark' : 'light';
      setResolvedTheme(newResolvedTheme);

      // Apply theme to container ref
      if (containerRef.current) {
        if (newResolvedTheme === 'dark') {
          containerRef.current.classList.add('dark');
        } else {
          containerRef.current.classList.remove('dark');
        }
      }
    };

    mediaQuery.addEventListener('change', handleChange);

    return () => {
      mediaQuery.removeEventListener('change', handleChange);
    };
  }, [theme]);

  const value = {
    theme,
    resolvedTheme,
    setTheme: (newTheme: Theme) => {
      localStorage.setItem(STORAGE_KEY, newTheme);
      setTheme(newTheme);
    },
  };

  return (
    <ThemeContext.Provider value={value}>
      <div
        ref={containerRef}
        className={cn(resolvedTheme === 'dark' ? 'dark' : '', 'text-foreground')}
      >
        {children}
      </div>
    </ThemeContext.Provider>
  );
}
