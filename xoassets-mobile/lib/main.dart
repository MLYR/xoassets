import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app/app.dart';

/// XOAssets Mobile V2 的启动入口，统一挂载 Riverpod ProviderScope。
void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const ProviderScope(child: XoAssetsApp()));
}
