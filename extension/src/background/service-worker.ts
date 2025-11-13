import browser from 'webextension-polyfill';
import { exchangeGoogleToken, logout as logoutService } from '@/services/authService';
import { getCurrentUser } from '@/services/userService';
import { env } from '@/config/env';
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
  | { type: 'LOGIN' }
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

/**
 * OAuth 로그인 처리 (Chrome Identity API - Google 직접 호출)
 *
 * Flow:
 * 1. chrome.identity.getRedirectURL()로 Extension redirect URI 획득
 * 2. Google OAuth URL 직접 생성 (백엔드 거치지 않음!)
 * 3. chrome.identity.launchWebAuthFlow()로 OAuth 팝업 실행
 * 4. Authorization code 추출
 * 5. 백엔드 API로 code 전송하여 JWT 토큰 교환
 * 6. 사용자 정보 조회 및 저장
 */
async function handleLogin(): Promise<void> {
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
    console.log('🔐 Starting OAuth flow with Google...');

    // 1. Extension Redirect URI 가져오기
    const redirectUri = chrome.identity.getRedirectURL();
    console.log('🆔 Extension Redirect URI:', redirectUri);

    // 2. Google OAuth URL 직접 생성 (백엔드 거치지 않음!)
    const googleAuthUrl = new URL('https://accounts.google.com/o/oauth2/v2/auth');
    googleAuthUrl.searchParams.set('client_id', env.googleClientId);
    googleAuthUrl.searchParams.set('redirect_uri', redirectUri);
    googleAuthUrl.searchParams.set('response_type', 'code');
    googleAuthUrl.searchParams.set('scope', 'openid email profile');
    googleAuthUrl.searchParams.set('access_type', 'offline');
    googleAuthUrl.searchParams.set('prompt', 'consent');

    console.log('🔗 Google OAuth URL:', googleAuthUrl.toString());

    // 3. Chrome Identity API로 OAuth 팝업 실행
    const redirectUrl = await chrome.identity.launchWebAuthFlow({
      url: googleAuthUrl.toString(),
      interactive: true,
    });

    // redirectUrl이 undefined인 경우 처리 (사용자가 취소했거나 실패)
    if (!redirectUrl) {
      console.error('❌ OAuth flow was cancelled or failed');
      throw new Error('OAuth authentication was cancelled or failed to complete');
    }

    console.log('✅ OAuth redirect received:', redirectUrl);

    // 4. Authorization Code 추출
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

    // 5. Google Authorization Code를 Backend JWT로 교환
    console.log('🔄 Exchanging Google code for JWT token...');
    const tokenData = await exchangeGoogleToken(code, redirectUri);

    if (!tokenData.success || !tokenData.data) {
      console.error('❌ Token exchange failed:', tokenData);
      throw new Error('Token exchange returned invalid data');
    }

    console.log('✅ Token exchange successful');

    const { accessToken } = tokenData.data;

    // 6. Access Token 저장 (getCurrentUser가 이 토큰을 사용함)
    await browser.storage.local.set({
      access_token: accessToken,
    });

    console.log('💾 Access token saved to storage');

    // 7. 사용자 정보 조회
    try {
      console.log('👤 Fetching user info...');
      const userInfo = await getCurrentUser();

      // 8. 최종 인증 상태 저장
      await browser.storage.local.set({
        authenticated: true,
        user: userInfo,
      });

      console.log('✅ Login successful! User:', userInfo.name);

      // 9. 모든 탭에 인증 변경 알림
      await notifyAuthChanged();
    } catch (userError) {
      // 사용자 정보 조회 실패 시 정리
      console.error('❌ Failed to fetch user info:', userError);
      await browser.storage.local.remove(['access_token', 'authenticated', 'user']);
      throw new Error('Failed to fetch user information after successful login');
    }
  } catch (error) {
    // OAuth 전체 실패 처리
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
            // 백그라운드에서 로그인 처리 (즉시 응답 반환)
            void handleLogin();
            sendResponse({ success: true });
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
