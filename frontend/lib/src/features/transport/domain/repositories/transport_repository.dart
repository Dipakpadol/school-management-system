import '../../../../core/network/page_payload.dart' as core;
import '../../../../core/result/result.dart';
import '../../../students/data/models/student_models.dart';
import '../../data/models/transport_models.dart';

abstract interface class TransportRepository {
  Future<Result<List<AcademicYearModel>>> academicYears();

  Future<Result<List<TransportDriverModel>>> drivers();

  Future<Result<TransportDriverModel>> createDriver(
    Map<String, dynamic> payload,
  );

  Future<Result<TransportDriverModel>> updateDriver(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<TransportDriverModel>> deleteDriver(String id);

  Future<Result<List<TransportVehicleModel>>> vehicles(String academicYearId);

  Future<Result<TransportVehicleModel>> createVehicle(
    Map<String, dynamic> payload,
  );

  Future<Result<TransportVehicleModel>> updateVehicle(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<TransportVehicleModel>> deleteVehicle(String id);

  Future<Result<List<TransportRouteModel>>> routes(String academicYearId);

  Future<Result<TransportRouteModel>> createRoute(Map<String, dynamic> payload);

  Future<Result<TransportRouteModel>> updateRoute(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<TransportRouteModel>> deleteRoute(String id);

  Future<Result<List<TransportPickupPointModel>>> pickupPoints(String routeId);

  Future<Result<List<TransportFeeStructureModel>>> feeStructures({
    String? academicYearId,
    String? routeId,
    String? pickupPointId,
    String? status,
  });

  Future<Result<core.PagePayload<TransportFeeStructureModel>>>
  feeStructuresPage({
    String? academicYearId,
    String? routeId,
    String? pickupPointId,
    String? status,
    int page = 0,
    int size = 20,
  });

  Future<Result<TransportFeeStructureModel>> createFeeStructure(
    Map<String, dynamic> payload,
  );

  Future<Result<TransportFeeStructureModel>> updateFeeStructure(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<TransportFeeStructureModel>> deleteFeeStructure(String id);

  Future<Result<TransportPickupPointModel>> createPickupPoint(
    String routeId,
    Map<String, dynamic> payload,
  );

  Future<Result<TransportPickupPointModel>> updatePickupPoint(
    String id,
    Map<String, dynamic> payload,
  );

  Future<Result<TransportPickupPointModel>> deletePickupPoint(String id);

  Future<Result<TransportVehicleDetailsModel>> vehicleDetails(
    String vehicleId,
    String academicYearId,
  );

  Future<Result<StudentTransportAssignmentModel?>> currentStudentAssignment(
    String studentId,
    String academicYearId,
  );

  Future<Result<StudentTransportAssignmentModel>> assignStudentTransport(
    String studentId,
    Map<String, dynamic> payload,
  );

  Future<Result<StudentTransportAssignmentModel>> changeStudentTransport(
    String studentId,
    String assignmentId,
    Map<String, dynamic> payload,
  );

  Future<Result<StudentTransportAssignmentModel>> removeStudentTransport(
    String studentId,
    String assignmentId,
    Map<String, dynamic> payload,
  );
}
