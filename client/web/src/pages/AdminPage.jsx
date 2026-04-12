import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { BarChart3, Users, Group, ArrowLeft, ClipboardList } from 'lucide-react';
import Stats from '../components/admin/Stats';
import UserManagement from '../components/admin/UserManagement';
import GroupManagement from '../components/admin/GroupManagement';
import AuditLog from '../components/admin/AuditLog';

const tabs = [
  { id: 'stats', icon: BarChart3, label: 'Thống kê' },
  { id: 'users', icon: Users, label: 'Người dùng' },
  { id: 'groups', icon: Group, label: 'Nhóm' },
  { id: 'audit', icon: ClipboardList, label: 'Nhật ký' },
];

export default function AdminPage() {
  const [activeTab, setActiveTab] = useState('stats');

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top navbar */}
      <div className="bg-[#0068ff] text-white px-6 py-4 shadow-md">
        <div className="max-w-7xl mx-auto flex items-center gap-4">
          <Link
            to="/chat"
            className="flex items-center gap-2 text-blue-100 hover:text-white transition-colors text-sm"
          >
            <ArrowLeft size={18} />
            Quay lại
          </Link>
          <div className="w-px h-5 bg-blue-400" />
          <h1 className="text-lg font-bold">Bảng quản trị</h1>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-6">
        {/* Tab navigation */}
        <div className="flex gap-2 mb-6 bg-white rounded-xl p-1.5 shadow-sm border border-gray-100 w-fit flex-wrap">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-2 px-5 py-2 rounded-lg text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-[#0068ff] text-white shadow-sm'
                    : 'text-gray-600 hover:bg-gray-50'
                }`}
              >
                <Icon size={16} />
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Tab content */}
        <div>
          {activeTab === 'stats' && <Stats />}
          {activeTab === 'users' && <UserManagement />}
          {activeTab === 'groups' && <GroupManagement />}
          {activeTab === 'audit' && <AuditLog />}
        </div>
      </div>
    </div>
  );
}
