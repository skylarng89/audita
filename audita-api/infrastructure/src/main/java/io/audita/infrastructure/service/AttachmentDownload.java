package io.audita.infrastructure.service;

import java.io.InputStream;

/**
 * Carries the data needed to stream an attachment back to the caller without
 * exposing the raw filesystem path in an API response.
 */
public record AttachmentDownload(
        InputStream stream,
        String fileName,
        String mimeType,
        long sizeBytes) {}
