// src/components/ChatScreen/ChatInput/index.tsx — Hỗ trợ Text, Media, File
import React, { useState } from 'react';
import {
  View,
  TextInput,
  TouchableOpacity,
  Platform,
  Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import * as ImagePicker from 'expo-image-picker';
import * as DocumentPicker from 'expo-document-picker';
import { useSendMessage, useUploadMedia } from '@/hooks/useMessages';

interface ChatInputProps {
  chatId: string;
}

const ChatInput = ({ chatId }: ChatInputProps) => {
  const [text, setText] = useState('');
  const insets = useSafeAreaInsets();
  const { mutate: sendMessage, isPending: isSendingText } = useSendMessage();
  const { mutate: uploadMedia, isPending: isUploading } = useUploadMedia(chatId);

  const handleSend = () => {
    const trimmed = text.trim();
    if (!trimmed || isSendingText || isUploading) return;

    sendMessage({
      chatId,
      content: trimmed,
      type: 'TEXT',
    });
    setText('');
  };

  const handlePickImage = async () => {
    try {
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.All, // Cả ảnh và video
        allowsEditing: false,
        quality: 0.8,
        allowsMultipleSelection: true,
      });

      if (!result.canceled && result.assets.length > 0) {
        for (const asset of result.assets) {
          const formData = new FormData();
          const uri = asset.uri;
          const fileName = uri.split('/').pop() || 'media';
          const fileType = asset.type === 'video' ? 'video/mp4' : 'image/jpeg';

          // @ts-ignore
          formData.append('file', {
            uri,
            name: fileName,
            type: fileType,
          });

          uploadMedia(formData);
        }
      }
    } catch (error) {
      Alert.alert("Lỗi", "Không thể chọn hình ảnh/video");
    }
  };

  const handlePickDocument = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: '*/*',
        copyToCacheDirectory: true,
      });

      if (!result.canceled && result.assets && result.assets.length > 0) {
        const asset = result.assets[0];
        const formData = new FormData();
        
        // @ts-ignore
        formData.append('file', {
          uri: asset.uri,
          name: asset.name,
          type: asset.mimeType || 'application/octet-stream',
        });

        uploadMedia(formData);
      }
    } catch (error) {
      Alert.alert("Lỗi", "Không thể chọn tài liệu");
    }
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
            <TouchableOpacity className="p-2" onPress={handlePickDocument}>
              <Ionicons name="ellipsis-horizontal" size={26} color="#636E72" />
            </TouchableOpacity>
            <TouchableOpacity className="p-2">
              <Ionicons name="mic-outline" size={26} color="#636E72" />
            </TouchableOpacity>
            <TouchableOpacity className="p-2" onPress={handlePickImage}>
              <Ionicons name="image-outline" size={28} color="#636E72" />
            </TouchableOpacity>
          </View>
        ) : (
          <TouchableOpacity className="p-2" onPress={handleSend} disabled={isSendingText || isUploading}>
            <Ionicons name="send" size={26} color={(isSendingText || isUploading) ? '#94a3b8' : '#0068FF'} />
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
};

export default ChatInput;
