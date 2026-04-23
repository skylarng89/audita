import { defineStore } from 'pinia'
import type { Notification } from '~/types'

interface NotificationState {
  items: Notification[]
  unreadCount: number
  isConnected: boolean
}

export const useNotificationStore = defineStore('notifications', {
  state: (): NotificationState => ({
    items: [],
    unreadCount: 0,
    isConnected: false,
  }),

  actions: {
    setUnreadCount(count: number) {
      this.unreadCount = count
    },

    prependNotification(notification: Notification) {
      this.items.unshift(notification)
      if (!notification.isRead) {
        this.unreadCount++
      }
    },

    setItems(items: Notification[]) {
      this.items = items
    },

    markRead(id: string) {
      const item = this.items.find(n => n.id === id)
      if (item && !item.isRead) {
        item.isRead = true
        this.unreadCount = Math.max(0, this.unreadCount - 1)
      }
    },

    markAllRead() {
      this.items.forEach(n => { n.isRead = true })
      this.unreadCount = 0
    },

    setConnected(connected: boolean) {
      this.isConnected = connected
    },
  },
})
