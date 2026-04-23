package io.audita.domain.exception;

import java.util.UUID;

public class NotFoundException extends DomainException {

    public NotFoundException(String entity, UUID id) {
        super(entity + " not found: " + id);
    }

    public NotFoundException(String entity, String identifier) {
        super(entity + " not found: " + identifier);
    }
}
