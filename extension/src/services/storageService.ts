import browser from 'webextension-polyfill';

/**
 * Storage Service
 * - chrome.storage.local API 래퍼
 * - 페이지 수집 목록 및 Overlay 상태 관리
 * - 탭 간 상태 동기화를 위한 리스너 제공
 */

// Storage Keys 정의
export const STORAGE_KEYS = {
  COLLECTED_PAGES: 'secondbrain-collected-pages',
  OVERLAY_STATE: 'secondbrain-overlay-state',
} as const;

// Overlay State 타입
export type OverlayState = 'expanded' | 'collapsed' | 'hidden';

// Storage Change Callback 타입
export type StorageChangeCallback = (key: string, newValue: unknown, oldValue: unknown) => void;

// ============================================================================
// Page Collection Functions
// ============================================================================

/**
 * 수집된 페이지 목록을 Storage에 저장
 * @param pages - URL 문자열 배열
 */
export async function saveCollectedPages(pages: string[]): Promise<void> {
  try {
    await browser.storage.local.set({
      [STORAGE_KEYS.COLLECTED_PAGES]: pages,
    });
  } catch (error) {
    console.error('[StorageService] Failed to save collected pages:', error);
    throw error;
  }
}

/**
 * Storage에서 수집된 페이지 목록 불러오기
 * @returns URL 문자열 배열 (없으면 빈 배열)
 */
export async function loadCollectedPages(): Promise<string[]> {
  try {
    const result = await browser.storage.local.get([STORAGE_KEYS.COLLECTED_PAGES]);
    const pages = result[STORAGE_KEYS.COLLECTED_PAGES];

    // 타입 가드: string 배열인지 검증
    if (Array.isArray(pages) && pages.every((item) => typeof item === 'string')) {
      return pages;
    }

    return []; // 기본값
  } catch (error) {
    console.error('[StorageService] Failed to load collected pages:', error);
    return []; // 기본값 반환 (우아한 실패)
  }
}

/**
 * 페이지 추가 (중복 자동 방지)
 * @param url - 추가할 URL
 * @returns 업데이트된 페이지 배열
 */
export async function addCollectedPage(url: string): Promise<string[]> {
  try {
    const pages = await loadCollectedPages();

    // 중복 체크
    if (!pages.includes(url)) {
      pages.push(url);
      await saveCollectedPages(pages);
    }

    return pages;
  } catch (error) {
    console.error('[StorageService] Failed to add page:', error);
    throw error;
  }
}

/**
 * 페이지 제거
 * @param url - 제거할 URL
 * @returns 업데이트된 페이지 배열
 */
export async function removeCollectedPage(url: string): Promise<string[]> {
  try {
    const pages = await loadCollectedPages();
    const filtered = pages.filter((p) => p !== url);
    await saveCollectedPages(filtered);
    return filtered;
  } catch (error) {
    console.error('[StorageService] Failed to remove page:', error);
    throw error;
  }
}

/**
 * 모든 수집된 페이지 삭제
 */
export async function clearCollectedPages(): Promise<void> {
  try {
    await saveCollectedPages([]);
  } catch (error) {
    console.error('[StorageService] Failed to clear pages:', error);
    throw error;
  }
}

// ============================================================================
// Overlay State Functions
// ============================================================================

/**
 * Overlay 상태를 Storage에 저장
 * @param state - Overlay 상태 ('expanded' | 'collapsed' | 'hidden')
 */
export async function saveOverlayState(state: OverlayState): Promise<void> {
  try {
    await browser.storage.local.set({
      [STORAGE_KEYS.OVERLAY_STATE]: state,
    });
  } catch (error) {
    console.error('[StorageService] Failed to save overlay state:', error);
    throw error;
  }
}

/**
 * Storage에서 Overlay 상태 불러오기
 * @returns Overlay 상태 (없으면 'expanded' 기본값)
 */
export async function loadOverlayState(): Promise<OverlayState> {
  try {
    const result = await browser.storage.local.get([STORAGE_KEYS.OVERLAY_STATE]);
    const state = result[STORAGE_KEYS.OVERLAY_STATE];

    // 타입 검증
    if (state === 'expanded' || state === 'collapsed' || state === 'hidden') {
      return state;
    }

    return 'expanded'; // 기본값
  } catch (error) {
    console.error('[StorageService] Failed to load overlay state:', error);
    return 'expanded'; // 기본값 반환
  }
}

// ============================================================================
// Storage Change Listener
// ============================================================================

/**
 * Storage 변경 감지 리스너 등록
 * - 다른 탭에서 Storage가 변경되면 콜백 실행
 * - 반환된 함수를 호출하면 리스너 제거
 *
 * @param callback - Storage 변경 시 실행할 콜백
 * @returns 리스너 제거 함수
 *
 * @example
 * const unwatch = watchStorageChanges((key, newValue, oldValue) => {
 *   if (key === STORAGE_KEYS.COLLECTED_PAGES) {
 *     console.log('Pages updated:', newValue);
 *   }
 * });
 *
 * // 리스너 제거
 * unwatch();
 */
export function watchStorageChanges(callback: StorageChangeCallback): () => void {
  const listener = (
    changes: Record<string, browser.Storage.StorageChange>,
    areaName: string,
  ) => {
    // local storage 변경만 감지
    if (areaName === 'local') {
      Object.entries(changes).forEach(([key, change]) => {
        callback(key, change.newValue, change.oldValue);
      });
    }
  };

  // 리스너 등록
  browser.storage.onChanged.addListener(listener);

  // Cleanup 함수 반환
  return () => {
    browser.storage.onChanged.removeListener(listener);
  };
}

// ============================================================================
// Migration Functions (localStorage → chrome.storage)
// ============================================================================

/**
 * localStorage에서 chrome.storage로 데이터 마이그레이션
 * - 기존 사용자의 데이터를 자동으로 이관
 * - 이미 마이그레이션되었으면 스킵
 */
export async function migrateFromLocalStorage(): Promise<void> {
  try {
    // 1. localStorage에서 기존 데이터 읽기
    const oldOverlayState = localStorage.getItem('secondbrain-overlay-state');

    // 2. chrome.storage에 이미 데이터 있는지 확인
    const existing = await browser.storage.local.get([STORAGE_KEYS.OVERLAY_STATE]);

    // 3. chrome.storage가 비어있고 localStorage에 데이터 있으면 마이그레이션
    if (!existing[STORAGE_KEYS.OVERLAY_STATE] && oldOverlayState) {
      await browser.storage.local.set({
        [STORAGE_KEYS.OVERLAY_STATE]: oldOverlayState,
      });

      console.log('[StorageService] Migrated overlay state from localStorage');
    }

    // 4. 마이그레이션 완료 후 localStorage 정리
    localStorage.removeItem('secondbrain-overlay-state');
  } catch (error) {
    console.error('[StorageService] Migration failed:', error);
    // 실패해도 계속 진행 (치명적 아님)
  }
}

/**
 * Storage 초기화
 * - 마이그레이션 실행
 * - 기본값 설정 (없으면)
 */
export async function initializeStorage(): Promise<void> {
  try {
    // 1. 마이그레이션 먼저 실행
    await migrateFromLocalStorage();

    // 2. Storage 초기값 설정 (없으면)
    const result = await browser.storage.local.get([
      STORAGE_KEYS.COLLECTED_PAGES,
      STORAGE_KEYS.OVERLAY_STATE,
    ]);

    const updates: Record<string, unknown> = {};

    if (!result[STORAGE_KEYS.COLLECTED_PAGES]) {
      updates[STORAGE_KEYS.COLLECTED_PAGES] = [];
    }

    if (!result[STORAGE_KEYS.OVERLAY_STATE]) {
      updates[STORAGE_KEYS.OVERLAY_STATE] = 'expanded';
    }

    // 업데이트할 내용이 있으면 저장
    if (Object.keys(updates).length > 0) {
      await browser.storage.local.set(updates);
    }

    console.log('[StorageService] Initialization complete');
  } catch (error) {
    console.error('[StorageService] Initialization failed:', error);
    throw error;
  }
}
