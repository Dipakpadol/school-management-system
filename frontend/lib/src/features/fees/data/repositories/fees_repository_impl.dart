import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/download/file_downloader.dart';
import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../domain/repositories/fees_repository.dart';
import '../datasources/fees_remote_data_source.dart';
import '../models/fee_models.dart';

final feesRepositoryProvider = Provider<FeesRepository>((ref) {
  return FeesRepositoryImpl(ref.watch(feesRemoteDataSourceProvider));
});

class FeesRepositoryImpl implements FeesRepository {
  const FeesRepositoryImpl(this._remoteDataSource);

  final FeesRemoteDataSource _remoteDataSource;

  @override
  Future<Result<List<FeeCategoryModel>>> categories() {
    return _guard(() async => (await _remoteDataSource.categories()).content);
  }

  @override
  Future<Result<FeeCategoryModel>> createCategory(
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.createCategory(payload));
  }

  @override
  Future<Result<FeeCategoryModel>> updateCategory(
    String id,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.updateCategory(id, payload));
  }

  @override
  Future<Result<FeeCategoryModel>> deleteCategory(String id) {
    return _guard(() => _remoteDataSource.deleteCategory(id));
  }

  @override
  Future<Result<List<FeeStructureModel>>> structures({
    String? academicYearId,
    String? classId,
    String? status,
  }) {
    return _guard(
      () async => (await _remoteDataSource.structures(
        academicYearId: academicYearId,
        classId: classId,
        status: status,
        page: 0,
        size: 100,
      )).content,
    );
  }

  @override
  Future<Result<PagePayload<FeeStructureModel>>> structuresPage({
    String? academicYearId,
    String? classId,
    String? status,
    required int page,
    required int size,
  }) {
    return _guard(
      () => _remoteDataSource.structures(
        academicYearId: academicYearId,
        classId: classId,
        status: status,
        page: page,
        size: size,
      ),
    );
  }

  @override
  Future<Result<FeeStructureModel>> structure(String id) {
    return _guard(() => _remoteDataSource.structure(id));
  }

  @override
  Future<Result<FeeStructureModel>> createStructure(
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.createStructure(payload));
  }

  @override
  Future<Result<FeeStructureModel>> updateStructure(
    String id,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.updateStructure(id, payload));
  }

  @override
  Future<Result<FeeStructureModel>> deleteStructure(String id) {
    return _guard(() => _remoteDataSource.deleteStructure(id));
  }

  @override
  Future<Result<List<StudentFeeAssignmentModel>>> assignments({
    String? academicYearId,
    String? classId,
    String? sectionId,
    String? feeCategoryId,
    String? feeStructureId,
    String? sourceType,
    String? status,
    String? query,
  }) {
    return _guard(
      () async => (await _remoteDataSource.assignments(
        academicYearId: academicYearId,
        classId: classId,
        sectionId: sectionId,
        feeCategoryId: feeCategoryId,
        feeStructureId: feeStructureId,
        sourceType: sourceType,
        status: status,
        query: query,
        page: 0,
        size: 100,
      )).content,
    );
  }

  @override
  Future<Result<PagePayload<StudentFeeAssignmentModel>>> assignmentsPage({
    String? academicYearId,
    String? classId,
    String? sectionId,
    String? feeCategoryId,
    String? feeStructureId,
    String? sourceType,
    String? status,
    String? query,
    required int page,
    required int size,
  }) {
    return _guard(
      () => _remoteDataSource.assignments(
        academicYearId: academicYearId,
        classId: classId,
        sectionId: sectionId,
        feeCategoryId: feeCategoryId,
        feeStructureId: feeStructureId,
        sourceType: sourceType,
        status: status,
        query: query,
        page: page,
        size: size,
      ),
    );
  }

  @override
  Future<Result<StudentFeeAssignmentModel>> assignment(String id) {
    return _guard(() => _remoteDataSource.assignment(id));
  }

  @override
  Future<Result<StudentFeeAssignmentModel>> createAssignment(
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.createAssignment(payload));
  }

  @override
  Future<Result<List<ClassStudentFeeModel>>> classStudents(
    String classId, {
    String? academicYearId,
  }) {
    return _guard(
      () => _remoteDataSource.classStudents(
        classId,
        academicYearId: academicYearId,
      ),
    );
  }

  @override
  Future<Result<List<ClassFeeAssignmentDetailModel>>> classFeeAssignments(
    String classId, {
    String? academicYearId,
  }) {
    return _guard(
      () => _remoteDataSource.classFeeAssignments(
        classId,
        academicYearId: academicYearId,
      ),
    );
  }

  @override
  Future<Result<ClassFeeAssignmentModel>> assignClassFee(
    String classId,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.assignClassFee(classId, payload));
  }

  @override
  Future<Result<FeeReceiptModel>> collectPayment(
    String assignmentId,
    Map<String, dynamic> payload,
  ) {
    return _guard(
      () => _remoteDataSource.collectPayment(assignmentId, payload),
    );
  }

  @override
  Future<Result<StudentFeeAssignmentModel>> applyDiscount(
    String assignmentId,
    Map<String, dynamic> payload,
  ) {
    return _guard(
      () => _remoteDataSource.applyDiscount(assignmentId, payload),
    );
  }

  @override
  Future<Result<StudentFeeSummaryModel>> studentSummary(
    String studentId, {
    String? academicYearId,
  }) {
    return _guard(
      () => _remoteDataSource.studentSummary(
        studentId,
        academicYearId: academicYearId,
      ),
    );
  }

  @override
  Future<Result<List<FeePaymentModel>>> studentPaymentHistory(
    String studentId,
  ) {
    return _guard(() => _remoteDataSource.studentPaymentHistory(studentId));
  }

  @override
  Future<Result<FeeReceiptModel>> collectStudentPayment(
    String studentId,
    Map<String, dynamic> payload,
  ) {
    return _guard(
      () => _remoteDataSource.collectStudentPayment(studentId, payload),
    );
  }

  @override
  Future<Result<FeeReceiptModel>> receipt(String receiptNumber) {
    return _guard(() => _remoteDataSource.receipt(receiptNumber));
  }

  @override
  Future<Result<List<FeeDefaulterModel>>> defaulters({
    String? academicYearId,
    String? classId,
    String? sectionId,
    String? sourceType,
    String? asOf,
    String? query,
  }) {
    return _guard(
      () async => (await _remoteDataSource.defaulters(
        academicYearId: academicYearId,
        classId: classId,
        sectionId: sectionId,
        sourceType: sourceType,
        asOf: asOf,
        query: query,
        page: 0,
        size: 100,
      )).content,
    );
  }

  @override
  Future<Result<PagePayload<FeeDefaulterModel>>> defaultersPage({
    String? academicYearId,
    String? classId,
    String? sectionId,
    String? sourceType,
    String? asOf,
    String? query,
    required int page,
    required int size,
  }) {
    return _guard(
      () => _remoteDataSource.defaulters(
        academicYearId: academicYearId,
        classId: classId,
        sectionId: sectionId,
        sourceType: sourceType,
        asOf: asOf,
        query: query,
        page: page,
        size: size,
      ),
    );
  }

  @override
  Future<Result<StudentFeeAssignmentModel>> reversePayment(String paymentId) {
    return _guard(() => _remoteDataSource.reversePayment(paymentId));
  }

  @override
  Future<Result<StudentFeeAssignmentModel>> voidPayment(String paymentId) {
    return _guard(() => _remoteDataSource.voidPayment(paymentId));
  }

  @override
  Future<Result<StudentFeeAssignmentModel>> refundPayment(String paymentId) {
    return _guard(() => _remoteDataSource.refundPayment(paymentId));
  }

  @override
  Future<Result<void>> exportStructuresExcel() {
    return _guard(() async {
      final bytes = await _remoteDataSource.exportStructuresExcel();
      await downloadBytes(
        bytes,
        'fee-structures.xlsx',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      );
    });
  }

  @override
  Future<Result<void>> exportAssignmentsExcel() {
    return _guard(() async {
      final bytes = await _remoteDataSource.exportAssignmentsExcel();
      await downloadBytes(
        bytes,
        'student-fee-assignments.xlsx',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      );
    });
  }

  @override
  Future<Result<void>> downloadStructureTemplate() {
    return _guard(() async {
      final bytes = await _remoteDataSource.structureTemplate();
      await downloadBytes(
        bytes,
        'fee-structure-import-template.xlsx',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      );
    });
  }

  @override
  Future<Result<void>> downloadAssignmentTemplate() {
    return _guard(() async {
      final bytes = await _remoteDataSource.assignmentTemplate();
      await downloadBytes(
        bytes,
        'student-fee-assignment-import-template.xlsx',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      );
    });
  }

  @override
  Future<Result<void>> downloadReceiptPdf(String receiptNumber) {
    return _guard(() async {
      final bytes = await _remoteDataSource.receiptPdf(receiptNumber);
      await downloadBytes(
        bytes,
        'fee-receipt-$receiptNumber.pdf',
        'application/pdf',
      );
    });
  }

  @override
  Future<Result<void>> downloadPaymentReceiptPdf(String paymentId) {
    return _guard(() async {
      final bytes = await _remoteDataSource.paymentReceiptPdf(paymentId);
      await downloadBytes(
        bytes,
        'fee-receipt-$paymentId.pdf',
        'application/pdf',
      );
    });
  }

  @override
  Future<Result<void>> exportDefaulters(
    String format, {
    String? academicYearId,
    String? classId,
    String? sectionId,
    String? sourceType,
    String? asOf,
  }) {
    return _guard(() async {
      final bytes = await _remoteDataSource.defaultersExport(
        format,
        academicYearId: academicYearId,
        classId: classId,
        sectionId: sectionId,
        sourceType: sourceType,
        asOf: asOf,
      );
      await downloadBytes(
        bytes,
        'fee-defaulters.${_extension(format)}',
        _contentType(format),
      );
    });
  }

  @override
  Future<Result<void>> exportCollection(String format) {
    return _guard(() async {
      final bytes = await _remoteDataSource.collectionExport(format);
      await downloadBytes(
        bytes,
        'fee-collection-summary.${_extension(format)}',
        _contentType(format),
      );
    });
  }

  @override
  Future<Result<void>> importStructuresExcel(List<int> bytes, String filename) {
    return _guard(
      () => _remoteDataSource.importStructuresExcel(bytes, filename),
    );
  }

  @override
  Future<Result<void>> importStructuresCsv(List<int> bytes, String filename) {
    return _guard(() => _remoteDataSource.importStructuresCsv(bytes, filename));
  }

  @override
  Future<Result<void>> importAssignmentsExcel(
    List<int> bytes,
    String filename,
  ) {
    return _guard(
      () => _remoteDataSource.importAssignmentsExcel(bytes, filename),
    );
  }

  @override
  Future<Result<void>> importAssignmentsCsv(List<int> bytes, String filename) {
    return _guard(
      () => _remoteDataSource.importAssignmentsCsv(bytes, filename),
    );
  }

  Future<Result<T>> _guard<T>(Future<T> Function() action) async {
    try {
      return Success(await action());
    } on DioException catch (error) {
      return FailureResult(_failureFromDio(error));
    } on FormatException catch (error) {
      return FailureResult(ValidationFailure(error.message));
    } catch (_) {
      return const FailureResult(UnexpectedFailure());
    }
  }

  Failure _failureFromDio(DioException error) {
    final statusCode = error.response?.statusCode;
    if (statusCode == 401) {
      return const UnauthorizedFailure();
    }
    if (statusCode == 400 || statusCode == 409 || statusCode == 422) {
      return ValidationFailure(_serverMessage(error) ?? 'Request is invalid.');
    }
    if (error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.receiveTimeout) {
      return const NetworkFailure('Unable to reach the server.');
    }
    return ValidationFailure(_serverMessage(error) ?? 'Request failed.');
  }

  String? _serverMessage(DioException error) {
    final data = error.response?.data;
    if (data is Map<String, dynamic>) {
      return data['message'] as String?;
    }
    return null;
  }

  String _extension(String format) {
    if (format == 'pdf') {
      return 'pdf';
    }
    if (format == 'csv') {
      return 'csv';
    }
    return 'xlsx';
  }

  String _contentType(String format) {
    if (format == 'pdf') {
      return 'application/pdf';
    }
    if (format == 'csv') {
      return 'text/csv';
    }
    return 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
  }
}
