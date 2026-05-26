import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/result/result.dart';
import '../../data/models/module_record_model.dart';
import '../../data/repositories/module_records_repository_impl.dart';

final moduleRecordsProvider =
    FutureProvider.family<List<ModuleRecordModel>, ModuleRecordsQuery>((
      ref,
      query,
    ) {
      return _resolve(
        ref
            .watch(moduleRecordsRepositoryProvider)
            .records(
              moduleId: query.moduleId,
              recordType: query.recordType,
              query: query.query,
            ),
      );
    });

class ModuleRecordsQuery {
  const ModuleRecordsQuery({
    required this.moduleId,
    required this.recordType,
    required this.query,
  });

  final String moduleId;
  final String recordType;
  final String query;

  @override
  bool operator ==(Object other) {
    return other is ModuleRecordsQuery &&
        other.moduleId == moduleId &&
        other.recordType == recordType &&
        other.query == query;
  }

  @override
  int get hashCode => Object.hash(moduleId, recordType, query);
}

Future<T> _resolve<T>(Future<Result<T>> resultFuture) async {
  final result = await resultFuture;
  return result.when(
    success: (value) => value,
    failure: (failure) => throw Exception(failure.message),
  );
}
