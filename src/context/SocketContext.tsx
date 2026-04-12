import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { useAuthStore } from '@/store';
import { useQueryClient } from '@tanstack/react-query';
import { router } from 'expo-router';
import { showLogoutAlert } from '@/lib/logout-guard';

// Polyfill TextEncoder/TextDecoder cho STOMP trên React Native
if (typeof TextEncoder === 'undefined') {
    const { TextEncoder, TextDecoder } = require('text-encoding');
    global.TextEncoder = TextEncoder;
    global.TextDecoder = TextDecoder;
}

interface SocketContextType {
    isConnected: boolean;
    publish: (destination: string, body: any) => void;
}

const SocketContext = createContext<SocketContextType | undefined>(undefined);

export const SocketProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const { token } = useAuthStore();
    const [isConnected, setIsConnected] = useState(false);
    const clientRef = useRef<Client | null>(null);
    const queryClient = useQueryClient();

    useEffect(() => {
        if (clientRef.current) {
            clientRef.current.deactivate();
            clientRef.current = null;
        }

        if (!token) return;

        // Chuyển http(s):// -> ws(s):// và thêm /websocket (SockJS raw endpoint)
        const baseUrl = process.env.EXPO_PUBLIC_SOCKET_URL || process.env.EXPO_PUBLIC_SERVER_URL?.split('/api/v1')[0] + '/ws';
        const wsUrl = baseUrl!
            .replace(/^http:\/\//, 'ws://')
            .replace(/^https:\/\//, 'wss://')
            .replace(/\/ws$/, '/ws/websocket'); // SockJS raw WebSocket path

        const client = new Client({
            // Dùng native WebSocket của React Native thay vì SockJS
            webSocketFactory: () => new WebSocket(wsUrl),
            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },
            debug: (str) => {
                if (__DEV__) console.log('📡 [STOMP]', str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = (frame) => {
            setIsConnected(true);
            console.log('✅ [Socket] Connected:', frame.headers['user-name']);

            // ─── Instant Update cho danh sách chat (Home Screen) ───────────────────
            const updateChatListCache = (chatId: string, content: string, createdAt: string, isGroup = false) => {
                const queryKey = isGroup ? ['groups'] : ['chats'];
                queryClient.setQueryData(queryKey, (oldList: any[]) => {
                    if (!oldList) return oldList;
                    
                    const newList = [...oldList];
                    const index = newList.findIndex(item => item.id === chatId);
                    
                    if (index !== -1) {
                        const updatedItem = { 
                            ...newList[index], 
                            lastMessage: content,
                            lastMessageTime: createdAt,
                            unreadCount: (newList[index].unreadCount || 0) + 1
                        };
                        // Đẩy lên đầu danh sách
                        newList.splice(index, 1);
                        newList.unshift(updatedItem);
                    }
                    return newList;
                });
                // Force invalidation để đồng bộ chuẩn xác với DB
                queryClient.invalidateQueries({ queryKey });
            };

            // Subscribe tin nhắn cá nhân
            client.subscribe('/user/queue/messages', (message) => {
                const msgData = JSON.parse(message.body);
                console.log('📧 New message received:', msgData);
                updateChatListCache(msgData.chatId, msgData.content, msgData.createdAt, false);
                if (msgData.chatId) {
                    queryClient.invalidateQueries({ queryKey: ['messages', msgData.chatId] });
                }
            });

            // Subscribe tin nhắn nhóm (Phải subscribe vào từng topic của nhóm)
            import('@/api/group').then(({ getMyGroups }) => {
                getMyGroups().then(groups => {
                    if (!groups) return;
                    groups.forEach(group => {
                        client.subscribe(`/topic/group/${group.id}`, (message) => {
                            const msgData = JSON.parse(message.body);
                            console.log('👥 Group message received:', msgData);
                            updateChatListCache(group.id, msgData.content, msgData.createdAt || msgData.createdDate, true);
                            queryClient.invalidateQueries({ queryKey: ['group-messages', group.id] });
                        });
                    });
                });
            });

            client.subscribe('/user/queue/notifications', (notif) => {
                console.log('🔔 Notification:', JSON.parse(notif.body));
                queryClient.invalidateQueries({ queryKey: ['chats'] });
                queryClient.invalidateQueries({ queryKey: ['groups'] });
            });

            client.subscribe('/user/queue/force-logout', (message) => {
                console.warn('🚨 Force logout triggered from another device');
                showLogoutAlert(
                  'Đăng xuất',
                  'Tài khoản của bạn đã được đăng nhập ở một thiết bị khác.',
                  () => {
                    useAuthStore.getState().logout();
                    client.deactivate();
                    router.replace('/(auth)/sign-in');
                  }
                );
            });
        };

        client.onStompError = (frame) => {
            console.warn('⚠️ [Socket] STOMP Error:', frame.headers['message']);
        };

        client.onWebSocketError = (event) => {
            console.warn('⚠️ [Socket] WebSocket Error:', event);
        };

        client.onDisconnect = () => {
            setIsConnected(false);
            console.log('📴 [Socket] Disconnected');
        };

        client.activate();
        clientRef.current = client;

        return () => {
            client.deactivate();
            clientRef.current = null;
        };
    }, [token, queryClient]);

    const publish = (destination: string, body: any) => {
        if (clientRef.current?.connected) {
            clientRef.current.publish({
                destination,
                body: JSON.stringify(body),
            });
        } else {
            console.warn('⚠️ [Socket] Cannot publish: not connected');
        }
    };

    return (
        <SocketContext.Provider value={{ isConnected, publish }}>
            {children}
        </SocketContext.Provider>
    );
};

export const useSocket = () => {
    const context = useContext(SocketContext);
    if (!context) throw new Error('useSocket must be used within a SocketProvider');
    return context;
};
