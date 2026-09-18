package com.school.erp.modules.library.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.academic.domain.Teacher;
import com.school.erp.modules.academic.infrastructure.TeacherRepository;
import com.school.erp.modules.library.api.dto.LibraryAuthorRequest;
import com.school.erp.modules.library.api.dto.LibraryAuthorResponse;
import com.school.erp.modules.library.api.dto.LibraryBookCopyRequest;
import com.school.erp.modules.library.api.dto.LibraryBookCopyResponse;
import com.school.erp.modules.library.api.dto.LibraryBookRequest;
import com.school.erp.modules.library.api.dto.LibraryBookResponse;
import com.school.erp.modules.library.api.dto.LibraryCategoryRequest;
import com.school.erp.modules.library.api.dto.LibraryCategoryResponse;
import com.school.erp.modules.library.api.dto.LibraryFinePaymentRequest;
import com.school.erp.modules.library.api.dto.LibraryFineResponse;
import com.school.erp.modules.library.api.dto.LibraryIssueRequest;
import com.school.erp.modules.library.api.dto.LibraryLoanResponse;
import com.school.erp.modules.library.api.dto.LibraryMembershipRequest;
import com.school.erp.modules.library.api.dto.LibraryMembershipResponse;
import com.school.erp.modules.library.api.dto.LibraryPublisherRequest;
import com.school.erp.modules.library.api.dto.LibraryPublisherResponse;
import com.school.erp.modules.library.api.dto.LibraryReturnRequest;
import com.school.erp.modules.library.api.dto.LibrarySummaryResponse;
import com.school.erp.modules.library.domain.LibraryAuthor;
import com.school.erp.modules.library.domain.LibraryBook;
import com.school.erp.modules.library.domain.LibraryBookCategory;
import com.school.erp.modules.library.domain.LibraryBookCopy;
import com.school.erp.modules.library.domain.LibraryBookCopyStatus;
import com.school.erp.modules.library.domain.LibraryFine;
import com.school.erp.modules.library.domain.LibraryFineStatus;
import com.school.erp.modules.library.domain.LibraryLoan;
import com.school.erp.modules.library.domain.LibraryLoanStatus;
import com.school.erp.modules.library.domain.LibraryMemberType;
import com.school.erp.modules.library.domain.LibraryMembership;
import com.school.erp.modules.library.domain.LibraryPublisher;
import com.school.erp.modules.library.infrastructure.LibraryAuthorRepository;
import com.school.erp.modules.library.infrastructure.LibraryBookCategoryRepository;
import com.school.erp.modules.library.infrastructure.LibraryBookCopyRepository;
import com.school.erp.modules.library.infrastructure.LibraryBookRepository;
import com.school.erp.modules.library.infrastructure.LibraryFineRepository;
import com.school.erp.modules.library.infrastructure.LibraryLoanRepository;
import com.school.erp.modules.library.infrastructure.LibraryMembershipRepository;
import com.school.erp.modules.library.infrastructure.LibraryPublisherRepository;
import com.school.erp.modules.settings.application.ApplicationSettingsService;
import com.school.erp.modules.staff.domain.Staff;
import com.school.erp.modules.staff.infrastructure.StaffRepository;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LibraryService {

	private static final String MODULE_NAME = "LIBRARY";
	private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Kolkata");
	private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

	private final LibraryBookCategoryRepository categoryRepository;
	private final LibraryAuthorRepository authorRepository;
	private final LibraryPublisherRepository publisherRepository;
	private final LibraryBookRepository bookRepository;
	private final LibraryBookCopyRepository copyRepository;
	private final LibraryMembershipRepository membershipRepository;
	private final LibraryLoanRepository loanRepository;
	private final LibraryFineRepository fineRepository;
	private final StudentRepository studentRepository;
	private final TeacherRepository teacherRepository;
	private final StaffRepository staffRepository;
	private final ApplicationSettingsService settingsService;
	private final LibraryMapper mapper;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public List<LibraryCategoryResponse> categories() {
		return categoryRepository.findAllByDeletedFalseOrderByNameAsc().stream()
				.map(mapper::toCategoryResponse)
				.toList();
	}

	@Transactional
	public LibraryCategoryResponse createCategory(LibraryCategoryRequest request) {
		validateCategoryName(request.name(), null);
		LibraryBookCategory category = categoryRepository.save(new LibraryBookCategory(
				request.name(),
				request.description(),
				active(request.active())));
		LibraryCategoryResponse response = mapper.toCategoryResponse(category);
		audit("LibraryBookCategory", category.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public LibraryCategoryResponse updateCategory(UUID categoryId, LibraryCategoryRequest request) {
		LibraryBookCategory category = loadCategory(categoryId);
		LibraryCategoryResponse oldValue = mapper.toCategoryResponse(category);
		validateCategoryName(request.name(), categoryId);
		category.update(request.name(), request.description(), active(request.active()));
		LibraryCategoryResponse response = mapper.toCategoryResponse(category);
		audit("LibraryBookCategory", categoryId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public List<LibraryAuthorResponse> authors() {
		return authorRepository.findAllByDeletedFalseOrderByNameAsc().stream()
				.map(mapper::toAuthorResponse)
				.toList();
	}

	@Transactional
	public LibraryAuthorResponse createAuthor(LibraryAuthorRequest request) {
		validateAuthorName(request.name(), null);
		LibraryAuthor author = authorRepository.save(new LibraryAuthor(
				request.name(),
				request.biography(),
				active(request.active())));
		LibraryAuthorResponse response = mapper.toAuthorResponse(author);
		audit("LibraryAuthor", author.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public LibraryAuthorResponse updateAuthor(UUID authorId, LibraryAuthorRequest request) {
		LibraryAuthor author = loadAuthor(authorId);
		LibraryAuthorResponse oldValue = mapper.toAuthorResponse(author);
		validateAuthorName(request.name(), authorId);
		author.update(request.name(), request.biography(), active(request.active()));
		LibraryAuthorResponse response = mapper.toAuthorResponse(author);
		audit("LibraryAuthor", authorId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public List<LibraryPublisherResponse> publishers() {
		return publisherRepository.findAllByDeletedFalseOrderByNameAsc().stream()
				.map(mapper::toPublisherResponse)
				.toList();
	}

	@Transactional
	public LibraryPublisherResponse createPublisher(LibraryPublisherRequest request) {
		validatePublisherName(request.name(), null);
		LibraryPublisher publisher = publisherRepository.save(new LibraryPublisher(
				request.name(),
				request.contactInfo(),
				active(request.active())));
		LibraryPublisherResponse response = mapper.toPublisherResponse(publisher);
		audit("LibraryPublisher", publisher.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public LibraryPublisherResponse updatePublisher(UUID publisherId, LibraryPublisherRequest request) {
		LibraryPublisher publisher = loadPublisher(publisherId);
		LibraryPublisherResponse oldValue = mapper.toPublisherResponse(publisher);
		validatePublisherName(request.name(), publisherId);
		publisher.update(request.name(), request.contactInfo(), active(request.active()));
		LibraryPublisherResponse response = mapper.toPublisherResponse(publisher);
		audit("LibraryPublisher", publisherId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public PageResponse<LibraryBookResponse> books(
			String keyword,
			UUID categoryId,
			UUID publisherId,
			Boolean active,
			PageRequestDto pageRequest) {
		return PageResponse.from(
				bookRepository.search(blankToNull(keyword), categoryId, publisherId, active, pageRequest.toPageable("title")),
				mapper::toBookResponse);
	}

	@Transactional(readOnly = true)
	public LibraryBookResponse getBook(UUID bookId) {
		return mapper.toBookResponse(loadBook(bookId));
	}

	@Transactional
	public LibraryBookResponse createBook(LibraryBookRequest request) {
		validateIsbn(request.isbn(), null);
		LibraryBook book = bookRepository.save(new LibraryBook(
				request.title(),
				request.isbn(),
				request.categoryId() == null ? null : loadActiveCategory(request.categoryId()),
				request.publisherId() == null ? null : loadActivePublisher(request.publisherId()),
				loadActiveAuthors(request.authorIds()),
				request.edition(),
				request.publicationYear(),
				request.language(),
				request.description(),
				request.shelfLocation(),
				active(request.active())));
		LibraryBookResponse response = mapper.toBookResponse(book);
		audit("LibraryBook", book.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public LibraryBookResponse updateBook(UUID bookId, LibraryBookRequest request) {
		LibraryBook book = loadBook(bookId);
		LibraryBookResponse oldValue = mapper.toBookResponse(book);
		validateIsbn(request.isbn(), bookId);
		book.update(
				request.title(),
				request.isbn(),
				request.categoryId() == null ? null : loadActiveCategory(request.categoryId()),
				request.publisherId() == null ? null : loadActivePublisher(request.publisherId()),
				loadActiveAuthors(request.authorIds()),
				request.edition(),
				request.publicationYear(),
				request.language(),
				request.description(),
				request.shelfLocation(),
				active(request.active()));
		LibraryBookResponse response = mapper.toBookResponse(book);
		audit("LibraryBook", bookId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public LibraryBookResponse deactivateBook(UUID bookId) {
		LibraryBook book = loadBook(bookId);
		LibraryBookResponse oldValue = mapper.toBookResponse(book);
		book.update(
				book.getTitle(),
				book.getIsbn(),
				book.getCategory(),
				book.getPublisher(),
				new LinkedHashSet<>(book.getAuthors()),
				book.getEdition(),
				book.getPublicationYear(),
				book.getLanguage(),
				book.getDescription(),
				book.getShelfLocation(),
				false);
		LibraryBookResponse response = mapper.toBookResponse(book);
		audit("LibraryBook", bookId, "DEACTIVATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public PageResponse<LibraryBookCopyResponse> copies(
			UUID bookId,
			LibraryBookCopyStatus status,
			String keyword,
			PageRequestDto pageRequest) {
		return PageResponse.from(
				copyRepository.search(bookId, status, blankToNull(keyword), pageRequest.toPageable("accessionNumber")),
				mapper::toCopyResponse);
	}

	@Transactional
	public LibraryBookCopyResponse createCopy(LibraryBookCopyRequest request) {
		validateAccession(request.accessionNumber(), null);
		if (request.status() == LibraryBookCopyStatus.ISSUED) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "New copies cannot start in ISSUED status.");
		}
		LibraryBook book = loadActiveBook(request.bookId());
		LibraryBookCopy copy = copyRepository.save(new LibraryBookCopy(
				book,
				request.accessionNumber(),
				request.shelfLocation(),
				request.acquiredOn(),
				request.price(),
				request.conditionNote(),
				request.status()));
		LibraryBookCopyResponse response = mapper.toCopyResponse(copy);
		audit("LibraryBookCopy", copy.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public LibraryBookCopyResponse updateCopy(UUID copyId, LibraryBookCopyRequest request) {
		LibraryBookCopy copy = loadCopy(copyId);
		LibraryBookCopyResponse oldValue = mapper.toCopyResponse(copy);
		validateAccession(request.accessionNumber(), copyId);
		LibraryLoan activeLoan = loanRepository
				.findFirstByCopyIdAndStatusAndDeletedFalse(copyId, LibraryLoanStatus.ACTIVE)
				.orElse(null);
		if (activeLoan != null && request.status() != LibraryBookCopyStatus.ISSUED) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Issued copy status is controlled by the active loan.");
		}
		LibraryBook book = loadActiveBook(request.bookId());
		copy.update(
				book,
				request.accessionNumber(),
				request.shelfLocation(),
				request.acquiredOn(),
				request.price(),
				request.conditionNote(),
				request.status());
		LibraryBookCopyResponse response = mapper.toCopyResponse(copy);
		audit("LibraryBookCopy", copyId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public LibraryBookCopyResponse markCopyLost(UUID copyId, String note) {
		LibraryBookCopy copy = loadCopy(copyId);
		LibraryBookCopyResponse oldValue = mapper.toCopyResponse(copy);
		copy.markLost(note);
		LibraryBookCopyResponse response = mapper.toCopyResponse(copy);
		audit("LibraryBookCopy", copyId, "LOST", oldValue, response);
		return response;
	}

	@Transactional
	public LibraryBookCopyResponse markCopyDamaged(UUID copyId, String note) {
		LibraryBookCopy copy = loadCopy(copyId);
		LibraryBookCopyResponse oldValue = mapper.toCopyResponse(copy);
		copy.markDamaged(note);
		LibraryBookCopyResponse response = mapper.toCopyResponse(copy);
		audit("LibraryBookCopy", copyId, "DAMAGED", oldValue, response);
		return response;
	}

	@Transactional
	public LibraryBookCopyResponse markCopyAvailable(UUID copyId) {
		LibraryBookCopy copy = loadCopy(copyId);
		if (loanRepository.findFirstByCopyIdAndStatusAndDeletedFalse(copyId, LibraryLoanStatus.ACTIVE).isPresent()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Copy has an active loan.");
		}
		LibraryBookCopyResponse oldValue = mapper.toCopyResponse(copy);
		copy.markAvailable();
		LibraryBookCopyResponse response = mapper.toCopyResponse(copy);
		audit("LibraryBookCopy", copyId, "AVAILABLE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public PageResponse<LibraryMembershipResponse> memberships(
			LibraryMemberType memberType,
			Boolean active,
			String keyword,
			PageRequestDto pageRequest) {
		return PageResponse.from(
				membershipRepository.search(memberType, active, blankToNull(keyword), pageRequest.toPageable("membershipNumber")),
				this::toMembershipResponse);
	}

	@Transactional
	public LibraryMembershipResponse createMembership(LibraryMembershipRequest request) {
		validateMembershipRequest(request, null);
		validateMembershipNumber(request.membershipNumber(), null);
		LibraryMembership membership = membershipRepository.save(new LibraryMembership(
				request.memberType(),
				request.studentId(),
				request.teacherId(),
				request.staffId(),
				request.membershipNumber(),
				request.startDate(),
				request.expiryDate(),
				active(request.active()),
				request.notes()));
		LibraryMembershipResponse response = toMembershipResponse(membership);
		audit("LibraryMembership", membership.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public LibraryMembershipResponse updateMembership(UUID membershipId, LibraryMembershipRequest request) {
		LibraryMembership membership = loadMembership(membershipId);
		LibraryMembershipResponse oldValue = toMembershipResponse(membership);
		validateMembershipRequest(request, membershipId);
		validateMembershipNumber(request.membershipNumber(), membershipId);
		membership.update(
				request.memberType(),
				request.studentId(),
				request.teacherId(),
				request.staffId(),
				request.membershipNumber(),
				request.startDate(),
				request.expiryDate(),
				active(request.active()),
				request.notes());
		LibraryMembershipResponse response = toMembershipResponse(membership);
		audit("LibraryMembership", membershipId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public LibraryMembershipResponse deactivateMembership(UUID membershipId) {
		LibraryMembership membership = loadMembership(membershipId);
		LibraryMembershipResponse oldValue = toMembershipResponse(membership);
		membership.deactivate();
		LibraryMembershipResponse response = toMembershipResponse(membership);
		audit("LibraryMembership", membershipId, "DEACTIVATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public PageResponse<LibraryLoanResponse> loans(
			LibraryLoanStatus status,
			UUID membershipId,
			UUID bookId,
			UUID copyId,
			LibraryMemberType memberType,
			LocalDate fromDate,
			LocalDate toDate,
			boolean overdueOnly,
			PageRequestDto pageRequest) {
		LocalDate today = today();
		return PageResponse.from(
				loanRepository.search(
						status,
						membershipId,
						bookId,
						copyId,
						memberType,
						fromDate,
						toDate,
						overdueOnly,
						today,
						pageRequest.toPageable("issueDate")),
				loan -> toLoanResponse(loan, today));
	}

	@Transactional
	public LibraryLoanResponse issue(LibraryIssueRequest request) {
		LocalDate issueDate = request.issueDate() == null ? today() : request.issueDate();
		LocalDate dueDate = request.dueDate() == null ? issueDate.plusDays(defaultLoanDays()) : request.dueDate();
		if (dueDate.isBefore(issueDate)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Due date cannot be before issue date.");
		}
		LibraryBookCopy copy = lockCopy(request.copyId());
		if (copy.getStatus() != LibraryBookCopyStatus.AVAILABLE) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only available copies can be issued.");
		}
		if (!copy.getBook().isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Inactive books cannot be issued.");
		}
		LibraryMembership membership = loadMembership(request.membershipId());
		if (!membership.isCurrent(issueDate)) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Library membership is not active for issue date.");
		}
		long activeLoans = loanRepository.countByMembershipIdAndStatusAndDeletedFalse(membership.getId(), LibraryLoanStatus.ACTIVE);
		if (activeLoans >= maxActiveLoans()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Member has reached the active loan limit.");
		}
		copy.markIssued();
		LibraryLoan loan = loanRepository.save(new LibraryLoan(
				copy,
				membership,
				issueDate,
				dueDate,
				currentActor(),
				request.remarks()));
		LibraryLoanResponse response = toLoanResponse(loan, today());
		audit("LibraryLoan", loan.getId(), "ISSUE", null, response);
		return response;
	}

	@Transactional
	public LibraryLoanResponse returnLoan(UUID loanId, LibraryReturnRequest request) {
		LibraryLoan loan = loadLoan(loanId);
		if (loan.getStatus() != LibraryLoanStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only active loans can be returned.");
		}
		LocalDate returnDate = request == null || request.returnDate() == null ? today() : request.returnDate();
		if (returnDate.isBefore(loan.getIssueDate())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Return date cannot be before issue date.");
		}
		LibraryLoanResponse oldValue = toLoanResponse(loan, today());
		LibraryBookCopy copy = lockCopy(loan.getCopy().getId());
		loan.markReturned(returnDate, currentActor(), request == null ? null : request.remarks());
		BigDecimal fineAmount = calculateFine(loan, returnDate);
		if (fineAmount.compareTo(BigDecimal.ZERO) > 0) {
			upsertFine(loan, fineAmount, "Overdue by " + ChronoUnit.DAYS.between(loan.getDueDate(), returnDate) + " day(s)", returnDate);
		}
		if (request != null && Boolean.TRUE.equals(request.markDamaged())) {
			copy.markDamaged(request.remarks());
		}
		else {
			copy.markAvailable();
		}
		LibraryLoanResponse response = toLoanResponse(loan, today());
		audit("LibraryLoan", loanId, "RETURN", oldValue, response);
		return response;
	}

	@Transactional
	public LibraryLoanResponse markLoanLost(UUID loanId, String note) {
		LibraryLoan loan = loadLoan(loanId);
		if (loan.getStatus() != LibraryLoanStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only active loans can be marked lost.");
		}
		LibraryLoanResponse oldValue = toLoanResponse(loan, today());
		LibraryBookCopy copy = lockCopy(loan.getCopy().getId());
		loan.markLost(note);
		copy.markLost(note);
		LibraryLoanResponse response = toLoanResponse(loan, today());
		audit("LibraryLoan", loanId, "LOST", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public PageResponse<LibraryFineResponse> fines(
			LibraryFineStatus status,
			UUID membershipId,
			LocalDate fromDate,
			LocalDate toDate,
			PageRequestDto pageRequest) {
		return PageResponse.from(
				fineRepository.search(status, membershipId, fromDate, toDate, pageRequest.toPageable("fineDate")),
				this::toFineResponse);
	}

	@Transactional
	public LibraryFineResponse payFine(UUID fineId, LibraryFinePaymentRequest request) {
		LibraryFine fine = loadFine(fineId);
		if (fine.getStatus() != LibraryFineStatus.PENDING) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only pending fines can be paid.");
		}
		LibraryFineResponse oldValue = toFineResponse(fine);
		BigDecimal amount = request == null || request.amount() == null ? fine.getAmount() : request.amount();
		if (amount.compareTo(BigDecimal.ZERO) < 0 || amount.compareTo(fine.getAmount()) > 0) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Fine payment amount is invalid.");
		}
		fine.pay(amount, currentActor());
		LibraryFineResponse response = toFineResponse(fine);
		audit("LibraryFine", fineId, "PAY", oldValue, response);
		return response;
	}

	@Transactional
	public LibraryFineResponse waiveFine(UUID fineId) {
		LibraryFine fine = loadFine(fineId);
		if (fine.getStatus() != LibraryFineStatus.PENDING) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only pending fines can be waived.");
		}
		LibraryFineResponse oldValue = toFineResponse(fine);
		fine.waive(currentActor());
		LibraryFineResponse response = toFineResponse(fine);
		audit("LibraryFine", fineId, "WAIVE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public LibrarySummaryResponse summary() {
		return new LibrarySummaryResponse(
				bookRepository.countByDeletedFalse(),
				copyRepository.countByDeletedFalse(),
				copyRepository.countByStatusAndDeletedFalse(LibraryBookCopyStatus.AVAILABLE),
				copyRepository.countByStatusAndDeletedFalse(LibraryBookCopyStatus.ISSUED),
				loanRepository.countByStatusAndDueDateBeforeAndDeletedFalse(LibraryLoanStatus.ACTIVE, today()),
				membershipRepository.countByActiveTrueAndDeletedFalse(),
				copyRepository.countByStatusAndDeletedFalse(LibraryBookCopyStatus.LOST),
				copyRepository.countByStatusAndDeletedFalse(LibraryBookCopyStatus.DAMAGED),
				scale(fineRepository.pendingAmount()));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> inventoryRows(UUID categoryId, UUID publisherId, Boolean active) {
		List<Map<String, Object>> rows = new java.util.ArrayList<>();
		int pageNumber = 0;
		Page<LibraryBook> page;
		do {
			page = bookRepository.search(
					null,
					categoryId,
					publisherId,
					active,
					PageRequest.of(pageNumber, 200, Sort.by("title").ascending()));
			page.getContent().stream().map(this::bookRow).forEach(rows::add);
			pageNumber++;
		}
		while (page.hasNext());
		return rows;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> copyRows(UUID bookId, LibraryBookCopyStatus status, String keyword) {
		List<Map<String, Object>> rows = new java.util.ArrayList<>();
		int pageNumber = 0;
		Page<LibraryBookCopy> page;
		do {
			page = copyRepository.search(
					bookId,
					status,
					blankToNull(keyword),
					PageRequest.of(pageNumber, 200, Sort.by("accessionNumber").ascending()));
			page.getContent().stream().map(this::copyRow).forEach(rows::add);
			pageNumber++;
		}
		while (page.hasNext());
		return rows;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> loanRows(
			LibraryLoanStatus status,
			UUID membershipId,
			UUID bookId,
			LibraryMemberType memberType,
			LocalDate fromDate,
			LocalDate toDate,
			boolean overdueOnly) {
		return loanRepository.reportRows(status, membershipId, bookId, memberType, fromDate, toDate, overdueOnly, today()).stream()
				.map(this::loanRow)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> fineRows(
			LibraryFineStatus status,
			UUID membershipId,
			LocalDate fromDate,
			LocalDate toDate) {
		return fineRepository.reportRows(status, membershipId, fromDate, toDate).stream()
				.map(this::fineRow)
				.toList();
	}

	private Map<String, Object> bookRow(LibraryBook book) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("Title", book.getTitle());
		row.put("ISBN", book.getIsbn());
		row.put("Category", book.getCategory() == null ? null : book.getCategory().getName());
		row.put("Publisher", book.getPublisher() == null ? null : book.getPublisher().getName());
		row.put("Authors", book.getAuthors().stream().map(LibraryAuthor::getName).sorted().toList());
		row.put("Publication Year", book.getPublicationYear());
		row.put("Shelf Location", book.getShelfLocation());
		row.put("Active", book.isActive() ? "YES" : "NO");
		row.put("Copies", copyRepository.countByBookIdAndDeletedFalse(book.getId()));
		return row;
	}

	private Map<String, Object> copyRow(LibraryBookCopy copy) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("Accession Number", copy.getAccessionNumber());
		row.put("Title", copy.getBook().getTitle());
		row.put("ISBN", copy.getBook().getIsbn());
		row.put("Status", copy.getStatus());
		row.put("Shelf Location", copy.getShelfLocation());
		row.put("Acquired On", copy.getAcquiredOn());
		row.put("Price", scale(copy.getPrice()));
		row.put("Condition Note", copy.getConditionNote());
		return row;
	}

	private Map<String, Object> loanRow(LibraryLoan loan) {
		MemberDetails member = memberDetails(loan.getMembership());
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("Loan ID", loan.getId());
		row.put("Accession Number", loan.getCopy().getAccessionNumber());
		row.put("Title", loan.getCopy().getBook().getTitle());
		row.put("Membership Number", loan.getMembership().getMembershipNumber());
		row.put("Member Type", loan.getMembership().getMemberType());
		row.put("Member Name", member.name());
		row.put("Issue Date", loan.getIssueDate());
		row.put("Due Date", loan.getDueDate());
		row.put("Return Date", loan.getReturnDate());
		row.put("Status", loan.getStatus());
		row.put("Overdue", mapper.isOverdue(loan, today()) ? "YES" : "NO");
		return row;
	}

	private Map<String, Object> fineRow(LibraryFine fine) {
		MemberDetails member = memberDetails(fine.getLoan().getMembership());
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("Accession Number", fine.getLoan().getCopy().getAccessionNumber());
		row.put("Title", fine.getLoan().getCopy().getBook().getTitle());
		row.put("Membership Number", fine.getLoan().getMembership().getMembershipNumber());
		row.put("Member Name", member.name());
		row.put("Fine Date", fine.getFineDate());
		row.put("Amount", scale(fine.getAmount()));
		row.put("Paid Amount", scale(fine.getPaidAmount()));
		row.put("Reason", fine.getReason());
		row.put("Status", fine.getStatus());
		return row;
	}

	private LibraryMembershipResponse toMembershipResponse(LibraryMembership membership) {
		MemberDetails details = memberDetails(membership);
		return mapper.toMembershipResponse(membership, details.code(), details.name());
	}

	private LibraryLoanResponse toLoanResponse(LibraryLoan loan, LocalDate today) {
		BigDecimal pendingFine = fineRepository.findByLoanIdAndDeletedFalse(loan.getId())
				.filter(fine -> fine.getStatus() == LibraryFineStatus.PENDING)
				.map(fine -> scale(fine.getAmount().subtract(fine.getPaidAmount())))
				.orElse(ZERO);
		return mapper.toLoanResponse(loan, memberDetails(loan.getMembership()).name(), mapper.isOverdue(loan, today), pendingFine);
	}

	private LibraryFineResponse toFineResponse(LibraryFine fine) {
		return mapper.toFineResponse(fine, memberDetails(fine.getLoan().getMembership()).name());
	}

	private void validateMembershipRequest(LibraryMembershipRequest request, UUID excludedId) {
		UUID studentId = request.memberType() == LibraryMemberType.STUDENT ? request.studentId() : null;
		UUID teacherId = request.memberType() == LibraryMemberType.TEACHER ? request.teacherId() : null;
		UUID staffId = request.memberType() == LibraryMemberType.STAFF ? request.staffId() : null;
		if (request.memberType() == LibraryMemberType.STUDENT) {
			if (studentId == null) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Student ID is required for student memberships.");
			}
			studentRepository.findByIdAndDeletedFalse(studentId)
					.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
		}
		else if (request.memberType() == LibraryMemberType.TEACHER) {
			if (teacherId == null) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Teacher ID is required for teacher memberships.");
			}
			Teacher teacher = teacherRepository.findByIdAndDeletedFalse(teacherId)
					.orElseThrow(() -> new ResourceNotFoundException("Teacher", teacherId));
			if (!teacher.isActive()) {
				throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Teacher is inactive.");
			}
		}
		else {
			if (staffId == null) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Staff ID is required for staff memberships.");
			}
			Staff staff = staffRepository.findByIdAndDeletedFalse(staffId)
					.orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));
			if (!staff.isActive()) {
				throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Staff is inactive.");
			}
		}
		if (request.expiryDate() != null && request.expiryDate().isBefore(request.startDate())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Membership expiry date cannot be before start date.");
		}
		if (active(request.active())
				&& membershipRepository.existsActiveMembershipForMember(
						request.memberType(),
						studentId,
						teacherId,
						staffId,
						excludedId)) {
			throw new BusinessException(ErrorCode.CONFLICT, "Active library membership already exists for this member.");
		}
	}

	private void validateCategoryName(String name, UUID excludedId) {
		categoryRepository.findByNameIgnoreCaseAndDeletedFalse(name)
				.filter(existing -> excludedId == null || !existing.getId().equals(excludedId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Library category name already exists.");
				});
	}

	private void validateAuthorName(String name, UUID excludedId) {
		authorRepository.findByNameIgnoreCaseAndDeletedFalse(name)
				.filter(existing -> excludedId == null || !existing.getId().equals(excludedId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Library author name already exists.");
				});
	}

	private void validatePublisherName(String name, UUID excludedId) {
		publisherRepository.findByNameIgnoreCaseAndDeletedFalse(name)
				.filter(existing -> excludedId == null || !existing.getId().equals(excludedId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Library publisher name already exists.");
				});
	}

	private void validateIsbn(String isbn, UUID excludedBookId) {
		String normalized = normalizeIdentifier(isbn);
		if (normalized == null) {
			return;
		}
		bookRepository.findByIsbnIgnoreCaseAndDeletedFalse(normalized)
				.filter(existing -> excludedBookId == null || !existing.getId().equals(excludedBookId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Library ISBN already exists.");
				});
	}

	private void validateAccession(String accessionNumber, UUID excludedCopyId) {
		copyRepository.findByAccessionNumberIgnoreCaseAndDeletedFalse(accessionNumber)
				.filter(existing -> excludedCopyId == null || !existing.getId().equals(excludedCopyId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Library accession number already exists.");
				});
	}

	private void validateMembershipNumber(String membershipNumber, UUID excludedMembershipId) {
		membershipRepository.findByMembershipNumberIgnoreCaseAndDeletedFalse(membershipNumber)
				.filter(existing -> excludedMembershipId == null || !existing.getId().equals(excludedMembershipId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Library membership number already exists.");
				});
	}

	private BigDecimal calculateFine(LibraryLoan loan, LocalDate returnDate) {
		if (!returnDate.isAfter(loan.getDueDate())) {
			return ZERO;
		}
		long overdueDays = ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);
		return scale(finePerDay().multiply(BigDecimal.valueOf(overdueDays)));
	}

	private LibraryFine upsertFine(LibraryLoan loan, BigDecimal amount, String reason, LocalDate fineDate) {
		LibraryFine fine = fineRepository.findByLoanIdAndDeletedFalse(loan.getId())
				.orElseGet(() -> new LibraryFine(loan, amount, reason, fineDate));
		fine.updateAmount(amount, reason, fineDate);
		return fineRepository.save(fine);
	}

	private LibraryBookCategory loadCategory(UUID categoryId) {
		return categoryRepository.findByIdAndDeletedFalse(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Library category", categoryId));
	}

	private LibraryBookCategory loadActiveCategory(UUID categoryId) {
		LibraryBookCategory category = loadCategory(categoryId);
		if (!category.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Library category is inactive.");
		}
		return category;
	}

	private LibraryAuthor loadAuthor(UUID authorId) {
		return authorRepository.findByIdAndDeletedFalse(authorId)
				.orElseThrow(() -> new ResourceNotFoundException("Library author", authorId));
	}

	private Set<LibraryAuthor> loadActiveAuthors(List<UUID> authorIds) {
		if (authorIds == null || authorIds.isEmpty()) {
			return Set.of();
		}
		Set<UUID> requested = new LinkedHashSet<>(authorIds);
		List<LibraryAuthor> authors = authorRepository.findByIdInAndDeletedFalse(requested);
		if (authors.size() != requested.size()) {
			throw new ResourceNotFoundException("Library author", requested);
		}
		authors.stream()
				.filter(author -> !author.isActive())
				.findFirst()
				.ifPresent(author -> {
					throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Library author is inactive.");
				});
		return new LinkedHashSet<>(authors);
	}

	private LibraryPublisher loadPublisher(UUID publisherId) {
		return publisherRepository.findByIdAndDeletedFalse(publisherId)
				.orElseThrow(() -> new ResourceNotFoundException("Library publisher", publisherId));
	}

	private LibraryPublisher loadActivePublisher(UUID publisherId) {
		LibraryPublisher publisher = loadPublisher(publisherId);
		if (!publisher.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Library publisher is inactive.");
		}
		return publisher;
	}

	private LibraryBook loadBook(UUID bookId) {
		return bookRepository.findByIdAndDeletedFalse(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("Library book", bookId));
	}

	private LibraryBook loadActiveBook(UUID bookId) {
		LibraryBook book = loadBook(bookId);
		if (!book.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Library book is inactive.");
		}
		return book;
	}

	private LibraryBookCopy loadCopy(UUID copyId) {
		return copyRepository.findByIdAndDeletedFalse(copyId)
				.orElseThrow(() -> new ResourceNotFoundException("Library book copy", copyId));
	}

	private LibraryBookCopy lockCopy(UUID copyId) {
		return copyRepository.findLockedById(copyId)
				.orElseThrow(() -> new ResourceNotFoundException("Library book copy", copyId));
	}

	private LibraryMembership loadMembership(UUID membershipId) {
		return membershipRepository.findByIdAndDeletedFalse(membershipId)
				.orElseThrow(() -> new ResourceNotFoundException("Library membership", membershipId));
	}

	private LibraryLoan loadLoan(UUID loanId) {
		return loanRepository.findByIdAndDeletedFalse(loanId)
				.orElseThrow(() -> new ResourceNotFoundException("Library loan", loanId));
	}

	private LibraryFine loadFine(UUID fineId) {
		return fineRepository.findByIdAndDeletedFalse(fineId)
				.orElseThrow(() -> new ResourceNotFoundException("Library fine", fineId));
	}

	private MemberDetails memberDetails(LibraryMembership membership) {
		return switch (membership.getMemberType()) {
			case STUDENT -> studentRepository.findByIdAndDeletedFalse(membership.getStudentId())
					.map(student -> new MemberDetails(student.getAdmissionNumber(), student.getDisplayName()))
					.orElse(new MemberDetails(null, "Student " + membership.getStudentId()));
			case TEACHER -> teacherRepository.findByIdAndDeletedFalse(membership.getTeacherId())
					.map(teacher -> new MemberDetails(teacher.getEmployeeNumber(), teacher.getDisplayName()))
					.orElse(new MemberDetails(null, "Teacher " + membership.getTeacherId()));
			case STAFF -> staffRepository.findByIdAndDeletedFalse(membership.getStaffId())
					.map(staff -> new MemberDetails(staff.getEmployeeCode(), staff.getDisplayName()))
					.orElse(new MemberDetails(null, "Staff " + membership.getStaffId()));
		};
	}

	private int defaultLoanDays() {
		return intSetting("defaultLoanDays", 14);
	}

	private int maxActiveLoans() {
		return intSetting("maxActiveLoans", 3);
	}

	private BigDecimal finePerDay() {
		try {
			return scale(new BigDecimal(settingsService.rawSettingValue("library", "finePerDay")));
		}
		catch (RuntimeException ex) {
			return ZERO;
		}
	}

	private int intSetting(String key, int fallback) {
		try {
			String raw = settingsService.rawSettingValue("library", key);
			return StringUtils.hasText(raw) ? Integer.parseInt(raw) : fallback;
		}
		catch (RuntimeException ex) {
			return fallback;
		}
	}

	private LocalDate today() {
		return LocalDate.now(SCHOOL_ZONE);
	}

	private BigDecimal scale(BigDecimal value) {
		return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
	}

	private boolean active(Boolean active) {
		return active == null || active;
	}

	private String normalizeIdentifier(String value) {
		return StringUtils.hasText(value) ? value.replaceAll("[\\s-]", "").trim().toUpperCase() : null;
	}

	private String blankToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "system";
		}
		return authentication.getName();
	}

	private void audit(String entityName, UUID entityId, String action, Object oldValue, Object newValue) {
		auditLogService.record(new AuditLogEvent(
				MODULE_NAME,
				entityName,
				entityId == null ? null : entityId.toString(),
				action,
				oldValue,
				newValue == null ? Map.of() : newValue));
	}

	private record MemberDetails(String code, String name) {
	}
}
