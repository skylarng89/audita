import { getRequestURL, proxyRequest } from "h3";

export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event);
  const base = (config.apiInternalBase as string).replace(/\/+$/, "");
  const url = getRequestURL(event);
  const target = `${base}${url.pathname}${url.search}`;

  return proxyRequest(event, target);
});
