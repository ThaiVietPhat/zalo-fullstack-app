// src/components/ChatScreen/MessageItem/index.tsx — Tương tác & Đa phương tiện
import React, { useState } from 'react';
import { 
  View, 
  Text, 
  Image, 
  TouchableOpacity, 
  StyleSheet, 
  Alert,
  Modal,
  TouchableWithoutFeedback,
  Dimensions
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { MessageState } from '@/types/type';
import { getImageUrl } from '@/lib/utils';
import { useRecallMessage, useDeleteMessage } from '@/hooks/useMessages';

interface Message {
  id: string;
  chatId?: string;
  senderId: string;
  text?: string;
  image?: string;
  time: string;
  type: 'TEXT' | 'IMAGE' | 'VIDEO' | 'AUDIO' | 'FILE';
  state?: MessageState;
  reactions?: Array<{ emoji: string; userId: string; userFullName?: string }>;
}

const { width: screenWidth } = Dimensions.get('window');

const emojis = ['❤️', '👍', '😆', '😲', '😢', '😡'];

const MessageItem = ({ item, isMe }: { item: Message; isMe: boolean }) => {
  const isDeleted = item.text === 'Tin nhắn đã bị xóa' || item.text === 'Tin nhắn đã bị thu hồi';
  const chatId = item.chatId || "";
  
  const { mutate: recall } = useRecallMessage(chatId);
  const { mutate: deleteForMe } = useDeleteMessage(chatId);

  const [showMenu, setShowMenu] = useState(false);

  const handleLongPress = () => {
    if (isDeleted) return;
    setShowMenu(true);
  };

  const handleAction = (action: string) => {
    setShowMenu(false);
    if (action === 'RECALL') {
      Alert.alert("Thu hồi", "Bạn có muốn thu hồi tin nhắn này?", [
        { text: "Hủy", style: "cancel" },
        { text: "Thu hồi", style: "destructive", onPress: () => recall(item.id) }
      ]);
    } else if (action === 'DELETE') {
      deleteForMe(item.id);
    }
  };

  // Trạng thái gửi (Sent, Delivered, Seen)
  const StateIcon = () => {
    if (!isMe || !item.state || isDeleted) return null;
    if (item.state === 'SEEN') return <Ionicons name="checkmark-done" size={14} color="#0068FF" />;
    if (item.state === 'DELIVERED') return <Ionicons name="checkmark-done" size={14} color="#94A3B8" />;
    return <Ionicons name="checkmark" size={14} color="#94A3B8" />;
  };

  return (
    <View className={`flex-row mb-3 ${isMe ? 'justify-end' : 'justify-start'} px-3`}>
      {/* Avatar người khác */}
      {!isMe && (
        <View className="mr-2 self-end pb-1">
          <View className="w-8 h-8 rounded-full bg-blue-100 items-center justify-center">
            <Ionicons name="person" size={18} color="#0068FF" />
          </View>
        </View>
      )}

      <View className={`${isMe ? 'items-end' : 'items-start'} max-w-[80%]`}>
        <TouchableOpacity 
          onLongPress={handleLongPress} 
          activeOpacity={0.9}
        >
          {/* Bong bóng tin nhắn */}
          <View
            style={[
              styles.bubble,
              isMe ? styles.myBubble : styles.otherBubble,
              isDeleted && styles.deletedBubble
            ]}
          >
            {/* 1. Hình ảnh */}
            {item.type === 'IMAGE' && item.image && !isDeleted ? (
              <View className="mb-1 rounded-xl overflow-hidden">
                <Image
                  source={{ uri: getImageUrl(item.image) }}
                  className="w-56 h-72"
                  resizeMode="cover"
                />
              </View>
            ) : item.type === 'VIDEO' ? (
              <View className="mb-1 rounded-xl overflow-hidden bg-black/80 w-56 h-40 items-center justify-center">
                <Ionicons name="play-circle" size={48} color="white" />
                <Text className="text-white text-xs mt-1">VIDEO</Text>
              </View>
            ) : item.type === 'FILE' ? (
              <View className="flex-row items-center bg-gray-50 border border-gray-100 p-3 rounded-xl mb-1">
                <Ionicons name="document-text" size={32} color="#0068FF" />
                <View className="ml-2 flex-1">
                  <Text className="text-gray-800 font-JakartaMedium text-sm" numberOfLines={1}>{item.text || 'Document'}</Text>
                  <Text className="text-gray-400 text-xs">FILE</Text>
                </View>
              </View>
            ) : (
              <Text
                style={[
                  styles.messageText,
                  isDeleted && styles.deletedText,
                  isMe ? styles.myText : styles.otherText
                ]}
              >
                {item.text}
              </Text>
            )}

            {/* Hàng chứa thời gian và tim */}
            {!isDeleted && (
              <View style={styles.footerRow}>
                <View style={styles.timeWrapper}>
                  <Text style={[styles.timeText, isMe ? styles.myTime : styles.otherTime]}>
                    {item.time}
                  </Text>
                  <StateIcon />
                </View>

                {/* Reaction heart (Quick Toggle) */}
                <TouchableOpacity className="ml-2">
                  <Ionicons 
                    name={item.reactions?.some(r => r.emoji === '❤️') ? "heart" : "heart-outline"} 
                    size={16} 
                    color={isMe ? "#0068FF" : "#8E8E93"} 
                  />
                </TouchableOpacity>
              </View>
            )}
          </View>
        </TouchableOpacity>

        {/* Reaction badge (Horizontal display) */}
        {item.reactions && item.reactions.length > 0 && (
          <View className="flex-row mt-[-5px] bg-white border border-gray-100 rounded-full px-1.5 py-0.5 shadow-sm">
            {item.reactions.slice(0, 3).map((r, i) => (
              <Text key={i} style={{ fontSize: 12 }}>{r.emoji}</Text>
            ))}
            {item.reactions.length > 1 && (
              <Text className="text-[10px] text-gray-500 ml-0.5"> {item.reactions.length}</Text>
            )}
          </View>
        )}
      </View>

      {/* ── Modal Menu (Long Press) ── */}
      <Modal visible={showMenu} transparent animationType="fade">
        <TouchableWithoutFeedback onPress={() => setShowMenu(false)}>
          <View className="flex-1 bg-black/30 justify-center items-center px-6">
            <View className="bg-white rounded-3xl p-6 w-full max-w-sm">
              <Text className="text-xl font-JakartaBold mb-6 text-gray-800">Tùy chọn tin nhắn</Text>
              
              <View className="flex-row justify-between mb-8">
                {emojis.map((emoji) => (
                  <TouchableOpacity key={emoji} onPress={() => setShowMenu(false)} className="bg-gray-50 p-2.5 rounded-full">
                    <Text className="text-2xl">{emoji}</Text>
                  </TouchableOpacity>
                ))}
              </View>

              <View className="gap-2">
                <MenuAction icon="copy-outline" label="Sao chép" onPress={() => setShowMenu(false)} />
                <MenuAction icon="arrow-redo-outline" label="Chuyển tiếp" onPress={() => setShowMenu(false)} />
                {isMe && <MenuAction icon="refresh-outline" label="Thu hồi" color="#EF4444" onPress={() => handleAction('RECALL')} />}
                <MenuAction icon="trash-outline" label="Xóa phía mình" color="#EF4444" onPress={() => handleAction('DELETE')} />
              </View>
            </View>
          </View>
        </TouchableWithoutFeedback>
      </Modal>
    </View>
  );
};

const MenuAction = ({ icon, label, color = "#1F2937", onPress }: { icon: any; label: string; color?: string; onPress: () => void }) => (
  <TouchableOpacity onPress={onPress} className="flex-row items-center py-3.5 border-b border-gray-50">
    <Ionicons name={icon} size={22} color={color} />
    <Text className="ml-4 font-JakartaMedium text-base" style={{ color }}>{label}</Text>
  </TouchableOpacity>
);

const styles = StyleSheet.create({
  bubble: {
    paddingHorizontal: 12,
    paddingTop: 8,
    paddingBottom: 4,
    borderRadius: 18,
    borderWidth: 0.5,
    minWidth: 80,
  },
  myBubble: { backgroundColor: '#EAF6FF', borderColor: '#D5E9F7', borderBottomRightRadius: 4 },
  otherBubble: { backgroundColor: '#FFFFFF', borderColor: '#E1E6E9', borderBottomLeftRadius: 4 },
  deletedBubble: { backgroundColor: '#F9FAFB', borderColor: '#F3F4F6' },
  messageText: { fontSize: 16, fontFamily: 'Jakarta', lineHeight: 22 },
  myText: { color: '#1F2937' },
  otherText: { color: '#1F2937' },
  deletedText: { color: '#9CA3AF', fontStyle: 'italic' },
  footerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 },
  timeWrapper: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  timeText: { fontSize: 10, fontFamily: 'Jakarta-Medium' },
  myTime: { color: '#6B7280' },
  otherTime: { color: '#94A3B8' },
});

export default MessageItem;
