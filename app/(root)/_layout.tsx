import { Stack } from "expo-router";

const Layout = () => {
  return (
    <Stack>
      <Stack.Screen name="tabs" options={{ headerShown: false }} />
      <Stack.Screen name="chat/[id]" options={{ headerShown: false }} />
      <Stack.Screen name="personal-wall" options={{ headerShown: false }} />
      <Stack.Screen name="personal-setting" options={{ headerShown: false }} />
    </Stack>
  );
};

export default Layout;
