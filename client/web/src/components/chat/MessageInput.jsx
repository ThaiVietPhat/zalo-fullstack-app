import React, { useState, useRef, useCallback } from 'react';
import { Send, Paperclip, Image, Smile, X } from 'lucide-react';
import EmojiPicker from 'emoji-picker-react';

export default function MessageInput({
  onSendText,
  onSendMedia,
  onTyping,
  placeholder = 'Nhập tin nhắn...',
  disabled = false,
}) {
  const [text, setText] = useState('');
  const [showEmoji, setShowEmoji] = useState(false);
  const [mediaPreview, setMediaPreview] = useState(null);
  const [mediaFile, setMediaFile] = useState(null);
  const typingTimeoutRef = useRef(null);
  const fileInputRef = useRef(null);
  const inputRef = useRef(null);

  const handleTyping = useCallback(() => {
    if (onTyping) {
      onTyping(true);
      clearTimeout(typingTimeoutRef.current);
      typingTimeoutRef.current = setTimeout(() => {
        onTyping(false);
      }, 2000);
    }
  }, [onTyping]);

  const handleChange = (e) => {
    setText(e.target.value);
    handleTyping();
  };

  const handleSend = () => {
    if (mediaFile) {
      onSendMedia && onSendMedia(mediaFile);
      setMediaFile(null);
      setMediaPreview(null);
      return;
    }
    const trimmed = text.trim();
    if (!trimmed) return;
    onSendText && onSendText(trimmed);
    setText('');
    if (onTyping) {
      clearTimeout(typingTimeoutRef.current);
      onTyping(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleEmojiClick = (emojiData) => {
    setText((prev) => prev + emojiData.emoji);
    setShowEmoji(false);
    inputRef.current?.focus();
  };

  const handleFileSelect = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setMediaFile(file);

    if (file.type.startsWith('image/')) {
      const url = URL.createObjectURL(file);
      setMediaPreview({ type: 'image', url, name: file.name });
    } else if (file.type.startsWith('video/')) {
      const url = URL.createObjectURL(file);
      setMediaPreview({ type: 'video', url, name: file.name });
    } else {
      setMediaPreview({ type: 'file', name: file.name });
    }
    e.target.value = '';
  };

  const clearMedia = () => {
    setMediaFile(null);
    setMediaPreview(null);
  };

  return (
    <div className="border-t border-gray-100 bg-white px-4 py-3">
      {/* Media preview */}
      {mediaPreview && (
        <div className="mb-3 flex items-start gap-3 bg-gray-50 rounded-xl p-3">
          {mediaPreview.type === 'image' && (
            <img
              src={mediaPreview.url}
              alt="preview"
              className="w-16 h-16 object-cover rounded-lg"
            />
          )}
          {mediaPreview.type === 'video' && (
            <video
              src={mediaPreview.url}
              className="w-16 h-16 object-cover rounded-lg"
            />
          )}
          {mediaPreview.type === 'file' && (
            <div className="w-16 h-16 bg-blue-100 rounded-lg flex items-center justify-center">
              <Paperclip size={20} className="text-blue-500" />
            </div>
          )}
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-gray-700 truncate">{mediaPreview.name}</p>
            <p className="text-xs text-gray-400 mt-0.5">
              {mediaPreview.type === 'image' ? 'Hình ảnh' : mediaPreview.type === 'video' ? 'Video' : 'Tệp'}
            </p>
          </div>
          <button onClick={clearMedia} className="p-1 hover:bg-gray-200 rounded-full">
            <X size={16} className="text-gray-500" />
          </button>
        </div>
      )}

      <div className="flex items-end gap-2">
        {/* Emoji */}
        <div className="relative">
          <button
            onClick={() => setShowEmoji(!showEmoji)}
            className="w-9 h-9 flex items-center justify-center rounded-full hover:bg-gray-100 transition-colors"
            disabled={disabled}
          >
            <Smile size={20} className="text-gray-500" />
          </button>
          {showEmoji && (
            <div className="absolute bottom-11 left-0 z-30">
              <EmojiPicker
                onEmojiClick={handleEmojiClick}
                width={300}
                height={380}
                searchPlaceHolder="Tìm emoji..."
              />
            </div>
          )}
        </div>

        {/* File attach */}
        <button
          onClick={() => fileInputRef.current?.click()}
          className="w-9 h-9 flex items-center justify-center rounded-full hover:bg-gray-100 transition-colors"
          disabled={disabled}
          title="Đính kèm tệp"
        >
          <Paperclip size={20} className="text-gray-500" />
        </button>

        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          accept="image/*,video/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip"
          onChange={handleFileSelect}
        />

        {/* Text input */}
        <div className="flex-1">
          <textarea
            ref={inputRef}
            value={text}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            placeholder={mediaFile ? 'Nhấn gửi để gửi tệp...' : placeholder}
            disabled={disabled || !!mediaFile}
            rows={1}
            className="w-full resize-none bg-gray-100 rounded-2xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-200 max-h-32 overflow-y-auto"
            style={{ minHeight: '40px' }}
            onInput={(e) => {
              e.target.style.height = 'auto';
              e.target.style.height = Math.min(e.target.scrollHeight, 128) + 'px';
            }}
          />
        </div>

        {/* Send */}
        <button
          onClick={handleSend}
          disabled={disabled || (!text.trim() && !mediaFile)}
          className={`w-9 h-9 flex items-center justify-center rounded-full transition-colors flex-shrink-0 ${
            text.trim() || mediaFile
              ? 'bg-[#0068ff] text-white hover:bg-blue-600'
              : 'bg-gray-200 text-gray-400 cursor-not-allowed'
          }`}
        >
          <Send size={18} />
        </button>
      </div>
    </div>
  );
}
