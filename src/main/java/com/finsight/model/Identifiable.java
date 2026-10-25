package com.finsight.model;

/**
 * Anything that is stored in the database with a numeric primary key.
 *
 * <p><b>OOP: interface.</b> The DAO layer is written against this contract,
 * so it can handle categories, budgets and transactions the same way.
 */
public interface Identifiable {

    long getId();

    void setId(long id);

    /** True until the object has been saved and given a database id. */
    default boolean isNew() {
        return getId() <= 0;
    }
}
