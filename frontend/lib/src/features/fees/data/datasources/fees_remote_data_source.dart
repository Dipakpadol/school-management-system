import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../models/fee_models.dart';

final feesRemoteDataSourceProvider = Provider<FeesRemoteDataSource>((ref) {
  return FeesRemoteDataSource(ref.watch(apiClientProvider));
});

class FeesRemoteDataSource {
  const FeesRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<PagePayload<FeeCategoryModel>> categories() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeCategories,
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      FeeCategoryModel.fromJson,
    );
  }

  Future<FeeCategoryModel> createCategory(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.feeCategories,
      data: payload,
    );
    return FeeCategoryModel.fromJson(_unwrapData(response.data));
  }

  Future<FeeCategoryModel> updateCategory(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.feeCategory(id),
      data: payload,
    );
    return FeeCategoryModel.fromJson(_unwrapData(response.data));
  }

  Future<FeeCategoryModel> deleteCategory(String id) async {
    final response = await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.feeCategory(id),
    );
    return FeeCategoryModel.fromJson(_unwrapData(response.data));
  }

  Future<PagePayload<FeeStructureModel>> structures({
    String? academicYearId,
    String? classId,
    String? status,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeStructures,
      queryParameters: {
        if (academicYearId != null && academicYearId.isNotEmpty)
          'academicYearId': academicYearId,
        if (classId != null && classId.isNotEmpty) 'classId': classId,
        if (status != null && status.isNotEmpty) 'status': status,
      },
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      FeeStructureModel.fromJson,
    );
  }

  Future<FeeStructureModel> structure(String id) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeStructure(id),
    );
    return FeeStructureModel.fromJson(_unwrapData(response.data));
  }

  Future<FeeStructureModel> createStructure(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.feeStructures,
      data: payload,
    );
    return FeeStructureModel.fromJson(_unwrapData(response.data));
  }

  Future<FeeStructureModel> updateStructure(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.feeStructure(id),
      data: payload,
    );
    return FeeStructureModel.fromJson(_unwrapData(response.data));
  }

  Future<FeeStructureModel> deleteStructure(String id) async {
    final response = await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.feeStructure(id),
    );
    return FeeStructureModel.fromJson(_unwrapData(response.data));
  }

  Future<PagePayload<StudentFeeAssignmentModel>> assignments({
    String? academicYearId,
    String? classId,
    String? sectionId,
    String? status,
    String? query,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeAssignments,
      queryParameters: {
        if (academicYearId != null && academicYearId.isNotEmpty)
          'academicYearId': academicYearId,
        if (classId != null && classId.isNotEmpty) 'classId': classId,
        if (sectionId != null && sectionId.isNotEmpty) 'sectionId': sectionId,
        if (status != null && status.isNotEmpty) 'status': status,
        if (query != null && query.trim().isNotEmpty)
          'studentName': query.trim(),
        'size': 100,
      },
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      StudentFeeAssignmentModel.fromJson,
    );
  }

  Future<StudentFeeAssignmentModel> assignment(String id) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeAssignment(id),
    );
    return StudentFeeAssignmentModel.fromJson(_unwrapData(response.data));
  }

  Future<StudentFeeAssignmentModel> createAssignment(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.feeAssignments,
      data: payload,
    );
    return StudentFeeAssignmentModel.fromJson(_unwrapData(response.data));
  }

  Future<List<ClassStudentFeeModel>> classStudents(
    String classId, {
    String? academicYearId,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeClassStudents(classId),
      queryParameters: {
        if (academicYearId != null && academicYearId.isNotEmpty)
          'academicYearId': academicYearId,
      },
    );
    return _unwrapList(response.data, ClassStudentFeeModel.fromJson);
  }

  Future<List<ClassFeeAssignmentDetailModel>> classFeeAssignments(
    String classId, {
    String? academicYearId,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeClassAssignments(classId),
      queryParameters: {
        if (academicYearId != null && academicYearId.isNotEmpty)
          'academicYearId': academicYearId,
      },
    );
    return _unwrapList(response.data, ClassFeeAssignmentDetailModel.fromJson);
  }

  Future<ClassFeeAssignmentModel> assignClassFee(
    String classId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.feeClassAssign(classId),
      data: payload,
    );
    return ClassFeeAssignmentModel.fromJson(_unwrapData(response.data));
  }

  Future<FeeReceiptModel> collectPayment(
    String assignmentId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.feeAssignmentPayments(assignmentId),
      data: payload,
    );
    return FeeReceiptModel.fromJson(_unwrapData(response.data));
  }

  Future<StudentFeeSummaryModel> studentSummary(
    String studentId, {
    String? academicYearId,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeStudentSummary(studentId),
      queryParameters: {
        if (academicYearId != null && academicYearId.isNotEmpty)
          'academicYearId': academicYearId,
      },
    );
    return StudentFeeSummaryModel.fromJson(_unwrapData(response.data));
  }

  Future<List<FeePaymentModel>> studentPaymentHistory(String studentId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeStudentPaymentHistory(studentId),
    );
    return _unwrapList(response.data, FeePaymentModel.fromJson);
  }

  Future<FeeReceiptModel> collectStudentPayment(
    String studentId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.feeStudentPayments(studentId),
      data: payload,
    );
    return FeeReceiptModel.fromJson(_unwrapData(response.data));
  }

  Future<FeeReceiptModel> receipt(String receiptNumber) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeReceipt(receiptNumber),
    );
    return FeeReceiptModel.fromJson(_unwrapData(response.data));
  }

  Future<StudentFeeAssignmentModel> reversePayment(String paymentId) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.feePaymentReverse(paymentId),
      data: {'reason': 'Reversed from admin panel'},
    );
    return StudentFeeAssignmentModel.fromJson(_unwrapData(response.data));
  }

  Future<StudentFeeAssignmentModel> voidPayment(String paymentId) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.feePaymentVoid(paymentId),
      data: {'reason': 'Voided from admin panel'},
    );
    return StudentFeeAssignmentModel.fromJson(_unwrapData(response.data));
  }

  Future<StudentFeeAssignmentModel> refundPayment(String paymentId) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.feePaymentRefund(paymentId),
      data: {'reason': 'Refunded from admin panel'},
    );
    return StudentFeeAssignmentModel.fromJson(_unwrapData(response.data));
  }

  Future<PagePayload<FeeDefaulterModel>> defaulters({
    String? academicYearId,
    String? classId,
    String? sectionId,
    String? sourceType,
    String? asOf,
    String? query,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeDefaulters,
      queryParameters: {
        if (academicYearId != null && academicYearId.isNotEmpty)
          'academicYearId': academicYearId,
        if (classId != null && classId.isNotEmpty) 'classId': classId,
        if (sectionId != null && sectionId.isNotEmpty) 'sectionId': sectionId,
        if (sourceType != null && sourceType.isNotEmpty)
          'sourceType': sourceType,
        if (asOf != null && asOf.isNotEmpty) 'dueDate': asOf,
        if (query != null && query.trim().isNotEmpty)
          'studentName': query.trim(),
        'size': 100,
      },
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      FeeDefaulterModel.fromJson,
    );
  }

  Future<List<int>> exportStructuresExcel() {
    return _apiClient.download(ApiPaths.feeStructuresExportExcel);
  }

  Future<List<int>> exportAssignmentsExcel() {
    return _apiClient.download(ApiPaths.feeAssignmentsExportExcel);
  }

  Future<List<int>> structureTemplate() {
    return _apiClient.download(ApiPaths.feeStructureTemplate);
  }

  Future<List<int>> assignmentTemplate() {
    return _apiClient.download(ApiPaths.feeAssignmentTemplate);
  }

  Future<List<int>> receiptPdf(String receiptNumber) {
    return _apiClient.download(ApiPaths.feeReceiptPdf(receiptNumber));
  }

  Future<List<int>> paymentReceiptPdf(String paymentId) {
    return _apiClient.download(ApiPaths.feePaymentReceiptPdf(paymentId));
  }

  Future<List<int>> defaultersExport(
    String format, {
    String? academicYearId,
    String? classId,
    String? sectionId,
    String? sourceType,
    String? asOf,
  }) {
    return _apiClient.download(
      ApiPaths.feeDefaultersExport(format),
      queryParameters: {
        if (academicYearId != null && academicYearId.isNotEmpty)
          'academicYearId': academicYearId,
        if (classId != null && classId.isNotEmpty) 'classId': classId,
        if (sectionId != null && sectionId.isNotEmpty) 'sectionId': sectionId,
        if (sourceType != null && sourceType.isNotEmpty)
          'sourceType': sourceType,
        if (asOf != null && asOf.isNotEmpty) 'dueDate': asOf,
      },
    );
  }

  Future<List<int>> collectionExport(String format) {
    return _apiClient.download(ApiPaths.feeCollectionExport(format));
  }

  Future<void> importStructuresExcel(List<int> bytes, String filename) async {
    await _postFile(ApiPaths.feeStructuresImportExcel, bytes, filename);
  }

  Future<void> importStructuresCsv(List<int> bytes, String filename) async {
    await _postFile(ApiPaths.feeStructuresImportCsv, bytes, filename);
  }

  Future<void> importAssignmentsExcel(List<int> bytes, String filename) async {
    await _postFile(ApiPaths.feeAssignmentsImportExcel, bytes, filename);
  }

  Future<void> importAssignmentsCsv(List<int> bytes, String filename) async {
    await _postFile(ApiPaths.feeAssignmentsImportCsv, bytes, filename);
  }

  Future<void> _postFile(String path, List<int> bytes, String filename) async {
    await _apiClient.post<Map<String, dynamic>>(
      path,
      data: FormData.fromMap({
        'file': MultipartFile.fromBytes(bytes, filename: filename),
      }),
    );
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Response payload is invalid.');
  }

  List<T> _unwrapList<T>(
    Map<String, dynamic>? body,
    T Function(Map<String, dynamic>) mapper,
  ) {
    final data = body?['data'];
    if (data is List) {
      return data.whereType<Map<String, dynamic>>().map(mapper).toList();
    }
    throw const FormatException('Response payload is invalid.');
  }
}
