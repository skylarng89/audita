import type { ActivityEntry } from "~/types";

function formatEnumLabel(value: string | null | undefined) {
  if (!value) return "\u2014";
  const normalized = value.replace(/^CR_/, "CHANGE_REQUEST_");
  return normalized
    .split("_")
    .filter(Boolean)
    .map((segment) => {
      if (segment === "CHANGE") return "Change";
      if (segment === "REQUEST") return "Request";
      return segment.charAt(0) + segment.slice(1).toLowerCase();
    })
    .join(" ");
}

export function buildActivitySummary(
  event: Pick<ActivityEntry, "actionType" | "payload">,
) {
  const reason = event.payload?.reason;
  if (typeof reason === "string" && reason.trim()) {
    return reason.trim();
  }

  if (event.actionType === "CR_APPROVER_ADDED") {
    return "Added approver.";
  }

  if (event.actionType === "CR_APPROVER_GROUP_ADDED") {
    const count = event.payload?.count;
    if (typeof count === "number") {
      return `Added ${count} approver${count === 1 ? "" : "s"} from group.`;
    }
    return "Added approvers from group.";
  }

  if (event.actionType === "CR_APPROVER_REMOVED") {
    return "Removed approver.";
  }

  if (event.actionType === "CR_APPROVER_REQUIREMENT_CHANGED") {
    const isRequired = event.payload?.isRequired;
    if (typeof isRequired === "boolean") {
      return isRequired
        ? "Marked approver as required."
        : "Marked approver as optional.";
    }
    return "Updated approver requirement.";
  }

  if (event.actionType === "CR_APPROVERS_REORDERED") {
    const count = event.payload?.count;
    if (typeof count === "number") {
      return `Reordered ${count} approver${count === 1 ? "" : "s"}.`;
    }
  }

  const mentions = event.payload?.mentions;
  if (typeof mentions === "number" && mentions > 0) {
    return `${mentions} mention${mentions === 1 ? "" : "s"} included in this comment.`;
  }

  return null;
}

export function formatActivityAction(actionType: string) {
  return formatEnumLabel(actionType);
}

export function formatActivityBadge(actionType: string) {
  return actionType.startsWith("CR_") ? "Workflow" : "Event";
}

function formatActivityValue(value: unknown) {
  if (value === null || value === undefined || value === "") {
    return "\u2014";
  }
  if (typeof value === "string") {
    return value.includes("_") ? formatEnumLabel(value) : value;
  }
  return String(value);
}

export function activityFields(event: ActivityEntry) {
  if (!event.payload) {
    return [];
  }
  return Object.entries(event.payload)
    .filter(
      ([key, value]) =>
        !key.endsWith("Id") &&
        key !== "reason" &&
        key !== "count" &&
        value !== null &&
        value !== undefined,
    )
    .map(([key, value]) => ({
      label: formatEnumLabel(key),
      value: formatActivityValue(value),
    }));
}
