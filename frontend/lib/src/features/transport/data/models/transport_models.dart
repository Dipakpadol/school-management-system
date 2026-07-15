class TransportDriverModel {
  const TransportDriverModel({
    required this.id,
    required this.displayName,
    required this.mobileNumber,
    required this.licenseNumber,
    required this.status,
    this.firstName = '',
    this.middleName,
    this.lastName,
    this.licenseExpiryDate,
    this.address,
  });

  factory TransportDriverModel.fromJson(Map<String, dynamic> json) {
    return TransportDriverModel(
      id: json['id'] as String? ?? '',
      firstName: json['firstName'] as String? ?? '',
      middleName: json['middleName'] as String?,
      lastName: json['lastName'] as String?,
      displayName: json['displayName'] as String? ?? '',
      mobileNumber: json['mobileNumber'] as String? ?? '',
      licenseNumber: json['licenseNumber'] as String? ?? '',
      licenseExpiryDate: _dateOrNull(json['licenseExpiryDate']),
      address: json['address'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }

  final String id;
  final String firstName;
  final String? middleName;
  final String? lastName;
  final String displayName;
  final String mobileNumber;
  final String licenseNumber;
  final DateTime? licenseExpiryDate;
  final String? address;
  final String status;
}

class TransportVehicleModel {
  const TransportVehicleModel({
    required this.id,
    required this.academicYearId,
    required this.academicYear,
    required this.vehicleNumber,
    required this.vehicleName,
    required this.vehicleType,
    required this.capacity,
    required this.occupiedCount,
    required this.availableSeats,
    required this.status,
    this.driverId,
    this.driverName,
    this.driverMobile,
  });

  factory TransportVehicleModel.fromJson(Map<String, dynamic> json) {
    return TransportVehicleModel(
      id: json['id'] as String? ?? '',
      academicYearId: json['academicYearId'] as String? ?? '',
      academicYear: json['academicYear'] as String? ?? '',
      vehicleNumber: json['vehicleNumber'] as String? ?? '',
      vehicleName: json['vehicleName'] as String? ?? '',
      vehicleType: json['vehicleType'] as String? ?? '',
      capacity: json['capacity'] as int? ?? 0,
      occupiedCount: json['occupiedCount'] as int? ?? 0,
      availableSeats: json['availableSeats'] as int? ?? 0,
      driverId: json['driverId'] as String?,
      driverName: json['driverName'] as String?,
      driverMobile: json['driverMobile'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }

  final String id;
  final String academicYearId;
  final String academicYear;
  final String vehicleNumber;
  final String vehicleName;
  final String vehicleType;
  final int capacity;
  final int occupiedCount;
  final int availableSeats;
  final String? driverId;
  final String? driverName;
  final String? driverMobile;
  final String status;
}

class TransportRouteModel {
  const TransportRouteModel({
    required this.id,
    required this.academicYearId,
    required this.academicYear,
    required this.routeName,
    required this.routeCode,
    required this.startLocation,
    required this.endLocation,
    required this.status,
    this.vehicleId,
    this.vehicleNumber,
    this.vehicleName,
  });

  factory TransportRouteModel.fromJson(Map<String, dynamic> json) {
    return TransportRouteModel(
      id: json['id'] as String? ?? '',
      academicYearId: json['academicYearId'] as String? ?? '',
      academicYear: json['academicYear'] as String? ?? '',
      routeName: json['routeName'] as String? ?? '',
      routeCode: json['routeCode'] as String? ?? '',
      startLocation: json['startLocation'] as String? ?? '',
      endLocation: json['endLocation'] as String? ?? '',
      vehicleId: json['vehicleId'] as String?,
      vehicleNumber: json['vehicleNumber'] as String?,
      vehicleName: json['vehicleName'] as String?,
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }

  final String id;
  final String academicYearId;
  final String academicYear;
  final String routeName;
  final String routeCode;
  final String startLocation;
  final String endLocation;
  final String? vehicleId;
  final String? vehicleNumber;
  final String? vehicleName;
  final String status;
}

class TransportPickupPointModel {
  const TransportPickupPointModel({
    required this.id,
    required this.routeId,
    required this.routeName,
    required this.pointName,
    required this.sequenceOrder,
    required this.status,
    this.pickupTime,
    this.dropTime,
    this.monthlyFee,
  });

  factory TransportPickupPointModel.fromJson(Map<String, dynamic> json) {
    return TransportPickupPointModel(
      id: json['id'] as String? ?? '',
      routeId: json['routeId'] as String? ?? '',
      routeName: json['routeName'] as String? ?? '',
      pointName: json['pointName'] as String? ?? '',
      pickupTime: json['pickupTime'] as String?,
      dropTime: json['dropTime'] as String?,
      monthlyFee: _moneyOrNull(json['monthlyFee']),
      sequenceOrder: json['sequenceOrder'] as int? ?? 0,
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }

  final String id;
  final String routeId;
  final String routeName;
  final String pointName;
  final String? pickupTime;
  final String? dropTime;
  final double? monthlyFee;
  final int sequenceOrder;
  final String status;
}

class TransportFeeStructureModel {
  const TransportFeeStructureModel({
    required this.id,
    required this.academicYearId,
    required this.academicYear,
    required this.routeId,
    required this.routeName,
    required this.routeCode,
    required this.feeCategoryId,
    required this.feeCategoryCode,
    required this.feeCategoryName,
    required this.feeStructureId,
    required this.feeStructureName,
    required this.amount,
    required this.status,
    required this.installmentAllowed,
    required this.numberOfInstallments,
    this.pickupPointId,
    this.pickupPointName,
    this.dueDate,
    this.createdAt,
    this.updatedAt,
  });

  factory TransportFeeStructureModel.fromJson(Map<String, dynamic> json) {
    return TransportFeeStructureModel(
      id: json['id'] as String? ?? '',
      academicYearId: json['academicYearId'] as String? ?? '',
      academicYear: json['academicYear'] as String? ?? '',
      routeId: json['routeId'] as String? ?? '',
      routeName: json['routeName'] as String? ?? '',
      routeCode: json['routeCode'] as String? ?? '',
      pickupPointId: json['pickupPointId'] as String?,
      pickupPointName: json['pickupPointName'] as String?,
      feeCategoryId: json['feeCategoryId'] as String? ?? '',
      feeCategoryCode: json['feeCategoryCode'] as String? ?? '',
      feeCategoryName: json['feeCategoryName'] as String? ?? '',
      feeStructureId: json['feeStructureId'] as String? ?? '',
      feeStructureName: json['feeStructureName'] as String? ?? '',
      amount: _moneyOrNull(json['amount']) ?? 0,
      dueDate: _dateOrNull(json['dueDate']),
      installmentAllowed: json['installmentAllowed'] as bool? ?? false,
      numberOfInstallments: json['numberOfInstallments'] as int? ?? 1,
      status: json['status'] as String? ?? 'DRAFT',
      createdAt: _dateOrNull(json['createdAt']),
      updatedAt: _dateOrNull(json['updatedAt']),
    );
  }

  final String id;
  final String academicYearId;
  final String academicYear;
  final String routeId;
  final String routeName;
  final String routeCode;
  final String? pickupPointId;
  final String? pickupPointName;
  final String feeCategoryId;
  final String feeCategoryCode;
  final String feeCategoryName;
  final String feeStructureId;
  final String feeStructureName;
  final double amount;
  final DateTime? dueDate;
  final bool installmentAllowed;
  final int numberOfInstallments;
  final String status;
  final DateTime? createdAt;
  final DateTime? updatedAt;
}

class TransportStudentAssignmentModel {
  const TransportStudentAssignmentModel({
    required this.assignmentId,
    required this.studentId,
    required this.studentName,
    required this.admissionNumber,
    required this.vehicleId,
    required this.vehicleNumber,
    required this.routeId,
    required this.routeName,
    required this.pickupPointId,
    required this.pickupPointName,
    required this.status,
    required this.feeAssignedStatus,
    this.academicYearId,
    this.academicYear,
    this.className,
    this.sectionName,
    this.assignmentDate,
    this.endDate,
  });

  factory TransportStudentAssignmentModel.fromJson(Map<String, dynamic> json) {
    return TransportStudentAssignmentModel(
      assignmentId:
          json['assignmentId'] as String? ?? json['id'] as String? ?? '',
      studentId: json['studentId'] as String? ?? '',
      studentName: json['studentName'] as String? ?? '',
      admissionNumber: json['admissionNumber'] as String? ?? '',
      academicYearId: json['academicYearId'] as String?,
      academicYear: json['academicYear'] as String?,
      className: json['className'] as String?,
      sectionName: json['sectionName'] as String?,
      vehicleId: json['vehicleId'] as String? ?? '',
      vehicleNumber: json['vehicleNumber'] as String? ?? '',
      routeId: json['routeId'] as String? ?? '',
      routeName: json['routeName'] as String? ?? '',
      pickupPointId: json['pickupPointId'] as String? ?? '',
      pickupPointName: json['pickupPointName'] as String? ?? '',
      assignmentDate: _dateOrNull(json['assignmentDate']),
      endDate: _dateOrNull(json['endDate']),
      status: json['status'] as String? ?? 'ASSIGNED',
      feeAssignedStatus: json['feeAssignedStatus'] as String? ?? 'NOT_ASSIGNED',
    );
  }

  final String assignmentId;
  final String studentId;
  final String studentName;
  final String admissionNumber;
  final String? academicYearId;
  final String? academicYear;
  final String? className;
  final String? sectionName;
  final String vehicleId;
  final String vehicleNumber;
  final String routeId;
  final String routeName;
  final String pickupPointId;
  final String pickupPointName;
  final DateTime? assignmentDate;
  final DateTime? endDate;
  final String status;
  final String feeAssignedStatus;
}

class StudentTransportAssignmentModel {
  const StudentTransportAssignmentModel({
    required this.id,
    required this.studentId,
    required this.studentName,
    required this.admissionNumber,
    required this.academicYearId,
    required this.academicYear,
    required this.vehicleId,
    required this.vehicleNumber,
    required this.vehicleName,
    required this.routeId,
    required this.routeName,
    required this.pickupPointId,
    required this.pickupPointName,
    required this.status,
    required this.feeAssignedStatus,
    this.pickupTime,
    this.dropTime,
    this.driverId,
    this.driverName,
    this.driverMobile,
    this.assignmentDate,
    this.endDate,
  });

  factory StudentTransportAssignmentModel.fromJson(Map<String, dynamic> json) {
    return StudentTransportAssignmentModel(
      id: json['id'] as String? ?? '',
      studentId: json['studentId'] as String? ?? '',
      studentName: json['studentName'] as String? ?? '',
      admissionNumber: json['admissionNumber'] as String? ?? '',
      academicYearId: json['academicYearId'] as String? ?? '',
      academicYear: json['academicYear'] as String? ?? '',
      vehicleId: json['vehicleId'] as String? ?? '',
      vehicleNumber: json['vehicleNumber'] as String? ?? '',
      vehicleName: json['vehicleName'] as String? ?? '',
      routeId: json['routeId'] as String? ?? '',
      routeName: json['routeName'] as String? ?? '',
      pickupPointId: json['pickupPointId'] as String? ?? '',
      pickupPointName: json['pickupPointName'] as String? ?? '',
      pickupTime: json['pickupTime'] as String?,
      dropTime: json['dropTime'] as String?,
      driverId: json['driverId'] as String?,
      driverName: json['driverName'] as String?,
      driverMobile: json['driverMobile'] as String?,
      assignmentDate: _dateOrNull(json['assignmentDate']),
      endDate: _dateOrNull(json['endDate']),
      status: json['status'] as String? ?? 'ASSIGNED',
      feeAssignedStatus: json['feeAssignedStatus'] as String? ?? 'NOT_ASSIGNED',
    );
  }

  final String id;
  final String studentId;
  final String studentName;
  final String admissionNumber;
  final String academicYearId;
  final String academicYear;
  final String vehicleId;
  final String vehicleNumber;
  final String vehicleName;
  final String routeId;
  final String routeName;
  final String pickupPointId;
  final String pickupPointName;
  final String? pickupTime;
  final String? dropTime;
  final String? driverId;
  final String? driverName;
  final String? driverMobile;
  final DateTime? assignmentDate;
  final DateTime? endDate;
  final String status;
  final String feeAssignedStatus;
}

class TransportVehicleDetailsModel {
  const TransportVehicleDetailsModel({
    required this.vehicle,
    required this.routes,
    required this.pickupPoints,
    required this.assignedStudents,
    this.driver,
  });

  factory TransportVehicleDetailsModel.fromJson(Map<String, dynamic> json) {
    return TransportVehicleDetailsModel(
      vehicle: TransportVehicleModel.fromJson(
        json['vehicle'] as Map<String, dynamic>? ?? const {},
      ),
      driver: json['driver'] is Map<String, dynamic>
          ? TransportDriverModel.fromJson(
              json['driver'] as Map<String, dynamic>,
            )
          : null,
      routes: _list(json['routes'], TransportRouteModel.fromJson),
      pickupPoints: _list(
        json['pickupPoints'],
        TransportPickupPointModel.fromJson,
      ),
      assignedStudents: _list(
        json['assignedStudents'],
        TransportStudentAssignmentModel.fromJson,
      ),
    );
  }

  final TransportVehicleModel vehicle;
  final TransportDriverModel? driver;
  final List<TransportRouteModel> routes;
  final List<TransportPickupPointModel> pickupPoints;
  final List<TransportStudentAssignmentModel> assignedStudents;
}

class TransportVehicleDetailsKey {
  const TransportVehicleDetailsKey({
    required this.vehicleId,
    required this.academicYearId,
  });

  final String vehicleId;
  final String academicYearId;

  @override
  bool operator ==(Object other) {
    return other is TransportVehicleDetailsKey &&
        other.vehicleId == vehicleId &&
        other.academicYearId == academicYearId;
  }

  @override
  int get hashCode => Object.hash(vehicleId, academicYearId);
}

class StudentTransportAssignmentKey {
  const StudentTransportAssignmentKey({
    required this.studentId,
    required this.academicYearId,
  });

  final String studentId;
  final String academicYearId;

  @override
  bool operator ==(Object other) {
    return other is StudentTransportAssignmentKey &&
        other.studentId == studentId &&
        other.academicYearId == academicYearId;
  }

  @override
  int get hashCode => Object.hash(studentId, academicYearId);
}

List<T> _list<T>(Object? value, T Function(Map<String, dynamic>) mapper) {
  if (value is! List) {
    return const [];
  }
  return value.whereType<Map<String, dynamic>>().map(mapper).toList();
}

DateTime? _dateOrNull(Object? value) {
  if (value is! String || value.isEmpty) {
    return null;
  }
  return DateTime.tryParse(value);
}

double? _moneyOrNull(Object? value) {
  if (value == null) {
    return null;
  }
  if (value is num) {
    return value.toDouble();
  }
  if (value is String) {
    return double.tryParse(value);
  }
  return null;
}
