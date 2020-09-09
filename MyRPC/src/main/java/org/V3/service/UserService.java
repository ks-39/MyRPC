package org.V3.service;


import org.V3.pojo.User;

public interface UserService {
    User getUserByUserId(Integer id);
    Integer insertUserId(User user);
}
