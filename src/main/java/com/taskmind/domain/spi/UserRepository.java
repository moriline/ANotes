package com.taskmind.domain.spi;

import com.taskmind.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Integer id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    /**
     * Действующие пользователи, у которых подстрока {@code query} встречается в
     * username, displayName или email; регистр не учитывается. Пустой запрос
     * возвращает начало справочника. Порядок — по username, чтобы выдача была
     * предсказуемой.
     */
    List<User> search(String query, int limit);
}
