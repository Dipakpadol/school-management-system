class PagePayload<T> {
  const PagePayload({
    required this.content,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  factory PagePayload.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) mapper,
  ) {
    final content = json['content'];
    return PagePayload(
      content: content is List
          ? content
                .whereType<Map<String, dynamic>>()
                .map(mapper)
                .toList(growable: false)
          : const [],
      page: json['page'] as int? ?? 0,
      size: json['size'] as int? ?? 20,
      totalElements: json['totalElements'] as int? ?? 0,
      totalPages: json['totalPages'] as int? ?? 0,
    );
  }

  final List<T> content;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;
}

class FeeCategoryModel {
  const FeeCategoryModel({
    required this.id,
    required this.code,
    required this.name,
    required this.active,
    required this.sortOrder,
    required this.isMandatory,
    this.description,
  });

  factory FeeCategoryModel.fromJson(Map<String, dynamic> json) {
    return FeeCategoryModel(
      id: json['id'] as String,
      code: json['code'] as String,
      name: json['name'] as String,
      description: json['description'] as String?,
      active: json['active'] as bool? ?? false,
      sortOrder: json['sortOrder'] as int? ?? 0,
      isMandatory:
          (json['isMandatory'] as bool?) ?? (json['mandatory'] as bool?) ?? true,
    );
  }

  final String id;
  final String code;
  final String name;
  final String? description;
  final bool active;
  final int sortOrder;
  final bool isMandatory;
}

class FeeStructureModel {
  const FeeStructureModel({
    required this.id,
    required this.academicYear,
    required this.className,
    required this.name,
    required this.status,
    required this.totalAmount,
    required this.items,
    required this.installments,
    this.academicYearId,
    this.classId,
    this.sectionName,
    this.description,
  });

  factory FeeStructureModel.fromJson(Map<String, dynamic> json) {
    return FeeStructureModel(
      id: json['id'] as String,
      academicYearId: json['academicYearId'] as String?,
      classId: json['classId'] as String?,
      academicYear: json['academicYear'] as String,
      className: json['className'] as String,
      sectionName: json['sectionName'] as String?,
      name: json['name'] as String,
      description: json['description'] as String?,
      status: json['status'] as String? ?? 'DRAFT',
      totalAmount: _money(json['totalAmount']),
      items: _list(json['items'], FeeStructureItemModel.fromJson),
      installments: _list(
        json['installments'],
        FeeStructureInstallmentModel.fromJson,
      ),
    );
  }

  final String id;
  final String? academicYearId;
  final String? classId;
  final String academicYear;
  final String className;
  final String? sectionName;
  final String name;
  final String? description;
  final String status;
  final double totalAmount;
  final List<FeeStructureItemModel> items;
  final List<FeeStructureInstallmentModel> installments;
}

class FeeStructureItemModel {
  const FeeStructureItemModel({
    required this.id,
    required this.categoryId,
    required this.categoryCode,
    required this.categoryName,
    required this.amount,
    required this.mandatory,
    required this.sortOrder,
  });

  factory FeeStructureItemModel.fromJson(Map<String, dynamic> json) {
    return FeeStructureItemModel(
      id: json['id'] as String,
      categoryId: json['categoryId'] as String,
      categoryCode: json['categoryCode'] as String? ?? '',
      categoryName: json['categoryName'] as String? ?? '',
      amount: _money(json['amount']),
      mandatory: json['mandatory'] as bool? ?? true,
      sortOrder: json['sortOrder'] as int? ?? 0,
    );
  }

  final String id;
  final String categoryId;
  final String categoryCode;
  final String categoryName;
  final double amount;
  final bool mandatory;
  final int sortOrder;
}

class FeeStructureInstallmentModel {
  const FeeStructureInstallmentModel({
    required this.id,
    required this.sequenceNo,
    required this.title,
    required this.dueDate,
    required this.amount,
  });

  factory FeeStructureInstallmentModel.fromJson(Map<String, dynamic> json) {
    return FeeStructureInstallmentModel(
      id: json['id'] as String,
      sequenceNo: json['sequenceNo'] as int? ?? 0,
      title: json['title'] as String? ?? '',
      dueDate: DateTime.parse(json['dueDate'] as String),
      amount: _money(json['amount']),
    );
  }

  final String id;
  final int sequenceNo;
  final String title;
  final DateTime dueDate;
  final double amount;
}

class StudentFeeAssignmentModel {
  const StudentFeeAssignmentModel({
    required this.id,
    required this.studentId,
    required this.admissionNumber,
    required this.studentName,
    required this.feeStructureId,
    required this.feeStructureName,
    required this.academicYear,
    required this.className,
    required this.status,
    required this.grossAmount,
    required this.discountAmount,
    required this.lateFeeAmount,
    required this.paidAmount,
    required this.balanceAmount,
    required this.installments,
    required this.payments,
    this.academicYearId,
    this.classId,
    this.sectionName,
  });

  factory StudentFeeAssignmentModel.fromJson(Map<String, dynamic> json) {
    return StudentFeeAssignmentModel(
      id: json['id'] as String,
      studentId: json['studentId'] as String,
      admissionNumber: json['admissionNumber'] as String? ?? '',
      studentName: json['studentName'] as String? ?? '',
      feeStructureId: json['feeStructureId'] as String,
      feeStructureName: json['feeStructureName'] as String? ?? '',
      academicYearId: json['academicYearId'] as String?,
      classId: json['classId'] as String?,
      academicYear: json['academicYear'] as String? ?? '',
      className: json['className'] as String? ?? '',
      sectionName: json['sectionName'] as String?,
      status: json['status'] as String? ?? 'PENDING',
      grossAmount: _money(json['grossAmount']),
      discountAmount: _money(json['discountAmount']),
      lateFeeAmount: _money(json['lateFeeAmount']),
      paidAmount: _money(json['paidAmount']),
      balanceAmount: _money(json['balanceAmount']),
      installments: _list(json['installments'], FeeInstallmentModel.fromJson),
      payments: _list(json['payments'], FeePaymentModel.fromJson),
    );
  }

  final String id;
  final String studentId;
  final String admissionNumber;
  final String studentName;
  final String feeStructureId;
  final String feeStructureName;
  final String? academicYearId;
  final String? classId;
  final String academicYear;
  final String className;
  final String? sectionName;
  final String status;
  final double grossAmount;
  final double discountAmount;
  final double lateFeeAmount;
  final double paidAmount;
  final double balanceAmount;
  final List<FeeInstallmentModel> installments;
  final List<FeePaymentModel> payments;
}

class FeeInstallmentModel {
  const FeeInstallmentModel({
    required this.id,
    required this.sequenceNo,
    required this.title,
    required this.dueDate,
    required this.amount,
    required this.payableAmount,
    required this.paidAmount,
    required this.balanceAmount,
    required this.status,
  });

  factory FeeInstallmentModel.fromJson(Map<String, dynamic> json) {
    return FeeInstallmentModel(
      id: json['id'] as String,
      sequenceNo: json['sequenceNo'] as int? ?? 0,
      title: json['title'] as String? ?? '',
      dueDate: DateTime.parse(json['dueDate'] as String),
      amount: _money(json['amount']),
      payableAmount: _money(json['payableAmount']),
      paidAmount: _money(json['paidAmount']),
      balanceAmount: _money(json['balanceAmount']),
      status: json['status'] as String? ?? 'PENDING',
    );
  }

  final String id;
  final int sequenceNo;
  final String title;
  final DateTime dueDate;
  final double amount;
  final double payableAmount;
  final double paidAmount;
  final double balanceAmount;
  final String status;
}

class FeePaymentModel {
  const FeePaymentModel({
    required this.id,
    required this.receiptNumber,
    required this.amount,
    required this.paymentDate,
    required this.paymentMode,
    required this.status,
  });

  factory FeePaymentModel.fromJson(Map<String, dynamic> json) {
    return FeePaymentModel(
      id: json['id'] as String,
      receiptNumber: json['receiptNumber'] as String? ?? '',
      amount: _money(json['amount']),
      paymentDate: DateTime.parse(json['paymentDate'] as String),
      paymentMode: json['paymentMode'] as String? ?? '',
      status: json['status'] as String? ?? '',
    );
  }

  final String id;
  final String receiptNumber;
  final double amount;
  final DateTime paymentDate;
  final String paymentMode;
  final String status;
}

class FeeReceiptModel {
  const FeeReceiptModel({
    required this.id,
    required this.receiptNumber,
    required this.studentName,
    required this.admissionNumber,
    required this.totalAmount,
    required this.receiptDate,
    required this.payerName,
    required this.paymentMode,
    required this.status,
  });

  factory FeeReceiptModel.fromJson(Map<String, dynamic> json) {
    return FeeReceiptModel(
      id: json['id'] as String,
      receiptNumber: json['receiptNumber'] as String? ?? '',
      studentName: json['studentName'] as String? ?? '',
      admissionNumber: json['admissionNumber'] as String? ?? '',
      totalAmount: _money(json['totalAmount']),
      receiptDate: DateTime.parse(json['receiptDate'] as String),
      payerName: json['payerName'] as String? ?? '',
      paymentMode: json['paymentMode'] as String? ?? '',
      status: json['status'] as String? ?? '',
    );
  }

  final String id;
  final String receiptNumber;
  final String studentName;
  final String admissionNumber;
  final double totalAmount;
  final DateTime receiptDate;
  final String payerName;
  final String paymentMode;
  final String status;
}

class FeeDefaulterModel {
  const FeeDefaulterModel({
    required this.assignmentId,
    required this.studentName,
    required this.admissionNumber,
    required this.academicYear,
    required this.className,
    required this.balanceAmount,
    required this.overdueInstallments,
    this.sectionName,
    this.oldestDueDate,
  });

  factory FeeDefaulterModel.fromJson(Map<String, dynamic> json) {
    final dueDate = json['oldestDueDate'] as String?;
    return FeeDefaulterModel(
      assignmentId: json['assignmentId'] as String,
      studentName: json['studentName'] as String? ?? '',
      admissionNumber: json['admissionNumber'] as String? ?? '',
      academicYear: json['academicYear'] as String? ?? '',
      className: json['className'] as String? ?? '',
      sectionName: json['sectionName'] as String?,
      balanceAmount: _money(json['balanceAmount']),
      oldestDueDate: dueDate == null ? null : DateTime.parse(dueDate),
      overdueInstallments: json['overdueInstallments'] as int? ?? 0,
    );
  }

  final String assignmentId;
  final String studentName;
  final String admissionNumber;
  final String academicYear;
  final String className;
  final String? sectionName;
  final double balanceAmount;
  final DateTime? oldestDueDate;
  final int overdueInstallments;
}

class ClassStudentFeeModel {
  const ClassStudentFeeModel({
    required this.studentId,
    required this.admissionNumber,
    required this.studentName,
    required this.grossAmount,
    required this.discountAmount,
    required this.paidAmount,
    required this.balanceAmount,
    this.rollNumber,
    this.assignmentId,
    this.feeStructureId,
    this.feeStructureName,
    this.status,
  });

  factory ClassStudentFeeModel.fromJson(Map<String, dynamic> json) {
    return ClassStudentFeeModel(
      studentId: json['studentId'] as String,
      admissionNumber: json['admissionNumber'] as String? ?? '',
      studentName: json['studentName'] as String? ?? '',
      rollNumber: json['rollNumber'] as String?,
      assignmentId: json['assignmentId'] as String?,
      feeStructureId: json['feeStructureId'] as String?,
      feeStructureName: json['feeStructureName'] as String?,
      grossAmount: _money(json['grossAmount']),
      discountAmount: _money(json['discountAmount']),
      paidAmount: _money(json['paidAmount']),
      balanceAmount: _money(json['balanceAmount']),
      status: json['status'] as String?,
    );
  }

  final String studentId;
  final String admissionNumber;
  final String studentName;
  final String? rollNumber;
  final String? assignmentId;
  final String? feeStructureId;
  final String? feeStructureName;
  final double grossAmount;
  final double discountAmount;
  final double paidAmount;
  final double balanceAmount;
  final String? status;
}

class ClassFeeAssignmentModel {
  const ClassFeeAssignmentModel({
    required this.classId,
    required this.feeStructureId,
    required this.totalStudents,
    required this.createdAssignments,
    required this.skippedAssignments,
  });

  factory ClassFeeAssignmentModel.fromJson(Map<String, dynamic> json) {
    return ClassFeeAssignmentModel(
      classId: json['classId'] as String,
      feeStructureId: json['feeStructureId'] as String,
      totalStudents: json['totalStudents'] as int? ?? 0,
      createdAssignments: json['createdAssignments'] as int? ?? 0,
      skippedAssignments: json['skippedAssignments'] as int? ?? 0,
    );
  }

  final String classId;
  final String feeStructureId;
  final int totalStudents;
  final int createdAssignments;
  final int skippedAssignments;
}

List<T> _list<T>(Object? value, T Function(Map<String, dynamic>) mapper) {
  if (value is! List) {
    return const [];
  }
  return value.whereType<Map<String, dynamic>>().map(mapper).toList();
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
