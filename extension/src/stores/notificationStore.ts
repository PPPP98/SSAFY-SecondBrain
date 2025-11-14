import { create } from 'zustand';

// Toast 알림은 항상 활성화됨
// 이 스토어는 추후 확장을 위해 유지 (예: 알림 히스토리 등)
type NotificationStore = object;

export const useNotificationStore = create<NotificationStore>(() => ({}));
