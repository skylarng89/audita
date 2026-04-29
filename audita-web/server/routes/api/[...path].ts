import { getRequestURL, proxyRequest } from "h3";

export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  const base = (config.apiInternalBase as string).replace(/\/+$/, "");
  const url = getRequestURL(event);
  const target = `${base}${url.pathname}${url.search}`;

  // This is an internal same-origin proxy hop. Forwarding browser Origin/Referer
  // to the API can trigger upstream CORS checks unnecessarily.
  delete event.node.req.headers.origin;
  delete event.node.req.headers.referer;
  delete event.node.req.headers.host;

  return proxyRequest(event, target);
});
