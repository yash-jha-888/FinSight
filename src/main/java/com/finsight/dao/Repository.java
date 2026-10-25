package com.finsight.dao;

import com.finsight.model.Identifiable;

import java.util.List;
import java.util.Optional;

/**
 * Basic create, read, update and delete operations for one kind of entity.
 *
 * <p><b>OOP: interface with generics.</b> The UI and services depend on this
 * contract, not on SQLite. Each table gets its own implementation.
 */
public interface Repository<T extends Identifiable> {

    Optional<T> findById(long id);

    List<T> findAll();

    /** Inserts when the entity is new, otherwise updates it. Returns the saved entity with its id set. */
    T save(T entity);

    boolean delete(long id);
}
