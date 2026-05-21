type ApiProblemDetail = {
  detail?: string;
  title?: string;
  errorCode?: string;
};

const API_ERROR_MESSAGES: Record<string, string> = {
  APPROVERS_LOCKED: "Approvers are locked for this change request.",
  INVALID_ORDER: "Approver order is out of date. Refresh and try again.",
  DUPLICATE_APPROVER: "That user is already an approver on this change request.",
  NOT_APPROVER: "You are not an approver on this change request.",
  OUT_OF_SEQUENCE: "Only next pending approver can act in linear workflow.",
  REQUESTER_SELF_APPROVAL_FORBIDDEN:
    "Request creators cannot approve or reject their own change request.",
  FILE_MISSING: "Attachment file is no longer available.",
  FILE_TOO_LARGE: "Attachment exceeds maximum allowed size.",
  INVALID_FILE_TYPE: "Attachment type is not allowed.",
  INVALID_FILE_CONTENT: "Attachment content does not match file type.",
  INVALID_FILE_PATH: "Attachment path is invalid.",
  EMPTY_FILE: "Please select a file before uploading.",
  UPLOAD_FAILED: "Could not upload attachment. Please try again.",
  DOWNLOAD_FAILED: "Could not download attachment. Please try again.",
  INVALID_CREDENTIALS: "Invalid email or password.",
  ACCOUNT_SUSPENDED: "Your account is suspended. Contact your administrator.",
  DOMAIN_NOT_PERMITTED: "Sign-in blocked for this email domain.",
  SSO_NOT_CONFIGURED: "Single sign-on is not configured for this tenant.",
  TENANT_CONTEXT_REQUIRED: "Tenant context is required for this action.",
  ALREADY_BOOTSTRAPPED: "Platform bootstrap is already complete.",
  FORBIDDEN: "You are not allowed to perform this action.",
  NOT_FOUND: "Requested resource was not found.",
  INVALID_INPUT: "Please review input values and try again.",
  INVALID_STATE: "Action is not valid in current state.",
  INVALID_ROLE_ASSIGNMENT: "Role assignment is invalid.",
  NAME_TAKEN: "Name is already in use.",
  EMAIL_TAKEN: "Email address is already in use.",
  SELF_DEACTIVATION: "You cannot deactivate your own account.",
  LAST_ADMIN: "At least one active admin must remain.",
  INVALID_ROLE: "Role data is invalid.",
  DUPLICATE_ROLE: "A role with this name already exists.",
  SYSTEM_ROLE_IMMUTABLE: "System roles cannot be changed.",
  INVALID_PERMISSION: "One or more permissions are invalid.",
  OVERLAPPING_PERMISSIONS: "Permission set overlaps with an existing role.",
  ALREADY_MEMBER: "User is already a member of this group.",
  MISSING_OPTIONS: "Dropdown fields require at least one option.",
  INVALID_FIELD_TYPE: "Custom field type is invalid.",
  EXPIRED: "This link has expired.",
  NOT_READY: "Requested export is not ready yet.",
};

export function resolveApiErrorMessage(error: unknown, fallback: string): string {
  const problem = (error as { data?: ApiProblemDetail } | null)?.data;
  if (!problem) {
    return fallback;
  }

  if (problem.errorCode && API_ERROR_MESSAGES[problem.errorCode]) {
    return API_ERROR_MESSAGES[problem.errorCode];
  }

  if (problem.detail && problem.detail.trim().length > 0) {
    return problem.detail;
  }

  if (problem.title && problem.title.trim().length > 0) {
    return problem.title;
  }

  return fallback;
}
