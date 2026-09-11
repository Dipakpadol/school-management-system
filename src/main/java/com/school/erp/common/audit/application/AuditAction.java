package com.school.erp.common.audit.application;

public final class AuditAction {

	public static final String CREATE = "CREATE";
	public static final String UPDATE = "UPDATE";
	public static final String DELETE = "DELETE";
	public static final String STATUS_CHANGE = "STATUS_CHANGE";
	public static final String LOGIN = "LOGIN";
	public static final String LOGIN_FAILED = "LOGIN_FAILED";
	public static final String LOGOUT = "LOGOUT";
	public static final String PAYMENT = "PAYMENT";
	public static final String PAYMENT_REVERSAL = "PAYMENT_REVERSAL";
	public static final String REFUND = "REFUND";
	public static final String IMPORT = "IMPORT";
	public static final String EXPORT = "EXPORT";

	private AuditAction() {
	}
}
