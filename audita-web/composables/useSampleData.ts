interface SampleDataResponse {
  usersCount: number;
  groupsCount: number;
  changeRequestsCount: number;
  commentsCount: number;
  customFieldsCount: number;
  message: string;
}

export function useSampleData() {
  const api = useApi();
  const { error: toastError, success: toastSuccess } = useToast();

  const sampleDataImported = ref(false);
  const sampleDataLoading = ref(false);

  async function checkStatus() {
    try {
      const response = await api<{ sampleDataImported: boolean }>(
        "/api/v1/settings",
        { method: "GET" },
      );
      sampleDataImported.value = response.sampleDataImported ?? false;
    } catch {
      sampleDataImported.value = false;
    }
  }

  async function importSampleData() {
    sampleDataLoading.value = true;
    try {
      const result = await api<SampleDataResponse>(
        "/api/v1/admin/sample-data",
        { method: "POST" },
      );
      sampleDataImported.value = true;
      toastSuccess(result.message || "Sample data imported successfully.");
      return result;
    } catch (error: unknown) {
      const msg =
        error instanceof Error ? error.message : "Failed to import sample data.";
      toastError(msg);
      return null;
    } finally {
      sampleDataLoading.value = false;
    }
  }

  async function removeSampleData() {
    sampleDataLoading.value = true;
    try {
      const result = await api<SampleDataResponse>(
        "/api/v1/admin/sample-data",
        { method: "DELETE" },
      );
      sampleDataImported.value = false;
      toastSuccess(result.message || "Sample data removed successfully.");
      return result;
    } catch (error: unknown) {
      const msg =
        error instanceof Error ? error.message : "Failed to remove sample data.";
      toastError(msg);
      return null;
    } finally {
      sampleDataLoading.value = false;
    }
  }

  function setSampleDataImported(value: boolean) {
    sampleDataImported.value = value;
  }

  return {
    sampleDataImported: readonly(sampleDataImported),
    sampleDataLoading: readonly(sampleDataLoading),
    checkStatus,
    importSampleData,
    removeSampleData,
    setSampleDataImported,
  };
}