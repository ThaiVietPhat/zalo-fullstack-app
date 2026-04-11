// components/Common/MainHeader/index.tsx
import React from "react";
import { View, Text, TouchableOpacity, StyleSheet, TextInput } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { SafeAreaView } from "react-native-safe-area-context";
import { StatusBar } from "expo-status-bar";

interface MainHeaderProps {
  title?: string;
  showSearch?: boolean;
}

const MainHeader = ({ title, showSearch = true }: MainHeaderProps) => {
  return (
    <SafeAreaView edges={["top"]} style={styles.wrapper}>
      {/* Hiện thông tin hệ thống chuẩn trên nền xanh */}
      <StatusBar style="light" backgroundColor="#0068FF" />
      
      <View style={styles.container}>
        {showSearch ? (
          <View style={styles.searchContainer}>
            <Ionicons name="search-outline" size={24} color="white" />
            <TouchableOpacity style={styles.inputArea}>
                <Text style={styles.placeholderText}>Tìm kiếm</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <Text style={styles.titleText}>{title}</Text>
        )}

        <View style={styles.actions}>
          <TouchableOpacity style={styles.iconBtn}>
            <Ionicons name="qr-code-outline" size={22} color="white" />
          </TouchableOpacity>
          <TouchableOpacity style={styles.iconBtn}>
            <Ionicons name="add-outline" size={28} color="white" />
          </TouchableOpacity>
        </View>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  wrapper: {
    backgroundColor: "#0068FF",
    zIndex: 100,
  },
  container: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
    paddingVertical: 8,
    height: 52,
  },
  searchContainer: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
  },
  inputArea: {
    flex: 1,
    marginLeft: 15,
  },
  placeholderText: {
    color: "rgba(255,255,255,0.7)",
    fontSize: 17,
    fontFamily: "Jakarta-Medium",
  },
  titleText: {
    flex: 1,
    color: "white",
    fontSize: 18,
    fontFamily: "Jakarta-Bold",
  },
  actions: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
  },
  iconBtn: {
    padding: 2,
  },
});

export default MainHeader;
