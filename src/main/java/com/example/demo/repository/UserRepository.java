package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * 根据用户名查询用户（参数化查询，防止SQL注入）
     * JPA 方法命名查询自动使用 PreparedStatement，安全无SQL注入风险
     */
    Optional<User> findByUsername(String username);

    /**
     * 检查用户名是否已存在
     * 返回值：存在返回 true
     */
    boolean existsByUsername(String username);

    /**
     * 管理员：按用户名模糊搜索（分页）
     */
    Page<User> findByUsernameContaining(String username, Pageable pageable);

    /**
     * 使用 @Query 显式参数化查询验证用户名和密码（防止SQL注入）
     * :username 和 :password 为命名参数，JPA 自动绑定，杜绝注入
     */
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.password = :password")
    Optional<User> findByUsernameAndPassword(@Param("username") String username,
                                              @Param("password") String password);

    /**
     * 原生SQL参数化查询示例（使用 ?1 索引参数绑定）
     * JPA 会将参数安全绑定到 PreparedStatement
     */
    @Query(value = "SELECT * FROM users WHERE username = :username", nativeQuery = true)
    Optional<User> findByUsernameNative(@Param("username") String username);
}
