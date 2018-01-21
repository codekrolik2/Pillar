package com.github.bamirov.pillar.db.interfaces;

/**
 * Abstract Transaction context for a DB
 * 
 * @author bamirov
 *
 */
public interface TransactionContext {
    void commit() throws Exception;
    void rollback() throws Exception;
    void close() throws Exception;
}
