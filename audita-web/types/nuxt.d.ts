import type { UserRole } from "~/types";

declare module "#app" {
  interface PageMeta {
    requiredRole?: UserRole;
  }
}

declare module "vue-router" {
  interface RouteMeta {
    requiredRole?: UserRole;
  }
}

export {};
