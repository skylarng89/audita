package io.audita.application.port;

public interface SampleDataPort {

    boolean isSampleDataImported(String tenantSlug);

    SampleDataSummary importSampleData(String tenantSlug);

    SampleDataSummary removeSampleData(String tenantSlug);

    record SampleDataSummary(
            int usersCount,
            int groupsCount,
            int changeRequestsCount,
            int commentsCount,
            int customFieldsCount,
            String message) {
    }
}