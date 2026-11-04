/**
 * Input rules shared by several forms, mirroring the backend's `CommonValidationUtils` and the DTO annotations.
 * The backend validates everything again; these only let a form show the problem before sending.
 * When a rule changes in the backend, it changes here in the same task.
 */

/** A code people type and read (SKU, later warehouse codes): letters, digits, ".", "_" and "-", no spaces. */
export const CODE_PATTERN = /^[A-Za-z0-9._-]*$/
export const CODE_MAX_LENGTH = 50

export const NAME_MAX_LENGTH = 100
export const EMAIL_MAX_LENGTH = 150
export const PHONE_MAX_LENGTH = 30

/** Money columns are DECIMAL(12,2): at most 10 digits before the point and 2 after. */
export const MONEY_MAX = 9_999_999_999.99
export const MONEY_DECIMALS = 2

/** Phone numbers: digits, spaces, "+", "-", "(" and ")": "+84 90 111 2233", "(028) 3800-1122". */
export const PHONE_PATTERN = /^[0-9+()\- ]*$/

/** `ProductRequest.name` is longer than the common name (VARCHAR(200)). */
export const PRODUCT_NAME_MAX_LENGTH = 200

/** `SupplierRequest.name`: VARCHAR(150). */
export const SUPPLIER_NAME_MAX_LENGTH = 150

/** Backend `PasswordPolicy`. BCrypt reads at most 72 bytes. */
export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_LENGTH = 72
