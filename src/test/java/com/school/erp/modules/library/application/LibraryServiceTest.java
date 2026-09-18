package com.school.erp.modules.library.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.academic.infrastructure.TeacherRepository;
import com.school.erp.modules.library.api.dto.LibraryAuthorRequest;
import com.school.erp.modules.library.api.dto.LibraryBookCopyRequest;
import com.school.erp.modules.library.api.dto.LibraryBookRequest;
import com.school.erp.modules.library.api.dto.LibraryCategoryRequest;
import com.school.erp.modules.library.api.dto.LibraryIssueRequest;
import com.school.erp.modules.library.api.dto.LibraryMembershipRequest;
import com.school.erp.modules.library.api.dto.LibraryPublisherRequest;
import com.school.erp.modules.library.api.dto.LibraryReturnRequest;
import com.school.erp.modules.library.domain.LibraryAuthor;
import com.school.erp.modules.library.domain.LibraryBook;
import com.school.erp.modules.library.domain.LibraryBookCategory;
import com.school.erp.modules.library.domain.LibraryBookCopy;
import com.school.erp.modules.library.domain.LibraryBookCopyStatus;
import com.school.erp.modules.library.domain.LibraryFine;
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
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.infrastructure.StudentRepository;
import com.school.erp.modules.staff.infrastructure.StaffRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

	@Mock
	private LibraryBookCategoryRepository categoryRepository;

	@Mock
	private LibraryAuthorRepository authorRepository;

	@Mock
	private LibraryPublisherRepository publisherRepository;

	@Mock
	private LibraryBookRepository bookRepository;

	@Mock
	private LibraryBookCopyRepository copyRepository;

	@Mock
	private LibraryMembershipRepository membershipRepository;

	@Mock
	private LibraryLoanRepository loanRepository;

	@Mock
	private LibraryFineRepository fineRepository;

	@Mock
	private StudentRepository studentRepository;

	@Mock
	private TeacherRepository teacherRepository;

	@Mock
	private StaffRepository staffRepository;

	@Mock
	private ApplicationSettingsService settingsService;

	@Mock
	private AuditLogService auditLogService;

	private LibraryService libraryService;
	private LibraryBookCategory category;
	private LibraryAuthor author;
	private LibraryPublisher publisher;
	private LibraryBook book;
	private LibraryBookCopy copy;
	private LibraryMembership membership;
	private Student student;
	private LibraryLoan savedLoan;

	@BeforeEach
	void setUp() {
		libraryService = new LibraryService(
				categoryRepository,
				authorRepository,
				publisherRepository,
				bookRepository,
				copyRepository,
				membershipRepository,
				loanRepository,
				fineRepository,
				studentRepository,
				teacherRepository,
				staffRepository,
				settingsService,
				new LibraryMapper(),
				auditLogService);
		category = entity(new LibraryBookCategory("Fiction", null, true));
		author = entity(new LibraryAuthor("R. K. Narayan", null, true));
		publisher = entity(new LibraryPublisher("Indian Thought", null, true));
		book = entity(new LibraryBook(
				"Malgudi Days",
				"978-81-85986-17-3",
				category,
				publisher,
				Set.of(author),
				"1",
				1943,
				"English",
				null,
				"A1",
				true));
		copy = entity(new LibraryBookCopy(
				book,
				"LIB-001",
				"A1",
				LocalDate.of(2026, 4, 1),
				new BigDecimal("250.00"),
				null,
				LibraryBookCopyStatus.AVAILABLE));
		student = entity(new Student("ADM-001", "Aarav", LocalDate.of(2014, 5, 1), Gender.MALE, LocalDate.of(2026, 4, 1)));
		membership = entity(new LibraryMembership(
				LibraryMemberType.STUDENT,
				student.getId(),
				null,
				null,
				"LIB-M-001",
				LocalDate.of(2026, 4, 1),
				null,
				true,
				null));
	}

	@Test
	void createsCanonicalMastersBookAndCopy() {
		when(categoryRepository.findByNameIgnoreCaseAndDeletedFalse("Fiction")).thenReturn(Optional.empty());
		when(categoryRepository.save(any(LibraryBookCategory.class))).thenAnswer(invocation -> entity(invocation.getArgument(0)));
		when(authorRepository.findByNameIgnoreCaseAndDeletedFalse("R. K. Narayan")).thenReturn(Optional.empty());
		when(authorRepository.save(any(LibraryAuthor.class))).thenAnswer(invocation -> entity(invocation.getArgument(0)));
		when(publisherRepository.findByNameIgnoreCaseAndDeletedFalse("Indian Thought")).thenReturn(Optional.empty());
		when(publisherRepository.save(any(LibraryPublisher.class))).thenAnswer(invocation -> entity(invocation.getArgument(0)));
		when(bookRepository.findByIsbnIgnoreCaseAndDeletedFalse("9788185986173")).thenReturn(Optional.empty());
		when(categoryRepository.findByIdAndDeletedFalse(category.getId())).thenReturn(Optional.of(category));
		when(publisherRepository.findByIdAndDeletedFalse(publisher.getId())).thenReturn(Optional.of(publisher));
		when(authorRepository.findByIdInAndDeletedFalse(any())).thenReturn(List.of(author));
		when(bookRepository.save(any(LibraryBook.class))).thenAnswer(invocation -> entity(invocation.getArgument(0)));
		when(copyRepository.findByAccessionNumberIgnoreCaseAndDeletedFalse("LIB-001")).thenReturn(Optional.empty());
		when(bookRepository.findByIdAndDeletedFalse(book.getId())).thenReturn(Optional.of(book));
		when(copyRepository.save(any(LibraryBookCopy.class))).thenAnswer(invocation -> entity(invocation.getArgument(0)));

		var categoryResponse = libraryService.createCategory(new LibraryCategoryRequest("Fiction", null, true));
		var authorResponse = libraryService.createAuthor(new LibraryAuthorRequest("R. K. Narayan", null, true));
		var publisherResponse = libraryService.createPublisher(new LibraryPublisherRequest("Indian Thought", null, true));
		var bookResponse = libraryService.createBook(new LibraryBookRequest(
				"Malgudi Days",
				"978-81-85986-17-3",
				category.getId(),
				publisher.getId(),
				List.of(author.getId()),
				"1",
				1943,
				"English",
				null,
				"A1",
				true));
		var copyResponse = libraryService.createCopy(new LibraryBookCopyRequest(
				book.getId(),
				"LIB-001",
				"A1",
				LocalDate.of(2026, 4, 1),
				new BigDecimal("250.00"),
				null,
				null));

		assertThat(categoryResponse.name()).isEqualTo("Fiction");
		assertThat(authorResponse.name()).isEqualTo("R. K. Narayan");
		assertThat(publisherResponse.name()).isEqualTo("Indian Thought");
		assertThat(bookResponse.isbn()).isEqualTo("9788185986173");
		assertThat(bookResponse.authors()).extracting("name").containsExactly("R. K. Narayan");
		assertThat(copyResponse.status()).isEqualTo(LibraryBookCopyStatus.AVAILABLE);
	}

	@Test
	void membershipPreventsDuplicateActiveMember() {
		when(studentRepository.findByIdAndDeletedFalse(student.getId())).thenReturn(Optional.of(student));
		when(membershipRepository.existsActiveMembershipForMember(
				LibraryMemberType.STUDENT,
				student.getId(),
				null,
				null,
				null))
				.thenReturn(true);

		assertThatThrownBy(() -> libraryService.createMembership(new LibraryMembershipRequest(
				LibraryMemberType.STUDENT,
				student.getId(),
				null,
				null,
				"LIB-M-002",
				LocalDate.of(2026, 4, 1),
				null,
				true,
				null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CONFLICT);
	}

	@Test
	void issueAndReturnUpdateCopyAndCreateOverdueFine() {
		when(copyRepository.findLockedById(copy.getId())).thenReturn(Optional.of(copy));
		when(membershipRepository.findByIdAndDeletedFalse(membership.getId())).thenReturn(Optional.of(membership));
		when(loanRepository.countByMembershipIdAndStatusAndDeletedFalse(membership.getId(), LibraryLoanStatus.ACTIVE)).thenReturn(0L);
		when(loanRepository.save(any(LibraryLoan.class))).thenAnswer(invocation -> {
			savedLoan = entity(invocation.getArgument(0));
			return savedLoan;
		});
		when(fineRepository.findByLoanIdAndDeletedFalse(any())).thenReturn(Optional.empty());
		when(settingsService.rawSettingValue("library", "finePerDay")).thenReturn("5.00");
		when(loanRepository.findByIdAndDeletedFalse(any())).thenAnswer(invocation -> Optional.of(savedLoan));
		when(fineRepository.save(any(LibraryFine.class))).thenAnswer(invocation -> entity(invocation.getArgument(0)));
		when(studentRepository.findByIdAndDeletedFalse(student.getId())).thenReturn(Optional.of(student));

		var issued = libraryService.issue(new LibraryIssueRequest(
				copy.getId(),
				membership.getId(),
				LocalDate.of(2026, 9, 1),
				LocalDate.of(2026, 9, 10),
				null));
		var returned = libraryService.returnLoan(issued.id(), new LibraryReturnRequest(
				LocalDate.of(2026, 9, 13),
				false,
				"Returned late"));

		assertThat(copy.getStatus()).isEqualTo(LibraryBookCopyStatus.AVAILABLE);
		assertThat(returned.status()).isEqualTo(LibraryLoanStatus.RETURNED);
		verify(fineRepository).save(any(LibraryFine.class));
	}

	@Test
	void issueRejectsUnavailableCopy() {
		copy.markDamaged("Binding torn");
		when(copyRepository.findLockedById(copy.getId())).thenReturn(Optional.of(copy));

		assertThatThrownBy(() -> libraryService.issue(new LibraryIssueRequest(
				copy.getId(),
				membership.getId(),
				LocalDate.of(2026, 9, 1),
				null,
				null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	private <T> T entity(T entity) {
		if (ReflectionTestUtils.getField(entity, "id") == null) {
			setId(entity, UUID.randomUUID());
		}
		return entity;
	}

	private void setId(Object entity, UUID id) {
		ReflectionTestUtils.setField(entity, "id", id);
	}
}
