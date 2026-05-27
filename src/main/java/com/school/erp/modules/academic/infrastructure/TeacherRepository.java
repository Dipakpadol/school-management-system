package com.school.erp.modules.academic.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.academic.domain.Teacher;

public interface TeacherRepository extends BaseRepository<Teacher, UUID> {

	List<Teacher> findAllByDeletedFalseAndActiveTrueOrderByFirstNameAscLastNameAsc();

	Optional<Teacher> findByEmployeeNumberIgnoreCaseAndDeletedFalse(String employeeNumber);
}
