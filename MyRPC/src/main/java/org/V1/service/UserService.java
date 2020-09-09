package org.V1.service;


import org.V1.pojo.User;

public interface UserService {
    User getUserByUserId(Integer id);
    Integer insertUserId(User user);
}
