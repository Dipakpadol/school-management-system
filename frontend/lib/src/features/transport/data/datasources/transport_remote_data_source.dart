import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/constants/api_paths.dart';
import '../../../../core/network/api_client.dart';
import '../../../students/data/models/student_models.dart';
import '../models/transport_models.dart';

final transportRemoteDataSourceProvider =
    Provider<TransportRemoteDataSource>((ref) {
  return TransportRemoteDataSource(ref.watch(apiClientProvider));
});

class TransportRemoteDataSource {
  const TransportRemoteDataSource(this._apiClient);

  final ApiClient _apiClient;

  Future<List<AcademicYearModel>> academicYears() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.transportAcademicYears,
    );
    return _unwrapList(response.data, AcademicYearModel.fromJson);
  }

  Future<List<TransportDriverModel>> drivers() async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.transportDrivers,
    );
    return _unwrapList(response.data, TransportDriverModel.fromJson);
  }

  Future<TransportDriverModel> createDriver(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.transportDrivers,
      data: payload,
    );
    return TransportDriverModel.fromJson(_unwrapData(response.data));
  }

  Future<TransportDriverModel> updateDriver(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.transportDriver(id),
      data: payload,
    );
    return TransportDriverModel.fromJson(_unwrapData(response.data));
  }

  Future<TransportDriverModel> deleteDriver(String id) async {
    final response = await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.transportDriver(id),
    );
    return TransportDriverModel.fromJson(_unwrapData(response.data));
  }

  Future<List<TransportVehicleModel>> vehicles(String academicYearId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.transportAcademicYearVehicles(academicYearId),
    );
    return _unwrapList(response.data, TransportVehicleModel.fromJson);
  }

  Future<TransportVehicleModel> createVehicle(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.transportVehicles,
      data: payload,
    );
    return TransportVehicleModel.fromJson(_unwrapData(response.data));
  }

  Future<TransportVehicleModel> updateVehicle(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.transportVehicle(id),
      data: payload,
    );
    return TransportVehicleModel.fromJson(_unwrapData(response.data));
  }

  Future<TransportVehicleModel> deleteVehicle(String id) async {
    final response = await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.transportVehicle(id),
    );
    return TransportVehicleModel.fromJson(_unwrapData(response.data));
  }

  Future<List<TransportRouteModel>> routes(String academicYearId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.transportAcademicYearRoutes(academicYearId),
    );
    return _unwrapList(response.data, TransportRouteModel.fromJson);
  }

  Future<TransportRouteModel> createRoute(Map<String, dynamic> payload) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.transportRoutes,
      data: payload,
    );
    return TransportRouteModel.fromJson(_unwrapData(response.data));
  }

  Future<TransportRouteModel> updateRoute(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.transportRoute(id),
      data: payload,
    );
    return TransportRouteModel.fromJson(_unwrapData(response.data));
  }

  Future<TransportRouteModel> deleteRoute(String id) async {
    final response = await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.transportRoute(id),
    );
    return TransportRouteModel.fromJson(_unwrapData(response.data));
  }

  Future<List<TransportPickupPointModel>> pickupPoints(String routeId) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.transportRoutePickupPoints(routeId),
    );
    return _unwrapList(response.data, TransportPickupPointModel.fromJson);
  }

  Future<List<TransportFeeStructureModel>> feeStructures({
    String? academicYearId,
    String? routeId,
    String? pickupPointId,
    String? status,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.transportFeeStructures,
      queryParameters: {
        if (academicYearId != null && academicYearId.isNotEmpty)
          'academicYearId': academicYearId,
        if (routeId != null && routeId.isNotEmpty) 'routeId': routeId,
        if (pickupPointId != null && pickupPointId.isNotEmpty)
          'pickupPointId': pickupPointId,
        if (status != null && status.isNotEmpty) 'status': status,
      },
    );
    return _unwrapList(response.data, TransportFeeStructureModel.fromJson);
  }

  Future<TransportFeeStructureModel> createFeeStructure(
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.transportFeeStructures,
      data: payload,
    );
    return TransportFeeStructureModel.fromJson(_unwrapData(response.data));
  }

  Future<TransportFeeStructureModel> updateFeeStructure(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.transportFeeStructure(id),
      data: payload,
    );
    return TransportFeeStructureModel.fromJson(_unwrapData(response.data));
  }

  Future<TransportFeeStructureModel> deleteFeeStructure(String id) async {
    final response = await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.transportFeeStructure(id),
    );
    return TransportFeeStructureModel.fromJson(_unwrapData(response.data));
  }

  Future<TransportPickupPointModel> createPickupPoint(
    String routeId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.transportRoutePickupPoints(routeId),
      data: payload,
    );
    return TransportPickupPointModel.fromJson(_unwrapData(response.data));
  }

  Future<TransportPickupPointModel> updatePickupPoint(
    String id,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.transportPickupPoint(id),
      data: payload,
    );
    return TransportPickupPointModel.fromJson(_unwrapData(response.data));
  }

  Future<TransportPickupPointModel> deletePickupPoint(String id) async {
    final response = await _apiClient.delete<Map<String, dynamic>>(
      ApiPaths.transportPickupPoint(id),
    );
    return TransportPickupPointModel.fromJson(_unwrapData(response.data));
  }

  Future<TransportVehicleDetailsModel> vehicleDetails(
    String vehicleId,
    String academicYearId,
  ) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.transportVehicleDetails(vehicleId),
      queryParameters: {'academicYearId': academicYearId},
    );
    return TransportVehicleDetailsModel.fromJson(_unwrapData(response.data));
  }

  Future<StudentTransportAssignmentModel?> currentStudentAssignment(
    String studentId,
    String academicYearId,
  ) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiPaths.studentTransportAssignment(studentId),
      queryParameters: {'academicYearId': academicYearId},
    );
    final data = response.data?['data'];
    if (data == null) {
      return null;
    }
    if (data is Map<String, dynamic>) {
      return StudentTransportAssignmentModel.fromJson(data);
    }
    throw const FormatException('Response payload is invalid.');
  }

  Future<StudentTransportAssignmentModel> assignStudentTransport(
    String studentId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiPaths.studentTransportAssignment(studentId),
      data: payload,
    );
    return StudentTransportAssignmentModel.fromJson(_unwrapData(response.data));
  }

  Future<StudentTransportAssignmentModel> changeStudentTransport(
    String studentId,
    String assignmentId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.studentTransportAssignmentChange(studentId, assignmentId),
      data: payload,
    );
    return StudentTransportAssignmentModel.fromJson(_unwrapData(response.data));
  }

  Future<StudentTransportAssignmentModel> removeStudentTransport(
    String studentId,
    String assignmentId,
    Map<String, dynamic> payload,
  ) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      ApiPaths.studentTransportAssignmentRemove(studentId, assignmentId),
      data: payload,
    );
    return StudentTransportAssignmentModel.fromJson(_unwrapData(response.data));
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
