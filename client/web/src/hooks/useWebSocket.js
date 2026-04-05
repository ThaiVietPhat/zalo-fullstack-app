import { useEffect, useRef } from 'react';
import wsService from '../services/websocket';
import useAuthStore from '../store/authStore';
import useChatStore from '../store/chatStore';

export function useWebSocket() {
  const { auth } = useAuthStore();
  const {
    addMessage,
    updateMessage,
    updateMessageReactions,
    addGroupMessage,
    updateGroupMessage,
    updateGroupMessageReactions,
    setTyping,
    setUserOnline,
    updateChatLastMessage,
    updateGroupLastMessage,
    incrementUnread,
    activeChatId,
    activeGroupId,
    clearUnread,
  } = useChatStore();

  const activeChatIdRef = useRef(activeChatId);
  const activeGroupIdRef = useRef(activeGroupId);

  useEffect(() => {
    activeChatIdRef.current = activeChatId;
  }, [activeChatId]);

  useEffect(() => {
    activeGroupIdRef.current = activeGroupId;
  }, [activeGroupId]);

  useEffect(() => {
    if (!auth?.accessToken) return;

    wsService.connect(auth.accessToken, () => {
      // Subscribe to personal message queue
      wsService.subscribe('/user/queue/messages', (message) => {
        const chatId = message.chatId;
        addMessage(chatId, message);
        updateChatLastMessage(chatId, message);
        if (activeChatIdRef.current !== chatId) {
          incrementUnread(chatId);
        }
      });

      // Subscribe to seen notifications
      wsService.subscribe('/user/queue/seen', (data) => {
        // Mark all messages in the chat as seen
        const { chatId } = data;
        clearUnread(chatId);
      });

      // Subscribe to message recalled
      wsService.subscribe('/user/queue/message-recalled', (data) => {
        const { messageId, chatId } = data;
        updateMessage(chatId, messageId, { deleted: true, content: '' });
      });

      // Subscribe to reactions
      wsService.subscribe('/user/queue/reactions', (data) => {
        const { messageId, chatId, reactions } = data;
        updateMessageReactions(chatId, messageId, reactions);
      });
    });

    return () => {
      // Don't disconnect on unmount of hook — managed by logout
    };
  }, [auth?.accessToken]);

  const subscribeToChat = (chatId) => {
    wsService.subscribe(`/topic/chat/${chatId}/typing`, (data) => {
      const { userId, isTyping } = data;
      if (userId === auth?.userId) return;
      setTyping(`chat_${chatId}`, userId, isTyping);
      if (isTyping) {
        setTimeout(() => {
          setTyping(`chat_${chatId}`, userId, false);
        }, 3000);
      }
    });
  };

  const unsubscribeFromChat = (chatId) => {
    wsService.unsubscribe(`/topic/chat/${chatId}/typing`);
  };

  const subscribeToGroup = (groupId) => {
    wsService.subscribe(`/topic/group/${groupId}`, (message) => {
      addGroupMessage(groupId, message);
      updateGroupLastMessage(groupId, message);
    });

    wsService.subscribe(`/topic/group/${groupId}/typing`, (data) => {
      const { userId, isTyping } = data;
      if (userId === auth?.userId) return;
      setTyping(`group_${groupId}`, userId, isTyping);
      if (isTyping) {
        setTimeout(() => {
          setTyping(`group_${groupId}`, userId, false);
        }, 3000);
      }
    });
  };

  const unsubscribeFromGroup = (groupId) => {
    wsService.unsubscribe(`/topic/group/${groupId}`);
    wsService.unsubscribe(`/topic/group/${groupId}/typing`);
  };

  const subscribeToUserStatus = (userId) => {
    wsService.subscribe(`/topic/user/${userId}/status`, (data) => {
      setUserOnline(data.userId, data.isOnline);
    });
  };

  const sendTyping = (chatId, typing) => {
    wsService.publish(`/app/chat/${chatId}/typing`, { typing });
  };

  const sendGroupTyping = (groupId, typing) => {
    wsService.publish(`/app/group/${groupId}/typing`, { typing });
  };

  return {
    subscribeToChat,
    unsubscribeFromChat,
    subscribeToGroup,
    unsubscribeFromGroup,
    subscribeToUserStatus,
    sendTyping,
    sendGroupTyping,
  };
}
