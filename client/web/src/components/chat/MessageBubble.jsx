import React, { useState } from 'react';
import { MoreHorizontal, RotateCcw, Smile } from 'lucide-react';
import { format } from 'date-fns';
import { vi } from 'date-fns/locale';
import EmojiPicker from 'emoji-picker-react';
import { recallMessage, toggleReaction, deleteReaction } from '../../api/message';
import useAuthStore from '../../store/authStore';
import useChatStore from '../../store/chatStore';
import toast from 'react-hot-toast';

const BASE_URL = 'http://localhost:8080';

function getMediaUrl(url) {
  if (!url) return null;
  if (url.startsWith('http')) return url;
  return `${BASE_URL}${url}`;
}

function formatTime(dateStr) {
  if (!dateStr) return '';
  try {
    return format(new Date(dateStr), 'HH:mm', { locale: vi });
  } catch {
    return '';
  }
}

export default function MessageBubble({ message, chatId, isGroup = false }) {
  const { auth } = useAuthStore();
  const { updateMessage, updateMessageReactions } = useChatStore();
  const [showActions, setShowActions] = useState(false);
  const [showEmoji, setShowEmoji] = useState(false);
  const [imagePreview, setImagePreview] = useState(null);

  const isMine = isGroup
    ? message.isMine || message.senderId === auth?.userId
    : message.senderId === auth?.userId;

  const handleRecall = async () => {
    try {
      await recallMessage(message.id);
      updateMessage(chatId, message.id, { deleted: true, content: '' });
      toast.success('Đã thu hồi tin nhắn');
    } catch {
      toast.error('Không thể thu hồi tin nhắn');
    }
    setShowActions(false);
  };

  const handleReaction = async (emojiData) => {
    try {
      const res = await toggleReaction(message.id, emojiData.emoji);
      if (res.data?.reactions) {
        updateMessageReactions(chatId, message.id, res.data.reactions);
      }
    } catch {}
    setShowEmoji(false);
  };

  const handleRemoveReaction = async () => {
    try {
      await deleteReaction(message.id);
      const existing = (message.reactions || []).filter((r) => r.userId !== auth?.userId);
      updateMessageReactions(chatId, message.id, existing);
    } catch {}
  };

  const myReaction = (message.reactions || []).find((r) => r.userId === auth?.userId);

  const renderContent = () => {
    if (message.deleted) {
      return (
        <span className="italic text-gray-400 text-sm">
          Tin nhắn đã được thu hồi
        </span>
      );
    }

    const type = message.type || 'TEXT';
    const mediaUrl = getMediaUrl(message.mediaUrl);

    if (type === 'IMAGE' && mediaUrl) {
      return (
        <div className="message-bubble">
          <img
            src={mediaUrl}
            alt="Hình ảnh"
            onClick={() => setImagePreview(mediaUrl)}
            className="rounded-xl cursor-pointer max-w-[240px] max-h-[240px] object-cover"
          />
        </div>
      );
    }

    if (type === 'VIDEO' && mediaUrl) {
      return (
        <div className="message-bubble">
          <video
            src={mediaUrl}
            controls
            className="rounded-xl max-w-[240px] max-h-[240px]"
          />
        </div>
      );
    }

    if ((type === 'FILE' || type === 'AUDIO') && mediaUrl) {
      const filename = message.mediaUrl?.split('/').pop() || 'Tệp đính kèm';
      return (
        <a
          href={mediaUrl}
          target="_blank"
          rel="noreferrer"
          className="flex items-center gap-2 text-sm underline"
        >
          📎 {filename}
        </a>
      );
    }

    return <span className="text-sm whitespace-pre-wrap break-words">{message.content}</span>;
  };

  const groupedReactions = (message.reactions || []).reduce((acc, r) => {
    acc[r.emoji] = (acc[r.emoji] || 0) + 1;
    return acc;
  }, {});

  return (
    <>
      <div
        className={`flex items-end gap-2 group px-4 py-0.5 ${isMine ? 'flex-row-reverse' : 'flex-row'}`}
        onMouseEnter={() => setShowActions(true)}
        onMouseLeave={() => { setShowActions(false); setShowEmoji(false); }}
      >
        <div className={`relative flex flex-col ${isMine ? 'items-end' : 'items-start'} max-w-[65%]`}>
          {/* Sender name for group */}
          {isGroup && !isMine && message.senderName && (
            <span className="text-xs text-blue-600 font-medium mb-1 ml-1">
              {message.senderName}
            </span>
          )}

          {/* Bubble */}
          <div
            className={`relative px-4 py-2.5 rounded-2xl shadow-sm ${
              message.deleted
                ? 'bg-gray-100 text-gray-400'
                : isMine
                ? 'bg-[#0068ff] text-white rounded-br-sm'
                : 'bg-white text-gray-800 rounded-bl-sm border border-gray-100'
            }`}
          >
            {renderContent()}
            <div
              className={`text-[10px] mt-1 ${
                isMine ? 'text-blue-100 text-right' : 'text-gray-400 text-right'
              }`}
            >
              {formatTime(message.createdAt || message.createdDate)}
              {isMine && message.state === 'SEEN' && (
                <span className="ml-1">✓✓</span>
              )}
            </div>
          </div>

          {/* Reactions */}
          {Object.keys(groupedReactions).length > 0 && (
            <div
              className={`flex items-center gap-1 mt-1 ${isMine ? 'flex-row-reverse' : ''}`}
            >
              {Object.entries(groupedReactions).map(([emoji, count]) => (
                <button
                  key={emoji}
                  onClick={myReaction?.emoji === emoji ? handleRemoveReaction : undefined}
                  className="flex items-center gap-0.5 bg-white border border-gray-200 rounded-full px-2 py-0.5 text-xs shadow-sm hover:bg-gray-50 transition-colors"
                >
                  <span>{emoji}</span>
                  {count > 1 && <span className="text-gray-600">{count}</span>}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Actions */}
        {!message.deleted && showActions && (
          <div className={`flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity ${isMine ? 'flex-row-reverse' : ''}`}>
            <div className="relative">
              <button
                onClick={() => setShowEmoji(!showEmoji)}
                className="w-7 h-7 flex items-center justify-center rounded-full bg-white shadow-md hover:bg-gray-100 transition-colors"
              >
                <Smile size={14} className="text-gray-500" />
              </button>
              {showEmoji && (
                <div
                  className={`absolute bottom-8 z-30 ${isMine ? 'right-0' : 'left-0'}`}
                  onClick={(e) => e.stopPropagation()}
                >
                  <EmojiPicker
                    onEmojiClick={handleReaction}
                    width={300}
                    height={350}
                    searchPlaceHolder="Tìm emoji..."
                  />
                </div>
              )}
            </div>
            {isMine && (
              <button
                onClick={handleRecall}
                className="w-7 h-7 flex items-center justify-center rounded-full bg-white shadow-md hover:bg-gray-100 transition-colors"
                title="Thu hồi"
              >
                <RotateCcw size={13} className="text-gray-500" />
              </button>
            )}
          </div>
        )}
      </div>

      {/* Image preview modal */}
      {imagePreview && (
        <div
          className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4"
          onClick={() => setImagePreview(null)}
        >
          <img
            src={imagePreview}
            alt="Preview"
            className="max-w-full max-h-full rounded-lg object-contain"
          />
        </div>
      )}
    </>
  );
}
