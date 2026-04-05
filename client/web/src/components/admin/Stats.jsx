import React, { useEffect, useState } from 'react';
import { Users, MessageCircle, Group, Wifi, Ban, TrendingUp } from 'lucide-react';
import { getStats } from '../../api/admin';

function StatCard({ icon: Icon, label, value, color }) {
  return (
    <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-500">{label}</p>
          <p className={`text-2xl font-bold mt-1 ${color}`}>{value ?? '—'}</p>
        </div>
        <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${color.replace('text-', 'bg-').replace('-600', '-100')}`}>
          <Icon size={22} className={color} />
        </div>
      </div>
    </div>
  );
}

export default function Stats() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getStats()
      .then((res) => setStats(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
        <StatCard icon={Users} label="Tổng người dùng" value={stats?.totalUsers} color="text-blue-600" />
        <StatCard icon={MessageCircle} label="Tổng tin nhắn" value={stats?.totalMessages} color="text-green-600" />
        <StatCard icon={Users} label="Tổng nhóm" value={stats?.totalGroups} color="text-purple-600" />
        <StatCard icon={Wifi} label="Đang trực tuyến" value={stats?.onlineUsers} color="text-emerald-600" />
        <StatCard icon={Ban} label="Người dùng bị khóa" value={stats?.bannedUsers} color="text-red-600" />
      </div>

      {/* Daily message chart (simple bar) */}
      {stats?.dailyMessageCounts && stats.dailyMessageCounts.length > 0 && (
        <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <div className="flex items-center gap-2 mb-4">
            <TrendingUp size={18} className="text-blue-500" />
            <h3 className="font-semibold text-gray-700">Tin nhắn theo ngày (7 ngày qua)</h3>
          </div>
          <div className="flex items-end gap-2 h-32">
            {stats.dailyMessageCounts.map((item, i) => {
              const maxCount = Math.max(...stats.dailyMessageCounts.map((d) => d.count || 0), 1);
              const height = ((item.count || 0) / maxCount) * 100;
              return (
                <div key={i} className="flex-1 flex flex-col items-center gap-1">
                  <span className="text-xs text-gray-500">{item.count || 0}</span>
                  <div
                    className="w-full bg-blue-500 rounded-t-sm transition-all"
                    style={{ height: `${Math.max(height, 4)}%` }}
                  />
                  <span className="text-[10px] text-gray-400 truncate w-full text-center">
                    {item.date ? new Date(item.date).toLocaleDateString('vi-VN', { month: 'short', day: 'numeric' }) : `Ngày ${i + 1}`}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Top active users */}
      {stats?.topActiveUsers && stats.topActiveUsers.length > 0 && (
        <div className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <h3 className="font-semibold text-gray-700 mb-4">Người dùng hoạt động nhất</h3>
          <div className="space-y-3">
            {stats.topActiveUsers.map((user, i) => (
              <div key={user.id || i} className="flex items-center gap-3">
                <span className="text-sm font-bold text-gray-400 w-6">{i + 1}</span>
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-800">
                    {user.firstName} {user.lastName}
                  </p>
                  <p className="text-xs text-gray-400">{user.email}</p>
                </div>
                <span className="text-sm font-semibold text-blue-600">{user.messageCount || 0} tin nhắn</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
