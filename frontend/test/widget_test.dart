import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:school_erp/src/app/school_erp_app.dart';
import 'package:school_erp/src/core/storage/token_storage.dart';

void main() {
  testWidgets('renders login screen for unauthenticated users', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          tokenStorageProvider.overrideWithValue(const _EmptyTokenStorage()),
        ],
        child: const SchoolErpApp(),
      ),
    );

    await tester.pump();
    await tester.pump(const Duration(milliseconds: 50));

    expect(find.text('School ERP'), findsWidgets);
    expect(find.text('Admin Panel'), findsOneWidget);
    expect(find.text('Email'), findsOneWidget);
  });
}

class _EmptyTokenStorage implements TokenStorage {
  const _EmptyTokenStorage();

  @override
  Future<void> clear() async {}

  @override
  Future<String?> readAccessToken() async => null;

  @override
  Future<String?> readRefreshToken() async => null;

  @override
  Future<void> saveTokens({
    required String accessToken,
    String? refreshToken,
  }) async {}
}
