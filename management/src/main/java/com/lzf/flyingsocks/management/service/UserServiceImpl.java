package com.lzf.flyingsocks.management.service;

import com.lzf.flyingsocks.management.service.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service("userService")
class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean verify(String username, String password) {
        if (StringUtils.isAnyBlank(username, password)) {
            return false;
        }

        String currectPassword = userRepository.queryPassword(username);
        return password.equals(currectPassword);
    }


    @Override
    @Async
    public void recordLoginHistory(String username, String ipAddress) {

    }
}
