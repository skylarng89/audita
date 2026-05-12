export const API_CONTRACT_HEADER = "X-Audita-Api-Contract";

export function isApiContractCompatible(
  actualVersion: string | null,
  expectedVersion: string | null,
) {
  if (!expectedVersion) {
    return true;
  }

  return actualVersion === expectedVersion;
}
