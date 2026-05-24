import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../data/models/audit_log_models.dart';
import '../../data/repositories/audit_logs_repository_impl.dart';

final auditModuleFilterProvider =
    NotifierProvider<AuditModuleFilterController, String>(
  AuditModuleFilterController.new,
);

final auditActionFilterProvider =
    NotifierProvider<AuditActionFilterController, String>(
  AuditActionFilterController.new,
);

final auditUserFilterProvider =
    NotifierProvider<AuditUserFilterController, String>(
  AuditUserFilterController.new,
);

final auditLogsProvider = FutureProvider<List<AuditLogModel>>((ref) {
  return _resolve(
    ref.watch(auditLogsRepositoryProvider).auditLogs(
          moduleName: ref.watch(auditModuleFilterProvider),
          action: ref.watch(auditActionFilterProvider),
          performedBy: ref.watch(auditUserFilterProvider),
        ),
  );
});

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}

class AuditModuleFilterController extends Notifier<String> {
  @override
  String build() => '';

  void set(String value) {
    state = value;
  }
}

class AuditActionFilterController extends Notifier<String> {
  @override
  String build() => '';

  void set(String value) {
    state = value;
  }
}

class AuditUserFilterController extends Notifier<String> {
  @override
  String build() => '';

  void set(String value) {
    state = value;
  }
}
