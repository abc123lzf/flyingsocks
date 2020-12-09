package com.lzf.flyingsocks.management.service.repository;

import com.lzf.flyingsocks.management.service.repository.model.UserPO;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository("userRepository")
class UserRepositoryImpl extends AbstractJdbcDAO implements UserRepository {

    private static final RowMapper<UserPO> ROW_MAPPER = (rs, row) -> {
        UserPO user = new UserPO();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setRegisterTime(rs.getDate("register_time"));
        user.setLastLoginTime(rs.getDate("last_login_time"));
        user.setStatus(rs.getInt("status"));
        user.setAdmin(rs.getBoolean("admin"));
        return user;
    };


    @Override
    public String queryPassword(String username) {
        UserPO user = getFirst(jdbcTemplate.query("select password from user where username = ?",
                parameters(username), ROW_MAPPER));
        return user != null ? user.getPassword() : null;
    }


}
