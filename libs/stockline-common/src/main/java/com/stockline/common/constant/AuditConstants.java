package com.stockline.common.constant;

/**
 * Values for the audit columns created_by / updated_by.
 */
public final class AuditConstants {

	/**
	 * The "who" for changes without a logged-in user: everything until login exists (Phase 3), and later
	 * scheduled jobs and Kafka consumers.
	 */
	public static final String SYSTEM_USER = "system";

	private AuditConstants() {
	}
}
