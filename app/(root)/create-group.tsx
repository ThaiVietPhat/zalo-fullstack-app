import React, { useState } from "react";
import { View, Text, FlatList, TouchableOpacity, Image, TextInput, ActivityIndicator, Alert } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { getAllUsers, UserDto } from "@/api/user";
import { createGroup } from "@/api/group";
import { useQuery } from "@tanstack/react-query";

const CreateGroupScreen = () => {
    const [groupName, setGroupName] = useState("");
    const [selectedUsers, setSelectedUsers] = useState<string[]>([]);
    const [isCreating, setIsCreating] = useState(false);

    const { data: users, isLoading } = useQuery({
        queryKey: ["users"],
        queryFn: getAllUsers
    });

    const toggleUser = (userId: string) => {
        if (selectedUsers.includes(userId)) {
            setSelectedUsers(selectedUsers.filter(id => id !== userId));
        } else {
            setSelectedUsers([...selectedUsers, userId]);
        }
    };

    const handleCreate = async () => {
        if (!groupName.trim()) {
            Alert.alert("Lỗi", "Vui lòng nhập tên nhóm");
            return;
        }
        if (selectedUsers.length < 2) {
            Alert.alert("Lỗi", "Nhóm phải có ít nhất 2 thành viên khác");
            return;
        }

        setIsCreating(true);
        try {
            const group = await createGroup({
                name: groupName,
                memberIds: selectedUsers,
            });
            Alert.alert("Thành công", "Đã tạo nhóm chat mới");
            router.replace({
                pathname: "/chat/[id]",
                params: { id: group.id, name: group.name, isGroup: "true" }
            });
        } catch (error) {
            console.error("Create group error:", error);
            Alert.alert("Lỗi", "Không thể tạo nhóm lúc này");
        } finally {
            setIsCreating(false);
        }
    };

    return (
        <SafeAreaView className="flex-1 bg-white">
            <View className="flex-row items-center justify-between px-4 py-3 border-b border-gray-100 bg-[#0068FF]">
                <View className="flex-row items-center">
                    <TouchableOpacity onPress={() => router.back()}>
                        <Ionicons name="close" size={28} color="white" />
                    </TouchableOpacity>
                    <Text className="text-white text-lg font-JakartaBold ml-4">Nhóm mới</Text>
                </View>
                <TouchableOpacity
                    onPress={handleCreate}
                    disabled={isCreating}
                    className={`px-4 py-1.5 rounded-full ${selectedUsers.length >= 2 ? 'bg-white' : 'bg-white/50'}`}
                >
                    <Text className="text-[#0068FF] font-JakartaBold">TẠO</Text>
                </TouchableOpacity>
            </View>

            <View className="p-4 border-b border-gray-100 flex-row items-center">
                <View className="w-12 h-12 bg-gray-100 rounded-full items-center justify-center">
                    <Ionicons name="camera" size={24} color="#9ca3af" />
                </View>
                <TextInput
                    className="flex-1 ml-4 text-lg font-JakartaMedium"
                    placeholder="Đặt tên nhóm"
                    value={groupName}
                    onChangeText={setGroupName}
                />
            </View>

            <View className="bg-gray-50 px-4 py-2">
                <Text className="text-gray-500 font-JakartaBold text-xs">CHỌN THÀNH VIÊN ({selectedUsers.length})</Text>
            </View>

            {isLoading ? (
                <View className="flex-1 justify-center items-center">
                    <ActivityIndicator size="large" color="#0068FF" />
                </View>
            ) : (
                <FlatList
                    data={users}
                    keyExtractor={(item) => item.id}
                    renderItem={({ item }) => (
                        <TouchableOpacity
                            className="flex-row items-center px-4 py-3 border-b border-gray-50"
                            onPress={() => toggleUser(item.id)}
                        >
                            <Image
                                source={{ uri: item.avatarUrl || `https://api.dicebear.com/9.x/avataaars/png?seed=${item.email}` }}
                                className="w-12 h-12 rounded-full"
                            />
                            <View className="ml-3 flex-1">
                                <Text className="font-JakartaBold text-base text-gray-800">
                                    {item.firstName} {item.lastName}
                                </Text>
                                <Text className="text-gray-500 text-sm font-JakartaMedium">
                                    {item.email}
                                </Text>
                            </View>
                            <Ionicons
                                name={selectedUsers.includes(item.id) ? "checkbox" : "square-outline"}
                                size={24}
                                color={selectedUsers.includes(item.id) ? "#0068FF" : "#d1d5db"}
                            />
                        </TouchableOpacity>
                    )}
                />
            )}

            {isCreating && (
                <View className="absolute inset-0 bg-black/20 justify-center items-center">
                    <View className="bg-white p-6 rounded-2xl items-center">
                        <ActivityIndicator size="large" color="#0068FF" />
                        <Text className="mt-4 font-JakartaBold text-gray-800">Đang tạo nhóm...</Text>
                    </View>
                </View>
            )}
        </SafeAreaView>
    );
};

export default CreateGroupScreen;
