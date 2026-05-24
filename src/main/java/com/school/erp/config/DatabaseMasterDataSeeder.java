package com.school.erp.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.school.erp.modules.fees.api.dto.FeeStructureInstallmentRequest;
import com.school.erp.modules.fees.api.dto.FeeStructureItemRequest;
import com.school.erp.modules.fees.api.dto.FeeStructureRequest;
import com.school.erp.modules.fees.api.dto.PaymentCollectionRequest;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentRequest;
import com.school.erp.modules.fees.application.FeeService;
import com.school.erp.modules.fees.domain.FeeCategory;
import com.school.erp.modules.fees.domain.FeeStructure;
import com.school.erp.modules.fees.domain.PaymentMode;
import com.school.erp.modules.fees.domain.StudentFeeAssignment;
import com.school.erp.modules.fees.infrastructure.FeeCategoryRepository;
import com.school.erp.modules.fees.infrastructure.FeeStructureRepository;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.ParentGuardian;
import com.school.erp.modules.students.domain.ParentRelation;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.infrastructure.ParentGuardianRepository;
import com.school.erp.modules.students.infrastructure.StudentRepository;
import com.school.erp.modules.users.domain.Permission;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.infrastructure.PermissionRepository;
import com.school.erp.modules.users.infrastructure.RoleRepository;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@Profile("!localqa")
@ConditionalOnProperty(prefix = "app.master-data.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DatabaseMasterDataSeeder {

	private static final Logger log = LoggerFactory.getLogger(DatabaseMasterDataSeeder.class);
	public static final String DEFAULT_ADMIN_PASSWORD = "Admin@12345678";
	public static final String DEFAULT_DEMO_PASSWORD = "Demo@12345678";
	private static final String DEMO_ACADEMIC_YEAR = "2026-2027";
	private static final String DEMO_CLASS_NAME = "Class 6";
	private static final String DEMO_SECTION_NAME = "A";

	private final UserAccountRepository userAccountRepository;
	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;
	private final PasswordEncoder passwordEncoder;
	private final StudentRepository studentRepository;
	private final ParentGuardianRepository parentGuardianRepository;
	private final FeeCategoryRepository feeCategoryRepository;
	private final FeeStructureRepository feeStructureRepository;
	private final StudentFeeAssignmentRepository assignmentRepository;
	private final FeeService feeService;

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void seed() {
		Map<String, Permission> permissions = seedPermissions();
		seedRolePermissions(permissions);
		List.of(
				user("admin@school.com", "admin", "System", "Admin", "9999999999", DEFAULT_ADMIN_PASSWORD, RoleName.SUPER_ADMIN),
				user("admin@school.test", "admin.test", "System", "Admin", "9999999900", DEFAULT_ADMIN_PASSWORD, RoleName.SUPER_ADMIN),
				user("principal@school.com", "principal", "Main", "Principal", "9999999998", DEFAULT_DEMO_PASSWORD, RoleName.PRINCIPAL),
				user("teacher1@school.com", "teacher1", "Amit", "Sharma", "9999999997", DEFAULT_DEMO_PASSWORD, RoleName.TEACHER),
				user("accountant@school.com", "accountant", "Kavita", "Rao", "9999999996", DEFAULT_DEMO_PASSWORD, RoleName.ACCOUNTANT),
				user("reception@school.com", "reception", "Nisha", "Kapoor", "9999999995", DEFAULT_DEMO_PASSWORD, RoleName.RECEPTIONIST),
				user("warden@school.com", "warden", "Sanjay", "Nair", "9999999994", DEFAULT_DEMO_PASSWORD, RoleName.WARDEN),
				user("student1@school.com", "student1", "Aarav", "Sharma", "9999999993", DEFAULT_DEMO_PASSWORD, RoleName.STUDENT),
				user("parent1@school.com", "parent1", "Rajesh", "Sharma", "9999999992", DEFAULT_DEMO_PASSWORD, RoleName.PARENT))
				.forEach(this::seedUser);

		List<Student> students = seedDemoStudents();
		FeeStructure feeStructure = seedDemoFees();
		students.forEach(student -> seedFeeAssignment(student, feeStructure));
		seedDemoCollections(students, feeStructure);
		log.info("School ERP master/demo data is ready. Admin login: admin@school.com / {}", DEFAULT_ADMIN_PASSWORD);
	}

	private void seedUser(UserSeed seed) {
		Role role = roleRepository.findByNameAndDeletedFalse(seed.roleName())
				.orElseGet(() -> roleRepository.save(new Role(seed.roleName(), displayName(seed.roleName()), displayName(seed.roleName()))));

		UserAccount user = userAccountRepository.findByEmailIgnoreCaseAndDeletedFalse(seed.email())
				.map(existing -> {
					existing.updateProfile(
							existing.getEmail(),
							existing.getUsername(),
							seed.firstName(),
							seed.lastName(),
							seed.phoneNumber());
					existing.changePassword(passwordEncoder.encode(seed.password()));
					existing.activate();
					return existing;
				})
				.orElseGet(() -> new UserAccount(
						seed.email(),
						seed.username(),
						passwordEncoder.encode(seed.password()),
						seed.firstName(),
						seed.lastName()));

		user.updateProfile(
				user.getEmail(),
				user.getUsername(),
				seed.firstName(),
				seed.lastName(),
				seed.phoneNumber());
		user.addRole(role);
		userAccountRepository.save(user);
	}

	private Map<String, Permission> seedPermissions() {
		Map<String, Permission> permissions = new LinkedHashMap<>();
		for (PermissionSeed seed : permissionSeeds()) {
			Permission permission = permissionRepository.findByCodeAndDeletedFalse(seed.code())
					.orElseGet(() -> permissionRepository.save(new Permission(seed.code(), seed.name(), seed.description())));
			permissions.put(seed.code(), permission);
		}
		return permissions;
	}

	private void seedRolePermissions(Map<String, Permission> permissions) {
		for (RoleSeed seed : roleSeeds()) {
			Role role = roleRepository.findByNameAndDeletedFalse(seed.name())
					.orElseGet(() -> roleRepository.save(new Role(seed.name(), seed.displayName(), seed.description())));
			for (String permissionCode : seed.permissionCodes()) {
				Permission permission = permissions.get(permissionCode);
				if (permission != null) {
					role.addPermission(permission);
				}
			}
			roleRepository.save(role);
		}
	}

	private UserSeed user(
			String email,
			String username,
			String firstName,
			String lastName,
			String phoneNumber,
			String password,
			RoleName roleName) {
		return new UserSeed(email, username, firstName, lastName, phoneNumber, password, roleName);
	}

	private List<PermissionSeed> permissionSeeds() {
		return List.of(
				permission("AUTH_PASSWORD_CHANGE", "Change own password"),
				permission("USERS_READ", "Read users"),
				permission("USERS_CREATE", "Create users"),
				permission("USERS_UPDATE", "Update users"),
				permission("USERS_DELETE", "Delete users"),
				permission("AUDIT_LOGS_READ", "Read audit logs"),
				permission("STUDENTS_READ", "Read students"),
				permission("STUDENTS_CREATE", "Create students"),
				permission("STUDENTS_UPDATE", "Update students"),
				permission("STUDENTS_DELETE", "Delete students"),
				permission("ACADEMIC_READ", "Read academic setup"),
				permission("ACADEMIC_MANAGE", "Manage academic setup"),
				permission("FEES_READ", "Read fees"),
				permission("FEES_MANAGE", "Manage fees"),
				permission("HOSTEL_READ", "Read hostel"),
				permission("HOSTEL_MANAGE", "Manage hostel"),
				permission("ATTENDANCE_READ", "Read attendance"),
				permission("ATTENDANCE_MARK", "Mark attendance"),
				permission("REPORTS_READ", "Read reports"),
				permission("NOTIFICATIONS_SEND", "Send notifications"),
				permission("SETTINGS_READ", "Read settings"),
				permission("SETTINGS_UPDATE", "Update settings"));
	}

	private List<RoleSeed> roleSeeds() {
		List<String> allPermissions = permissionSeeds().stream().map(PermissionSeed::code).toList();
		return List.of(
				role(RoleName.SUPER_ADMIN, "Super Admin", "Full platform owner access.", allPermissions),
				role(RoleName.ADMIN, "Admin", "Institution administrator access.", allPermissions),
				role(RoleName.PRINCIPAL, "Principal", "Academic and operational leadership access.", List.of(
						"AUTH_PASSWORD_CHANGE", "USERS_READ", "STUDENTS_READ", "STUDENTS_UPDATE",
						"ACADEMIC_READ", "ACADEMIC_MANAGE", "ATTENDANCE_READ", "REPORTS_READ",
						"NOTIFICATIONS_SEND", "SETTINGS_READ")),
				role(RoleName.TEACHER, "Teacher", "Teaching staff access.", List.of(
						"AUTH_PASSWORD_CHANGE", "STUDENTS_READ", "ACADEMIC_READ", "ATTENDANCE_READ",
						"ATTENDANCE_MARK", "REPORTS_READ")),
				role(RoleName.ACCOUNTANT, "Accountant", "Fees and finance access.", List.of(
						"AUTH_PASSWORD_CHANGE", "STUDENTS_READ", "FEES_READ", "FEES_MANAGE", "REPORTS_READ")),
				role(RoleName.RECEPTIONIST, "Receptionist", "Front office access.", List.of(
						"AUTH_PASSWORD_CHANGE", "STUDENTS_READ", "STUDENTS_CREATE", "STUDENTS_UPDATE",
						"FEES_READ", "NOTIFICATIONS_SEND")),
				role(RoleName.STUDENT, "Student", "Student portal access.", List.of(
						"AUTH_PASSWORD_CHANGE", "STUDENTS_READ", "ACADEMIC_READ", "ATTENDANCE_READ", "FEES_READ")),
				role(RoleName.PARENT, "Parent", "Parent portal access.", List.of(
						"AUTH_PASSWORD_CHANGE", "STUDENTS_READ", "ACADEMIC_READ", "ATTENDANCE_READ", "FEES_READ")),
				role(RoleName.WARDEN, "Warden", "Hostel operations access.", List.of(
						"AUTH_PASSWORD_CHANGE", "STUDENTS_READ", "HOSTEL_READ", "HOSTEL_MANAGE", "REPORTS_READ")));
	}

	private PermissionSeed permission(String code, String name) {
		return new PermissionSeed(code, name, "Seeded permission for " + name.toLowerCase() + ".");
	}

	private RoleSeed role(RoleName name, String displayName, String description, List<String> permissionCodes) {
		return new RoleSeed(name, displayName, description, permissionCodes);
	}

	private String displayName(RoleName roleName) {
		String[] words = roleName.name().split("_");
		return java.util.Arrays.stream(words)
				.map(word -> word.charAt(0) + word.substring(1).toLowerCase())
				.collect(java.util.stream.Collectors.joining(" "));
	}

	private List<Student> seedDemoStudents() {
		return List.of(
				seedStudent(new StudentSeed(
						"ADM-DEMO-0001",
						"Aarav",
						null,
						"Sharma",
						Gender.MALE,
						LocalDate.now().minusYears(12),
						"B+",
						"aarav.sharma@student.school.test",
						"9890000001",
						"6A-01",
						"Rajesh",
						"Sharma",
						"rajesh.sharma@parent.school.test",
						"9880000001",
						ParentRelation.FATHER)),
				seedStudent(new StudentSeed(
						"ADM-DEMO-0002",
						"Anaya",
						null,
						"Reddy",
						Gender.FEMALE,
						LocalDate.of(2014, 11, 4),
						"O+",
						"anaya.reddy@student.school.test",
						"9890000002",
						"6A-02",
						"Priya",
						"Reddy",
						"priya.reddy@parent.school.test",
						"9880000002",
						ParentRelation.MOTHER)),
				seedStudent(new StudentSeed(
						"ADM-DEMO-0003",
						"Kabir",
						"Dev",
						"Singh",
						Gender.MALE,
						LocalDate.of(2014, 6, 21),
						"A+",
						"kabir.singh@student.school.test",
						"9890000003",
						"6A-03",
						"Manish",
						"Singh",
						"manish.singh@parent.school.test",
						"9880000003",
						ParentRelation.FATHER)));
	}

	private Student seedStudent(StudentSeed seed) {
		Student student = studentRepository.findByAdmissionNumberIgnoreCaseAndDeletedFalse(seed.admissionNumber())
				.orElseGet(() -> {
					Student created = new Student(
							seed.admissionNumber(),
							seed.firstName(),
							seed.dateOfBirth(),
							seed.gender(),
							LocalDate.of(2026, 4, 1));
					created.updateProfile(
							seed.firstName(),
							seed.middleName(),
							seed.lastName(),
							seed.dateOfBirth(),
							seed.gender(),
							seed.bloodGroup(),
							seed.email(),
							seed.phoneNumber(),
							LocalDate.of(2026, 4, 1),
							"Green Valley Primary School",
							"12 MG Road",
							"Near City Library",
							"Bengaluru",
							"Karnataka",
							"560001",
							"India");
					created.assignClassSection(DEMO_ACADEMIC_YEAR, DEMO_CLASS_NAME, DEMO_SECTION_NAME, seed.rollNumber(), LocalDate.of(2026, 4, 1));
					return studentRepository.save(created);
				});

		ParentGuardian parent = seedParent(seed);
		if (!student.hasParentMapping(parent, seed.parentRelation())) {
			student.addParent(parent, seed.parentRelation(), true, true, true);
			student = studentRepository.save(student);
		}
		return student;
	}

	private ParentGuardian seedParent(StudentSeed seed) {
		ParentGuardian parent = parentGuardianRepository.findByEmailIgnoreCaseAndDeletedFalse(seed.parentEmail())
				.orElseGet(() -> new ParentGuardian(seed.parentFirstName(), seed.parentLastName(), seed.parentEmail(), seed.parentPhoneNumber()));
		parent.updateProfile(
				seed.parentFirstName(),
				seed.parentLastName(),
				seed.parentEmail(),
				seed.parentPhoneNumber(),
				null,
				"Demo parent",
				"12 MG Road",
				null,
				"Bengaluru",
				"Karnataka",
				"560001",
				"India",
				null);
		return parentGuardianRepository.save(parent);
	}

	private FeeStructure seedDemoFees() {
		FeeCategory tuition = seedFeeCategory("TUITION", "Tuition Fee", "Core academic tuition.", 1);
		FeeCategory library = seedFeeCategory("LIBRARY", "Library Fee", "Annual library services.", 2);
		FeeCategory transport = seedFeeCategory("TRANSPORT", "Transport Fee", "Bus transport charges.", 3);

		FeeStructure existing = feeStructureRepository
				.findByAcademicYearIgnoreCaseAndClassNameIgnoreCaseAndSectionNameIgnoreCaseAndDeletedFalse(
						DEMO_ACADEMIC_YEAR,
						DEMO_CLASS_NAME,
						DEMO_SECTION_NAME)
				.orElse(null);
		if (existing != null) {
			return existing;
		}

		feeService.createFeeStructure(new FeeStructureRequest(
				DEMO_ACADEMIC_YEAR,
				DEMO_CLASS_NAME,
				DEMO_SECTION_NAME,
				"Class 6 Annual Fee",
				"Demo annual fee structure for customer walkthroughs.",
				true,
				List.of(
						new FeeStructureItemRequest(tuition.getId(), money("18000.00"), true, 1),
						new FeeStructureItemRequest(library.getId(), money("3000.00"), true, 2),
						new FeeStructureItemRequest(transport.getId(), money("5000.00"), false, 3)),
				List.of(
						new FeeStructureInstallmentRequest(1, "First Term", LocalDate.of(2026, 6, 15), money("13000.00")),
						new FeeStructureInstallmentRequest(2, "Second Term", LocalDate.of(2026, 10, 15), money("13000.00")))));

		return feeStructureRepository
				.findByAcademicYearIgnoreCaseAndClassNameIgnoreCaseAndSectionNameIgnoreCaseAndDeletedFalse(
						DEMO_ACADEMIC_YEAR,
						DEMO_CLASS_NAME,
						DEMO_SECTION_NAME)
				.orElseThrow();
	}

	private FeeCategory seedFeeCategory(String code, String name, String description, int sortOrder) {
		return feeCategoryRepository.findByCodeIgnoreCaseAndDeletedFalse(code)
				.orElseGet(() -> feeCategoryRepository.save(new FeeCategory(code, name, description, sortOrder)));
	}

	private void seedFeeAssignment(Student student, FeeStructure feeStructure) {
		if (assignmentRepository.existsByStudentIdAndFeeStructureIdAndDeletedFalse(student.getId(), feeStructure.getId())) {
			return;
		}
		feeService.assignFeeToStudent(new StudentFeeAssignmentRequest(
				student.getId(),
				feeStructure.getId(),
				LocalDate.of(2026, 4, 1),
				"Seeded for demo dashboard and fees workflows."));
	}

	private void seedDemoCollections(List<Student> students, FeeStructure feeStructure) {
		if (students.isEmpty()) {
			return;
		}
		seedDemoCollection(students.getFirst(), feeStructure, money("5000.00"), "DEMO-UPI-0001", "Rajesh Sharma");
		if (students.size() > 1) {
			seedDemoCollection(students.get(1), feeStructure, money("13000.00"), "DEMO-BANK-0002", "Priya Reddy");
		}
	}

	private void seedDemoCollection(
			Student student,
			FeeStructure feeStructure,
			BigDecimal amount,
			String referenceNumber,
			String payerName) {
		assignmentRepository.findAll().stream()
				.filter(assignment -> !assignment.isDeleted())
				.filter(assignment -> assignment.getStudent().getId().equals(student.getId()))
				.filter(assignment -> assignment.getFeeStructure().getId().equals(feeStructure.getId()))
				.findFirst()
				.flatMap(assignment -> assignmentRepository.findDetailedByIdAndDeletedFalse(assignment.getId()))
				.filter(assignment -> assignment.getPaidAmount().signum() == 0)
				.filter(assignment -> assignment.getBalanceAmount().compareTo(amount) >= 0)
				.map(StudentFeeAssignment::getId)
				.ifPresent(assignmentId -> feeService.collectPayment(
						assignmentId,
						new PaymentCollectionRequest(
								amount,
								LocalDate.of(2026, 6, 20),
								PaymentMode.UPI,
								referenceNumber,
								payerName,
								"accountant@school.com",
								"Opening demo collection.",
								false)));
	}

	private BigDecimal money(String value) {
		return new BigDecimal(value);
	}

	private record UserSeed(
			String email,
			String username,
			String firstName,
			String lastName,
			String phoneNumber,
			String password,
			RoleName roleName) {
	}

	private record PermissionSeed(String code, String name, String description) {
	}

	private record RoleSeed(RoleName name, String displayName, String description, List<String> permissionCodes) {
	}

	private record StudentSeed(
			String admissionNumber,
			String firstName,
			String middleName,
			String lastName,
			Gender gender,
			LocalDate dateOfBirth,
			String bloodGroup,
			String email,
			String phoneNumber,
			String rollNumber,
			String parentFirstName,
			String parentLastName,
			String parentEmail,
			String parentPhoneNumber,
			ParentRelation parentRelation) {
	}
}
