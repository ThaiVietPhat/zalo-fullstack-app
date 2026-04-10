import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Info, Users, LogOut, ChevronLeft, UserPlus, Pencil, Camera } from 'lucide-react';
import { getGroupDetail, getGroupMessages, sendGroupMessage, uploadGroupMedia, leaveGroup, addGroupMembers, removeGroupMember, updateGroup, uploadGroupAvatar, recallGroupMessage, setMemberAsAdmin, dissolveGroup } from '../../api/group';
import { getContacts } from '../../api/friendRequest';
import useChatStore from '../../store/chatStore';
import useAuthStore from '../../store/authStore';
import wsService from '../../services/websocket';
import Avatar from '../common/Avatar';
import MessageInput from '../chat/MessageInput';
import TypingIndicator from '../chat/TypingIndicator';
import Modal from '../common/Modal';
import toast from 'react-hot-toast';
import { RotateCcw, Smile } from 'lucide-react';
import { format } from 'date-fns';
import { vi } from 'date-fns/locale';
import { toggleGroupReaction, deleteGroupReaction } from '../../api/group';
import EmojiPicker from 'emoji-picker-react';

function GroupMessageBubble({ message, groupId }) {
  const { auth } = useAuthStore();
  const { updateGroupMessage, updateGroupMessageReactions } = useChatStore();
  const [showActions, setShowActions] = useState(false);
  const [showEmoji, setShowEmoji] = useState(false);
  const [imagePreview, setImagePreview] = useState(null);

  const isMine = message.senderId === auth?.userId;
  const BASE_URL = 'http://localhost:8080';

  const getMediaUrl = (url) => {
    if (!url) return null;
    if (url.startsWith('http')) return url;
    return `${BASE_URL}${url}`;
  };

  const downloadFile = async (mediaUrl, filename) => {
    try {
      const rawName = mediaUrl.split('/').pop()?.split('?')[0] || filename || 'download';
      const downloadUrl = `${BASE_URL}/api/v1/message/media/${rawName}?download=true`;
      const auth = JSON.parse(localStorage.getItem('auth') || '{}');
      const res = await fetch(downloadUrl, {
        headers: auth.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {},
      });
      if (!res.ok) throw new Error('fetch failed');
      const blob = await res.blob();
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = filename || rawName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(a.href);
    } catch {
      window.open(mediaUrl, '_blank');
    }
  };

  const handleRecall = async () => {
    try {
      await recallGroupMessage(groupId, message.id);
      updateGroupMessage(groupId, message.id, { deleted: true, content: '' });
      toast.success('Đã thu hồi tin nhắn');
    } catch {
      toast.error('Không thể thu hồi tin nhắn');
    }
  };

  const handleReaction = async (emojiData) => {
    setShowEmoji(false);
    setShowActions(false);
    try {
      const res = await toggleGroupReaction(message.id, emojiData.emoji);
      if (Array.isArray(res.data)) {
        updateGroupMessageReactions(groupId, message.id, res.data);
      }
    } catch {}
  };

  const handleRemoveReaction = async () => {
    try {
      await deleteGroupReaction(message.id);
      const existing = (message.reactions || []).filter((r) => r.userId !== auth?.userId);
      updateGroupMessageReactions(groupId, message.id, existing);
    } catch {}
  };

  const myReaction = (message.reactions || []).find((r) => r.userId === auth?.userId);

  const groupedReactions = (message.reactions || []).reduce((acc, r) => {
    acc[r.emoji] = (acc[r.emoji] || 0) + 1;
    return acc;
  }, {});

  const renderContent = () => {
    if (message.deleted) {
      return <span className="italic text-gray-400 text-sm">Tin nhắn đã được thu hồi</span>;
    }
    const type = message.type || 'TEXT';
    const mediaUrl = getMediaUrl(message.mediaUrl);

    if (type === 'IMAGE' && mediaUrl) {
      return (
        <div className="relative group/img">
          <img
            src={mediaUrl}
            alt="img"
            onClick={() => setImagePreview(mediaUrl)}
            className="rounded-xl cursor-pointer max-w-[240px] max-h-[240px] object-cover"
          />
          <a
            href={`${mediaUrl}?download=true`}
            className="absolute top-1 right-1 bg-black/50 text-white rounded-full p-1 opacity-0 group-hover/img:opacity-100 transition-opacity"
            title="Tải xuống"
            onClick={(e) => e.stopPropagation()}
          >⬇</a>
        </div>
      );
    }
    if (type === 'VIDEO' && mediaUrl) {
      return (
        <div className="relative group/vid">
          <video src={mediaUrl} controls className="rounded-xl max-w-[240px] max-h-[240px]" />
          <a
            href={`${mediaUrl}?download=true`}
            className="absolute top-1 right-1 bg-black/50 text-white rounded-full p-1 opacity-0 group-hover/vid:opacity-100 transition-opacity"
            title="Tải xuống"
          >⬇</a>
        </div>
      );
    }
    if ((type === 'FILE' || type === 'AUDIO') && mediaUrl) {
      const displayName = message.fileName
        || message.mediaUrl?.split('/').pop()?.split('?')[0]
        || 'Tệp đính kèm';
      return (
        <div className="flex items-center gap-2">
          <a
            href={mediaUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-2 text-sm underline min-w-0"
            title="Mở file"
          >
            📎 <span className="truncate max-w-[180px]">{displayName}</span>
          </a>
          <button
            onClick={(e) => { e.stopPropagation(); downloadFile(mediaUrl, displayName); }}
            className="flex-shrink-0 text-xs py-1 px-2 rounded-lg bg-black/10 hover:bg-black/20 transition-colors"
            title="Tải xuống"
          >
            ⬇
          </button>
        </div>
      );
    }
    return <span className="text-sm whitespace-pre-wrap break-words">{message.content}</span>;
  };

  return (
    <>
      <div
        className={`flex items-end gap-2 px-4 py-0.5 ${isMine ? 'flex-row-reverse' : 'flex-row'}`}
        onMouseEnter={() => setShowActions(true)}
        onMouseLeave={() => { if (!showEmoji) setShowActions(false); }}
      >
        <div className={`relative flex flex-col ${isMine ? 'items-end' : 'items-start'} max-w-[65%]`}>
          {!isMine && message.senderName && (
            <span className="text-xs text-blue-600 font-medium mb-1 ml-1">{message.senderName}</span>
          )}
          <div className={`relative px-4 py-2.5 rounded-2xl shadow-sm ${
            message.deleted
              ? 'bg-gray-100 text-gray-400'
              : isMine
              ? 'bg-[#0068ff] text-white rounded-br-sm'
              : 'bg-white text-gray-800 rounded-bl-sm border border-gray-100'
          }`}>
            {renderContent()}
            <div className={`text-[10px] mt-1 ${isMine ? 'text-blue-100 text-right' : 'text-gray-400 text-right'}`}>
              {message.createdDate ? format(new Date(message.createdDate), 'HH:mm', { locale: vi }) : ''}
            </div>
          </div>
          {Object.keys(groupedReactions).length > 0 && (
            <div className={`flex items-center gap-1 mt-1 ${isMine ? 'flex-row-reverse' : ''}`}>
              {Object.entries(groupedReactions).map(([emoji, count]) => (
                <button key={emoji} onClick={myReaction?.emoji === emoji ? handleRemoveReaction : undefined}
                  className="flex items-center gap-0.5 bg-white border border-gray-200 rounded-full px-2 py-0.5 text-xs shadow-sm hover:bg-gray-50">
                  <span>{emoji}</span>
                  {count > 1 && <span className="text-gray-600">{count}</span>}
                </button>
              ))}
            </div>
          )}
        </div>

        {!message.deleted && showActions && (
          <div className={`flex items-center gap-1 ${isMine ? 'flex-row-reverse' : ''}`}>
            <div className="relative">
              <button onClick={() => setShowEmoji(!showEmoji)}
                className="w-7 h-7 flex items-center justify-center rounded-full bg-white shadow-md hover:bg-gray-100">
                <Smile size={14} className="text-gray-500" />
              </button>
              {showEmoji && (
                <div className={`absolute bottom-8 z-30 ${isMine ? 'right-0' : 'left-0'}`} onClick={(e) => e.stopPropagation()}>
                  <EmojiPicker onEmojiClick={handleReaction} width={300} height={350} searchPlaceHolder="Tìm emoji..." />
                </div>
              )}
            </div>
            {isMine && (
              <button onClick={handleRecall} className="w-7 h-7 flex items-center justify-center rounded-full bg-white shadow-md hover:bg-gray-100" title="Thu hồi">
                <RotateCcw size={13} className="text-gray-500" />
              </button>
            )}
          </div>
        )}
      </div>
      {imagePreview && (
        <div className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4" onClick={() => setImagePreview(null)}>
          <img src={imagePreview} alt="Preview" className="max-w-full max-h-full rounded-lg object-contain" />
        </div>
      )}
    </>
  );
}

export default function GroupWindow() {
  const { activeGroupId, groupMessages, typingUsers, setGroupMessages, prependGroupMessages, setActiveGroupId, groups, setGroups, addGroupMessage, updateGroupLastMessage, updateGroupMessageReactions, setTyping } = useChatStore();
  const { auth } = useAuthStore();

  const subscribeToGroup = (groupId) => {
    wsService.subscribe(`/topic/group/${groupId}`, (data) => {
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
        setTimeout(() => setTyping(`group_${groupId}`, userId, false), 3000);
      }
    });
  };

  const unsubscribeFromGroup = (groupId) => {
    wsService.unsubscribe(`/topic/group/${groupId}`);
    wsService.unsubscribe(`/topic/group/${groupId}/typing`);
  };

  const sendGroupTyping = (groupId, isTypingNow) => {
    wsService.publish(`/app/group/${groupId}/typing`, { typing: isTypingNow });
  };
  const [groupDetail, setGroupDetail] = useState(null);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [showInfo, setShowInfo] = useState(false);
  const [showAddMember, setShowAddMember] = useState(false);
  const [allUsers, setAllUsers] = useState([]);
  const [selectedToAdd, setSelectedToAdd] = useState([]);
  const [editMode, setEditMode] = useState(false);
  const [editName, setEditName] = useState('');
  const [editDesc, setEditDesc] = useState('');
  const messagesEndRef = useRef(null);
  const scrollContainerRef = useRef(null);
  const prevScrollHeightRef = useRef(0);

  const msgs = groupMessages[activeGroupId] || [];
  const typingKey = `group_${activeGroupId}`;
  const typingSet = typingUsers[typingKey] || new Set();
  const isTyping = typingSet.size > 0;

  const isAdmin = groupDetail?.isAdmin || groupDetail?.createdById === auth?.userId;

  useEffect(() => {
    if (!activeGroupId) return;
    setPage(0);
    setHasMore(true);
    setLoading(true);

    getGroupDetail(activeGroupId)
      .then((res) => {
        setGroupDetail(res.data);
        setEditName(res.data.name || '');
        setEditDesc(res.data.description || '');
      })
      .catch(() => {});

    getGroupMessages(activeGroupId, 0, 30)
      .then((res) => {
        const data = res.data;
        const fetchedMsgs = Array.isArray(data) ? data : (data.content || []);
        const sorted = fetchedMsgs.slice().reverse();
        // Merge: keep only WS messages newer than the latest API message (avoids re-appending old paginated messages)
        const alreadyInStore = useChatStore.getState().groupMessages[activeGroupId] || [];
        const fetchedIds = new Set(sorted.map((m) => m.id));
        const latestFetchedTime = sorted.length > 0
          ? new Date(sorted[sorted.length - 1].createdDate).getTime()
          : 0;
        const wsOnlyMsgs = alreadyInStore.filter(
          (m) => !fetchedIds.has(m.id) && new Date(m.createdDate).getTime() > latestFetchedTime
        );
        setGroupMessages(activeGroupId, [...sorted, ...wsOnlyMsgs]);
        setHasMore(fetchedMsgs.length === 30);
        setTimeout(() => messagesEndRef.current?.scrollIntoView(), 100);
      })
      .catch(() => {})
      .finally(() => setLoading(false));

    subscribeToGroup(activeGroupId);

    return () => { unsubscribeFromGroup(activeGroupId); };
  }, [activeGroupId]);

  // Re-fetch messages after WebSocket reconnect to catch any missed during disconnect
  useEffect(() => {
    if (!activeGroupId) return;
    const refetch = () => {
      getGroupMessages(activeGroupId, 0, 30)
        .then((res) => {
          const data = res.data;
          const fetchedMsgs = Array.isArray(data) ? data : (data.content || []);
          const sorted = fetchedMsgs.slice().reverse();
          const existing = useChatStore.getState().groupMessages[activeGroupId] || [];
          const fetchedIds = new Set(sorted.map((m) => m.id));
          const latestTime = sorted.length > 0 ? new Date(sorted[sorted.length - 1].createdDate).getTime() : 0;
          const wsOnly = existing.filter((m) => !fetchedIds.has(m.id) && new Date(m.createdDate).getTime() > latestTime);
          setGroupMessages(activeGroupId, [...sorted, ...wsOnly]);
        })
        .catch(() => {});
    };
    wsService.onReconnect(refetch);
    return () => wsService.offReconnect(refetch);
  }, [activeGroupId]);

  useEffect(() => {
    const container = scrollContainerRef.current;
    if (!container) return;
    const isNearBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 200;
    if (isNearBottom) messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [msgs.length]);

  const handleScroll = useCallback(async () => {
    const container = scrollContainerRef.current;
    if (!container || loadingMore || !hasMore) return;
    if (container.scrollTop < 100) {
      setLoadingMore(true);
      prevScrollHeightRef.current = container.scrollHeight;
      const nextPage = page + 1;
      try {
        const res = await getGroupMessages(activeGroupId, nextPage, 30);
        const data = res.data;
        const msgList = Array.isArray(data) ? data : (data.content || []);
        if (msgList.length === 0) {
          setHasMore(false);
        } else {
          prependGroupMessages(activeGroupId, msgList.slice().reverse());
          setPage(nextPage);
          setTimeout(() => {
            const container = scrollContainerRef.current;
            if (container) container.scrollTop = container.scrollHeight - prevScrollHeightRef.current;
          }, 50);
        }
      } catch {}
      setLoadingMore(false);
    }
  }, [activeGroupId, page, loadingMore, hasMore]);

  const handleSendText = async (content) => {
    try {
      await sendGroupMessage(activeGroupId, { content });
    } catch { toast.error('Không thể gửi tin nhắn'); }
  };

  const handleSendMedia = async (file) => {
    try {
      await uploadGroupMedia(activeGroupId, file);
    } catch { toast.error('Không thể gửi tệp'); }
  };

  const handleTyping = (isTypingNow) => { sendGroupTyping(activeGroupId, isTypingNow); };

  const handleLeave = async () => {
    if (!window.confirm('Bạn có chắc muốn rời nhóm?')) return;
    try {
      await leaveGroup(activeGroupId);
      setGroups(groups.filter((g) => g.id !== activeGroupId));
      setActiveGroupId(null);
      toast.success('Đã rời nhóm');
    } catch { toast.error('Không thể rời nhóm'); }
  };

  const handleSetMemberAsAdmin = async (userId) => {
    if (!window.confirm('Gán quyền admin cho thành viên này?')) return;
    try {
      await setMemberAsAdmin(activeGroupId, userId);
      const res = await getGroupDetail(activeGroupId);
      setGroupDetail(res.data);
      toast.success('Đã gán quyền admin');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Không thể gán quyền admin');
    }
  };

  const handleDissolveGroup = async () => {
    if (!window.confirm('Bạn có chắc muốn giải tán nhóm? Thao tác này không thể hoàn tác.')) return;
    try {
      await dissolveGroup(activeGroupId);
      setGroups(groups.filter((g) => g.id !== activeGroupId));
      setActiveGroupId(null);
      toast.success('Đã giải tân nhóm');
    } catch { toast.error('Không thể giải tân nhóm'); }
  };

  const handleSaveEdit = async () => {
    try {
      await updateGroup(activeGroupId, { name: editName, description: editDesc });
      setGroupDetail((prev) => ({ ...prev, name: editName, description: editDesc }));
      setGroups(groups.map((g) => g.id === activeGroupId ? { ...g, name: editName, description: editDesc } : g));
      setEditMode(false);
      toast.success('Đã cập nhật nhóm');
    } catch { toast.error('Không thể cập nhật nhóm'); }
  };

  const handleAddMembers = async () => {
    if (selectedToAdd.length === 0) return;
    try {
      await addGroupMembers(activeGroupId, selectedToAdd.map((u) => u.id));
      const res = await getGroupDetail(activeGroupId);
      setGroupDetail(res.data);
      setShowAddMember(false);
      setSelectedToAdd([]);
      toast.success('Đã thêm thành viên');
    } catch { toast.error('Không thể thêm thành viên'); }
  };

  const handleRemoveMember = async (userId) => {
    if (!window.confirm('Xóa thành viên này?')) return;
    try {
      await removeGroupMember(activeGroupId, userId);
      setGroupDetail((prev) => ({ ...prev, members: prev.members.filter((m) => m.userId !== userId) }));
      toast.success('Đã xóa thành viên');
    } catch { toast.error('Không thể xóa thành viên'); }
  };

  const handleAvatarChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      await uploadGroupAvatar(activeGroupId, file);
      const res = await getGroupDetail(activeGroupId);
      setGroupDetail(res.data);
      toast.success('Đã cập nhật ảnh nhóm');
    } catch { toast.error('Không thể cập nhật ảnh nhóm'); }
  };

  if (!activeGroupId) {
    return (
      <div className="flex-1 flex items-center justify-center bg-gray-50">
        <div className="text-center text-gray-400">
          <div className="text-6xl mb-4">👥</div>
          <p className="text-lg font-medium">Chọn một nhóm</p>
          <p className="text-sm mt-1">để bắt đầu trò chuyện</p>
        </div>
      </div>
    );
  }

  const existingMemberIds = new Set((groupDetail?.members || []).map((m) => m.id));

  return (
    <>
      <div className="flex-1 flex flex-col h-full bg-gray-50">
        {/* Header */}
        <div className="flex items-center gap-3 px-4 py-3 bg-white border-b border-gray-100 shadow-sm">
          <button className="md:hidden p-1 hover:bg-gray-100 rounded-full" onClick={() => setActiveGroupId(null)}>
            <ChevronLeft size={20} />
          </button>
          <Avatar src={groupDetail?.avatarUrl} name={groupDetail?.name} size={40} />
          <div className="flex-1 min-w-0">
            <h3 className="font-semibold text-gray-800 truncate">{groupDetail?.name}</h3>
            <p className="text-xs text-gray-400">{groupDetail?.memberCount} thành viên</p>
          </div>
          <div className="flex items-center gap-1">
            <button onClick={() => { getContacts().then((r) => setAllUsers(r.data || [])); setShowInfo(true); }}
              className="w-9 h-9 flex items-center justify-center rounded-full hover:bg-gray-100 transition-colors">
              <Info size={18} className="text-gray-600" />
            </button>
          </div>
        </div>

        {/* Messages */}
        <div ref={scrollContainerRef} onScroll={handleScroll} className="flex-1 overflow-y-auto py-4">
          {loadingMore && (
            <div className="flex justify-center py-2">
              <div className="w-5 h-5 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
            </div>
          )}
          {loading ? (
            <div className="flex justify-center py-8">
              <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : (
            msgs.map((msg) => (
              <GroupMessageBubble key={msg.id} message={msg} groupId={activeGroupId} />
            ))
          )}
          {isTyping && <TypingIndicator />}
          <div ref={messagesEndRef} />
        </div>

        {/* Input */}
        <MessageInput
          onSendText={handleSendText}
          onSendMedia={handleSendMedia}
          onTyping={handleTyping}
          placeholder="Nhập tin nhắn nhóm..."
        />
      </div>

      {/* Group info modal */}
      <Modal isOpen={showInfo} onClose={() => { setShowInfo(false); setEditMode(false); }} title="Thông tin nhóm" size="md">
        <div className="p-6 space-y-4">
          {/* Avatar */}
          <div className="flex flex-col items-center gap-3">
            <div className="relative">
              <Avatar src={groupDetail?.avatarUrl} name={groupDetail?.name} size={80} />
              {isAdmin && (
                <label className="absolute bottom-0 right-0 w-7 h-7 bg-blue-500 rounded-full flex items-center justify-center cursor-pointer hover:bg-blue-600 transition-colors">
                  <Camera size={14} className="text-white" />
                  <input type="file" className="hidden" accept="image/*" onChange={handleAvatarChange} />
                </label>
              )}
            </div>
            {editMode ? (
              <div className="w-full space-y-2">
                <input value={editName} onChange={(e) => setEditName(e.target.value)}
                  className="w-full border border-gray-200 rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-200"
                  placeholder="Tên nhóm" />
                <textarea value={editDesc} onChange={(e) => setEditDesc(e.target.value)} rows={2}
                  className="w-full border border-gray-200 rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-200 resize-none"
                  placeholder="Mô tả nhóm" />
                <div className="flex gap-2">
                  <button onClick={() => setEditMode(false)}
                    className="flex-1 border border-gray-200 py-2 rounded-xl text-sm hover:bg-gray-50">Hủy</button>
                  <button onClick={handleSaveEdit}
                    className="flex-1 bg-[#0068ff] text-white py-2 rounded-xl text-sm hover:bg-blue-600">Lưu</button>
                </div>
              </div>
            ) : (
              <>
                <div className="text-center">
                  <h3 className="font-bold text-gray-800 text-lg">{groupDetail?.name}</h3>
                  {groupDetail?.description && <p className="text-sm text-gray-500 mt-1">{groupDetail.description}</p>}
                </div>
                {isAdmin && (
                  <button onClick={() => setEditMode(true)}
                    className="flex items-center gap-2 text-sm text-blue-500 hover:underline">
                    <Pencil size={14} /> Chỉnh sửa
                  </button>
                )}
              </>
            )}
          </div>

          {/* Members */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h4 className="font-semibold text-gray-700 text-sm">Thành viên ({groupDetail?.members?.length || 0})</h4>
              {isAdmin && (
                <button onClick={() => { getContacts().then((r) => setAllUsers(r.data || [])); setShowAddMember(true); }}
                  className="flex items-center gap-1 text-xs text-blue-500 hover:underline">
                  <UserPlus size={14} /> Thêm
                </button>
              )}
            </div>
            <div className="space-y-2 max-h-48 overflow-y-auto">
              {(groupDetail?.members || []).map((member) => (
                <div key={member.userId} className="flex items-center gap-3">
                  <Avatar src={member.avatarUrl} name={`${member.firstName} ${member.lastName}`} size={36} online={member.online} />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-800 truncate">
                      {member.firstName} {member.lastName}
                      {member.admin && (
                        <span className="ml-1 text-xs text-blue-500 font-normal">(Admin)</span>
                      )}
                    </p>
                    <p className="text-xs text-gray-400 truncate">{member.email}</p>
                  </div>
                  {isAdmin && member.userId !== auth?.userId && member.userId !== groupDetail?.createdById && (
                    <div className="flex items-center gap-1">
                      {!member.admin && (
                        <button onClick={() => handleSetMemberAsAdmin(member.userId)}
                          className="text-xs text-blue-400 hover:text-blue-600 transition-colors px-2 py-1 rounded hover:bg-blue-50">
                          Gán admin
                        </button>
                      )}
                      <button onClick={() => handleRemoveMember(member.userId)}
                        className="text-xs text-red-400 hover:text-red-600 transition-colors px-2 py-1 rounded hover:bg-red-50">
                        Xóa
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Leave group */}
          <button onClick={handleLeave}
            className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl border border-red-200 text-red-500 hover:bg-red-50 transition-colors text-sm font-medium">
            <LogOut size={16} /> Rời nhóm
          </button>

          {/* Dissolve group */}
          {isAdmin && (
            <button onClick={handleDissolveGroup}
              className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl border border-red-300 bg-red-50 text-red-600 hover:bg-red-100 transition-colors text-sm font-medium">
              ⚠️ Giải tán nhóm
            </button>
          )}
        </div>
      </Modal>

      {/* Add member modal */}
      <Modal isOpen={showAddMember} onClose={() => setShowAddMember(false)} title="Thêm thành viên" size="sm">
        <div className="p-4 space-y-3">
          <div className="max-h-64 overflow-y-auto space-y-1">
            {allUsers
              .filter((u) => !existingMemberIds.has(u.id))
              .map((user) => {
                const isSelected = !!selectedToAdd.find((u) => u.id === user.id);
                return (
                  <button key={user.id} onClick={() => setSelectedToAdd((prev) =>
                    isSelected ? prev.filter((u) => u.id !== user.id) : [...prev, user]
                  )}
                    className={`flex items-center gap-3 w-full px-3 py-2 rounded-xl transition-colors ${isSelected ? 'bg-blue-50' : 'hover:bg-gray-50'}`}>
                    <Avatar src={user.avatarUrl} name={`${user.firstName} ${user.lastName}`} size={36} />
                    <div className="flex-1 text-left min-w-0">
                      <p className="text-sm font-medium truncate">{user.firstName} {user.lastName}</p>
                      <p className="text-xs text-gray-400 truncate">{user.email}</p>
                    </div>
                    {isSelected && <div className="w-5 h-5 rounded-full bg-blue-500 flex items-center justify-center flex-shrink-0">✓</div>}
                  </button>
                );
              })}
          </div>
          <button onClick={handleAddMembers} disabled={selectedToAdd.length === 0}
            className="w-full bg-[#0068ff] text-white py-2.5 rounded-xl text-sm font-medium hover:bg-blue-600 disabled:opacity-50">
            Thêm ({selectedToAdd.length})
          </button>
        </div>
      </Modal>
    </>
  );
}
