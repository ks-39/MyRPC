package org.V2.service;


import org.V2.pojo.User;

public interface UserService {
    User getUserByUserId(Integer id);
    Integer insertUserId(User user);
}
