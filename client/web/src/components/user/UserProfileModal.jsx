import { useState, useEffect } from 'react';
import { MessageCircle, UserPlus, UserCheck, Check, X, Loader } from 'lucide-react';
import Modal from '../common/Modal';
import Avatar from '../common/Avatar';
import useAuthStore from '../../store/authStore';
import useChatStore from '../../store/chatStore';
import { getUserById } from '../../api/user';
import { sendFriendRequest, acceptFriendRequest, rejectFriendRequest } from '../../api/friendRequest';
import { startChat } from '../../api/chat';
import toast from 'react-hot-toast';

export default function UserProfileModal() {
  const { auth } = useAuthStore();
  const {
    viewingProfileId, setViewingProfileId,
    pendingRequests, removePendingRequest, addContact, addSentRequest,
    setChats, setActiveChatId, setActiveTab,
  } = useChatStore();

  const isOpen = viewingProfileId !== null;

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [friendStatus, setFriendStatus] = useState('NONE');
  const [loadingAction, setLoadingAction] = useState(null);

  useEffect(() => {
    if (!viewingProfileId) return;
    setProfile(null);
    setLoading(true);

    getUserById(viewingProfileId)
      .then((res) => {
        setProfile(res.data);
        setFriendStatus(res.data.friendshipStatus || 'NONE');
      })
      .catch(() => toast.error('Không thể tải thông tin người dùng'))
      .finally(() => setLoading(false));
  }, [viewingProfileId]);

  const handleSendRequest = async () => {
    setLoadingAction('add');
    try {
      const res = await sendFriendRequest(viewingProfileId);
      addSentRequest(res.data);
      setFriendStatus('PENDING_SENT');
      toast.success('Đã gửi lời mời kết bạn');
    } catch (e) {
      toast.error(e?.response?.data?.message || 'Không thể gửi lời mời');
    } finally {
      setLoadingAction(null);
    }
  };

  const handleAccept = async () => {
    const req = pendingRequests.find((r) => r.senderId === viewingProfileId);
    if (!req) return;
    setLoadingAction('accept');
    try {
      await acceptFriendRequest(req.id);
      removePendingRequest(req.id);
      addContact({ ...profile, friendshipStatus: 'ACCEPTED' });
      setFriendStatus('ACCEPTED');
      toast.success('Đã chấp nhận lời mời kết bạn');
    } catch {
      toast.error('Không thể chấp nhận lời mời');
    } finally {
      setLoadingAction(null);
    }
  };

  const handleReject = async () => {
    const req = pendingRequests.find((r) => r.senderId === viewingProfileId);
    if (!req) return;
    setLoadingAction('reject');
    try {
      await rejectFriendRequest(req.id);
      removePendingRequest(req.id);
      setFriendStatus('NONE');
    } catch {
      toast.error('Không thể từ chối');
    } finally {
      setLoadingAction(null);
    }
  };

  const handleStartChat = async () => {
    setLoadingAction('chat');
    try {
      const res = await startChat(viewingProfileId);
      const chat = res.data;
      setChats((prev) => Array.isArray(prev) ? (prev.find((c) => c.id === chat.id) ? prev : [chat, ...prev]) : [chat]);
      setActiveChatId(chat.id);
      setActiveTab('chats');
      setViewingProfileId(null);
    } catch {
      toast.error('Không thể bắt đầu cuộc trò chuyện');
    } finally {
      setLoadingAction(null);
    }
  };

  const isSelf = auth?.userId === viewingProfileId;
  const profileName = profile ? `${profile.firstName || ''} ${profile.lastName || ''}`.trim() : '';

  return (
    <Modal isOpen={isOpen} onClose={() => setViewingProfileId(null)} title="Trang cá nhân" size="xl">
      <div className="p-6 flex flex-col gap-5">
        {loading && (
          <div className="flex justify-center py-10">
            <div className="w-6 h-6 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {!loading && profile && (
          <div className="flex flex-col items-center gap-3 pb-4">
            <Avatar src={profile.avatarUrl} name={profileName} size={80} online={profile.online} />
            <div className="text-center">
              <h3 className="text-lg font-bold text-gray-800">{profileName}</h3>
              <p className="text-sm text-gray-400">{profile.email}</p>
              <p className={`text-xs mt-1 font-medium ${profile.online ? 'text-green-500' : 'text-gray-400'}`}>
                {profile.online ? 'Đang hoạt động' : 'Không hoạt động'}
              </p>
            </div>

            {!isSelf && !profile.banned && (
              <div className="flex items-center gap-2 mt-1">
                {friendStatus === 'NONE' && (
                  <button
                    onClick={handleSendRequest}
                    disabled={loadingAction === 'add'}
                    className="flex items-center gap-1.5 px-4 py-2 rounded-full bg-blue-500 text-white hover:bg-blue-600 text-sm font-medium transition-colors disabled:opacity-50"
                  >
                    {loadingAction === 'add' ? <Loader size={14} className="animate-spin" /> : <UserPlus size={14} />}
                    Kết bạn
                  </button>
                )}
                {friendStatus === 'PENDING_SENT' && (
                  <span className="flex items-center gap-1.5 px-4 py-2 rounded-full bg-gray-100 text-gray-500 text-sm font-medium">
                    <UserCheck size={14} /> Đã gửi lời mời
                  </span>
                )}
                {friendStatus === 'PENDING_RECEIVED' && (
                  <>
                    <button
                      onClick={handleAccept}
                      disabled={loadingAction === 'accept'}
                      className="flex items-center gap-1.5 px-4 py-2 rounded-full bg-blue-500 text-white hover:bg-blue-600 text-sm font-medium transition-colors disabled:opacity-50"
                    >
                      {loadingAction === 'accept' ? <Loader size={14} className="animate-spin" /> : <Check size={14} />}
                      Chấp nhận
                    </button>
                    <button
                      onClick={handleReject}
                      disabled={loadingAction === 'reject'}
                      className="flex items-center gap-1.5 px-4 py-2 rounded-full bg-gray-100 text-gray-600 hover:bg-red-50 hover:text-red-400 text-sm font-medium transition-colors disabled:opacity-50"
                    >
                      {loadingAction === 'reject' ? <Loader size={14} className="animate-spin" /> : <X size={14} />}
                      Từ chối
                    </button>
                  </>
                )}
                {friendStatus === 'ACCEPTED' && (
                  <button
                    onClick={handleStartChat}
                    disabled={loadingAction === 'chat'}
                    className="flex items-center gap-1.5 px-4 py-2 rounded-full bg-green-500 text-white hover:bg-green-600 text-sm font-medium transition-colors disabled:opacity-50"
                  >
                    {loadingAction === 'chat' ? <Loader size={14} className="animate-spin" /> : <MessageCircle size={14} />}
                    Nhắn tin
                  </button>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
}
