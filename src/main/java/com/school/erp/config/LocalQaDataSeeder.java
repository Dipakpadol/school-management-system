package com.school.erp.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
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
import com.school.erp.modules.fees.infrastructure.FeePaymentRepository;
import com.school.erp.modules.fees.infrastructure.FeeStructureRepository;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.academic.infrastructure.AcademicYearRepository;
import com.school.erp.modules.academic.infrastructure.ClassEntityRepository;
import com.school.erp.modules.academic.infrastructure.SectionEntityRepository;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentClassAssignment;
import com.school.erp.modules.students.infrastructure.StudentRepository;
import com.school.erp.modules.users.domain.Permission;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.infrastructure.PermissionRepository;
import com.school.erp.modules.users.infrastructure.RoleRepository;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@Profile("localqa")
@RequiredArgsConstructor
public class LocalQaDataSeeder {

	public static final String QA_ADMIN_EMAIL = "admin@school.test";
	public static final String QA_ADMIN_PASSWORD = "Admin@12345678";
	public static final String QA_DEMO_PASSWORD = "Demo@12345678";
	public static final String QA_ACADEMIC_YEAR = "2026-2027";
	public static final String QA_CLASS_NAME = "Class 6";
	public static final String QA_SECTION_NAME = "A";
	public static final String QA_ADMISSION_NUMBER = "ADM-2026-0001";

	private final PermissionRepository permissionRepository;
	private final RoleRepository roleRepository;
	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final StudentRepository studentRepository;
	private final AcademicYearRepository academicYearRepository;
	private final ClassEntityRepository classEntityRepository;
	private final SectionEntityRepository sectionEntityRepository;
	private final FeeCategoryRepository feeCategoryRepository;
	private final FeePaymentRepository feePaymentRepository;
	private final FeeStructureRepository feeStructureRepository;
	private final StudentFeeAssignmentRepository assignmentRepository;
	private final FeeService feeService;

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void seed() {
		Map<String, Permission> permissions = seedPermissions();
		Map<RoleName, Role> roles = seedRoles(permissions);
		seedAdminUser(roles.get(RoleName.SUPER_ADMIN));
		seedDemoUsers(roles);
		seedAcademicHierarchy();
		List<Student> students = seedStudents();
		FeeStructure structure = seedFees();
		students.forEach(student -> seedFeeAssignment(student, structure));
		seedDemoCollections(students, structure);
	}

	private Map<String, Permission> seedPermissions() {
		List<PermissionSeed> seeds = List.of(
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

		java.util.LinkedHashMap<String, Permission> permissions = new java.util.LinkedHashMap<>();
		for (PermissionSeed seed : seeds) {
			Permission permission = permissionRepository.findByCodeAndDeletedFalse(seed.code())
					.orElseGet(() -> permissionRepository.save(new Permission(seed.code(), seed.name(), seed.description())));
			permissions.put(seed.code(), permission);
		}
		return permissions;
	}

	private Map<RoleName, Role> seedRoles(Map<String, Permission> permissions) {
		Map<RoleName, Role> roles = new EnumMap<>(RoleName.class);
		for (RoleSeed seed : roleSeeds()) {
			Role role = roleRepository.findByNameAndDeletedFalse(seed.name())
					.orElseGet(() -> roleRepository.save(new Role(seed.name(), seed.displayName(), seed.description())));
			for (String permissionCode : seed.permissionCodes()) {
				role.addPermission(permissions.get(permissionCode));
			}
			roles.put(seed.name(), roleRepository.save(role));
		}
		return roles;
	}

	private void seedAdminUser(Role superAdminRole) {
		if (userAccountRepository.existsByEmailIgnoreCaseAndDeletedFalse(QA_ADMIN_EMAIL)) {
			return;
		}
		UserAccount user = new UserAccount(
				QA_ADMIN_EMAIL,
				QA_ADMIN_EMAIL,
				passwordEncoder.encode(QA_ADMIN_PASSWORD),
				"Local QA",
				"Admin");
		user.addRole(superAdminRole);
		userAccountRepository.save(user);
	}

	private void seedDemoUsers(Map<RoleName, Role> roles) {
		seedUser("principal@school.test", "principal", "Meera", "Iyer", "+919810000001", roles.get(RoleName.PRINCIPAL));
		seedUser("accountant@school.test", "accountant", "Kavita", "Rao", "+919810000002", roles.get(RoleName.ACCOUNTANT));
		seedUser("reception@school.test", "reception", "Nisha", "Kapoor", "+919810000003", roles.get(RoleName.RECEPTIONIST));
		seedUser("teacher@school.test", "teacher", "Arjun", "Menon", "+919810000004", roles.get(RoleName.TEACHER));
		seedUser("warden@school.test", "warden", "Sanjay", "Nair", "+919810000005", roles.get(RoleName.WARDEN));
	}

	private void seedUser(String email, String username, String firstName, String lastName, String phoneNumber, Role role) {
		if (userAccountRepository.existsByEmailIgnoreCaseAndDeletedFalse(email)) {
			return;
		}
		UserAccount user = new UserAccount(
				email,
				username,
				passwordEncoder.encode(QA_DEMO_PASSWORD),
				firstName,
				lastName);
		user.updateProfile(email, username, firstName, lastName, phoneNumber);
		user.addRole(role);
		userAccountRepository.save(user);
	}

	private List<Student> seedStudents() {
		return List.of(
				seedStudent(new StudentSeed(
						QA_ADMISSION_NUMBER,
						"Aarav",
						null,
						"Sharma",
						Gender.MALE,
						LocalDate.of(2014, 8, 17),
						"B+",
						"aarav.sharma@student.school.test",
						"+919876543210",
						"6A-01")),
				seedStudent(new StudentSeed(
						"ADM-2026-0002",
						"Anaya",
						null,
						"Reddy",
						Gender.FEMALE,
						LocalDate.of(2014, 11, 4),
						"O+",
						"anaya.reddy@student.school.test",
						"+919876543211",
						"6A-02")),
				seedStudent(new StudentSeed(
						"ADM-2026-0003",
						"Kabir",
						"Dev",
						"Singh",
						Gender.MALE,
						LocalDate.of(2014, 6, 21),
						"A+",
						"kabir.singh@student.school.test",
						"+919876543212",
						"6A-03")),
				seedStudent(new StudentSeed(
						"ADM-2026-0004",
						"Zoya",
						null,
						"Khan",
						Gender.FEMALE,
						LocalDate.of(2014, 9, 12),
						"AB+",
						"zoya.khan@student.school.test",
						"+919876543213",
						"6A-04")),
				seedStudent(new StudentSeed(
						"ADM-2026-0005",
						"Vihaan",
						null,
						"Patel",
						Gender.MALE,
						LocalDate.of(2014, 5, 29),
						"B-",
						"vihaan.patel@student.school.test",
						"+919876543214",
						"6A-05")));
	}

	private Student seedStudent(StudentSeed seed) {
		AcademicYear academicYear = seedAcademicYear();
		ClassEntity classEntity = seedClass(academicYear, 6);
		SectionEntity section = seedSection(classEntity, "A", "Division A", 1);
		Student student = studentRepository.findByAdmissionNumberIgnoreCaseAndDeletedFalse(seed.admissionNumber())
				.orElseGet(() -> {
					Student newStudent = new Student(
							seed.admissionNumber(),
							seed.firstName(),
							seed.dateOfBirth(),
							seed.gender(),
							LocalDate.of(2026, 4, 1));
					newStudent.updateProfile(
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
					return studentRepository.save(newStudent);
				});
		linkDemoAssignment(student, academicYear, classEntity, section, seed.rollNumber());
		return student;
	}

	private void seedAcademicHierarchy() {
		AcademicYear academicYear = seedAcademicYear();
		for (int grade = 1; grade <= 10; grade++) {
			ClassEntity classEntity = seedClass(academicYear, grade);
			seedSection(classEntity, "A", "Division A", 1);
			seedSection(classEntity, "B", "Division B", 2);
			seedSection(classEntity, "C", "Division C", 3);
		}
	}

	private AcademicYear seedAcademicYear() {
		return academicYearRepository.findByNameIgnoreCaseAndDeletedFalse(QA_ACADEMIC_YEAR)
				.or(() -> academicYearRepository.findByCodeIgnoreCaseAndDeletedFalse("AY-2026-27"))
				.orElseGet(() -> academicYearRepository.save(new AcademicYear(
						"AY-2026-27",
						QA_ACADEMIC_YEAR,
						LocalDate.of(2026, 4, 1),
						LocalDate.of(2027, 3, 31))));
	}

	private ClassEntity seedClass(AcademicYear academicYear, int grade) {
		String name = "Class " + grade;
		return classEntityRepository.findByAcademicYearIdAndNameIgnoreCaseAndDeletedFalse(academicYear.getId(), name)
				.orElseGet(() -> classEntityRepository.save(new ClassEntity(
						academicYear,
						"CLASS-" + grade,
						name,
						grade)));
	}

	private SectionEntity seedSection(ClassEntity classEntity, String code, String name, int displayOrder) {
		return sectionEntityRepository.findByClassEntityIdAndCodeIgnoreCaseAndDeletedFalse(classEntity.getId(), code)
				.orElseGet(() -> sectionEntityRepository.save(new SectionEntity(
						classEntity,
						code,
						name,
						40,
						displayOrder)));
	}

	private void linkDemoAssignment(
			Student student,
			AcademicYear academicYear,
			ClassEntity classEntity,
			SectionEntity section,
			String rollNumber) {
		StudentClassAssignment assignment = student.getClassAssignments().stream()
				.filter(existing -> !existing.isDeleted())
				.filter(StudentClassAssignment::isActive)
				.filter(existing -> existing.isForAcademicYear(academicYear))
				.findFirst()
				.orElse(null);
		if (assignment == null) {
			student.assignClassSection(academicYear, classEntity, section, rollNumber, LocalDate.of(2026, 4, 1));
		}
		else {
			assignment.update(academicYear, classEntity, section, rollNumber, LocalDate.of(2026, 4, 1), null, true);
		}
		studentRepository.save(student);
	}

	private FeeStructure seedFees() {
		FeeCategory tuition = seedFeeCategory("TUITION", "Tuition Fee", "Core academic tuition.", 1);
		FeeCategory library = seedFeeCategory("LIBRARY", "Library Fee", "Annual library services.", 2);
		FeeCategory transport = seedFeeCategory("TRANSPORT", "Transport Fee", "Bus transport charges.", 3);
		seedFeeCategory("LAB", "Lab Fee", "Laboratory and practical resources.", 4);
		seedFeeCategory("EXAM", "Exam Fee", "Assessment and exam administration.", 5);

		FeeStructure existing = feeStructureRepository
				.findByAcademicYearIgnoreCaseAndClassNameIgnoreCaseAndSectionNameIgnoreCaseAndDeletedFalse(
						QA_ACADEMIC_YEAR,
						QA_CLASS_NAME,
						QA_SECTION_NAME)
				.orElse(null);
		if (existing != null) {
			return existing;
		}

		feeService.createFeeStructure(new FeeStructureRequest(
				QA_ACADEMIC_YEAR,
				QA_CLASS_NAME,
				QA_SECTION_NAME,
				"Class 6 Annual Fee",
				"Local QA annual structure for Class 6 A.",
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
						QA_ACADEMIC_YEAR,
						QA_CLASS_NAME,
						QA_SECTION_NAME)
				.orElseThrow();
	}

	private FeeCategory seedFeeCategory(String code, String name, String description, int sortOrder) {
		return feeCategoryRepository.findByCodeIgnoreCaseAndDeletedFalse(code)
				.orElseGet(() -> feeCategoryRepository.save(new FeeCategory(code, name, description, sortOrder)));
	}

	private void seedFeeAssignment(Student student, FeeStructure structure) {
		if (assignmentRepository.existsByStudentIdAndFeeStructureIdAndDeletedFalse(student.getId(), structure.getId())) {
			return;
		}
		feeService.assignFeeToStudent(new StudentFeeAssignmentRequest(
				student.getId(),
				structure.getId(),
				LocalDate.of(2026, 4, 1),
				"Local QA seeded assignment."));
	}

	private void seedDemoCollections(List<Student> students, FeeStructure structure) {
		if (students.isEmpty()) {
			return;
		}
		seedDemoCollection(students.getFirst(), structure, money("5000.00"), "QA-UPI-0001", "Rajesh Sharma");
		if (students.size() > 1) {
			seedDemoCollection(students.get(1), structure, money("13000.00"), "QA-BANK-0002", "Priya Reddy");
		}
	}

	private void seedDemoCollection(
			Student student,
			FeeStructure structure,
			BigDecimal amount,
			String referenceNumber,
			String payerName) {
		if (feePaymentRepository.existsByPaymentModeAndReferenceNumberAndDeletedFalse(PaymentMode.UPI, referenceNumber)) {
			return;
		}
		assignmentRepository.findAll().stream()
				.filter(assignment -> !assignment.isDeleted())
				.filter(assignment -> assignment.getStudent().getId().equals(student.getId()))
				.filter(assignment -> assignment.getFeeStructure().getId().equals(structure.getId()))
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
								"accountant@school.test",
								"Local QA opening collection.",
								false)));
	}

	private BigDecimal money(String value) {
		return new BigDecimal(value);
	}

	private PermissionSeed permission(String code, String name) {
		return new PermissionSeed(code, name, "Local QA seed for " + name.toLowerCase() + ".");
	}

	private List<RoleSeed> roleSeeds() {
		List<String> allPermissions = List.of(
				"AUTH_PASSWORD_CHANGE",
				"USERS_READ", "USERS_CREATE", "USERS_UPDATE", "USERS_DELETE",
				"AUDIT_LOGS_READ",
				"STUDENTS_READ", "STUDENTS_CREATE", "STUDENTS_UPDATE", "STUDENTS_DELETE",
				"ACADEMIC_READ", "ACADEMIC_MANAGE",
				"FEES_READ", "FEES_MANAGE",
				"HOSTEL_READ", "HOSTEL_MANAGE",
				"ATTENDANCE_READ", "ATTENDANCE_MARK",
				"REPORTS_READ",
				"NOTIFICATIONS_SEND",
				"SETTINGS_READ", "SETTINGS_UPDATE");
		return List.of(
				role(RoleName.SUPER_ADMIN, "Super Admin", "Full local QA platform owner access.", allPermissions),
				role(RoleName.ADMIN, "Admin", "Local QA institution administrator access.", allPermissions),
				role(RoleName.ACCOUNTANT, "Accountant", "Local QA fees and finance access.", List.of(
						"AUTH_PASSWORD_CHANGE", "STUDENTS_READ", "FEES_READ", "FEES_MANAGE", "REPORTS_READ")),
				role(RoleName.PRINCIPAL, "Principal", "Local QA academic leadership access.", List.of(
						"AUTH_PASSWORD_CHANGE", "USERS_READ", "STUDENTS_READ", "ACADEMIC_READ", "ACADEMIC_MANAGE", "REPORTS_READ")),
				role(RoleName.TEACHER, "Teacher", "Local QA teaching staff access.", List.of(
						"AUTH_PASSWORD_CHANGE", "STUDENTS_READ", "ACADEMIC_READ", "ATTENDANCE_READ", "ATTENDANCE_MARK")),
				role(RoleName.RECEPTIONIST, "Receptionist", "Local QA front office access.", List.of(
						"AUTH_PASSWORD_CHANGE", "STUDENTS_READ", "STUDENTS_CREATE", "STUDENTS_UPDATE", "FEES_READ")),
				role(RoleName.STUDENT, "Student", "Local QA student portal access.", List.of(
						"AUTH_PASSWORD_CHANGE", "STUDENTS_READ", "ACADEMIC_READ", "ATTENDANCE_READ", "FEES_READ")),
				role(RoleName.PARENT, "Parent", "Local QA parent portal access.", List.of(
						"AUTH_PASSWORD_CHANGE", "STUDENTS_READ", "ACADEMIC_READ", "ATTENDANCE_READ", "FEES_READ")),
				role(RoleName.WARDEN, "Warden", "Local QA hostel operations access.", List.of(
						"AUTH_PASSWORD_CHANGE", "STUDENTS_READ", "HOSTEL_READ", "HOSTEL_MANAGE", "REPORTS_READ")));
	}

	private RoleSeed role(RoleName name, String displayName, String description, List<String> permissionCodes) {
		return new RoleSeed(name, displayName, description, permissionCodes);
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
			String rollNumber) {
	}
}
