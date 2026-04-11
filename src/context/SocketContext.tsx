import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { useAuthStore } from '@/store';
import { useQueryClient } from '@tanstack/react-query';

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

            client.subscribe('/user/queue/messages', (message) => {
                const msgData = JSON.parse(message.body);
                console.log('📧 New message received:', msgData);
                queryClient.invalidateQueries({ queryKey: ['chats'] });
                if (msgData.chatId) {
                    queryClient.invalidateQueries({ queryKey: ['messages', msgData.chatId] });
                }
            });

            client.subscribe('/user/queue/notifications', (notif) => {
                console.log('🔔 Notification:', JSON.parse(notif.body));
                queryClient.invalidateQueries({ queryKey: ['chats'] });
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
