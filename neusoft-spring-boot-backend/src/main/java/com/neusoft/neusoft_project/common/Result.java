package com.neusoft.neusoft_project.common;

import lombok.Data;

/**
 * Universal Response Wrapper
 * Ensures Frontend always gets { code, msg, data }
 */
@Data
public class Result {
    private String code; // "200" for success, "500" for error
    private String msg;  // Message like "Login Successful"
    private Object data; // The actual data (User object, Token, etc.)

    public static Result success(Object data) {
        Result r = new Result();
        r.setCode("200");
        r.setMsg("Success");
        r.setData(data);
        return r;
    }

    public static Result error(String msg) {
        Result r = new Result();
        r.setCode("500");
        r.setMsg(msg);
        return r;
    }
}