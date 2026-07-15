import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../../students/data/models/student_models.dart';
import '../../domain/repositories/transport_repository.dart';
import '../datasources/transport_remote_data_source.dart';
import '../models/transport_models.dart';

final transportRepositoryProvider = Provider<TransportRepository>((ref) {
  return TransportRepositoryImpl(ref.watch(transportRemoteDataSourceProvider));
});

class TransportRepositoryImpl implements TransportRepository {
  const TransportRepositoryImpl(this._remote);

  final TransportRemoteDataSource _remote;

  @override
  Future<Result<List<AcademicYearModel>>> academicYears() =>
      _guard(_remote.academicYears);

  @override
  Future<Result<List<TransportDriverModel>>> drivers() =>
      _guard(_remote.drivers);

  @override
  Future<Result<TransportDriverModel>> createDriver(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createDriver(payload));

  @override
  Future<Result<TransportDriverModel>> updateDriver(
    String id,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateDriver(id, payload));

  @override
  Future<Result<TransportDriverModel>> deleteDriver(String id) =>
      _guard(() => _remote.deleteDriver(id));

  @override
  Future<Result<List<TransportVehicleModel>>> vehicles(String academicYearId) =>
      _guard(() => _remote.vehicles(academicYearId));

  @override
  Future<Result<TransportVehicleModel>> createVehicle(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createVehicle(payload));

  @override
  Future<Result<TransportVehicleModel>> updateVehicle(
    String id,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateVehicle(id, payload));

  @override
  Future<Result<TransportVehicleModel>> deleteVehicle(String id) =>
      _guard(() => _remote.deleteVehicle(id));

  @override
  Future<Result<List<TransportRouteModel>>> routes(String academicYearId) =>
      _guard(() => _remote.routes(academicYearId));

  @override
  Future<Result<TransportRouteModel>> createRoute(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createRoute(payload));

  @override
  Future<Result<TransportRouteModel>> updateRoute(
    String id,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateRoute(id, payload));

  @override
  Future<Result<TransportRouteModel>> deleteRoute(String id) =>
      _guard(() => _remote.deleteRoute(id));

  @override
  Future<Result<List<TransportPickupPointModel>>> pickupPoints(
    String routeId,
  ) => _guard(() => _remote.pickupPoints(routeId));

  @override
  Future<Result<List<TransportFeeStructureModel>>> feeStructures({
    String? academicYearId,
    String? routeId,
    String? pickupPointId,
    String? status,
  }) => _guard(
    () => _remote.feeStructures(
      academicYearId: academicYearId,
      routeId: routeId,
      pickupPointId: pickupPointId,
      status: status,
    ),
  );

  @override
  Future<Result<TransportFeeStructureModel>> createFeeStructure(
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createFeeStructure(payload));

  @override
  Future<Result<TransportFeeStructureModel>> updateFeeStructure(
    String id,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updateFeeStructure(id, payload));

  @override
  Future<Result<TransportFeeStructureModel>> deleteFeeStructure(String id) =>
      _guard(() => _remote.deleteFeeStructure(id));

  @override
  Future<Result<TransportPickupPointModel>> createPickupPoint(
    String routeId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.createPickupPoint(routeId, payload));

  @override
  Future<Result<TransportPickupPointModel>> updatePickupPoint(
    String id,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.updatePickupPoint(id, payload));

  @override
  Future<Result<TransportPickupPointModel>> deletePickupPoint(String id) =>
      _guard(() => _remote.deletePickupPoint(id));

  @override
  Future<Result<TransportVehicleDetailsModel>> vehicleDetails(
    String vehicleId,
    String academicYearId,
  ) => _guard(() => _remote.vehicleDetails(vehicleId, academicYearId));

  @override
  Future<Result<StudentTransportAssignmentModel?>> currentStudentAssignment(
    String studentId,
    String academicYearId,
  ) =>
      _guard(() => _remote.currentStudentAssignment(studentId, academicYearId));

  @override
  Future<Result<StudentTransportAssignmentModel>> assignStudentTransport(
    String studentId,
    Map<String, dynamic> payload,
  ) => _guard(() => _remote.assignStudentTransport(studentId, payload));

  @override
  Future<Result<StudentTransportAssignmentModel>> changeStudentTransport(
    String studentId,
    String assignmentId,
    Map<String, dynamic> payload,
  ) => _guard(
    () => _remote.changeStudentTransport(studentId, assignmentId, payload),
  );

  @override
  Future<Result<StudentTransportAssignmentModel>> removeStudentTransport(
    String studentId,
    String assignmentId,
    Map<String, dynamic> payload,
  ) => _guard(
    () => _remote.removeStudentTransport(studentId, assignmentId, payload),
  );

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
}
