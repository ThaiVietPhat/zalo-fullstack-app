import { useEffect, useRef } from 'react';
import wsService from '../services/websocket';
import useAuthStore from '../store/authStore';
import useChatStore from '../store/chatStore';
import { getChatDetail } from '../api/chat';
import toast from 'react-hot-toast';

export function useWebSocket() {
  const { auth, setSessionReplaced } = useAuthStore();
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
    groups,
    chats,
    setChats,
    addPendingRequest,
    addContact,
    updateSentRequest,
  } = useChatStore();

  const chatsRef = useRef(chats);
  useEffect(() => { chatsRef.current = chats; }, [chats]);

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
        // If chat was deleted (removed from local list), re-fetch and add it back
        if (!chatsRef.current.find((c) => c.id === chatId)) {
          getChatDetail(chatId)
            .then((res) => setChats((prev) => [res.data, ...prev.filter((c) => c.id !== chatId)]))
            .catch(() => {});
        }
        addMessage(chatId, message);
        updateChatLastMessage(chatId, message);
        if (activeChatIdRef.current !== chatId) {
          incrementUnread(chatId);
        }
      });

      // Subscribe to seen notifications
      wsService.subscribe('/user/queue/seen', (data) => {
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

      // Subscribe to incoming friend requests
      wsService.subscribe('/user/queue/friend-request', (data) => {
        addPendingRequest(data);
        toast(`${data.senderName.trim()} đã gửi lời mời kết bạn`);
      });

      // Subscribe to force-logout (đăng nhập từ nơi khác)
      wsService.subscribe('/user/queue/force-logout', () => {
        wsService.disconnect();
        setSessionReplaced();
      });

      // Subscribe to friend request accepted
      wsService.subscribe('/user/queue/friend-request-accepted', (data) => {
        const friend = {
          id: data.receiverId,
          firstName: data.receiverName.split(' ')[0],
          lastName: data.receiverName.split(' ').slice(1).join(' '),
          email: data.receiverEmail,
          avatarUrl: data.receiverAvatarUrl,
          friendshipStatus: 'ACCEPTED',
        };
        addContact(friend);
        updateSentRequest(data.id, { status: 'ACCEPTED' });
        toast.success(`${data.receiverName.trim()} đã chấp nhận lời mời kết bạn`);
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
    wsService.subscribe(`/topic/group/${groupId}`, (data) => {
      // ReactionGroupEvent has { messageId, reactions }, new messages have { id, content, ... }
      if (data.messageId !== undefined && data.reactions !== undefined && !data.id) {
        updateGroupMessageReactions(groupId, data.messageId, data.reactions);
      } else {
        addGroupMessage(groupId, data);
        updateGroupLastMessage(groupId, data);
      }
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
