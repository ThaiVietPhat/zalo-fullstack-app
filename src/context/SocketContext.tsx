/* eslint-disable */
import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { useAuthStore } from '@/store';
import { useQueryClient } from '@tanstack/react-query';
import { getMyGroups } from '@/api/group';
import { getAllChats } from '@/api/chat';
// @ts-ignore
import SockJS from 'sockjs-client';

if (typeof TextEncoder === 'undefined') {
    const { TextEncoder, TextDecoder } = require('text-encoding');
    global.TextEncoder = TextEncoder;
    global.TextDecoder = TextDecoder;
}

interface SocketContextType {
    isConnected: boolean;
    publish: (destination: string, body: any) => void;
    subscribeToChat: (chatId: string, callback: (data: any) => void) => () => void;
    setActiveChat: (chatId: string | null) => void;
}

const SocketContext = createContext<SocketContextType | undefined>(undefined);

export function SocketProvider({ children }: { children: React.ReactNode }) {
    const token = useAuthStore((state: any) => state.token);
    const [isConnected, setIsConnected] = useState(false);
    const clientRef = useRef<Client | null>(null);
    const queryClient = useQueryClient();
    const chatListeners = useRef<Record<string, ((data: any) => void)[]>>({});
    const activeChatIdRef = useRef<string | null>(null);

    useEffect(() => {
        if (!token) return;

        const baseUrl = (process.env.EXPO_PUBLIC_SOCKET_URL || process.env.EXPO_PUBLIC_SERVER_URL?.split('/api/v1')[0] + '/ws') as string;
        const client = new Client({
            webSocketFactory: () => new SockJS(baseUrl),
            connectHeaders: { Authorization: `Bearer ${token}` },
            debug: (str) => console.log('[Socket-Debug]', str),
            reconnectDelay: 5000,
            heartbeatIncoming: 0,
            heartbeatOutgoing: 0,
        });

        const updateCache = (chatId: string, messageId: string, content: string, createdAt: any, senderName: string, type: string, isGroup: boolean, senderId: string, mediaUrl: string | null = null, deleted: boolean = false) => {
            if (!content && !mediaUrl && !deleted) return;

            const listKey = isGroup ? ['groups'] : ['chats'];
            const detailKey = isGroup ? ['group-messages', chatId, 0] : ['messages', chatId, 0];

            let time = createdAt;
            if (Array.isArray(createdAt)) {
                time = new Date(createdAt[0], createdAt[1] - 1, createdAt[2], createdAt[3], createdAt[4], createdAt[5]).toISOString();
            } else if (!createdAt) {
                time = new Date().toISOString();
            }

            // 1. Cập nhật HOME LIST
            queryClient.setQueryData(listKey, (oldList: any[] | undefined) => {
                if (!oldList) return oldList;
                const newList = [...oldList];
                const index = newList.findIndex((item: any) => item.id === chatId);
                if (index !== -1) {
                    const currentUser = useAuthStore.getState().user;
                    const isMe = senderId && currentUser && senderId === (currentUser as any).id;
                    const isNotMe = !isMe;
                    const shouldIncrement = isNotMe && (activeChatIdRef.current !== chatId);

                    const updated = {
                        ...newList[index],
                        lastMessage: deleted ? "Tin nhắn đã bị thu hồi" : (content || "[Hình ảnh/Video]"),
                        lastMessageType: type || 'TEXT',
                        lastMessageTime: time,
                        lastMessageSenderName: isMe ? "Bạn" : (senderName || ""),
                        unreadCount: (newList[index].unreadCount || 0) + (shouldIncrement ? 1 : 0)
                    };
                    newList.splice(index, 1);
                    newList.unshift(updated);
                    return newList;
                }
                return oldList;
            });

            // 2. Cập nhật CHI TIẾT
            queryClient.setQueryData(detailKey, (oldMessages: any[] | undefined) => {
                if (!oldMessages) return oldMessages;

                // Nếu là tin nhắn thu hồi/xóa
                if (deleted) {
                    const recallText = senderName ? `Tin nhắn đã được ${senderName} thu hồi` : "Tin nhắn đã được thu hồi";
                    return oldMessages.map((m: any) =>
                        (m.id === messageId || m.content === content || m.id === content)
                            ? { ...m, deleted: true, content: recallText, text: recallText }
                            : m
                    );
                }

                // Kiểm tra trùng lặp bằng ID chính xác từ server (Tránh trùng lặp do nhận Socket và REST cùng lúc)
                const isDup = oldMessages.some((m: any) => m.id === messageId);
                if (isDup) {
                    // Nếu đã có (từ REST onMutate onSuccess), cập nhật lại với data chuẩn từ Socket
                    return oldMessages.map(m => m.id === messageId ? { ...m, mediaUrl } : m);
                }

                const newMessage = {
                    id: messageId,
                    chatId: chatId,
                    content: content,
                    text: content,
                    type: type || 'TEXT',
                    createdAt: time,
                    createdDate: time,
                    senderId: senderId,
                    senderName: senderName,
                    state: 'SENT',
                    deleted: deleted,
                    mediaUrl: mediaUrl,
                    reactions: []
                };
                return [newMessage, ...oldMessages];
            });

            if (chatListeners.current[chatId]) {
                chatListeners.current[chatId].forEach(cb => cb({ chatId, messageId, content, createdAt: time, senderId, senderName, type, mediaUrl, deleted }));
            }
        };

        client.onConnect = () => {
            setIsConnected(true);
            setTimeout(async () => {
                try {
                    const [chats, groups] = await Promise.all([getAllChats(), getMyGroups()]);
                    chats?.forEach(chat => {
                        client.subscribe(`/topic/chat/${chat.id}`, (msg) => {
                            const data = JSON.parse(msg.body);
                            if (data.id) {
                                updateCache(chat.id, data.id, data.content, data.createdAt, data.senderName, data.type, false, data.senderId, data.mediaUrl, data.deleted);
                            }
                        });
                    });
                    groups?.forEach(group => {
                        client.subscribe(`/topic/group/${group.id}`, (msg) => {
                            const data = JSON.parse(msg.body);
                            if (data.id) {
                                updateCache(group.id, data.id, data.content, data.createdDate, data.senderName, data.type, true, data.senderId, data.mediaUrl, data.deleted);
                            }
                        });
                    });

                    // Lắng nghe thu hồi tin nhắn chat đơn từ người kia
                    client.subscribe('/user/queue/message-recalled', (msg) => {
                        const data = JSON.parse(msg.body);
                        if (data.messageId && data.chatId) {
                            const recallText = data.senderName ? `Tin nhắn đã được ${data.senderName} thu hồi` : "Tin nhắn đã được thu hồi";
                            queryClient.setQueryData(['messages', data.chatId, 0], (old: any[] | undefined) => {
                                if (!old) return old;
                                return old.map((m: any) => m.id === data.messageId
                                    ? { ...m, deleted: true, content: recallText, text: recallText }
                                    : m
                                );
                            });
                        }
                    });
                } catch (e) { }
            }, 500);
        };

        client.onDisconnect = () => setIsConnected(false);
        client.activate();
        clientRef.current = client;

        return () => {
            if (clientRef.current) {
                clientRef.current.deactivate();
                clientRef.current = null;
            }
        };
    }, [token, queryClient]);

    const setActiveChat = (chatId: string | null) => {
        activeChatIdRef.current = chatId;
    };

    const subscribeToChat = (chatId: string, callback: (data: any) => void) => {
        if (!chatListeners.current[chatId]) chatListeners.current[chatId] = [];
        chatListeners.current[chatId].push(callback);
        return () => {
            if (chatListeners.current[chatId]) {
                chatListeners.current[chatId] = chatListeners.current[chatId].filter(cb => cb !== callback);
            }
        };
    };

    const publish = (destination: string, body: any) => {
        if (clientRef.current?.connected) {
            clientRef.current.publish({ destination, body: JSON.stringify(body) });
        }
    };

    return (
        <SocketContext.Provider value={{ isConnected, publish, subscribeToChat, setActiveChat }}>
            {children}
        </SocketContext.Provider>
    );
}

export function useSocket() {
    const context = useContext(SocketContext);
    if (!context) throw new Error('useSocket must be used within a SocketProvider');
    return context;
}
