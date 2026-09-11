import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/network/page_payload.dart' as core;
import '../../../fees/data/models/fee_models.dart';
import '../../../students/data/models/student_models.dart';
import '../models/hostel_models.dart';

final hostelRemoteDataSourceProvider = Provider<HostelRemoteDataSource>((ref) {
  return HostelRemoteDataSource(ref.watch(apiClientProvider));
});

class HostelRemoteDataSource {
  const HostelRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<List<AcademicYearModel>> academicYears() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.hostelAcademicYears,
    );
    return _unwrapList(response.data, AcademicYearModel.fromJson);
  }

  Future<List<HostelSummaryModel>> hostels() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.hostels,
    );
    return _unwrapList(response.data, HostelSummaryModel.fromJson);
  }

  Future<HostelSummaryModel> createHostel(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.hostels,
      data: payload,
    );
    return HostelSummaryModel.fromJson(_unwrapData(response.data));
  }

  Future<HostelSummaryModel> updateHostel(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.hostel(id),
      data: payload,
    );
    return HostelSummaryModel.fromJson(_unwrapData(response.data));
  }

  Future<HostelSummaryModel> deleteHostel(String id) async {
    final response = await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.hostel(id),
    );
    return HostelSummaryModel.fromJson(_unwrapData(response.data));
  }

  Future<List<HostelRoomSummaryModel>> rooms(String academicYearId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.hostelAcademicYearRooms(academicYearId),
    );
    return _unwrapList(response.data, HostelRoomSummaryModel.fromJson);
  }

  Future<HostelRoomSummaryModel> createRoom(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.hostelRooms,
      data: payload,
    );
    return HostelRoomSummaryModel.fromJson(_unwrapData(response.data));
  }

  Future<HostelRoomSummaryModel> updateRoom(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.hostelRoom(id),
      data: payload,
    );
    return HostelRoomSummaryModel.fromJson(_unwrapData(response.data));
  }

  Future<HostelRoomSummaryModel> deleteRoom(String id) async {
    final response = await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.hostelRoom(id),
    );
    return HostelRoomSummaryModel.fromJson(_unwrapData(response.data));
  }

  Future<HostelRoomDetailsModel> roomDetails(
    String roomId,
    String academicYearId,
  ) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.hostelRoomDetails(roomId),
      queryParameters: {'academicYearId': academicYearId},
    );
    return HostelRoomDetailsModel.fromJson(_unwrapData(response.data));
  }

  Future<HostelAllocationModel> assignStudentToRoom(
    String roomId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.hostelRoomAssignStudent(roomId),
      data: payload,
    );
    return HostelAllocationModel.fromJson(_unwrapData(response.data));
  }

  Future<HostelAllocationModel> changeRoom(
    String allocationId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.hostelAllocationChangeRoom(allocationId),
      data: payload,
    );
    return HostelAllocationModel.fromJson(_unwrapData(response.data));
  }

  Future<HostelAllocationModel> vacate(
    String allocationId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.hostelAllocationVacate(allocationId),
      data: payload,
    );
    return HostelAllocationModel.fromJson(_unwrapData(response.data));
  }

  Future<HostelAllocationModel?> currentStudentAllocation(
    String studentId,
    String academicYearId,
  ) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.studentHostelAllocation(studentId),
      queryParameters: {'academicYearId': academicYearId},
    );
    final data = _unwrapNullableData(response.data);
    return data == null ? null : HostelAllocationModel.fromJson(data);
  }

  Future<HostelAllocationModel> assignStudentHostelAllocation(
    String studentId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.studentHostelAllocation(studentId),
      data: payload,
    );
    return HostelAllocationModel.fromJson(_unwrapData(response.data));
  }

  Future<HostelAllocationModel> changeStudentHostelRoom(
    String studentId,
    String allocationId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.studentHostelAllocationChangeRoom(studentId, allocationId),
      data: payload,
    );
    return HostelAllocationModel.fromJson(_unwrapData(response.data));
  }

  Future<HostelAllocationModel> vacateStudentHostelAllocation(
    String studentId,
    String allocationId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.studentHostelAllocationVacate(studentId, allocationId),
      data: payload,
    );
    return HostelAllocationModel.fromJson(_unwrapData(response.data));
  }

  Future<List<HostelAllocationModel>> studentAllocations(
    String studentId,
  ) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.hostelStudentAllocations(studentId),
    );
    return _unwrapList(response.data, HostelAllocationModel.fromJson);
  }

  Future<List<StudentFeeAssignmentModel>> studentHostelFees(
    String studentId,
  ) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.studentHostelFees(studentId),
    );
    return _unwrapList(response.data, StudentFeeAssignmentModel.fromJson);
  }

  Future<List<HostelFeeStructureModel>> feeStructures({
    String? academicYearId,
    String? hostelId,
    String? roomType,
  }) async {
    return (await feeStructuresPage(
      academicYearId: academicYearId,
      hostelId: hostelId,
      roomType: roomType,
      page: 0,
      size: 100,
    )).content;
  }

  Future<core.PagePayload<HostelFeeStructureModel>> feeStructuresPage({
    String? academicYearId,
    String? hostelId,
    String? roomType,
    String? status,
    int page = 0,
    int size = 20,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.hostelFeeStructures,
      queryParameters: {
        if (academicYearId != null && academicYearId.isNotEmpty)
          'academicYearId': academicYearId,
        if (hostelId != null && hostelId.isNotEmpty) 'hostelId': hostelId,
        if (roomType != null && roomType.trim().isNotEmpty)
          'roomType': roomType.trim(),
        if (status != null && status.isNotEmpty) 'status': status,
        'page': page,
        'size': size,
      },
    );
    return core.PagePayload.fromJson(
      _unwrapData(response.data),
      HostelFeeStructureModel.fromJson,
    );
  }

  Future<HostelFeeStructureModel> createFeeStructure(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.hostelFeeStructures,
      data: payload,
    );
    return HostelFeeStructureModel.fromJson(_unwrapData(response.data));
  }

  Future<HostelFeeStructureModel> updateFeeStructure(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.hostelFeeStructure(id),
      data: payload,
    );
    return HostelFeeStructureModel.fromJson(_unwrapData(response.data));
  }

  Future<HostelFeeStructureModel> deleteFeeStructure(String id) async {
    final response = await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.hostelFeeStructure(id),
    );
    return HostelFeeStructureModel.fromJson(_unwrapData(response.data));
  }

  Future<void> assignHostelFee(Map<String, dynamic> payload) async {
    await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.hostelFeeAssign,
      data: payload,
    );
  }

  Map<String, dynamic> _unwrapData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    throw const FormatException('Response payload is invalid.');
  }

  Map<String, dynamic>? _unwrapNullableData(Map<String, dynamic>? body) {
    final data = body?['data'];
    if (data == null) {
      return null;
    }
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
    if (data is Map<String, dynamic> && data['content'] is List) {
      return (data['content'] as List)
          .whereType<Map<String, dynamic>>()
          .map(mapper)
          .toList();
    }
    if (data is List) {
      return data.whereType<Map<String, dynamic>>().map(mapper).toList();
    }
    throw const FormatException('Response payload is invalid.');
  }
}
