import '../../../../core/network/page_payload.dart';

typedef LibraryBookPage = PagePayload<LibraryBookModel>;
typedef LibraryCopyPage = PagePayload<LibraryBookCopyModel>;
typedef LibraryMembershipPage = PagePayload<LibraryMembershipModel>;
typedef LibraryLoanPage = PagePayload<LibraryLoanModel>;
typedef LibraryFinePage = PagePayload<LibraryFineModel>;

class LibrarySummaryModel {
  const LibrarySummaryModel({
    required this.totalBooks,
    required this.totalCopies,
    required this.availableCopies,
    required this.issuedCopies,
    required this.overdueLoans,
    required this.activeMembers,
    required this.lostCopies,
    required this.damagedCopies,
    required this.pendingFineAmount,
  });

  factory LibrarySummaryModel.fromJson(Map<String, dynamic> json) {
    return LibrarySummaryModel(
      totalBooks: _int(json['totalBooks']),
      totalCopies: _int(json['totalCopies']),
      availableCopies: _int(json['availableCopies']),
      issuedCopies: _int(json['issuedCopies']),
      overdueLoans: _int(json['overdueLoans']),
      activeMembers: _int(json['activeMembers']),
      lostCopies: _int(json['lostCopies']),
      damagedCopies: _int(json['damagedCopies']),
      pendingFineAmount: _double(json['pendingFineAmount']),
    );
  }

  final int totalBooks;
  final int totalCopies;
  final int availableCopies;
  final int issuedCopies;
  final int overdueLoans;
  final int activeMembers;
  final int lostCopies;
  final int damagedCopies;
  final double pendingFineAmount;
}

class LibraryCategoryModel {
  const LibraryCategoryModel({
    required this.id,
    required this.name,
    required this.active,
    this.description,
  });

  factory LibraryCategoryModel.fromJson(Map<String, dynamic> json) {
    return LibraryCategoryModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      description: json['description']?.toString(),
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String name;
  final String? description;
  final bool active;
}

class LibraryAuthorModel {
  const LibraryAuthorModel({
    required this.id,
    required this.name,
    required this.active,
    this.biography,
  });

  factory LibraryAuthorModel.fromJson(Map<String, dynamic> json) {
    return LibraryAuthorModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      biography: json['biography']?.toString(),
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String name;
  final String? biography;
  final bool active;
}

class LibraryPublisherModel {
  const LibraryPublisherModel({
    required this.id,
    required this.name,
    required this.active,
    this.contactInfo,
  });

  factory LibraryPublisherModel.fromJson(Map<String, dynamic> json) {
    return LibraryPublisherModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      contactInfo: json['contactInfo']?.toString(),
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String name;
  final String? contactInfo;
  final bool active;
}

class LibraryBookModel {
  const LibraryBookModel({
    required this.id,
    required this.title,
    required this.authors,
    required this.active,
    this.isbn,
    this.categoryId,
    this.categoryName,
    this.publisherId,
    this.publisherName,
    this.edition,
    this.publicationYear,
    this.language,
    this.description,
    this.shelfLocation,
  });

  factory LibraryBookModel.fromJson(Map<String, dynamic> json) {
    final authors = json['authors'];
    return LibraryBookModel(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      isbn: json['isbn']?.toString(),
      categoryId: json['categoryId']?.toString(),
      categoryName: json['categoryName']?.toString(),
      publisherId: json['publisherId']?.toString(),
      publisherName: json['publisherName']?.toString(),
      authors: authors is List
          ? authors
                .whereType<Map<String, dynamic>>()
                .map(LibraryAuthorModel.fromJson)
                .toList(growable: false)
          : const [],
      edition: json['edition']?.toString(),
      publicationYear: _nullableInt(json['publicationYear']),
      language: json['language']?.toString(),
      description: json['description']?.toString(),
      shelfLocation: json['shelfLocation']?.toString(),
      active: json['active'] as bool? ?? true,
    );
  }

  final String id;
  final String title;
  final String? isbn;
  final String? categoryId;
  final String? categoryName;
  final String? publisherId;
  final String? publisherName;
  final List<LibraryAuthorModel> authors;
  final String? edition;
  final int? publicationYear;
  final String? language;
  final String? description;
  final String? shelfLocation;
  final bool active;
}

class LibraryBookCopyModel {
  const LibraryBookCopyModel({
    required this.id,
    required this.bookId,
    required this.bookTitle,
    required this.accessionNumber,
    required this.status,
    this.shelfLocation,
    this.acquiredOn,
    this.price,
    this.conditionNote,
  });

  factory LibraryBookCopyModel.fromJson(Map<String, dynamic> json) {
    return LibraryBookCopyModel(
      id: json['id']?.toString() ?? '',
      bookId: json['bookId']?.toString() ?? '',
      bookTitle: json['bookTitle']?.toString() ?? '',
      accessionNumber: json['accessionNumber']?.toString() ?? '',
      status: json['status']?.toString() ?? 'AVAILABLE',
      shelfLocation: json['shelfLocation']?.toString(),
      acquiredOn: _date(json['acquiredOn']),
      price: _nullableDouble(json['price']),
      conditionNote: json['conditionNote']?.toString(),
    );
  }

  final String id;
  final String bookId;
  final String bookTitle;
  final String accessionNumber;
  final String status;
  final String? shelfLocation;
  final DateTime? acquiredOn;
  final double? price;
  final String? conditionNote;
}

class LibraryMembershipModel {
  const LibraryMembershipModel({
    required this.id,
    required this.memberType,
    required this.memberId,
    required this.memberCode,
    required this.memberName,
    required this.membershipNumber,
    required this.startDate,
    required this.active,
    this.expiryDate,
    this.notes,
  });

  factory LibraryMembershipModel.fromJson(Map<String, dynamic> json) {
    return LibraryMembershipModel(
      id: json['id']?.toString() ?? '',
      memberType: json['memberType']?.toString() ?? 'STUDENT',
      memberId: json['memberId']?.toString() ?? '',
      memberCode: json['memberCode']?.toString() ?? '',
      memberName: json['memberName']?.toString() ?? '',
      membershipNumber: json['membershipNumber']?.toString() ?? '',
      startDate: _date(json['startDate']) ?? DateTime.now(),
      expiryDate: _date(json['expiryDate']),
      active: json['active'] as bool? ?? true,
      notes: json['notes']?.toString(),
    );
  }

  final String id;
  final String memberType;
  final String memberId;
  final String memberCode;
  final String memberName;
  final String membershipNumber;
  final DateTime startDate;
  final DateTime? expiryDate;
  final bool active;
  final String? notes;
}

class LibraryLoanModel {
  const LibraryLoanModel({
    required this.id,
    required this.copyId,
    required this.accessionNumber,
    required this.bookId,
    required this.bookTitle,
    required this.membershipId,
    required this.membershipNumber,
    required this.memberType,
    required this.memberName,
    required this.issueDate,
    required this.dueDate,
    required this.status,
    required this.overdue,
    required this.pendingFine,
    this.returnDate,
    this.remarks,
  });

  factory LibraryLoanModel.fromJson(Map<String, dynamic> json) {
    return LibraryLoanModel(
      id: json['id']?.toString() ?? '',
      copyId: json['copyId']?.toString() ?? '',
      accessionNumber: json['accessionNumber']?.toString() ?? '',
      bookId: json['bookId']?.toString() ?? '',
      bookTitle: json['bookTitle']?.toString() ?? '',
      membershipId: json['membershipId']?.toString() ?? '',
      membershipNumber: json['membershipNumber']?.toString() ?? '',
      memberType: json['memberType']?.toString() ?? 'STUDENT',
      memberName: json['memberName']?.toString() ?? '',
      issueDate: _date(json['issueDate']) ?? DateTime.now(),
      dueDate: _date(json['dueDate']) ?? DateTime.now(),
      returnDate: _date(json['returnDate']),
      status: json['status']?.toString() ?? 'ACTIVE',
      overdue: json['overdue'] as bool? ?? false,
      pendingFine: _double(json['pendingFine']),
      remarks: json['remarks']?.toString(),
    );
  }

  final String id;
  final String copyId;
  final String accessionNumber;
  final String bookId;
  final String bookTitle;
  final String membershipId;
  final String membershipNumber;
  final String memberType;
  final String memberName;
  final DateTime issueDate;
  final DateTime dueDate;
  final DateTime? returnDate;
  final String status;
  final bool overdue;
  final double pendingFine;
  final String? remarks;
}

class LibraryFineModel {
  const LibraryFineModel({
    required this.id,
    required this.loanId,
    required this.accessionNumber,
    required this.bookTitle,
    required this.membershipNumber,
    required this.memberName,
    required this.amount,
    required this.paidAmount,
    required this.reason,
    required this.fineDate,
    required this.status,
  });

  factory LibraryFineModel.fromJson(Map<String, dynamic> json) {
    return LibraryFineModel(
      id: json['id']?.toString() ?? '',
      loanId: json['loanId']?.toString() ?? '',
      accessionNumber: json['accessionNumber']?.toString() ?? '',
      bookTitle: json['bookTitle']?.toString() ?? '',
      membershipNumber: json['membershipNumber']?.toString() ?? '',
      memberName: json['memberName']?.toString() ?? '',
      amount: _double(json['amount']),
      paidAmount: _double(json['paidAmount']),
      reason: json['reason']?.toString() ?? '',
      fineDate: _date(json['fineDate']) ?? DateTime.now(),
      status: json['status']?.toString() ?? 'PENDING',
    );
  }

  final String id;
  final String loanId;
  final String accessionNumber;
  final String bookTitle;
  final String membershipNumber;
  final String memberName;
  final double amount;
  final double paidAmount;
  final String reason;
  final DateTime fineDate;
  final String status;
}

class LibraryBookFilter {
  const LibraryBookFilter({
    this.keyword,
    this.categoryId,
    this.publisherId,
    this.active,
    this.page = 0,
    this.size = 20,
  });

  final String? keyword;
  final String? categoryId;
  final String? publisherId;
  final bool? active;
  final int page;
  final int size;

  Map<String, dynamic> toQuery() => {
        if (_has(keyword)) 'keyword': keyword,
        if (_has(categoryId)) 'categoryId': categoryId,
        if (_has(publisherId)) 'publisherId': publisherId,
        if (active != null) 'active': active,
        'page': page,
        'size': size,
      };

  LibraryBookFilter copyWith({
    String? keyword,
    String? categoryId,
    String? publisherId,
    bool? active,
    int? page,
    int? size,
    bool clearKeyword = false,
    bool clearCategory = false,
    bool clearPublisher = false,
    bool clearActive = false,
  }) {
    return LibraryBookFilter(
      keyword: clearKeyword ? null : keyword ?? this.keyword,
      categoryId: clearCategory ? null : categoryId ?? this.categoryId,
      publisherId: clearPublisher ? null : publisherId ?? this.publisherId,
      active: clearActive ? null : active ?? this.active,
      page: page ?? this.page,
      size: size ?? this.size,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is LibraryBookFilter &&
        other.keyword == keyword &&
        other.categoryId == categoryId &&
        other.publisherId == publisherId &&
        other.active == active &&
        other.page == page &&
        other.size == size;
  }

  @override
  int get hashCode => Object.hash(
        keyword,
        categoryId,
        publisherId,
        active,
        page,
        size,
      );
}

class LibraryCopyFilter {
  const LibraryCopyFilter({
    this.bookId,
    this.status,
    this.keyword,
    this.page = 0,
    this.size = 20,
  });

  final String? bookId;
  final String? status;
  final String? keyword;
  final int page;
  final int size;

  Map<String, dynamic> toQuery() => {
        if (_has(bookId)) 'bookId': bookId,
        if (_has(status)) 'status': status,
        if (_has(keyword)) 'keyword': keyword,
        'page': page,
        'size': size,
      };

  LibraryCopyFilter copyWith({
    String? bookId,
    String? status,
    String? keyword,
    int? page,
    int? size,
    bool clearBook = false,
    bool clearStatus = false,
    bool clearKeyword = false,
  }) {
    return LibraryCopyFilter(
      bookId: clearBook ? null : bookId ?? this.bookId,
      status: clearStatus ? null : status ?? this.status,
      keyword: clearKeyword ? null : keyword ?? this.keyword,
      page: page ?? this.page,
      size: size ?? this.size,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is LibraryCopyFilter &&
        other.bookId == bookId &&
        other.status == status &&
        other.keyword == keyword &&
        other.page == page &&
        other.size == size;
  }

  @override
  int get hashCode => Object.hash(bookId, status, keyword, page, size);
}

class LibraryMembershipFilter {
  const LibraryMembershipFilter({
    this.memberType,
    this.active,
    this.keyword,
    this.page = 0,
    this.size = 20,
  });

  final String? memberType;
  final bool? active;
  final String? keyword;
  final int page;
  final int size;

  Map<String, dynamic> toQuery() => {
        if (_has(memberType)) 'memberType': memberType,
        if (active != null) 'active': active,
        if (_has(keyword)) 'keyword': keyword,
        'page': page,
        'size': size,
      };

  LibraryMembershipFilter copyWith({
    String? memberType,
    bool? active,
    String? keyword,
    int? page,
    int? size,
    bool clearMemberType = false,
    bool clearActive = false,
    bool clearKeyword = false,
  }) {
    return LibraryMembershipFilter(
      memberType: clearMemberType ? null : memberType ?? this.memberType,
      active: clearActive ? null : active ?? this.active,
      keyword: clearKeyword ? null : keyword ?? this.keyword,
      page: page ?? this.page,
      size: size ?? this.size,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is LibraryMembershipFilter &&
        other.memberType == memberType &&
        other.active == active &&
        other.keyword == keyword &&
        other.page == page &&
        other.size == size;
  }

  @override
  int get hashCode => Object.hash(memberType, active, keyword, page, size);
}

class LibraryLoanFilter {
  const LibraryLoanFilter({
    this.status,
    this.membershipId,
    this.bookId,
    this.copyId,
    this.memberType,
    this.fromDate,
    this.toDate,
    this.overdueOnly = false,
    this.page = 0,
    this.size = 20,
  });

  final String? status;
  final String? membershipId;
  final String? bookId;
  final String? copyId;
  final String? memberType;
  final DateTime? fromDate;
  final DateTime? toDate;
  final bool overdueOnly;
  final int page;
  final int size;

  Map<String, dynamic> toQuery() => {
        if (_has(status)) 'status': status,
        if (_has(membershipId)) 'membershipId': membershipId,
        if (_has(bookId)) 'bookId': bookId,
        if (_has(copyId)) 'copyId': copyId,
        if (_has(memberType)) 'memberType': memberType,
        if (fromDate != null) 'fromDate': libraryDateParam(fromDate!),
        if (toDate != null) 'toDate': libraryDateParam(toDate!),
        'overdueOnly': overdueOnly,
        'page': page,
        'size': size,
      };

  LibraryLoanFilter copyWith({
    String? status,
    String? membershipId,
    String? bookId,
    String? copyId,
    String? memberType,
    DateTime? fromDate,
    DateTime? toDate,
    bool? overdueOnly,
    int? page,
    int? size,
    bool clearStatus = false,
    bool clearMembership = false,
    bool clearBook = false,
    bool clearCopy = false,
    bool clearMemberType = false,
    bool clearFromDate = false,
    bool clearToDate = false,
  }) {
    return LibraryLoanFilter(
      status: clearStatus ? null : status ?? this.status,
      membershipId: clearMembership ? null : membershipId ?? this.membershipId,
      bookId: clearBook ? null : bookId ?? this.bookId,
      copyId: clearCopy ? null : copyId ?? this.copyId,
      memberType: clearMemberType ? null : memberType ?? this.memberType,
      fromDate: clearFromDate ? null : fromDate ?? this.fromDate,
      toDate: clearToDate ? null : toDate ?? this.toDate,
      overdueOnly: overdueOnly ?? this.overdueOnly,
      page: page ?? this.page,
      size: size ?? this.size,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is LibraryLoanFilter &&
        other.status == status &&
        other.membershipId == membershipId &&
        other.bookId == bookId &&
        other.copyId == copyId &&
        other.memberType == memberType &&
        other.fromDate == fromDate &&
        other.toDate == toDate &&
        other.overdueOnly == overdueOnly &&
        other.page == page &&
        other.size == size;
  }

  @override
  int get hashCode => Object.hash(
        status,
        membershipId,
        bookId,
        copyId,
        memberType,
        fromDate,
        toDate,
        overdueOnly,
        page,
        size,
      );
}

class LibraryFineFilter {
  const LibraryFineFilter({
    this.status,
    this.membershipId,
    this.fromDate,
    this.toDate,
    this.page = 0,
    this.size = 20,
  });

  final String? status;
  final String? membershipId;
  final DateTime? fromDate;
  final DateTime? toDate;
  final int page;
  final int size;

  Map<String, dynamic> toQuery() => {
        if (_has(status)) 'status': status,
        if (_has(membershipId)) 'membershipId': membershipId,
        if (fromDate != null) 'fromDate': libraryDateParam(fromDate!),
        if (toDate != null) 'toDate': libraryDateParam(toDate!),
        'page': page,
        'size': size,
      };

  LibraryFineFilter copyWith({
    String? status,
    String? membershipId,
    DateTime? fromDate,
    DateTime? toDate,
    int? page,
    int? size,
    bool clearStatus = false,
    bool clearMembership = false,
    bool clearFromDate = false,
    bool clearToDate = false,
  }) {
    return LibraryFineFilter(
      status: clearStatus ? null : status ?? this.status,
      membershipId: clearMembership ? null : membershipId ?? this.membershipId,
      fromDate: clearFromDate ? null : fromDate ?? this.fromDate,
      toDate: clearToDate ? null : toDate ?? this.toDate,
      page: page ?? this.page,
      size: size ?? this.size,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is LibraryFineFilter &&
        other.status == status &&
        other.membershipId == membershipId &&
        other.fromDate == fromDate &&
        other.toDate == toDate &&
        other.page == page &&
        other.size == size;
  }

  @override
  int get hashCode => Object.hash(
        status,
        membershipId,
        fromDate,
        toDate,
        page,
        size,
      );
}

String libraryDateParam(DateTime date) {
  final month = date.month.toString().padLeft(2, '0');
  final day = date.day.toString().padLeft(2, '0');
  return '${date.year}-$month-$day';
}

DateTime? _date(Object? value) {
  if (value == null) {
    return null;
  }
  return DateTime.tryParse(value.toString());
}

int _int(Object? value) {
  if (value is int) {
    return value;
  }
  if (value is num) {
    return value.toInt();
  }
  return int.tryParse(value?.toString() ?? '') ?? 0;
}

int? _nullableInt(Object? value) {
  if (value == null) {
    return null;
  }
  return _int(value);
}

double _double(Object? value) {
  if (value is num) {
    return value.toDouble();
  }
  return double.tryParse(value?.toString() ?? '') ?? 0;
}

double? _nullableDouble(Object? value) {
  if (value == null) {
    return null;
  }
  return _double(value);
}

bool _has(String? value) => value != null && value.trim().isNotEmpty;
