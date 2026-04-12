/* eslint-disable */
import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  Image,
  TouchableOpacity,
  StyleSheet,
  Alert,
  Modal,
  TouchableWithoutFeedback,
  Dimensions,
  ActivityIndicator
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as WebBrowser from 'expo-web-browser';
import { WebView } from 'react-native-webview';
import * as MediaLibrary from 'expo-media-library';
import { cacheDirectory, downloadAsync } from 'expo-file-system/legacy';
import { Audio, Video, ResizeMode } from 'expo-av';
import ForwardModal from '../ForwardModal';
import { ChatMessage } from '@/types/type';
import { getImageUrl } from '@/lib/utils';
import { useRecallMessage, useDeleteMessage } from '@/hooks/useMessages';
import { useReactToMessage, useReactToGroupMessage } from '@/hooks/useReaction';
import { useRecallGroupMessage, useDeleteGroupMessage } from '@/hooks/useGroup';

const emojis = ['❤️', '👍', '😆', '😲', '😢', '😡'];

const MessageItem = ({ item, isMe, isGroup }: { item: ChatMessage; isMe: boolean; isGroup: boolean }) => {
  const isDeleted = item.deleted === true || item.text === 'Tin nhắn đã bị xóa' || (item.text || '').includes('thu hồi') || (item.content || '').includes('thu hồi');
  const chatId = item.chatId || "";

  const { mutate: recallPrivate } = useRecallMessage(chatId);
  const { mutate: deletePrivate } = useDeleteMessage(chatId);
  const { mutate: recallGroup } = useRecallGroupMessage(chatId);
  const { mutate: deleteGroup } = useDeleteGroupMessage(chatId);
  const { mutate: reactPrivate } = useReactToMessage(chatId);
  const { mutate: reactGroup } = useReactToGroupMessage(chatId);

  const react = isGroup ? reactGroup : reactPrivate;
  const doRecall = isGroup ? recallGroup : recallPrivate;
  const doDelete = isGroup ? deleteGroup : deletePrivate;

  const [showMenu, setShowMenu] = useState(false);
  const [showForward, setShowForward] = useState(false);
  const [isPlayingMedia, setIsPlayingMedia] = useState(false);
  const [sound, setSound] = useState<Audio.Sound | null>(null);
  const [isAudioPlaying, setIsAudioPlaying] = useState(false);
  const [audioLoading, setAudioLoading] = useState(false);

  useEffect(() => {
    return () => {
      if (sound) sound.unloadAsync();
    };
  }, [sound]);

  const handlePlayAudio = async () => {
    const mUrl = getImageUrl(item.image || item.mediaUrl);
    if (!mUrl) return;

    if (sound) {
      try {
        const status = await sound.getStatusAsync();
        if (!status.isLoaded) {
          setSound(null); // Force reload if instance unmounted or corrupted
        } else {
          if (isAudioPlaying) {
            await sound.pauseAsync();
            setIsAudioPlaying(false);
          } else {
            await Audio.setAudioModeAsync({
              allowsRecordingIOS: false,
              playsInSilentModeIOS: true,
              shouldDuckAndroid: true,
              playThroughEarpieceAndroid: false,
              staysActiveInBackground: false,
            });
            if (status.didJustFinish || status.positionMillis === status.durationMillis) {
              await sound.replayAsync();
            } else {
              await sound.playAsync();
            }
            setIsAudioPlaying(true);
          }
          return;
        }
      } catch (e) {
        setSound(null);
      }
    }

    try {
      setAudioLoading(true);
      await Audio.setAudioModeAsync({
        allowsRecordingIOS: false,
        playsInSilentModeIOS: true,
        shouldDuckAndroid: true,
        playThroughEarpieceAndroid: false,
        staysActiveInBackground: false,
      });

      const { sound: newSound } = await Audio.Sound.createAsync({ uri: mUrl }, { shouldPlay: true });
      setSound(newSound);
      setIsAudioPlaying(true);
      newSound.setOnPlaybackStatusUpdate((status: any) => {
        if (status.didJustFinish) {
          setIsAudioPlaying(false);
          newSound.setPositionAsync(0);
        }
      });
    } catch (error) {
      Alert.alert("Lỗi", "Không thể phát âm thanh");
    } finally {
      setAudioLoading(false);
    }
  };

  const handleDownload = async () => {
    setShowMenu(false);
    const mUrl = getImageUrl(item.image || item.mediaUrl);
    if (!mUrl) return;

    if (item.type === 'IMAGE' || item.type === 'VIDEO') {
      try {
        const { status } = await MediaLibrary.requestPermissionsAsync();
        if (status !== 'granted') return Alert.alert("Lỗi", "Cần quyền");
        const dir = cacheDirectory;
        if (!dir) return Alert.alert("Lỗi", "Không tìm thấy bộ nhớ tạm");

        const fileName = (item.image || item.mediaUrl || 'm').split('/').pop() || 'media';
        const downloadPath = dir + (dir.endsWith('/') ? '' : '/') + fileName;
        const downloadResult = await downloadAsync(mUrl, downloadPath);
        if (downloadResult.status === 200) {
          const asset = await MediaLibrary.createAssetAsync(downloadResult.uri);
          await MediaLibrary.createAlbumAsync("Zalo Clone", asset, false);
          Alert.alert("Thành công", "Đã lưu");
        }
      } catch (e) { Alert.alert("Lỗi", "Không thể lưu"); }
    } else { WebBrowser.openBrowserAsync(mUrl); }
  };

  const renderContent = () => {
    if (isDeleted) return <Text style={[styles.messageText, styles.deletedText]}>{item.text || item.content}</Text>;

    const mUrl = getImageUrl(item.image || item.mediaUrl);
    const fileName = (item.text || item.content || '').toLowerCase();
    const isVideo = item.type === 'VIDEO' || fileName.endsWith('.mp4');
    const isAudio = item.type === 'AUDIO' || item.type === 'VOICE' || fileName.endsWith('.m4a');
    const isImage = item.type === 'IMAGE' || fileName.endsWith('.jpg') || fileName.endsWith('.png');

    if (isImage && mUrl) {
      return (
        <View style={styles.mediaContainer}>
          <Image source={{ uri: mUrl }} style={styles.imageBox} resizeMode="cover" />
          {item.state === 'SENDING' && <View style={styles.mediaOverlay}><ActivityIndicator color="white" /></View>}
        </View>
      );
    }

    if (isVideo && mUrl) {
      if (isPlayingMedia) {
        return (
          <View style={styles.videoActive}>
            <Video
              source={{ uri: mUrl }}
              style={{ flex: 1, borderRadius: 8 }}
              useNativeControls
              resizeMode={ResizeMode.CONTAIN}
              isLooping={false}
              shouldPlay
            />
            <TouchableOpacity style={styles.closeVideo} onPress={() => setIsPlayingMedia(false)}><Ionicons name="close" size={16} color="white" /></TouchableOpacity>
          </View>
        );
      }
      return (
        <TouchableOpacity style={styles.videoPlaceholder} onPress={() => setIsPlayingMedia(true)}>
          <Ionicons name="play-circle" size={48} color="white" /><Text style={styles.videoTag}>VIDEO</Text>
        </TouchableOpacity>
      );
    }

    if (isAudio && mUrl) {
      return (
        <TouchableOpacity style={styles.audioRow} onPress={handlePlayAudio} disabled={item.state === 'SENDING'}>
          <View style={styles.audioIconBox}>
            {audioLoading ? <ActivityIndicator size="small" color="#0068FF" /> : <Ionicons name={isAudioPlaying ? "pause" : "play"} size={22} color="#0068FF" />}
          </View>
          <View style={styles.audioMeta}>
            <Text style={styles.audioLabel}>Tin nhắn thoại</Text>
            <View style={styles.audioTrack}><View style={[styles.audioProgress, { width: isAudioPlaying ? '100%' : '0%' }]} /></View>
          </View>
        </TouchableOpacity>
      );
    }

    if (item.type === 'FILE' && mUrl) {
      return (
        <TouchableOpacity style={styles.fileRow} onPress={() => WebBrowser.openBrowserAsync(mUrl)} disabled={item.state === 'SENDING'}>
          <View style={styles.fileIconBox}><Ionicons name="document-text" size={24} color="#EF4444" /></View>
          <View style={styles.fileMeta}><Text style={styles.fileLabel} numberOfLines={1}>{item.text || item.content || 'Tài liệu'}</Text><Text style={styles.fileAction}>NHẤN ĐỂ XEM</Text></View>
        </TouchableOpacity>
      );
    }

    return <Text style={[styles.messageText, isMe ? styles.myText : styles.otherText]}>{item.text || item.content}</Text>;
  };

  return (
    <View style={[styles.container, isMe ? styles.myContainer : styles.otherContainer]}>
      {!isMe && (
        <View style={styles.avatarSpace}>
          <Image source={{ uri: item.avatar || `https://api.dicebear.com/9.x/avataaars/png?seed=${encodeURIComponent(item.senderName || "User")}` }} style={styles.avatar} />
        </View>
      )}

      <View style={[styles.contentArea, isMe ? styles.myContent : styles.otherContent]}>
        {isGroup && !isMe && item.senderName && (<Text style={styles.senderNameText}>{item.senderName}</Text>)}

        <TouchableOpacity onLongPress={() => !isDeleted && setShowMenu(true)} activeOpacity={0.9}>
          <View style={[styles.bubble, isMe ? styles.myBubble : styles.otherBubble, isDeleted && styles.deletedBubble]}>
            {renderContent()}
            {!isDeleted && (
              <View style={styles.footer}>
                <View style={styles.timeLine}>
                  <Text style={[styles.timeText, isMe ? styles.myTime : styles.otherTime]}>{item.time}</Text>
                  {isMe && item.state === 'SENDING' && <ActivityIndicator size="small" color="#94A3B8" style={{ marginLeft: 4 }} />}
                  {isMe && item.state === 'SEEN' && <Ionicons name="checkmark-done" size={14} color="#0068FF" style={{ marginLeft: 4 }} />}
                  {isMe && (item.state === 'DELIVERED' || item.state === 'SENT') && <Ionicons name={item.state === 'SENT' ? "checkmark" : "checkmark-done"} size={14} color="#94A3B8" style={{ marginLeft: 4 }} />}
                </View>
                <TouchableOpacity style={{ marginLeft: 8 }} onPress={() => react({ messageId: item.id, emoji: '❤️' })}>
                  {item.reactions && item.reactions.length > 0 ? (
                    <Text style={{ fontSize: 12 }}>{item.reactions[item.reactions.length - 1].emoji}</Text>
                  ) : (<Ionicons name="heart-outline" size={16} color={isMe ? "#0068FF" : "#8E8E93"} />)}
                </TouchableOpacity>
              </View>
            )}
          </View>
        </TouchableOpacity>

        {item.reactions && item.reactions.length > 0 && (
          <View style={styles.reactionBadge}>
            {item.reactions.slice(0, 3).map((r, i) => <Text key={i} style={{ fontSize: 10 }}>{r.emoji}</Text>)}
          </View>
        )}
      </View>

      <Modal visible={showMenu} transparent animationType="fade">
        <TouchableWithoutFeedback onPress={() => setShowMenu(false)}>
          <View style={styles.modalOverlay}>
            <View style={styles.menuBox}>
              <Text style={styles.menuTitle}>Tùy chọn</Text>
              <View style={styles.emojiRow}>
                {emojis.map(e => (
                  <TouchableOpacity key={e} onPress={() => { react({ messageId: item.id, emoji: e }); setShowMenu(false); }}>
                    <Text style={{ fontSize: 24 }}>{e}</Text>
                  </TouchableOpacity>
                ))}
              </View>
              <View style={{ gap: 4 }}>
                <MenuAction icon="arrow-redo-outline" label="Chuyển tiếp" onPress={() => { setShowMenu(false); setShowForward(true); }} />
                {item.type !== 'TEXT' && <MenuAction icon="download-outline" label="Tải về" color="#0068FF" onPress={handleDownload} />}
                {isMe && <MenuAction icon="refresh-outline" label="Thu hồi" color="#EF4444" onPress={() => { setShowMenu(false); doRecall(item.id); }} />}
                <MenuAction icon="trash-outline" label="Xóa phía mình" color="#EF4444" onPress={() => { setShowMenu(false); doDelete(item.id); }} />
              </View>
            </View>
          </View>
        </TouchableWithoutFeedback>
      </Modal>

      <ForwardModal visible={showForward} onClose={() => setShowForward(false)} messageContent={item.text || item.content || ""} messageType={item.type} mediaUrl={item.image || item.mediaUrl} />
    </View>
  );
};

const MenuAction = ({ icon, label, color = "#1F2937", onPress }: { icon: any; label: string; color?: string; onPress: () => void }) => (
  <TouchableOpacity onPress={onPress} style={styles.menuItem}>
    <Ionicons name={icon} size={22} color={color} />
    <Text style={[styles.menuItemText, { color }]}>{label}</Text>
  </TouchableOpacity>
);

const styles = StyleSheet.create({
  container: { flexDirection: 'row', marginBottom: 12, paddingHorizontal: 12 },
  myContainer: { justifyContent: 'flex-end' },
  otherContainer: { justifyContent: 'flex-start' },
  avatarSpace: { marginRight: 8, alignSelf: 'flex-end', paddingBottom: 4 },
  avatar: { width: 32, height: 32, borderRadius: 16 },
  contentArea: { maxWidth: '80%' },
  myContent: { alignItems: 'flex-end' },
  otherContent: { alignItems: 'flex-start' },
  senderNameText: { fontSize: 11, color: '#0068FF', fontWeight: 'bold', marginBottom: 4, marginLeft: 4 },
  bubble: { paddingHorizontal: 12, paddingTop: 8, paddingBottom: 6, borderRadius: 18, borderWidth: 0.5, minWidth: 80 },
  myBubble: { backgroundColor: '#EAF6FF', borderColor: '#D5E9F7', borderBottomRightRadius: 4 },
  otherBubble: { backgroundColor: '#FFFFFF', borderColor: '#E1E6E9', borderBottomLeftRadius: 4 },
  deletedBubble: { backgroundColor: '#F9FAFB', borderColor: '#F3F4F6' },
  messageText: { fontSize: 16, lineHeight: 22, color: '#1F2937' },
  myText: { color: '#1F2937' },
  otherText: { color: '#1F2937' },
  deletedText: { color: '#9CA3AF', fontStyle: 'italic' },
  footer: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 },
  timeLine: { flexDirection: 'row', alignItems: 'center' },
  timeText: { fontSize: 10, color: '#6B7280' },
  myTime: { color: '#6B7280' },
  otherTime: { color: '#94A3B8' },
  mediaContainer: { marginBottom: 4, borderRadius: 12, overflow: 'hidden', shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.2, shadowRadius: 1.41, elevation: 2 },
  imageBox: { width: 224, height: 288 },
  mediaOverlay: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.3)', alignItems: 'center', justifyContent: 'center' },
  videoActive: { width: 256, height: 192, borderRadius: 12, overflow: 'hidden', backgroundColor: 'black' },
  closeVideo: { position: 'absolute', top: 4, right: 4, backgroundColor: 'rgba(0,0,0,0.5)', padding: 4, borderRadius: 12 },
  videoPlaceholder: { width: 224, height: 160, borderRadius: 12, overflow: 'hidden', backgroundColor: 'rgba(0,0,0,0.8)', alignItems: 'center', justifyContent: 'center' },
  videoTag: { color: 'white', fontWeight: 'bold', fontSize: 10, marginTop: 4 },
  audioRow: { flexDirection: 'row', alignItems: 'center', padding: 12, minWidth: 180, backgroundColor: 'rgba(0, 104, 255, 0.05)', borderRadius: 16 },
  audioIconBox: { backgroundColor: '#DBEAFE', padding: 8, borderRadius: 20 },
  audioMeta: { marginLeft: 12, flex: 1 },
  audioLabel: { color: '#1F2937', fontWeight: 'bold', fontSize: 14 },
  audioTrack: { height: 4, backgroundColor: '#BFDBFE', width: '100%', marginTop: 6, borderRadius: 2 },
  audioProgress: { height: '100%', backgroundColor: '#0068FF' },
  fileRow: { flexDirection: 'row', alignItems: 'center', backgroundColor: 'rgba(249, 250, 251, 0.5)', borderWidth: 1, borderColor: '#F3F4F6', padding: 12, borderRadius: 12, marginBottom: 4, minWidth: 200 },
  fileIconBox: { backgroundColor: '#FEE2E2', padding: 8, borderRadius: 8 },
  fileMeta: { marginLeft: 12, flex: 1 },
  fileLabel: { color: '#1F2937', fontWeight: 'bold', fontSize: 14 },
  fileAction: { color: '#9CA3AF', fontSize: 10, marginTop: 2 },
  reactionBadge: { flexDirection: 'row', marginTop: -6, backgroundColor: 'white', borderWidth: 1, borderColor: '#F3F4F6', borderRadius: 12, paddingHorizontal: 6, paddingVertical: 2 },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.3)', justifyContent: 'center', alignItems: 'center', paddingHorizontal: 24 },
  menuBox: { backgroundColor: 'white', borderRadius: 24, padding: 24, width: '100%', maxWidth: 360 },
  menuTitle: { fontSize: 20, fontWeight: 'bold', marginBottom: 16, color: '#1F2937' },
  emojiRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 24 },
  menuItem: { flexDirection: 'row', alignItems: 'center', paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: '#F9FAFB' },
  menuItemText: { marginLeft: 16, fontSize: 16, fontWeight: '500' }
});

export default MessageItem;
