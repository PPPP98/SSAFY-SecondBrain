import browser from 'webextension-polyfill';
import type { PendingTextSnippet } from '@/types/pendingTextSnippet';
import type { SaveRequest } from '@/types/save';

/**
 * Storage Service
 * - chrome.storage.local API 래퍼
 * - 페이지 수집 목록 및 Overlay 상태 관리
 * - 임시 텍스트 조각 관리
 * - 저장 상태 관리 (Save Status)
 * - 탭 간 상태 동기화를 위한 리스너 제공
 */

// Storage Keys 정의
export const STORAGE_KEYS = {
  COLLECTED_PAGES: 'secondbrain-collected-pages',
  OVERLAY_STATE: 'secondbrain-overlay-state',
  PENDING_TEXT_SNIPPETS: 'secondbrain-pending-text-snippets',
  SAVE_STATUS_REQUESTS: 'secondbrain-save-status-requests',
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
// Pending Text Snippets Functions
// ============================================================================

/**
 * 임시 텍스트 조각 목록을 Storage에 저장
 * @param snippets - PendingTextSnippet 배열
 */
export async function savePendingSnippets(snippets: PendingTextSnippet[]): Promise<void> {
  try {
    await browser.storage.local.set({
      [STORAGE_KEYS.PENDING_TEXT_SNIPPETS]: snippets,
    });
  } catch (error) {
    console.error('[StorageService] Failed to save pending snippets:', error);
    throw error;
  }
}

/**
 * Storage에서 임시 텍스트 조각 목록 불러오기
 * @returns PendingTextSnippet 배열 (없으면 빈 배열)
 */
export async function loadPendingSnippets(): Promise<PendingTextSnippet[]> {
  try {
    const result = await browser.storage.local.get([STORAGE_KEYS.PENDING_TEXT_SNIPPETS]);
    const snippets = result[STORAGE_KEYS.PENDING_TEXT_SNIPPETS];

    // 타입 가드: 배열인지 검증
    if (Array.isArray(snippets)) {
      return snippets as PendingTextSnippet[];
    }

    return []; // 기본값
  } catch (error) {
    console.error('[StorageService] Failed to load pending snippets:', error);
    return []; // 기본값 반환
  }
}

/**
 * 텍스트 조각 추가
 * @param snippet - 추가할 텍스트 조각
 * @returns 업데이트된 조각 배열
 */
export async function addPendingSnippet(
  snippet: PendingTextSnippet,
): Promise<PendingTextSnippet[]> {
  try {
    const snippets = await loadPendingSnippets();
    snippets.push(snippet);
    await savePendingSnippets(snippets);
    return snippets;
  } catch (error) {
    console.error('[StorageService] Failed to add snippet:', error);
    throw error;
  }
}

/**
 * 텍스트 조각 제거
 * @param id - 제거할 조각 ID
 * @returns 업데이트된 조각 배열
 */
export async function removePendingSnippet(id: string): Promise<PendingTextSnippet[]> {
  try {
    const snippets = await loadPendingSnippets();
    const filtered = snippets.filter((s) => s.id !== id);
    await savePendingSnippets(filtered);
    return filtered;
  } catch (error) {
    console.error('[StorageService] Failed to remove snippet:', error);
    throw error;
  }
}

/**
 * 모든 임시 텍스트 조각 삭제
 */
export async function clearPendingSnippets(): Promise<void> {
  try {
    await savePendingSnippets([]);
  } catch (error) {
    console.error('[StorageService] Failed to clear snippets:', error);
    throw error;
  }
}

// ============================================================================
// Save Status Functions
// ============================================================================

/**
 * 저장 요청 목록을 Storage에 저장
 * @param requests - SaveRequest 배열
 */
export async function saveSaveStatusRequests(requests: SaveRequest[]): Promise<void> {
  try {
    await browser.storage.local.set({
      [STORAGE_KEYS.SAVE_STATUS_REQUESTS]: requests,
    });
  } catch (error) {
    console.error('[StorageService] Failed to save status requests:', error);
    throw error;
  }
}

/**
 * Storage에서 저장 요청 목록 불러오기
 * @returns SaveRequest 배열 (없으면 빈 배열)
 */
export async function loadSaveStatusRequests(): Promise<SaveRequest[]> {
  try {
    const result = await browser.storage.local.get([STORAGE_KEYS.SAVE_STATUS_REQUESTS]);
    const requests = result[STORAGE_KEYS.SAVE_STATUS_REQUESTS];

    // 타입 가드: 배열인지 검증
    if (Array.isArray(requests)) {
      return requests as SaveRequest[];
    }

    return []; // 기본값
  } catch (error) {
    console.error('[StorageService] Failed to load status requests:', error);
    return []; // 기본값 반환
  }
}

/**
 * 모든 저장 요청 삭제
 */
export async function clearSaveStatusRequests(): Promise<void> {
  try {
    await saveSaveStatusRequests([]);
  } catch (error) {
    console.error('[StorageService] Failed to clear status requests:', error);
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
  const listener = (changes: Record<string, browser.Storage.StorageChange>, areaName: string) => {
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
      STORAGE_KEYS.PENDING_TEXT_SNIPPETS,
      STORAGE_KEYS.SAVE_STATUS_REQUESTS,
    ]);

    const updates: Record<string, unknown> = {};

    if (!result[STORAGE_KEYS.COLLECTED_PAGES]) {
      updates[STORAGE_KEYS.COLLECTED_PAGES] = [];
    }

    if (!result[STORAGE_KEYS.OVERLAY_STATE]) {
      updates[STORAGE_KEYS.OVERLAY_STATE] = 'expanded';
    }

    if (!result[STORAGE_KEYS.PENDING_TEXT_SNIPPETS]) {
      updates[STORAGE_KEYS.PENDING_TEXT_SNIPPETS] = [];
    }

    if (!result[STORAGE_KEYS.SAVE_STATUS_REQUESTS]) {
      updates[STORAGE_KEYS.SAVE_STATUS_REQUESTS] = [];
    }

    // 업데이트할 내용이 있으면 저장
    if (Object.keys(updates).length > 0) {
      await browser.storage.local.set(updates);
    }
  } catch (error) {
    console.error('[StorageService] Initialization failed:', error);
    throw error;
  }
}
