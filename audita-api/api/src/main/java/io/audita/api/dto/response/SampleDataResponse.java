package io.audita.api.dto.response;

public record SampleDataResponse(
        int usersCount,
        int groupsCount,
        int changeRequestsCount,
        int commentsCount,
        int customFieldsCount,
        String message) {
}