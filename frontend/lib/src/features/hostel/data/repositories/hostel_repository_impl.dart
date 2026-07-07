import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/failure.dart';
import '../../../../core/result/result.dart';
import '../../../fees/data/models/fee_models.dart';
import '../../../students/data/models/student_models.dart';
import '../../domain/repositories/hostel_repository.dart';
import '../datasources/hostel_remote_data_source.dart';
import '../models/hostel_models.dart';

final hostelRepositoryProvider = Provider<HostelRepository>((ref) {
  return HostelRepositoryImpl(ref.watch(hostelRemoteDataSourceProvider));
});

class HostelRepositoryImpl implements HostelRepository {
  const HostelRepositoryImpl(this._remoteDataSource);

  final HostelRemoteDataSource _remoteDataSource;

  @override
  Future<Result<List<AcademicYearModel>>> academicYears() {
    return _guard(_remoteDataSource.academicYears);
  }

  @override
  Future<Result<List<HostelSummaryModel>>> hostels() {
    return _guard(_remoteDataSource.hostels);
  }

  @override
  Future<Result<HostelSummaryModel>> createHostel(
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.createHostel(payload));
  }

  @override
  Future<Result<HostelSummaryModel>> updateHostel(
    String id,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.updateHostel(id, payload));
  }

  @override
  Future<Result<HostelSummaryModel>> deleteHostel(String id) {
    return _guard(() => _remoteDataSource.deleteHostel(id));
  }

  @override
  Future<Result<List<HostelRoomSummaryModel>>> rooms(String academicYearId) {
    return _guard(() => _remoteDataSource.rooms(academicYearId));
  }

  @override
  Future<Result<HostelRoomSummaryModel>> createRoom(
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.createRoom(payload));
  }

  @override
  Future<Result<HostelRoomSummaryModel>> updateRoom(
    String id,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.updateRoom(id, payload));
  }

  @override
  Future<Result<HostelRoomSummaryModel>> deleteRoom(String id) {
    return _guard(() => _remoteDataSource.deleteRoom(id));
  }

  @override
  Future<Result<HostelRoomDetailsModel>> roomDetails(
    String roomId,
    String academicYearId,
  ) {
    return _guard(() => _remoteDataSource.roomDetails(roomId, academicYearId));
  }

  @override
  Future<Result<HostelAllocationModel>> assignStudentToRoom(
    String roomId,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.assignStudentToRoom(roomId, payload));
  }

  @override
  Future<Result<HostelAllocationModel>> changeRoom(
    String allocationId,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.changeRoom(allocationId, payload));
  }

  @override
  Future<Result<HostelAllocationModel>> vacate(
    String allocationId,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.vacate(allocationId, payload));
  }

  @override
  Future<Result<HostelAllocationModel?>> currentStudentAllocation(
    String studentId,
    String academicYearId,
  ) {
    return _guard(
      () =>
          _remoteDataSource.currentStudentAllocation(studentId, academicYearId),
    );
  }

  @override
  Future<Result<HostelAllocationModel>> assignStudentHostelAllocation(
    String studentId,
    Map<String, dynamic> payload,
  ) {
    return _guard(
      () => _remoteDataSource.assignStudentHostelAllocation(studentId, payload),
    );
  }

  @override
  Future<Result<HostelAllocationModel>> changeStudentHostelRoom(
    String studentId,
    String allocationId,
    Map<String, dynamic> payload,
  ) {
    return _guard(
      () => _remoteDataSource.changeStudentHostelRoom(
        studentId,
        allocationId,
        payload,
      ),
    );
  }

  @override
  Future<Result<HostelAllocationModel>> vacateStudentHostelAllocation(
    String studentId,
    String allocationId,
    Map<String, dynamic> payload,
  ) {
    return _guard(
      () => _remoteDataSource.vacateStudentHostelAllocation(
        studentId,
        allocationId,
        payload,
      ),
    );
  }

  @override
  Future<Result<List<HostelAllocationModel>>> studentAllocations(
    String studentId,
  ) {
    return _guard(() => _remoteDataSource.studentAllocations(studentId));
  }

  @override
  Future<Result<List<StudentFeeAssignmentModel>>> studentHostelFees(
    String studentId,
  ) {
    return _guard(() => _remoteDataSource.studentHostelFees(studentId));
  }

  @override
  Future<Result<List<HostelFeeStructureModel>>> feeStructures({
    String? academicYearId,
    String? hostelId,
    String? roomType,
  }) {
    return _guard(
      () => _remoteDataSource.feeStructures(
        academicYearId: academicYearId,
        hostelId: hostelId,
        roomType: roomType,
      ),
    );
  }

  @override
  Future<Result<HostelFeeStructureModel>> createFeeStructure(
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.createFeeStructure(payload));
  }

  @override
  Future<Result<HostelFeeStructureModel>> updateFeeStructure(
    String id,
    Map<String, dynamic> payload,
  ) {
    return _guard(() => _remoteDataSource.updateFeeStructure(id, payload));
  }

  @override
  Future<Result<HostelFeeStructureModel>> deleteFeeStructure(String id) {
    return _guard(() => _remoteDataSource.deleteFeeStructure(id));
  }

  @override
  Future<Result<void>> assignHostelFee(Map<String, dynamic> payload) {
    return _guard(() => _remoteDataSource.assignHostelFee(payload));
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
}
