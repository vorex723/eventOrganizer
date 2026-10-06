package com.mazurek.eventOrganizer.testSupport.database;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.ArrayList;
import java.util.List;

/** Captures SQL only on the calling thread and within the supplied action. */
public final class SqlCapture implements StatementInspector {
    private final ThreadLocal<List<String>> capturedStatements = new ThreadLocal<>();

    public List<String> capture(Runnable action) {
        List<String> statements = new ArrayList<>();
        capturedStatements.set(statements);
        try {
            action.run();
            return List.copyOf(statements);
        } finally {
            capturedStatements.remove();
        }
    }

    @Override
    public String inspect(String sql) {
        List<String> statements = capturedStatements.get();
        if (statements != null) {
            statements.add(sql);
        }
        return sql;
    }
}
