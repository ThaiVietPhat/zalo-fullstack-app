import { useState } from 'react';
import { Flag, X } from 'lucide-react';
import { createReport } from '../../api/report';
import toast from 'react-hot-toast';

const REASONS = [
  'Spam',
  'Quấy rối',
  'Nội dung không phù hợp',
  'Tài khoản giả mạo',
  'Khác',
];

export default function ReportModal({ userId, userName, onClose }) {
  const [reason, setReason] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!reason) return;
    setLoading(true);
    try {
      await createReport(userId, { reason, description: description.trim() || undefined });
      toast.success('Đã gửi tố cáo. Chúng tôi sẽ xem xét sớm nhất.');
      onClose();
    } catch (err) {
      const msg = err?.response?.data?.message || 'Không thể gửi tố cáo';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/40">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-full bg-orange-100 flex items-center justify-center">
              <Flag size={15} className="text-orange-500" />
            </div>
            <div>
              <p className="text-sm font-semibold text-gray-800">Tố cáo người dùng</p>
              {userName && <p className="text-xs text-gray-400">{userName}</p>}
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-7 h-7 flex items-center justify-center rounded-full hover:bg-gray-100 transition-colors"
          >
            <X size={15} className="text-gray-500" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          <div>
            <label className="block text-xs font-medium text-gray-700 mb-2">
              Lý do tố cáo <span className="text-red-500">*</span>
            </label>
            <div className="space-y-2">
              {REASONS.map((r) => (
                <label key={r} className="flex items-center gap-2.5 cursor-pointer group">
                  <input
                    type="radio"
                    name="reason"
                    value={r}
                    checked={reason === r}
                    onChange={() => setReason(r)}
                    className="accent-orange-500"
                  />
                  <span className="text-sm text-gray-700 group-hover:text-gray-900">{r}</span>
                </label>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-700 mb-1.5">
              Mô tả thêm <span className="text-gray-400">(tuỳ chọn)</span>
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Cung cấp thêm thông tin chi tiết..."
              rows={3}
              maxLength={500}
              className="w-full border border-gray-200 rounded-xl px-3 py-2.5 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-orange-200"
            />
            <p className="text-right text-xs text-gray-400 mt-0.5">{description.length}/500</p>
          </div>

          <div className="flex gap-2 pt-1">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2.5 rounded-xl border border-gray-200 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={!reason || loading}
              className="flex-1 py-2.5 rounded-xl bg-orange-500 text-white text-sm font-medium hover:bg-orange-600 transition-colors disabled:opacity-50"
            >
              {loading ? 'Đang gửi...' : 'Gửi tố cáo'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
