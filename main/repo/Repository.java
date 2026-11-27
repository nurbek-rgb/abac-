package kz.abac.repo;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Generic CRUD abstraction shared by the memory and JDBC implementations. */
public interface Repository<T> {
    T save(T record);
    Optional<T> findById(long id);
    List<T> findAll();
    boolean deleteById(long id);
    long count();
    default List<T> findWhere(Predicate<T> predicate) {
        return findAll().stream().filter(predicate).toList();
    }
}
