import browser from 'webextension-polyfill';
import { exchangeToken, logout as logoutService } from '@/services/authService';
import { getCurrentUser } from '@/services/userService';
import type { UserInfo } from '@/types/auth';

/**
 * Background Service Worker
 * - 확장프로그램 아이콘 클릭 이벤트 처리
 * - Content Script와 메시지 통신
 * - 웹앱 쿠키 기반 인증 관리
 */

// 메시지 타입 정의
type ExtensionMessage =
  | { type: 'CHECK_AUTH' }
  | { type: 'LOGIN'; url: string }
  | { type: 'LOGOUT' }
  | { type: 'OPEN_TAB'; url: string }
  | { type: 'AUTH_CHANGED' };

interface AuthResponse {
  authenticated: boolean;
  user?: UserInfo;
}

// chrome.storage에서 인증 상태 확인
async function checkAuth(): Promise<AuthResponse> {
  try {
    const result = await browser.storage.local.get(['authenticated', 'user']);

    if (result.authenticated) {
      console.log('✅ User is authenticated');
      return {
        authenticated: true,
        user: result.user as UserInfo | undefined,
      };
    }

    console.log('❌ Not authenticated');
    return { authenticated: false };
  } catch (error) {
    console.error('checkAuth failed:', error);
    return { authenticated: false };
  }
}

// OAuth 로그인 처리 (Chrome Identity API 사용)
async function handleLogin(authUrl: string): Promise<void> {
  // 모든 Extension 탭에 인증 상태 변경 알림
  const notifyAuthChanged = async () => {
    const tabs = await browser.tabs.query({});
    for (const tab of tabs) {
      if (tab.id && tab.url && (tab.url.startsWith('http://') || tab.url.startsWith('https://'))) {
        try {
          await browser.tabs.sendMessage(tab.id, { type: 'AUTH_CHANGED' });
        } catch {
          // Content script 없는 탭 무시
        }
      }
    }
  };

  try {
    console.log('🔐 Starting OAuth flow with chrome.identity...');

    // 1. Extension의 정확한 Redirect URI 가져오기
    const extensionRedirectUri = chrome.identity.getRedirectURL();
    console.log('🆔 Extension Redirect URI:', extensionRedirectUri);
    console.log('📍 Base OAuth URL:', authUrl);

    // 2. OAuth URL에 redirect_uri 파라미터 추가
    const oauthUrl = new URL(authUrl);
    oauthUrl.searchParams.set('redirect_uri', extensionRedirectUri);

    console.log('🔗 Final OAuth URL:', oauthUrl.toString());

    // 3. chrome.identity API로 OAuth 팝업 실행
    const redirectUrl = await chrome.identity.launchWebAuthFlow({
      url: oauthUrl.toString(),
      interactive: true,
    });

    // redirectUrl이 undefined인 경우 처리 (사용자가 취소했거나 실패)
    if (!redirectUrl) {
      console.error('❌ OAuth flow was cancelled or failed');
      throw new Error('OAuth authentication was cancelled or failed to complete');
    }

    console.log('✅ OAuth redirect received:', redirectUrl);

    // 2. Authorization Code 추출
    const callbackUrl = new URL(redirectUrl);
    const code = callbackUrl.searchParams.get('code');

    if (!code) {
      console.error('❌ No authorization code found in callback URL:', redirectUrl);
      throw new Error(
        'OAuth callback did not contain authorization code. ' +
          'Check if redirect_uri is correctly configured in Google Cloud Console.',
      );
    }

    console.log('📋 Authorization code received');

    // 3. 토큰 교환 (기존 로직 유지)
    console.log('🔄 Exchanging code for token...');
    const tokenData = await exchangeToken(code);

    if (!tokenData.success || !tokenData.data) {
      console.error('❌ Token exchange failed:', tokenData);
      throw new Error('Token exchange returned invalid data');
    }

    console.log('✅ Token exchange successful');

    const { accessToken } = tokenData.data;

    // 4. Access Token 저장 (getCurrentUser가 이 토큰을 사용함)
    await browser.storage.local.set({
      access_token: accessToken,
    });

    console.log('💾 Access token saved to storage');

    // 5. 사용자 정보 조회 (기존 로직 유지)
    try {
      console.log('👤 Fetching user info...');
      const userInfo = await getCurrentUser();

      // 6. 최종 인증 상태 저장 (기존 로직 유지)
      await browser.storage.local.set({
        authenticated: true,
        user: userInfo,
      });

      console.log('✅ Login successful! User:', userInfo.name);

      // 7. 모든 탭에 인증 변경 알림 (기존 로직 유지)
      await notifyAuthChanged();
    } catch (userError) {
      // 사용자 정보 조회 실패 시 정리 (기존 에러 처리 유지)
      console.error('❌ Failed to fetch user info:', userError);
      await browser.storage.local.remove(['access_token', 'authenticated', 'user']);
      throw new Error('Failed to fetch user information after successful login');
    }
  } catch (error) {
    // OAuth 전체 실패 처리 (기존 에러 처리 유지)
    console.error('❌ OAuth login failed:', error);
    throw error;
  }
}

// 확장프로그램 아이콘 클릭 이벤트
browser.action.onClicked.addListener((tab) => {
  const tabId = tab.id;
  const tabUrl = tab.url;

  if (!tabId || !tabUrl) return;

  // 시스템 페이지에서는 작동하지 않음
  if (!tabUrl.startsWith('http://') && !tabUrl.startsWith('https://')) {
    console.log('Extension cannot run on this page:', tabUrl);
    return;
  }

  void (async () => {
    try {
      // 1단계: Content Script가 준비되었는지 확인 (PING)
      try {
        await browser.tabs.sendMessage(tabId, { type: 'PING' });
      } catch {
        // Content script가 없으면 동적으로 주입
        console.log('Content script not found. Injecting...');
        try {
          await browser.scripting.executeScript({
            target: { tabId },
            files: ['src/content-scripts/overlay/index.tsx'],
          });
          // 주입 후 잠시 대기
          await new Promise((resolve) => setTimeout(resolve, 1000));
        } catch (injectError) {
          console.error('Failed to inject content script:', injectError);
          console.log('Please refresh the page and try again.');
          return;
        }
      }

      // 2단계: Content Script에 overlay toggle 메시지 전송
      await browser.tabs.sendMessage(tabId, { type: 'TOGGLE_OVERLAY' });
      console.log('Toggle overlay message sent');
    } catch (error) {
      console.error('Failed to send message to content script:', error);
      console.log('Tip: Please refresh the page and try again.');

      // 최종 재시도
      await new Promise((resolve) => setTimeout(resolve, 500));

      try {
        await browser.tabs.sendMessage(tabId, { type: 'TOGGLE_OVERLAY' });
        console.log('Retry successful');
      } catch {
        console.error('Retry failed. Please refresh the page.');
      }
    }
  })();
});

// Content Script로부터 메시지 수신
browser.runtime.onMessage.addListener(
  (
    message: unknown,
    _sender: browser.Runtime.MessageSender,
    sendResponse: (response: AuthResponse | { success: boolean }) => void,
  ) => {
    void (async () => {
      try {
        const msg = message as ExtensionMessage;

        switch (msg.type) {
          case 'CHECK_AUTH': {
            const authResponse = await checkAuth();
            sendResponse(authResponse);
            break;
          }

          case 'LOGIN': {
            if ('url' in msg) {
              // 백그라운드에서 로그인 처리 (즉시 응답 반환)
              void handleLogin(msg.url);
              sendResponse({ success: true });
            } else {
              sendResponse({ authenticated: false });
            }
            break;
          }

          case 'LOGOUT': {
            try {
              // 백엔드 로그아웃 API 호출 (Refresh Token 무효화)
              await logoutService();
              console.log('✅ Backend logout successful');
            } catch (error) {
              console.error('Backend logout failed:', error);
              // 백엔드 로그아웃 실패해도 클라이언트 측 로그아웃은 진행
            }

            // chrome.storage에서 인증 정보 삭제
            await browser.storage.local.remove([
              'access_token',
              'refresh_token',
              'user',
              'authenticated',
            ]);
            console.log('✅ Local storage cleared - logout complete');
            sendResponse({ success: true });
            break;
          }

          case 'OPEN_TAB': {
            // 새 탭에서 URL 열기
            await browser.tabs.create({ url: msg.url });
            sendResponse({ success: true });
            break;
          }

          default:
            sendResponse({ success: false });
        }
      } catch (error) {
        console.error('Message handler error:', error);
        sendResponse({ authenticated: false });
      }
    })();

    // 비동기 응답을 위해 true 반환
    return true;
  },
);

console.log('SecondBrain Extension Background Service Worker loaded');
