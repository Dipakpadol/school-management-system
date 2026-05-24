import 'package:flutter_test/flutter_test.dart';
import 'package:school_erp/src/core/error/failure.dart';
import 'package:school_erp/src/core/result/result.dart';

void main() {
  test('Success resolves through success branch', () {
    const result = Success<int>(42);

    final value = result.when(
      success: (value) => value,
      failure: (_) => 0,
    );

    expect(value, 42);
  });

  test('FailureResult resolves through failure branch', () {
    const result = FailureResult<int>(NetworkFailure('offline'));

    final value = result.when(
      success: (_) => 'ok',
      failure: (failure) => failure.message,
    );

    expect(value, 'offline');
  });
}
