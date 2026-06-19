import Svg, { Defs, LinearGradient, Path, RadialGradient, Rect, Stop } from 'react-native-svg';

interface AuthLogoProps {
  size?: number;
}

// 上传的 SVG 以 React Native SVG 组件渲染，避免 Expo Go 直接 require SVG 资源。
export function AuthLogo({ size = 76 }: AuthLogoProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 1192 1198">
      <Defs>
        <LinearGradient id="authLogoBg" x1="0" y1="0" x2="1" y2="1">
          <Stop offset="0" stopColor="#2b2b2b" />
          <Stop offset="0.35" stopColor="#101010" />
          <Stop offset="1" stopColor="#020202" />
        </LinearGradient>
        <RadialGradient id="authLogoSoft" cx="18%" cy="9%" r="72%">
          <Stop offset="0" stopColor="#4a4a4a" stopOpacity="0.46" />
          <Stop offset="0.55" stopColor="#111111" stopOpacity="0.12" />
          <Stop offset="1" stopColor="#000000" stopOpacity="0" />
        </RadialGradient>
      </Defs>
      <Rect width="1192" height="1198" fill="#fff" />
      <Rect x="4" y="3" width="1184" height="1188" rx="118" ry="118" fill="url(#authLogoBg)" />
      <Rect x="4" y="3" width="1184" height="1188" rx="118" ry="118" fill="url(#authLogoSoft)" />
      <Path
        d="M 249 303 L 231 316 L 220 334 L 217 347 L 217 359 L 225 380 L 384 571 L 451 486 L 451 484 L 303 310 L 291 303 L 277 299 L 264 299 Z M 992 413 L 929 345 L 840 303 L 733 299 L 640 335 L 579 389 L 225 842 L 217 863 L 224 895 L 257 917 L 300 905 L 656 456 L 699 420 L 768 399 L 811 402 L 859 423 L 898 461 L 925 512 L 938 571 L 937 646 L 920 712 L 892 762 L 854 798 L 813 817 L 768 822 L 724 813 L 666 776 L 563 641 L 499 721 L 600 848 L 681 900 L 738 915 L 799 916 L 857 903 L 905 880 L 958 836 L 990 793 L 1035 674 L 1036 530 L 1019 467 Z"
        fill="#fff"
        fillRule="evenodd"
      />
    </Svg>
  );
}
