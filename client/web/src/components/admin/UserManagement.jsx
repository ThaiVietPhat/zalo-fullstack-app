import React, { useEffect, useState } from 'react';
import { Ban, CheckCircle, Trash2, ShieldCheck, ShieldOff, ChevronLeft, ChevronRight } from 'lucide-react';
import { getAdminUsers, banUser, unbanUser, deleteUser, promoteUser, demoteUser } from '../../api/admin';
import Avatar from '../common/Avatar';
import toast from 'react-hot-toast';

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);

  const fetchUsers = (p = 0) => {
    setLoading(true);
    getAdminUsers(p, 20)
      .then((res) => {
        const data = res.data;
        if (Array.isArray(data)) {
          setUsers(data);
          setTotalPages(1);
        } else {
          setUsers(data.content || []);
          setTotalPages(data.totalPages || 1);
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchUsers(page); }, [page]);

  const handleBan = async (userId, isBanned) => {
    try {
      if (isBanned) {
        await unbanUser(userId);
        setUsers((prev) => prev.map((u) => u.id === userId ? { ...u, banned: false } : u));
        toast.success('Đã mở khóa tài khoản');
      } else {
        await banUser(userId);
        setUsers((prev) => prev.map((u) => u.id === userId ? { ...u, banned: true } : u));
        toast.success('Đã khóa tài khoản');
      }
    } catch { toast.error('Thao tác thất bại'); }
  };

  const handleDelete = async (userId) => {
    if (!window.confirm('Bạn chắc chắn muốn xóa người dùng này?')) return;
    try {
      await deleteUser(userId);
      setUsers((prev) => prev.filter((u) => u.id !== userId));
      toast.success('Đã xóa người dùng');
    } catch { toast.error('Không thể xóa người dùng'); }
  };

  const handlePromote = async (userId, isAdmin) => {
    try {
      if (isAdmin) {
        await demoteUser(userId);
        setUsers((prev) => prev.map((u) => u.id === userId ? { ...u, role: 'ROLE_USER' } : u));
        toast.success('Đã hạ cấp người dùng');
      } else {
        await promoteUser(userId);
        setUsers((prev) => prev.map((u) => u.id === userId ? { ...u, role: 'ROLE_ADMIN' } : u));
        toast.success('Đã thăng cấp Admin');
      }
    } catch { toast.error('Thao tác thất bại'); }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-gray-700">Quản lý người dùng</h3>
        <span className="text-sm text-gray-400">{users.length} người dùng</span>
      </div>

      {loading ? (
        <div className="flex justify-center py-8">
          <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-100">
                  <th className="text-left text-xs font-medium text-gray-500 px-4 py-3">Người dùng</th>
                  <th className="text-left text-xs font-medium text-gray-500 px-4 py-3">Email</th>
                  <th className="text-left text-xs font-medium text-gray-500 px-4 py-3">Vai trò</th>
                  <th className="text-left text-xs font-medium text-gray-500 px-4 py-3">Trạng thái</th>
                  <th className="text-right text-xs font-medium text-gray-500 px-4 py-3">Hành động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {users.map((user) => {
                  const isAdmin = user.role === 'ROLE_ADMIN';
                  return (
                    <tr key={user.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          <Avatar
                            src={user.avatarUrl}
                            name={`${user.firstName} ${user.lastName}`}
                            size={34}
                            online={user.online}
                          />
                          <span className="text-sm font-medium text-gray-800">
                            {user.firstName} {user.lastName}
                          </span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-500">{user.email}</td>
                      <td className="px-4 py-3">
                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                          isAdmin
                            ? 'bg-blue-100 text-blue-600'
                            : 'bg-gray-100 text-gray-500'
                        }`}>
                          {isAdmin ? 'Admin' : 'User'}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                          user.banned
                            ? 'bg-red-100 text-red-600'
                            : 'bg-green-100 text-green-600'
                        }`}>
                          {user.banned ? 'Bị khóa' : 'Hoạt động'}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            onClick={() => handleBan(user.id, user.banned)}
                            title={user.banned ? 'Mở khóa' : 'Khóa tài khoản'}
                            className={`w-7 h-7 flex items-center justify-center rounded-full transition-colors ${
                              user.banned
                                ? 'hover:bg-green-100 text-green-500'
                                : 'hover:bg-orange-100 text-orange-500'
                            }`}
                          >
                            {user.banned ? <CheckCircle size={15} /> : <Ban size={15} />}
                          </button>
                          <button
                            onClick={() => handlePromote(user.id, isAdmin)}
                            title={isAdmin ? 'Hạ cấp' : 'Thăng cấp Admin'}
                            className="w-7 h-7 flex items-center justify-center rounded-full hover:bg-blue-100 text-blue-500 transition-colors"
                          >
                            {isAdmin ? <ShieldOff size={15} /> : <ShieldCheck size={15} />}
                          </button>
                          <button
                            onClick={() => handleDelete(user.id)}
                            title="Xóa người dùng"
                            className="w-7 h-7 flex items-center justify-center rounded-full hover:bg-red-100 text-red-400 transition-colors"
                          >
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="flex items-center gap-1 text-sm text-gray-600 hover:text-blue-600 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                <ChevronLeft size={16} /> Trước
              </button>
              <span className="text-sm text-gray-500">
                Trang {page + 1} / {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="flex items-center gap-1 text-sm text-gray-600 hover:text-blue-600 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                Sau <ChevronRight size={16} />
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
