import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:xoassets_mobile/app/app.dart';

void main() {
  testWidgets('starts shell and reaches main tabs', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: XoAssetsApp()));

    expect(find.text('小〇财迹'), findsOneWidget);
    expect(find.byType(MaterialApp), findsOneWidget);

    await tester.pump(const Duration(milliseconds: 600));
    await tester.pumpAndSettle();

    expect(find.text('早上好，看看今天的钱包'), findsOneWidget);
    expect(find.text('首页'), findsOneWidget);
  });
}
