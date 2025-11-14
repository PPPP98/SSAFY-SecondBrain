import browser from 'webextension-polyfill';
import { env } from '@/config/env';
import type { DragSearchMessage, DragSearchResponse } from '@/types/dragSearch';
import type { NoteSearchApiResponse, NoteSearchResult } from '@/types/note';

// 캐시 인터페이스
interface SearchCache {
  keyword: string;
  results: NoteSearchResult[];
  totalCount: number;
  timestamp: number;
}

// 캐시 저장소 (메모리)
const searchCache = new Map<string, SearchCache>();
const CACHE_TTL = 5 * 60 * 1000; // 5분

/**
 * 드래그 검색 메시지 핸들러 (캐싱 포함)
 * Content Script로부터 검색 요청을 받아 백엔드 API 호출
 *
 * @param message - 드래그 검색 메시지
 * @param sender - 메시지 발신자 정보
 */
export async function handleDragSearchMessage(
  message: DragSearchMessage,
  sender: browser.Runtime.MessageSender,
): Promise<void> {
  try {
    // 캐시 키 생성 (대소문자 무시)
    const cacheKey = message.keyword.toLowerCase();

    // 캐시 확인
    const cached = searchCache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
      console.log(`[SecondBrain] 캐시에서 검색 결과 반환: "${message.keyword}"`);

      // 캐시된 결과 전송
      const cachedResponse: DragSearchResponse = {
        type: 'DRAG_SEARCH_RESULT',
        keyword: message.keyword,
        results: cached.results,
        totalCount: cached.totalCount,
      };
      await browser.tabs.sendMessage(sender.tab!.id!, cachedResponse);
      return;
    }

    // Access Token 가져오기
    const { access_token } = await browser.storage.local.get(['access_token']);
    const token = access_token as string | undefined;

    if (!token) {
      const errorResponse: DragSearchResponse = {
        type: 'DRAG_SEARCH_ERROR',
        keyword: message.keyword,
        error: 'NO_TOKEN',
      };
      await browser.tabs.sendMessage(sender.tab!.id!, errorResponse);
      console.error('[SecondBrain] 드래그 검색 실패: 로그인 필요');
      return;
    }

    console.log(`[SecondBrain] 드래그 검색 시작 (API 호출): "${message.keyword}"`);

    // API 호출 (최대 5개 결과)
    const response = await fetch(
      `${env.apiBaseUrl}/api/notes/search?keyword=${encodeURIComponent(message.keyword)}&page=0&size=5`,
      {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      },
    );

    if (!response.ok) {
      throw new Error(`API Error: ${response.status} ${response.statusText}`);
    }

    const data = (await response.json()) as NoteSearchApiResponse;

    console.log(
      `[SecondBrain] 드래그 검색 완료: "${message.keyword}" - ${data.data.totalCount}개 결과`,
    );

    // 캐시 저장
    searchCache.set(cacheKey, {
      keyword: message.keyword,
      results: data.data.results,
      totalCount: data.data.totalCount,
      timestamp: Date.now(),
    });

    // 캐시 정리 (오래된 항목 삭제)
    for (const [key, value] of searchCache.entries()) {
      if (Date.now() - value.timestamp > CACHE_TTL) {
        searchCache.delete(key);
      }
    }

    // 결과 전송
    const successResponse: DragSearchResponse = {
      type: 'DRAG_SEARCH_RESULT',
      keyword: message.keyword,
      results: data.data.results,
      totalCount: data.data.totalCount,
    };

    await browser.tabs.sendMessage(sender.tab!.id!, successResponse);
  } catch (error) {
    console.error('[SecondBrain] 드래그 검색 오류:', error);

    const errorResponse: DragSearchResponse = {
      type: 'DRAG_SEARCH_ERROR',
      keyword: message.keyword,
      error: error instanceof Error ? error.message : 'UNKNOWN_ERROR',
    };

    try {
      await browser.tabs.sendMessage(sender.tab!.id!, errorResponse);
    } catch (sendError) {
      console.error('[SecondBrain] 에러 응답 전송 실패:', sendError);
    }
  }
}
