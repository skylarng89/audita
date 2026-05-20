import { createError, getRequestURL, proxyRequest } from "h3";
import {
  buildProxyTarget,
  sanitizeProxyHeaders,
  validateProxyRequest,
} from "~/server/utils/apiProxy";

export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  const base = config.apiInternalBase as string;
  if (!base || base.trim() === "") {
    throw createError({ statusCode: 500, statusMessage: "Proxy base URL is not configured" });
  }

  const url = getRequestURL(event);
  try {
    validateProxyRequest(event.node.req.method ?? "GET", url.pathname, event.node.req.headers["content-type"] ?? null);
  } catch (error) {
    throw createError({ statusCode: 400, statusMessage: (error as Error).message });
  }

  event.node.req.headers = sanitizeProxyHeaders(event.node.req.headers) as typeof event.node.req.headers;

  const target = buildProxyTarget(base, url.pathname, url.search);

  return proxyRequest(event, target);
});
