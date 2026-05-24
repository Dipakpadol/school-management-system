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

  Future<PagePayload<FeeStructureModel>> structures() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeStructures,
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

  Future<FeeStructureModel> createStructure(Map<String, dynamic> payload) async {
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

  Future<PagePayload<StudentFeeAssignmentModel>> assignments() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeAssignments,
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      StudentFeeAssignmentModel.fromJson,
    );
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

  Future<FeeReceiptModel> receipt(String receiptNumber) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeReceipt(receiptNumber),
    );
    return FeeReceiptModel.fromJson(_unwrapData(response.data));
  }

  Future<PagePayload<FeeDefaulterModel>> defaulters() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.feeDefaulters,
    );
    return PagePayload.fromJson(
      _unwrapData(response.data),
      FeeDefaulterModel.fromJson,
    );
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Response payload is invalid.');
  }
}
