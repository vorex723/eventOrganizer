package com.mazurek.eventOrganizer.auth;

import org.hibernate.exception.ConstraintViolationException;

final class EmailUniqueConstraint {
    private static final String USER_EMAIL = "users_email_key";
    private static final String PENDING_EMAIL = "email_change_tokens_pending_email_key";

    private EmailUniqueConstraint() {
    }

    static boolean isUserEmailConflict(Throwable exception) {
        return hasConstraint(exception, USER_EMAIL);
    }

    static boolean isPendingEmailConflict(Throwable exception) {
        return hasConstraint(exception, PENDING_EMAIL);
    }

    private static boolean hasConstraint(Throwable exception, String constraintName) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && constraintName.equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
