package org.V0.service;


import org.V0.pojo.User;

public interface UserService {
    User getUserByUserId(Integer id);
    Integer insertUserId(User user);
}
