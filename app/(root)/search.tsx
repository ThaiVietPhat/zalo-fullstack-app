import React, { useState } from "react";
import { View, Text, TextInput, FlatList, TouchableOpacity, Image, ActivityIndicator } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { searchUsers, UserDto } from "@/api/user";
import { startOrGetChat } from "@/api/chat";

const SearchScreen = () => {
    const [keyword, setKeyword] = useState("");
    const [results, setResults] = useState<UserDto[]>([]);
    const [loading, setLoading] = useState(false);

    const handleSearch = async (text: string) => {
        setKeyword(text);
        if (text.length < 2) {
            setResults([]);
            return;
        }

        setLoading(true);
        try {
            const data = await searchUsers(text);
            setResults(data);
        } catch (error) {
            console.error("Search error:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleStartChat = async (userId: string, name: string) => {
        try {
            const chat = await startOrGetChat(userId);
            router.push({
                pathname: "/chat/[id]",
                params: { id: chat.id, name: name }
            });
        } catch (error) {
            console.error("Start chat error:", error);
        }
    };

    return (
        <SafeAreaView className="flex-1 bg-white">
            <View className="flex-row items-center px-4 py-2 border-b border-gray-100">
                <TouchableOpacity onPress={() => router.back()}>
                    <Ionicons name="arrow-back" size={24} color="#0068FF" />
                </TouchableOpacity>
                <View className="flex-1 flex-row items-center bg-gray-100 ml-3 px-3 py-1 rounded-full">
                    <Ionicons name="search" size={20} color="#9ca3af" />
                    <TextInput
                        className="flex-1 ml-2 font-JakartaMedium py-1"
                        placeholder="Tìm kiếm bạn bè qua email..."
                        value={keyword}
                        onChangeText={handleSearch}
                        autoFocus
                    />
                    {keyword.length > 0 && (
                        <TouchableOpacity onPress={() => handleSearch("")}>
                            <Ionicons name="close-circle" size={20} color="#9ca3af" />
                        </TouchableOpacity>
                    )}
                </View>
            </View>

            {loading ? (
                <View className="flex-1 justify-center items-center">
                    <ActivityIndicator size="large" color="#0068FF" />
                </View>
            ) : results.length > 0 ? (
                <FlatList
                    data={results}
                    keyExtractor={(item) => item.id}
                    renderItem={({ item }) => (
                        <TouchableOpacity 
                            className="flex-row items-center px-4 py-3 border-b border-gray-50"
                            onPress={() => handleStartChat(item.id, `${item.firstName} ${item.lastName}`)}
                        >
                            <Image 
                                source={{ uri: item.avatarUrl || `https://api.dicebear.com/9.x/avataaars/png?seed=${item.email}` }} 
                                className="w-12 h-12 rounded-full"
                            />
                            <View className="ml-3">
                                <Text className="font-JakartaBold text-base text-gray-800">
                                    {item.firstName} {item.lastName}
                                </Text>
                                <Text className="text-gray-500 text-sm font-JakartaMedium">
                                    {item.email}
                                </Text>
                            </View>
                            <View className="flex-1" />
                            <Ionicons name="chatbubble-ellipses-outline" size={24} color="#0068FF" />
                        </TouchableOpacity>
                    )}
                />
            ) : keyword.length >= 2 ? (
                <View className="flex-1 justify-center items-center px-10">
                    <Ionicons name="person-add-outline" size={64} color="#e5e7eb" />
                    <Text className="text-gray-400 text-center mt-4 font-JakartaMedium">
                        Không tìm thấy người dùng phù hợp với email này
                    </Text>
                </View>
            ) : (
                <View className="p-4">
                    <Text className="text-gray-400 font-JakartaMedium text-sm">Gợi ý: Nhập địa chỉ Email chính xác</Text>
                </View>
            )}
        </SafeAreaView>
    );
};

export default SearchScreen;
