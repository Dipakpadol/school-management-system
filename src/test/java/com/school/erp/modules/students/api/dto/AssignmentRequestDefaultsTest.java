package com.school.erp.modules.students.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.school.erp.modules.hostel.api.dto.HostelAssignmentRequest;
import com.school.erp.modules.transport.api.dto.TransportAssignmentRequest;

import org.junit.jupiter.api.Test;

class AssignmentRequestDefaultsTest {

	@Test
	void hostelFeeAppliesUnlessExplicitlyDisabled() {
		assertThat(new HostelAssignmentRequest(true, null, null, null, null, null, null, null, null, null)
				.appliesHostelFee()).isTrue();
		assertThat(new HostelAssignmentRequest(true, null, null, null, null, null, null, null, null, true)
				.appliesHostelFee()).isTrue();
		assertThat(new HostelAssignmentRequest(true, null, null, null, null, null, null, null, null, false)
				.appliesHostelFee()).isFalse();
	}

	@Test
	void transportFeeAppliesUnlessExplicitlyDisabled() {
		assertThat(new TransportAssignmentRequest(true, null, null, null, null, null, null, null, null, null)
				.appliesTransportFee()).isTrue();
		assertThat(new TransportAssignmentRequest(true, null, null, null, null, null, null, null, null, true)
				.appliesTransportFee()).isTrue();
		assertThat(new TransportAssignmentRequest(true, null, null, null, null, null, null, null, null, false)
				.appliesTransportFee()).isFalse();
	}
}
