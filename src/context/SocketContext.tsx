import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '@/store';
import { useQueryClient } from '@tanstack/react-query';
import { Platform } from 'react-native';
import { Buffer } from 'buffer';

// @ts-ignore
if (typeof BigInt === 'undefined') {
    global.BigInt = require('big-integer');
}

// Bổ sung polyfill cho TextEncoder và TextDecoder (rất quan trọng cho STOMP)
if (typeof TextEncoder === 'undefined') {
    const { TextEncoder, TextDecoder } = require('text-encoding');
    global.TextEncoder = TextEncoder;
    global.TextDecoder = TextDecoder;
}

global.Buffer = Buffer;

interface SocketContextType {
    isConnected: boolean;
    publish: (destination: string, body: any) => void;
}

const SocketContext = createContext<SocketContextType | undefined>(undefined);

export const SocketProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const { token, user } = useAuthStore();
    const [isConnected, setIsConnected] = useState(false);
    const clientRef = useRef<Client | null>(null);
    const queryClient = useQueryClient();

    useEffect(() => {
        if (!token) {
            if (clientRef.current) {
                clientRef.current.deactivate();
            }
            return;
        }

        const baseUrl = process.env.EXPO_PUBLIC_SERVER_URL || "";
        const socketUrl = baseUrl.split('/api/v1')[0] + '/ws';

        const client = new Client({
            webSocketFactory: () => new SockJS(socketUrl),
            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },
            debug: (str) => {
                if (__DEV__) console.log('📡 [STOMP] ' + str);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = (frame) => {
            setIsConnected(true);
            console.log('✅ [Socket] Connected: ' + frame.headers['user-name']);

            // Subscribe to private messages
            client.subscribe('/user/queue/messages', (message) => {
                const msgData = JSON.parse(message.body);
                console.log('📧 New message received:', msgData);

                // Invalidate React Query to refresh UI
                queryClient.invalidateQueries({ queryKey: ['chats'] });
                if (msgData.chatId) {
                    queryClient.invalidateQueries({ queryKey: ['messages', msgData.chatId] });
                }
            });

            // Subscribe to notifications or other events if needed
            client.subscribe('/user/queue/notifications', (notif) => {
                console.log('🔔 Notification:', JSON.parse(notif.body));
            });
        };

        client.onStompError = (frame) => {
            console.error('❌ [Socket] STOMP Error:', frame.headers['message']);
        };

        client.onDisconnect = () => {
            setIsConnected(false);
            console.log('📴 [Socket] Disconnected');
        };

        client.activate();
        clientRef.current = client;

        return () => {
            client.deactivate();
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
