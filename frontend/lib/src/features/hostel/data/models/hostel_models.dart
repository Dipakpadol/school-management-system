class HostelSummaryModel {
  const HostelSummaryModel({
    required this.id,
    required this.code,
    required this.name,
    required this.active,
    this.address,
  });

  factory HostelSummaryModel.fromJson(Map<String, dynamic> json) {
    return HostelSummaryModel(
      id: json['id'] as String? ?? '',
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      address: json['address'] as String?,
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String code;
  final String name;
  final String? address;
  final bool active;
}

class HostelBedModel {
  const HostelBedModel({
    required this.id,
    required this.bedNumber,
    required this.active,
    required this.occupied,
  });

  factory HostelBedModel.fromJson(Map<String, dynamic> json) {
    return HostelBedModel(
      id: json['id'] as String? ?? '',
      bedNumber: json['bedNumber'] as String? ?? '',
      active: json['active'] as bool? ?? true,
      occupied: json['occupied'] as bool? ?? false,
    );
  }

  final String id;
  final String bedNumber;
  final bool active;
  final bool occupied;
}

class HostelRoomSummaryModel {
  const HostelRoomSummaryModel({
    required this.id,
    required this.hostelId,
    required this.hostelName,
    required this.roomNumber,
    required this.roomType,
    required this.capacity,
    required this.occupiedCount,
    required this.availableBeds,
    required this.bedConceptEnabled,
    required this.active,
    required this.beds,
  });

  factory HostelRoomSummaryModel.fromJson(Map<String, dynamic> json) {
    return HostelRoomSummaryModel(
      id: json['id'] as String? ?? '',
      hostelId: json['hostelId'] as String? ?? '',
      hostelName: json['hostelName'] as String? ?? '',
      roomNumber: json['roomNumber'] as String? ?? '',
      roomType: json['roomType'] as String? ?? '',
      capacity: json['capacity'] as int? ?? 0,
      occupiedCount: json['occupiedCount'] as int? ?? 0,
      availableBeds: json['availableBeds'] as int? ?? 0,
      bedConceptEnabled: json['bedConceptEnabled'] as bool? ?? false,
      active: json['active'] as bool? ?? true,
      beds: _list(json['beds'], HostelBedModel.fromJson),
    );
  }

  final String id;
  final String hostelId;
  final String hostelName;
  final String roomNumber;
  final String roomType;
  final int capacity;
  final int occupiedCount;
  final int availableBeds;
  final bool bedConceptEnabled;
  final bool active;
  final List<HostelBedModel> beds;
}

class HostelRoomDetailsModel {
  const HostelRoomDetailsModel({
    required this.roomId,
    required this.hostelId,
    required this.hostelName,
    required this.roomNumber,
    required this.roomType,
    required this.capacity,
    required this.occupiedCount,
    required this.availableBeds,
    required this.bedConceptEnabled,
    required this.beds,
    required this.students,
  });

  factory HostelRoomDetailsModel.fromJson(Map<String, dynamic> json) {
    return HostelRoomDetailsModel(
      roomId: json['roomId'] as String? ?? '',
      hostelId: json['hostelId'] as String? ?? '',
      hostelName: json['hostelName'] as String? ?? '',
      roomNumber: json['roomNumber'] as String? ?? '',
      roomType: json['roomType'] as String? ?? '',
      capacity: json['capacity'] as int? ?? 0,
      occupiedCount: json['occupiedCount'] as int? ?? 0,
      availableBeds: json['availableBeds'] as int? ?? 0,
      bedConceptEnabled: json['bedConceptEnabled'] as bool? ?? false,
      beds: _list(json['beds'], HostelBedModel.fromJson),
      students: _list(json['students'], HostelRoomStudentModel.fromJson),
    );
  }

  final String roomId;
  final String hostelId;
  final String hostelName;
  final String roomNumber;
  final String roomType;
  final int capacity;
  final int occupiedCount;
  final int availableBeds;
  final bool bedConceptEnabled;
  final List<HostelBedModel> beds;
  final List<HostelRoomStudentModel> students;
}

class HostelRoomStudentModel {
  const HostelRoomStudentModel({
    required this.allocationId,
    required this.studentId,
    required this.studentName,
    required this.admissionNumber,
    required this.academicYear,
    required this.className,
    required this.divisionName,
    required this.status,
    this.academicYearId,
    this.classId,
    this.sectionId,
    this.bedNumber,
    this.allocationDate,
    this.vacateDate,
  });

  factory HostelRoomStudentModel.fromJson(Map<String, dynamic> json) {
    return HostelRoomStudentModel(
      allocationId: json['allocationId'] as String? ?? '',
      studentId: json['studentId'] as String? ?? '',
      studentName: json['studentName'] as String? ?? '',
      admissionNumber: json['admissionNumber'] as String? ?? '',
      academicYearId: json['academicYearId'] as String?,
      academicYear:
          json['academicYearName'] as String? ??
          json['academicYear'] as String? ??
          '',
      classId: json['classId'] as String?,
      sectionId: json['sectionId'] as String?,
      className: json['className'] as String? ?? '',
      divisionName: json['divisionName'] as String? ?? '',
      bedNumber: json['bedNumber'] as String?,
      allocationDate: _dateOrNull(json['allocationDate']),
      vacateDate: _dateOrNull(json['vacateDate']),
      status: json['status'] as String? ?? 'ACTIVE',
    );
  }

  final String allocationId;
  final String studentId;
  final String studentName;
  final String admissionNumber;
  final String? academicYearId;
  final String academicYear;
  final String? classId;
  final String? sectionId;
  final String className;
  final String divisionName;
  final String? bedNumber;
  final DateTime? allocationDate;
  final DateTime? vacateDate;
  final String status;
}

class HostelAllocationModel {
  const HostelAllocationModel({
    required this.id,
    required this.studentId,
    required this.admissionNumber,
    required this.studentName,
    required this.academicYear,
    required this.hostelName,
    required this.roomNumber,
    required this.roomType,
    required this.status,
    this.academicYearId,
    this.hostelId,
    this.roomId,
    this.bedId,
    this.bedNumber,
    this.allocationDate,
    this.vacateDate,
    this.feeAssignedStatus = 'NOT_ASSIGNED',
  });

  factory HostelAllocationModel.fromJson(Map<String, dynamic> json) {
    return HostelAllocationModel(
      id: json['allocationId'] as String? ?? json['id'] as String? ?? '',
      studentId: json['studentId'] as String? ?? '',
      admissionNumber: json['admissionNumber'] as String? ?? '',
      studentName: json['studentName'] as String? ?? '',
      academicYearId: json['academicYearId'] as String?,
      academicYear:
          json['academicYearName'] as String? ??
          json['academicYear'] as String? ??
          '',
      hostelId: json['hostelId'] as String?,
      hostelName: json['hostelName'] as String? ?? '',
      roomId: json['roomId'] as String?,
      roomNumber: json['roomNumber'] as String? ?? '',
      roomType: json['roomType'] as String? ?? '',
      bedId: json['bedId'] as String?,
      bedNumber: json['bedNumber'] as String?,
      allocationDate: _dateOrNull(json['allocationDate']),
      vacateDate: _dateOrNull(json['vacateDate']),
      status: json['status'] as String? ?? 'ACTIVE',
      feeAssignedStatus: json['feeAssignedStatus'] as String? ?? 'NOT_ASSIGNED',
    );
  }

  final String id;
  final String studentId;
  final String admissionNumber;
  final String studentName;
  final String? academicYearId;
  final String academicYear;
  final String? hostelId;
  final String hostelName;
  final String? roomId;
  final String roomNumber;
  final String roomType;
  final String? bedId;
  final String? bedNumber;
  final DateTime? allocationDate;
  final DateTime? vacateDate;
  final String status;
  final String feeAssignedStatus;
}

class HostelFeeStructureModel {
  const HostelFeeStructureModel({
    required this.id,
    required this.academicYearId,
    required this.academicYear,
    required this.hostelId,
    required this.hostelName,
    required this.feeCategoryId,
    required this.feeCategoryName,
    required this.feeStructureId,
    required this.feeStructureName,
    required this.amount,
    required this.installmentAllowed,
    required this.numberOfInstallments,
    required this.status,
    this.roomId,
    this.roomNumber,
    this.roomType,
    this.feeCategoryCode,
    this.dueDate,
  });

  factory HostelFeeStructureModel.fromJson(Map<String, dynamic> json) {
    return HostelFeeStructureModel(
      id: json['id'] as String? ?? '',
      academicYearId: json['academicYearId'] as String? ?? '',
      academicYear:
          json['academicYearName'] as String? ??
          json['academicYear'] as String? ??
          '',
      hostelId: json['hostelId'] as String? ?? '',
      hostelName: json['hostelName'] as String? ?? '',
      roomId: json['roomId'] as String?,
      roomNumber: json['roomNumber'] as String?,
      roomType: json['roomType'] as String?,
      feeCategoryId: json['feeCategoryId'] as String? ?? '',
      feeCategoryCode: json['feeCategoryCode'] as String?,
      feeCategoryName: json['feeCategoryName'] as String? ?? '',
      feeStructureId: json['feeStructureId'] as String? ?? '',
      feeStructureName: json['feeStructureName'] as String? ?? '',
      amount: _money(json['amount']),
      dueDate: _dateOrNull(json['dueDate']),
      installmentAllowed: json['installmentAllowed'] as bool? ?? false,
      numberOfInstallments: json['numberOfInstallments'] as int? ?? 1,
      status: json['status'] as String? ?? 'DRAFT',
    );
  }

  final String id;
  final String academicYearId;
  final String academicYear;
  final String hostelId;
  final String hostelName;
  final String? roomId;
  final String? roomNumber;
  final String? roomType;
  final String feeCategoryId;
  final String? feeCategoryCode;
  final String feeCategoryName;
  final String feeStructureId;
  final String feeStructureName;
  final double amount;
  final DateTime? dueDate;
  final bool installmentAllowed;
  final int numberOfInstallments;
  final String status;
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

double _money(Object? value) {
  if (value is num) {
    return value.toDouble();
  }
  if (value is String) {
    return double.tryParse(value) ?? 0;
  }
  return 0;
}
