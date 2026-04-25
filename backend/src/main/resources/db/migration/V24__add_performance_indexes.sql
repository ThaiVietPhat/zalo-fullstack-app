-- Performance indexes để tối ưu query speed
-- group_member indexes đã có qua @Index annotation nhưng thêm vào đây cho chắc chắn

-- Chat queries
CREATE INDEX IF NOT EXISTS idx_chat_user1_deleted ON chat(user1_id, deleted_by_user1);
CREATE INDEX IF NOT EXISTS idx_chat_user2_deleted ON chat(user2_id, deleted_by_user2);
CREATE INDEX IF NOT EXISTS idx_chat_last_modified ON chat(last_modified_date DESC);

-- Message queries
CREATE INDEX IF NOT EXISTS idx_message_chat_id ON message(chat_id);
CREATE INDEX IF NOT EXISTS idx_message_created_date ON message(chat_id, created_date DESC);

-- Group message queries
CREATE INDEX IF NOT EXISTS idx_group_message_group_id ON group_message(group_id);
CREATE INDEX IF NOT EXISTS idx_group_message_created_date ON group_message(group_id, created_date DESC);

-- Friend request queries
CREATE INDEX IF NOT EXISTS idx_friend_request_sender ON friend_request(sender_id, status);
CREATE INDEX IF NOT EXISTS idx_friend_request_receiver ON friend_request(receiver_id, status);
