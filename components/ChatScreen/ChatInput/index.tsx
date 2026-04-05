// components/ChatScreen/ChatInput/index.tsx — đồng bộ với MessageDto.java
// Gửi tin nhắn qua POST /api/v1/message (REST fallback)
import React, { useState } from 'react';
import {
  View,
  TextInput,
  TouchableOpacity,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useSendMessage } from '@/hooks/useMessages';

interface ChatInputProps {
  chatId: string;
}

const ChatInput = ({ chatId }: ChatInputProps) => {
  const [text, setText] = useState('');
  const insets = useSafeAreaInsets();
  const { mutate: sendMessage, isPending } = useSendMessage();

  const handleSend = () => {
    const trimmed = text.trim();
    if (!trimmed || isPending) return;

    sendMessage({
      chatId,
      content: trimmed,
      type: 'TEXT',
    });
    setText('');
  };

  return (
    <View
      className="bg-white border-t border-[#E1E6E9] flex-row items-center px-2 py-2"
      style={{ paddingBottom: Math.max(insets.bottom, 10) }}
    >
      <TouchableOpacity className="p-2">
        <Ionicons name="happy-outline" size={28} color="#636E72" />
      </TouchableOpacity>

      <View className="flex-1 min-h-[40px] px-3 justify-center">
        <TextInput
          placeholder="Tin nhắn"
          placeholderTextColor="#94a3b8"
          value={text}
          onChangeText={setText}
          className="text-lg font-Jakarta text-gray-800"
          multiline
          returnKeyType="send"
          onSubmitEditing={handleSend}
          blurOnSubmit={false}
        />
      </View>

      <View className="flex-row items-center">
        {!text.trim() ? (
          <View className="flex-row items-center">
            <TouchableOpacity className="p-2">
              <Ionicons name="ellipsis-horizontal" size={24} color="#636E72" />
            </TouchableOpacity>
            <TouchableOpacity className="p-2">
              <Ionicons name="mic-outline" size={24} color="#636E72" />
            </TouchableOpacity>
            <TouchableOpacity className="p-2">
              <Ionicons name="image-outline" size={24} color="#636E72" />
            </TouchableOpacity>
          </View>
        ) : (
          <TouchableOpacity className="p-2" onPress={handleSend} disabled={isPending}>
            <Ionicons name="send" size={26} color={isPending ? '#94a3b8' : '#0068FF'} />
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
};

export default ChatInput;
