// src/components/ChatScreen/ChatInput/index.tsx — Hỗ trợ Text, Media, File, Emoji, Audio (Optimistic UI)
import React, { useState, useRef } from 'react';
import {
  View,
  TextInput,
  TouchableOpacity,
  Platform,
  Alert,
  FlatList,
  Text,
  Keyboard,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import * as ImagePicker from 'expo-image-picker';
import * as DocumentPicker from 'expo-document-picker';
import { Audio } from 'expo-av';
import { useSendMessage, useUploadMedia } from '@/hooks/useMessages';
import { useSendGroupMessage, useUploadGroupMedia } from '@/hooks/useGroup';

interface ChatInputProps {
  chatId: string;
  isGroup?: boolean;
}

const COMMON_EMOJIS = [
    '😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '😇', '🙂', '🙃', '😉', '😌', '😍', '🥰',
    '😘', '😗', '😙', '😚', '😋', '😛', '😝', '😜', '🤪', '🤨', '🧐', '🤓', '😎', '🤩', '🥳', '😏',
    '😒', '😞', '😔', '😟', '😕', '🙁', '☹️', '😣', '😖', '😫', '😩', '🥺', '😢', '😭', '😤', '😠',
    '😡', '🤬', '🤯', '😳', '🥵', '🥶', '😱', '😨', '😰', '😥', '😓', '🤗', '🤔', '🤭', '🤫', '🤥',
    '😶', '😐', '😑', '😬', '🙄', '😯', '😦', '😧', '😮', '😲', '🥱', '😴', '🤤', '😪', '😵', '🤐',
    '🥴', '🤢', '🤮', '🤧', '😷', '🤒', '🤕', '🤑', '🤠', '😈', '👿', '👹', '👺', '🤡', '💩', '👻',
    '💀', '☠️', '👽', '👾', '🤖', '🎃', '😺', '😸', '😻', '😼', '😽', '🙀', '😿', '😾', '❤️', '🧡',
    '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟'
  ];

const ChatInput = ({ chatId, isGroup = false }: ChatInputProps) => {
  const [text, setText] = useState('');
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [recording, setRecording] = useState<Audio.Recording | null>(null);
  const [isRecording, setIsRecording] = useState(false);
  const insets = useSafeAreaInsets();

  const { mutate: sendPrivateMessage } = useSendMessage();
  const { mutate: uploadPrivateMedia } = useUploadMedia(chatId);
  const { mutate: sendGroupMessage } = useSendGroupMessage(chatId);
  const { mutate: uploadGroupMedia } = useUploadGroupMedia(chatId);

  const sendMessageFunc = isGroup ? sendGroupMessage : sendPrivateMessage;
  const uploadMediaFunc = isGroup ? uploadGroupMedia : uploadPrivateMedia;

  const handleSend = () => {
    const trimmed = text.trim();
    if (!trimmed) return;

    if (isGroup) {
      sendGroupMessage({ content: trimmed, type: 'TEXT' });
    } else {
      sendPrivateMessage({ chatId, content: trimmed, type: 'TEXT' });
    }
    setText('');
    setShowEmojiPicker(false);
  };

  const handlePickImage = async () => {
    try {
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.All,
        allowsEditing: false,
        quality: 0.8,
        allowsMultipleSelection: true,
      });

      if (!result.canceled && result.assets.length > 0) {
        for (const asset of result.assets) {
          const uri = asset.uri;
          let fileType = asset.mimeType || (asset.type === 'video' ? 'video/mp4' : 'image/jpeg');
          if (fileType === 'video/quicktime') fileType = 'video/mp4';
          
          const fileName = asset.fileName || (uri.split('/').pop() || (asset.type === 'video' ? 'video.mp4' : 'image.jpeg')).replace('.mov', '.mp4');

          const formData = new FormData();
          // @ts-ignore
          formData.append('file', {
            uri: Platform.OS === 'android' ? uri : uri.replace('file://', ''),
            name: fileName,
            type: fileType,
          });

          uploadMediaFunc({ formData, localUri: uri, fileType, fileName });
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
        const uri = asset.uri;
        const name = asset.name;
        let fileType = asset.mimeType || 'application/octet-stream';

        // Tự động nhận diện loại file dựa trên đuôi nếu mimeType bị thiếu hoặc chung chung
        const ext = name.split('.').pop()?.toLowerCase();
        if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'heic'].includes(ext || '')) {
          fileType = 'image/jpeg';
        } else if (['mp4', 'mov', 'm4v'].includes(ext || '')) {
          fileType = 'video/mp4';
        } else if (['m4a', 'mp3', 'wav', 'aac'].includes(ext || '')) {
          fileType = 'audio/m4a';
        }

        const formData = new FormData();
        // @ts-ignore
        formData.append('file', {
          uri: asset.uri,
          name: asset.name,
          type: fileType,
        });

        uploadMediaFunc({ formData, localUri: asset.uri, fileType, fileName: asset.name });
      }
    } catch (error) {
      Alert.alert("Lỗi", "Không thể chọn tài liệu");
    }
  };

  const startRecording = async () => {
    try {
      const permission = await Audio.requestPermissionsAsync();
      if (permission.status !== 'granted') return;

      await Audio.setAudioModeAsync({ allowsRecordingIOS: true, playsInSilentModeIOS: true });
      const { recording } = await Audio.Recording.createAsync(Audio.RecordingOptionsPresets.HIGH_QUALITY);
      setRecording(recording);
      setIsRecording(true);
    } catch (err) {
      Alert.alert('Lỗi', 'Không thể khởi động ghi âm');
    }
  };

  const stopRecording = async () => {
    if (!recording) return;
    setIsRecording(false);
    try {
      await recording.stopAndUnloadAsync();
      const uri = recording.getURI();
      if (uri) {
        const fileName = `audio_${Date.now()}.m4a`;
        const formData = new FormData();
        // @ts-ignore
        formData.append('file', { uri, name: fileName, type: 'audio/m4a' });
        uploadMediaFunc({ formData, localUri: uri, fileType: 'audio/m4a', fileName });
      }
    } catch (err) { }
    setRecording(null);
  };

  return (
    <View className="bg-white border-t border-[#E1E6E9]">
      {isRecording && (
        <View className="flex-row items-center justify-between px-4 py-2 bg-red-50">
          <View className="flex-row items-center">
            <View className="w-2 h-2 rounded-full bg-red-500 animate-pulse mr-2" />
            <Text className="text-red-500 font-JakartaBold">Đang ghi âm...</Text>
          </View>
          <TouchableOpacity onPress={() => { setRecording(null); setIsRecording(false); }}>
             <Text className="text-gray-500">Hủy</Text>
          </TouchableOpacity>
        </View>
      )}

      <View className="flex-row items-end px-2 py-2" style={{ minHeight: 60 }}>
        <TouchableOpacity className="p-2" onPress={() => setShowEmojiPicker(!showEmojiPicker)}>
          <Ionicons
            name={(showEmojiPicker ? "keypad-outline" : "happy-outline") as any}
            size={28}
            color={showEmojiPicker ? "#0068FF" : "#636E72"}
          />
        </TouchableOpacity>

        <View className="flex-1 bg-[#F1F2F6] rounded-2xl px-3 py-2 mx-1 border border-[#E1E6E9]">
          <TextInput
            placeholder="Tin nhắn"
            placeholderTextColor="#94a3b8"
            value={text}
            onChangeText={setText}
            onFocus={() => setShowEmojiPicker(false)}
            className="text-lg font-Jakarta text-gray-800"
            style={{ maxHeight: 120 }}
            multiline
          />
        </View>

        <View className="flex-row items-center">
          {!text.trim() ? (
            <View className="flex-row items-center">
              <TouchableOpacity className="p-2" onPress={handlePickDocument}>
                <Ionicons name="ellipsis-horizontal" size={26} color="#636E72" />
              </TouchableOpacity>
              
              <TouchableOpacity 
                className={`p-2 rounded-full ${isRecording ? 'bg-red-100' : ''}`}
                onPressIn={startRecording}
                onPressOut={stopRecording}
              >
                <Ionicons name="mic-outline" size={28} color={isRecording ? "#EF4444" : "#636E72"} />
              </TouchableOpacity>

              <TouchableOpacity className="p-2" onPress={handlePickImage}>
                <Ionicons name="image-outline" size={28} color="#636E72" />
              </TouchableOpacity>
            </View>
          ) : (
            <TouchableOpacity className="p-2" onPress={handleSend}>
              <Ionicons name="send" size={26} color="#0068FF" />
            </TouchableOpacity>
          )}
        </View>
      </View>

      {showEmojiPicker && (
        <View style={{ height: 250, paddingBottom: 10 }}>
          <FlatList
            data={COMMON_EMOJIS}
            renderItem={({ item }) => (
              <TouchableOpacity
                style={{ flex: 1, paddingVertical: 10, alignItems: 'center' }}
                onPress={() => setText(prev => prev + item)}
              >
                <Text style={{ fontSize: 26 }}>{item}</Text>
              </TouchableOpacity>
            )}
            numColumns={8}
            keyExtractor={(item) => item}
          />
        </View>
      )}
      <View style={{ height: Math.max(insets.bottom, 10), backgroundColor: 'white' }} />
    </View>
  );
};

export default ChatInput;
