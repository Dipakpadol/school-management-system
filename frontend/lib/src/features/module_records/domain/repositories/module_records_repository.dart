import '../../../../core/result/result.dart';
import '../../data/models/module_record_model.dart';

abstract interface class ModuleRecordsRepository {
  Future<Result<List<ModuleRecordModel>>> records({
    required String moduleId,
    required String recordType,
    String? query,
  });

  Future<Result<ModuleRecordModel>> create({
    required String moduleId,
    required String recordType,
    required Map<String, dynamic> payload,
  });

  Future<Result<ModuleRecordModel>> update({
    required String moduleId,
    required String recordType,
    required String id,
    required Map<String, dynamic> payload,
  });

  Future<Result<void>> delete({
    required String moduleId,
    required String recordType,
    required String id,
  });

  Future<Result<void>> export({
    required String moduleId,
    required String recordType,
    required String format,
    String? query,
  });

  Future<Result<void>> template({
    required String moduleId,
    required String recordType,
  });

  Future<Result<void>> importFile({
    required String moduleId,
    required String recordType,
    required String format,
    required List<int> bytes,
    required String filename,
  });
}
