package com.lzf.flyingsocks.management.service.repository.model;

import lombok.Data;

import java.sql.Date;

@Data
public class UserPO {

    private Integer userId;

    private String username;

    private String password;

    private String email;

    private Date registerTime;

    private Date lastLoginTime;

    private Integer status;

    private Boolean admin;
}
