import type { ActivityEntry } from "~/types";

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
