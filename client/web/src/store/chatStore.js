import { create } from 'zustand';

const useChatStore = create((set) => ({
  chats: [],
  groups: [],
  contacts: [], // accepted friends (UserDto[])
  pendingRequests: [], // incoming friend requests (FriendRequestDto[])
  sentRequests: [], // outgoing pending requests (FriendRequestDto[])
  activeChatId: null,
  activeGroupId: null,
  activeTab: 'chats', // 'chats' | 'groups' | 'ai' | 'contacts'
  messages: {}, // { chatId: [MessageDto] }
  groupMessages: {}, // { groupId: [GroupMessageDto] }
  typingUsers: {}, // { 'chat_chatId': Set<userId>, 'group_groupId': Set<userId> }
  onlineUsers: {}, // { userId: boolean }

  setChats: (chats) =>
    set((state) => ({ chats: typeof chats === 'function' ? chats(state.chats) : chats })),

  setContacts: (contacts) => set({ contacts }),
  addContact: (contact) =>
    set((state) => {
      if (state.contacts.find((c) => c.id === contact.id)) return state;
      return { contacts: [...state.contacts, contact] };
    }),
  removeContact: (userId) =>
    set((state) => ({ contacts: state.contacts.filter((c) => c.id !== userId) })),

  setPendingRequests: (pendingRequests) => set({ pendingRequests }),
  addPendingRequest: (req) =>
    set((state) => {
      if (state.pendingRequests.find((r) => r.id === req.id)) return state;
      return { pendingRequests: [...state.pendingRequests, req] };
    }),
  removePendingRequest: (requestId) =>
    set((state) => ({ pendingRequests: state.pendingRequests.filter((r) => r.id !== requestId) })),

  setSentRequests: (sentRequests) => set({ sentRequests }),
  addSentRequest: (req) =>
    set((state) => {
      if (state.sentRequests.find((r) => r.id === req.id)) return state;
      return { sentRequests: [...state.sentRequests, req] };
    }),
  updateSentRequest: (requestId, updates) =>
    set((state) => ({
      sentRequests: state.sentRequests.map((r) => (r.id === requestId ? { ...r, ...updates } : r)),
    })),

  removeChat: (chatId) =>
    set((state) => {
      // eslint-disable-next-line no-unused-vars
      const { [chatId]: _removed, ...remainingMessages } = state.messages;
      return {
        chats: state.chats.filter((c) => c.id !== chatId),
        activeChatId: state.activeChatId === chatId ? null : state.activeChatId,
        messages: remainingMessages,
      };
    }),

  updateChat: (chatId, updates) =>
    set((state) => ({
      chats: state.chats.map((c) => (c.id === chatId ? { ...c, ...updates } : c)),
    })),
  setGroups: (groups) => set({ groups }),
  setActiveTab: (tab) => set({ activeTab: tab }),

  setActiveChatId: (id) => set({ activeChatId: id, activeGroupId: null }),
  setActiveGroupId: (id) => set({ activeGroupId: id, activeChatId: null }),

  setMessages: (chatId, messages) =>
    set((state) => ({
      messages: { ...state.messages, [chatId]: messages },
    })),

  prependMessages: (chatId, messages) =>
    set((state) => ({
      messages: {
        ...state.messages,
        [chatId]: [...messages, ...(state.messages[chatId] || [])],
      },
    })),

  addMessage: (chatId, message) =>
    set((state) => {
      const existing = state.messages[chatId] || [];
      // avoid duplicates
      if (existing.find((m) => m.id === message.id)) return state;
      return {
        messages: { ...state.messages, [chatId]: [...existing, message] },
      };
    }),

  updateMessage: (chatId, messageId, updates) =>
    set((state) => ({
      messages: {
        ...state.messages,
        [chatId]: (state.messages[chatId] || []).map((m) =>
          m.id === messageId ? { ...m, ...updates } : m
        ),
      },
    })),

  updateMessageReactions: (chatId, messageId, reactions) =>
    set((state) => ({
      messages: {
        ...state.messages,
        [chatId]: (state.messages[chatId] || []).map((m) =>
          m.id === messageId ? { ...m, reactions } : m
        ),
      },
    })),

  setGroupMessages: (groupId, messages) =>
    set((state) => ({
      groupMessages: { ...state.groupMessages, [groupId]: messages },
    })),

  prependGroupMessages: (groupId, messages) =>
    set((state) => ({
      groupMessages: {
        ...state.groupMessages,
        [groupId]: [...messages, ...(state.groupMessages[groupId] || [])],
      },
    })),

  addGroupMessage: (groupId, message) =>
    set((state) => {
      const existing = state.groupMessages[groupId] || [];
      if (existing.find((m) => m.id === message.id)) return state;
      return {
        groupMessages: {
          ...state.groupMessages,
          [groupId]: [...existing, message],
        },
      };
    }),

  updateGroupMessage: (groupId, messageId, updates) =>
    set((state) => ({
      groupMessages: {
        ...state.groupMessages,
        [groupId]: (state.groupMessages[groupId] || []).map((m) =>
          m.id === messageId ? { ...m, ...updates } : m
        ),
      },
    })),

  updateGroupMessageReactions: (groupId, messageId, reactions) =>
    set((state) => ({
      groupMessages: {
        ...state.groupMessages,
        [groupId]: (state.groupMessages[groupId] || []).map((m) =>
          m.id === messageId ? { ...m, reactions } : m
        ),
      },
    })),

  updateChatLastMessage: (chatId, message) =>
    set((state) => ({
      chats: state.chats.map((c) =>
        c.id === chatId
          ? {
              ...c,
              lastMessage: message.content,
              lastMessageType: message.type,
              lastMessageTime: message.createdAt,
            }
          : c
      ),
    })),

  updateGroupLastMessage: (groupId, message) =>
    set((state) => ({
      groups: state.groups.map((g) =>
        g.id === groupId
          ? {
              ...g,
              lastMessage: message.content,
              lastMessageType: message.type,
              lastMessageTime: message.createdDate,
              lastMessageSenderName: message.senderName,
            }
          : g
      ),
    })),

  setTyping: (key, userId, isTyping) =>
    set((state) => {
      const current = new Set(state.typingUsers[key] || []);
      if (isTyping) {
        current.add(userId);
      } else {
        current.delete(userId);
      }
      return { typingUsers: { ...state.typingUsers, [key]: current } };
    }),

  setUserOnline: (userId, isOnline) =>
    set((state) => ({
      onlineUsers: { ...state.onlineUsers, [userId]: isOnline },
    })),

  incrementUnread: (chatId) =>
    set((state) => ({
      chats: state.chats.map((c) =>
        c.id === chatId ? { ...c, unreadCount: (c.unreadCount || 0) + 1 } : c
      ),
    })),

  clearUnread: (chatId) =>
    set((state) => ({
      chats: state.chats.map((c) =>
        c.id === chatId ? { ...c, unreadCount: 0 } : c
      ),
    })),
}));

export default useChatStore;
