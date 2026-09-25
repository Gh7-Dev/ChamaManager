package chamamanager.dao;

import java.util.List;

/**
 * Generic contract for basic create/read/update/delete data operations on an
 * entity type {@code T}.
 *
 * @param <T> the entity type this contract operates on
 */
public interface CrudOperations<T> {

    void create(T item);

    T getById(int id);

    List<T> getAll();

    void update(T item);

    void delete(int id);
}
