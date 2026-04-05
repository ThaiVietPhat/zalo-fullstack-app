// app/(root)/tabs/activities.tsx — Danh bạ (Contacts)
import React, { useState } from "react";
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  Image,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useUsers, useStartChat } from "@/hooks/useChat";
import { ChatDto } from "@/api/chat";
import { UserDto } from "@/api/user";
import { router } from "expo-router";
import MainHeader from "@/components/Common/MainHeader";
import { getAvatarUrl, formatFullName } from "@/lib/utils";

export default function ActivitiesScreen() {
  const [search, setSearch] = useState("");
  const { data: users, isLoading } = useUsers();
  const { mutate: startChat, isPending } = useStartChat();

  const filtered = users?.filter((u: UserDto) =>
    `${u.firstName} ${u.lastName} ${u.email}`
      .toLowerCase()
      .includes(search.toLowerCase())
  );

  const handleStartChat = (userId: string) => {
    startChat(userId, {
      onSuccess: (chat: ChatDto) => {
        router.push(`/(root)/chat/${chat.id}`);
      },
    });
  };

  return (
    <View className="flex-1 bg-white">
      <MainHeader title="Danh bạ" showSearch={true} />

      {isLoading ? (
        <View className="flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#0068FF" />
        </View>
      ) : (
        <FlatList
          data={filtered}
          keyExtractor={(item) => item.id}
          contentContainerStyle={{ paddingBottom: 20 }}
          renderItem={({ item }) => {
            const name = formatFullName(item.firstName, item.lastName);
            return (
              <TouchableOpacity
                onPress={() => handleStartChat(item.id)}
                className="flex-row items-center px-4 py-3 border-b border-gray-100"
              >
                <View className="relative">
                  <Image
                    source={{ uri: getAvatarUrl(name, item.avatarUrl) }}
                    className="w-12 h-12 rounded-full bg-gray-200"
                  />
                  {item.online && (
                    <View className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-white" />
                  )}
                </View>
                <View className="ml-3 flex-1">
                  <Text className="font-JakartaBold text-gray-800 text-base">
                    {name}
                  </Text>
                  <Text className="text-gray-500 font-JakartaMedium text-sm">
                    {item.email}
                  </Text>
                </View>
                <Ionicons name="chatbubble-outline" size={20} color="#0068FF" />
              </TouchableOpacity>
            );
          }}
          ListEmptyComponent={() => (
            <View className="flex-1 justify-center items-center mt-20">
              <Ionicons name="people-outline" size={48} color="#CBD5E1" />
              <Text className="text-gray-400 font-JakartaMedium mt-2">
                Không tìm thấy người dùng
              </Text>
            </View>
          )}
        />
      )}
    </View>
  );
}
