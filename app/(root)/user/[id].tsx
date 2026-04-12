import React, { useEffect, useState } from "react";
import { View, Text, Image, TouchableOpacity, ScrollView, ActivityIndicator, Alert } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useLocalSearchParams, router } from "expo-router";
import { useQuery } from "@tanstack/react-query";
import { fetchAPI } from "@/lib/fetch";
import { UserDto } from "@/api/user";
import { useSendFriendRequest, useSentRequests, useUnfriend, useContacts } from "@/hooks/useFriend";
import { useStartChat } from "@/hooks/useChat";
import { getAvatarUrl, formatFullName } from "@/lib/utils";

const fetchUser = async (id: string): Promise<UserDto> => {
  return fetchAPI(`/user/${id}`);
};

export default function UserProfileScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  if (!id) return null;

  const { data: user, isLoading } = useQuery({
    queryKey: ["user", id],
    queryFn: () => fetchUser(id),
  });

  const { data: contacts } = useContacts();
  const { data: sentRequests } = useSentRequests();
  const { mutate: sendReq, isPending: isSendingReq } = useSendFriendRequest();
  const { mutate: unfriend, isPending: isUnfriending } = useUnfriend();
  const { mutate: startChat, isPending: isStartingChat } = useStartChat();

  const isFriend = contacts?.some(c => c.id === id);
  const isSent = sentRequests?.some(r => r.receiverId === id);

  const handleStartChat = () => {
    startChat(id, {
      onSuccess: (chat) => {
        router.push({ pathname: "/(root)/chat/[id]", params: { id: chat.id, name: name } });
      }
    });
  };

  const handleFriendAction = () => {
    if (isFriend) {
      Alert.alert("Hủy kết bạn", `Bạn có chắc chắn muốn hủy kết bạn với ${name}?`, [
        { text: "Hủy", style: "cancel" },
        { text: "Đồng ý", style: "destructive", onPress: () => unfriend(id) }
      ]);
    } else if (!isSent) {
      sendReq(id);
    }
  };

  if (isLoading || !user) {
    return (
      <View className="flex-1 bg-white justify-center items-center">
        <ActivityIndicator size="large" color="#0068FF" />
      </View>
    );
  }

  const name = formatFullName(user.firstName, user.lastName);

  return (
    <View className="flex-1 bg-white">
      {/* Absolute Header overlay */}
      <View className="absolute top-12 left-4 z-10 flex-row items-center">
        <TouchableOpacity
          className="bg-black/30 w-10 h-10 rounded-full items-center justify-center"
          onPress={() => router.back()}
        >
          <Ionicons name="chevron-back" size={24} color="white" />
        </TouchableOpacity>
      </View>

      <ScrollView bounces={false}>
        {/* Cover Image Placeholder */}
        <View className="h-64 bg-gray-200 w-full relative overflow-hidden">
          <Image
            source={{ uri: "https://images.unsplash.com/photo-1579546929518-9e396f3cc809" }} // Generic background
            className="w-full h-full"
            resizeMode="cover"
          />
        </View>

        {/* Profile Info */}
        <View className="px-4 pb-6 border-b border-gray-100 bg-white">
          <View className="flex-row items-end justify-between -mt-12 mb-4">
            <View className="bg-white p-1 rounded-full relative">
              <Image
                source={{ uri: getAvatarUrl(name, user.avatarUrl) }}
                className="w-24 h-24 rounded-full bg-gray-100"
              />
              {user.online && (
                <View className="absolute bottom-1 right-2 w-5 h-5 bg-green-500 rounded-full border-2 border-white" />
              )}
            </View>
          </View>

          <View className="mb-6">
            <Text className="text-2xl font-JakartaBold text-gray-800">{name}</Text>
          </View>

          {/* Action Buttons */}
          <View className="flex-row gap-3">
            <TouchableOpacity
              className="flex-1 items-center justify-center flex-row bg-[#0068FF] rounded-full py-3"
              onPress={handleStartChat}
              disabled={isStartingChat}
            >
              <Ionicons name="chatbubble-ellipses" size={20} color="white" />
              <Text className="text-white font-JakartaMedium ml-2">Nhắn tin</Text>
            </TouchableOpacity>

            <TouchableOpacity
              className={`w-14 items-center justify-center rounded-full ${isFriend ? 'bg-gray-100' : isSent ? 'bg-orange-50' : 'bg-gray-100'}`}
              onPress={handleFriendAction}
              disabled={isSendingReq || isUnfriending || isSent}
            >
              {isSendingReq || isUnfriending ? (
                <ActivityIndicator color="#0068FF" />
              ) : isFriend ? (
                <View className="items-center">
                  <Ionicons name="person-remove" size={20} color="#EF4444" />
                  <Text className="text-[10px] text-red-500 font-JakartaMedium">Hủy</Text>
                </View>
              ) : isSent ? (
                <View className="items-center">
                  <Ionicons name="time" size={20} color="#F97316" />
                  <Text className="text-[10px] text-orange-500 font-JakartaMedium">Đã gửi</Text>
                </View>
              ) : (
                <View className="items-center">
                  <Ionicons name="person-add" size={20} color="#0068FF" />
                  <Text className="text-[10px] text-blue-500 font-JakartaMedium">Kết bạn</Text>
                </View>
              )}
            </TouchableOpacity>
          </View>

          {isSent && (
            <Text className="text-center text-orange-500 font-Jakarta text-xs mt-2">
              Đã gửi lời mời kết bạn
            </Text>
          )}
        </View>

        {/* Details Section */}
        <View className="mt-2 bg-white">
          <View className="px-4 py-3 bg-gray-50 border-b border-gray-100">
            <Text className="font-JakartaBold text-gray-700">Thông tin cá nhân</Text>
          </View>

          <View className="px-4 py-4 border-b border-gray-50 flex-row">
            <Text className="w-24 text-gray-400 font-JakartaMedium">Email</Text>
            <Text className="flex-1 font-Jakarta text-gray-800">{user.email}</Text>
          </View>

          <View className="px-4 py-4 border-b border-gray-50 flex-row">
            <Text className="w-24 text-gray-400 font-JakartaMedium">Tình trạng</Text>
            <Text className="flex-1 font-Jakarta text-gray-800">
              {user.online ? 'Đang hoạt động' : (user.lastSeen ? `Truy cập ${new Date(user.lastSeen).toLocaleDateString()}` : 'Offline')}
            </Text>
          </View>
        </View>
      </ScrollView>
    </View>
  );
}
