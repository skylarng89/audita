// ── Auth & Users ──────────────────────────────────────────────────────────────

export type UserRole =
  | "Admin"
  | "Requester"
  | "Approver"
  | "Auditor"
  | "SUPER_ADMIN";
export type UserStatus = "PENDING" | "ACTIVE" | "SUSPENDED";

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: { id: string; name: string };
  status: UserStatus;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  email: string;
  fullName: string;
  role: UserRole;
  tenantSlug: string | null;
}

// ── Change Requests ───────────────────────────────────────────────────────────

export type CRStatus =
  | "DRAFT"
  | "PENDING_APPROVAL"
  | "APPROVED"
  | "REJECTED"
  | "CANCELLED";
export type Priority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type ApprovalType = "LINEAR" | "NON_LINEAR";
export type ApproverStatus = "PENDING" | "APPROVED" | "REJECTED";
export type WorkflowMode = "APPROVAL_ONLY" | "DELIVERY_PIPELINE";
export type CompletionStatus = "IN_PROGRESS" | "COMPLETED";
export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED" | null;

export interface ChangeRequest {
  id: string;
  displayId: string | null;
  title: string;
  description: string | null;
  priority: Priority;
  riskLevel: RiskLevel;
  category: string | null;
  status: CRStatus;
  approvalType: ApprovalType;
  approvalStatus: string | null;
  completionStatus: CompletionStatus | null;
  approvalLocked: boolean;
  workflowMode: WorkflowMode | null;
  requestDepartmentId: string | null;
  destinationDepartmentId: string | null;
  requestGroupId: string | null;
  destinationGroupId: string | null;
  scheduledStart: string | null;
  scheduledEnd: string | null;
  affectedSystems: string[];
  slaDeadline: string | null;
  slaBreached: boolean;
  createdBy: string | null;
  createdByEmail: string | null;
  createdByFullName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Department {
  id: string;
  name: string;
  code: string | null;
  isActive: boolean;
  displayOrder: number;
}

export interface CrApprover {
  id: string;
  userId: string;
  userEmail: string;
  userFullName: string;
  userRole: string | null;
  isRequired: boolean;
  position: number;
  status: ApproverStatus;
  rejectionReason: string | null;
  decidedAt: string | null;
  isAdHoc: boolean;
}

export interface ApproverCandidate {
  id: string;
  kind: "USER" | "GROUP";
  label: string;
  secondary: string | null;
  role: string | null;
}

export interface ChangeRequestCustomFieldValue {
  fieldId: string;
  value: string | null;
}

export interface Attachment {
  id: string;
  fileName: string;
  mimeType: string | null;
  sizeBytes: number;
  storagePath: string | null;
  uploaderId: string | null;
  uploaderName: string | null;
  createdAt: string;
}

// ── UAT ────────────────────────────────────────────────────────────────────────

export type UatStatus = "DRAFT" | "APPROVED" | "REJECTED" | "PROMOTED";

export interface UatApprover {
  id: string;
  userId: string;
  userFullName: string;
  userEmail: string;
  isRequired: boolean;
  status: ApproverStatus;
  rejectionReason: string | null;
  decidedAt: string | null;
}

export interface Uat {
  id: string;
  title: string;
  details: string;
  status: UatStatus;
  readOnly: boolean;
  requesterSignedOff: boolean;
  approvers: UatApprover[];
  createdBy: string | null;
  createdByFullName: string | null;
  createdAt: string;
  updatedAt: string;
  promotedAt: string | null;
}

// ── Deployment ────────────────────────────────────────────────────────────────

export type DeploymentStatus = "PENDING_APPROVAL" | "APPROVED" | "REJECTED";

export interface DeploymentApprover {
  id: string;
  userId: string;
  userFullName: string;
  userEmail: string;
  status: ApproverStatus;
  decidedAt: string | null;
  rejectionReason: string | null;
}

export interface Deployment {
  id: string;
  requestId: string;
  status: DeploymentStatus;
  promotedAt: string;
  approvers: DeploymentApprover[];
}

// ── Comments & Activity ───────────────────────────────────────────────────────

export interface Comment {
  id: string;
  author: {
    id: string;
    email: string;
    fullName: string;
    roleId: string | null;
    roleName: string | null;
    status: UserStatus;
    createdAt: string;
  } | null;
  body: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ActivityEntry {
  id: string;
  actorId: string | null;
  actorEmail: string | null;
  actorFullName: string | null;
  actionType: string;
  payload: Record<string, unknown> | null;
  createdAt: string;
}

// ── Notifications ─────────────────────────────────────────────────────────────

export interface Notification {
  id: string;
  type: string;
  title: string | null;
  body: string | null;
  link: string | null;
  isRead: boolean;
  createdAt: string;
}

// ── Pagination ────────────────────────────────────────────────────────────────

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// ── Audit Log ─────────────────────────────────────────────────────────────────

export interface AuditLogEntry {
  id: string;
  actorId: string | null;
  actorFullName: string | null;
  actorEmail: string | null;
  actionType: string;
  entityType: string | null;
  entityId: string | null;
  payload: Record<string, unknown> | null;
  ipAddress: string | null;
  createdAt: string;
}

// ── Settings ──────────────────────────────────────────────────────────────────

export interface OrgSettings {
  approvalType: ApprovalType;
  maxUploadSizeMb: number;
  storageBackend: "LOCAL" | "S3";
  smtpHost: string;
  smtpPort: number;
  timezone: string;
}

export interface CustomFieldDefinition {
  id: string;
  label: string;
  fieldType: "TEXT" | "NUMBER" | "DATE" | "DROPDOWN" | "CHECKBOX";
  options: string[] | null;
  isRequired: boolean;
  displayOrder: number;
}

export interface SlaPolicy {
  id: string;
  name: string;
  priorityTrigger: Priority | "ALL" | null;
  deadlineHours: number;
  warningBeforeHours: number | null;
}

// ── Groups ─────────────────────────────────────────────────────────────────────

export interface Group {
  id: string
  name: string
  description?: string
  isActive: boolean
  displayOrder: number
  memberCount: number
  createdAt: string
}

export interface CreateGroupRequest {
  name: string
  description?: string
  memberIds?: string[]
}

export interface BatchMembersRequest {
  userIds: string[]
}

export interface UserSearchResult {
  id: string
  email: string
  fullName: string
  role: string
  status: string
}

// ── Tenant (Super Admin) ──────────────────────────────────────────────────────

export interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: "ACTIVE" | "SUSPENDED";
  createdAt: string;
  updatedAt: string;
}
