import browser from 'webextension-polyfill';

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
  user?: {
    id: string;
    name: string;
    email: string;
  };
}

// chrome.storage에서 인증 상태 확인
async function checkAuth(): Promise<AuthResponse> {
  try {
    const result = await browser.storage.local.get(['authenticated', 'user']);

    if (result.authenticated) {
      console.log('✅ User is authenticated');
      return {
        authenticated: true,
        user: result.user as { id: string; name: string; email: string } | undefined,
      };
    }

    console.log('❌ Not authenticated');
    return { authenticated: false };
  } catch (error) {
    console.error('checkAuth failed:', error);
    return { authenticated: false };
  }
}

// OAuth 로그인 처리
async function handleLogin(authUrl: string): Promise<void> {
  try {
    // 새 탭에서 OAuth 진행
    const authTab = await browser.tabs.create({ url: authUrl, active: true });
    const authTabId = authTab.id;

    if (!authTabId) {
      throw new Error('Failed to create auth tab');
    }

    console.log('OAuth tab opened:', authTabId);

    // 모든 Extension 탭에 인증 상태 변경 알림
    const notifyAuthChanged = async () => {
      const tabs = await browser.tabs.query({});
      for (const tab of tabs) {
        if (
          tab.id &&
          tab.url &&
          (tab.url.startsWith('http://') || tab.url.startsWith('https://'))
        ) {
          try {
            await browser.tabs.sendMessage(tab.id, { type: 'AUTH_CHANGED' });
          } catch {
            // Content script 없는 탭 무시
          }
        }
      }
    };

    // OAuth 콜백 URL 감지
    const handleUrlChange = (tabId: number, changeInfo: browser.Tabs.OnUpdatedChangeInfoType) => {
      if (tabId === authTabId && changeInfo.url) {
        try {
          const url = new URL(changeInfo.url);

          // /auth/callback?code=xxx 감지
          if (url.pathname === '/auth/callback') {
            const code = url.searchParams.get('code');

            if (code) {
              console.log('OAuth callback with code:', code);
              browser.tabs.onUpdated.removeListener(handleUrlChange);
              browser.tabs.onRemoved.removeListener(handleTabClosed);

              // Authorization code를 토큰으로 교환
              void (async () => {
                try {
                  const apiBaseUrl = authUrl.split('/oauth2')[0];
                  console.log('Exchanging code for token at:', apiBaseUrl);

                  // 1. 토큰 교환
                  const tokenResponse = await fetch(`${apiBaseUrl}/api/auth/token?code=${code}`, {
                    method: 'POST',
                    credentials: 'include',
                  });

                  if (tokenResponse.ok) {
                    const tokenData = (await tokenResponse.json()) as {
                      success: boolean;
                      data?: { accessToken: string; refreshToken: string };
                    };

                    if (tokenData.success && tokenData.data) {
                      console.log('✅ Token exchange successful!');

                      // JWT에서 사용자 정보 추출
                      const payload = JSON.parse(
                        atob(tokenData.data.accessToken.split('.')[1]),
                      ) as {
                        userId?: number;
                        sub?: string;
                        name?: string;
                        email?: string;
                      };
                      const user = {
                        id: payload.userId?.toString() || payload.sub || '',
                        name: payload.name || payload.email?.split('@')[0] || 'User',
                        email: payload.email || '',
                      };

                      // chrome.storage에 저장
                      await browser.storage.local.set({
                        access_token: tokenData.data.accessToken,
                        refresh_token: tokenData.data.refreshToken,
                        authenticated: true,
                        user: user,
                      });

                      console.log('✅ Login successful!', user.name);

                      // 즉시 모든 탭에 인증 변경 알림
                      await notifyAuthChanged();

                      // OAuth 탭 바로 닫기
                      await browser.tabs.remove(authTabId);
                    } else {
                      console.error('Token data invalid:', tokenData);
                      await browser.tabs.remove(authTabId);
                    }
                  } else {
                    console.error(
                      'Token exchange failed:',
                      tokenResponse.status,
                      await tokenResponse.text(),
                    );
                    await browser.tabs.remove(authTabId);
                  }
                } catch (e) {
                  console.error('Token exchange failed:', e);
                  await browser.tabs.remove(authTabId);
                }
              })();
            }
          }
        } catch (e) {
          console.debug('URL parsing failed:', e);
        }
      }
    };

    // 탭 수동 종료 시 리스너 정리
    const handleTabClosed = (tabId: number) => {
      if (tabId === authTabId) {
        console.log('OAuth tab closed manually');
        browser.tabs.onUpdated.removeListener(handleUrlChange);
        browser.tabs.onRemoved.removeListener(handleTabClosed);
        void notifyAuthChanged();
      }
    };

    browser.tabs.onUpdated.addListener(handleUrlChange);
    browser.tabs.onRemoved.addListener(handleTabClosed);
  } catch (error) {
    console.error('OAuth login failed:', error);
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
      // Content Script에 overlay toggle 메시지 전송
      await browser.tabs.sendMessage(tabId, { type: 'TOGGLE_OVERLAY' });
      console.log('Toggle overlay message sent');
    } catch (error) {
      console.error('Failed to send message to content script:', error);
      console.log('Tip: Please refresh the page and try again.');

      // 재시도
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
            // chrome.storage에서 인증 정보 삭제
            await browser.storage.local.remove([
              'access_token',
              'refresh_token',
              'user',
              'authenticated',
            ]);
            console.log('Logged out - storage cleared');
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
