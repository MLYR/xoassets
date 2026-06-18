import Constants from 'expo-constants';
import { Platform } from 'react-native';

function getExpoHostIp() {
  const hostUri = Constants.expoConfig?.hostUri || Constants.manifest2?.hostUri || Constants.platform?.hostUri;
  const host = hostUri?.split(':')[0];
  return host || null;
}

export function getApiBaseUrl() {
  const configured = process.env.EXPO_PUBLIC_API_BASE_URL;
  if (configured) {
    if (Platform.OS === 'android' && configured.includes('localhost')) {
      return configured.replace('localhost', '10.0.2.2');
    }
    return configured;
  }

  const host = getExpoHostIp();
  if (!host) {
    return 'http://localhost:8080';
  }

  return Platform.OS === 'android' ? `http://10.0.2.2:8080` : `http://${host}:8080`;
}
