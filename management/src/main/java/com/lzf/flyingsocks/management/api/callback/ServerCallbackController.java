package com.lzf.flyingsocks.management.api.callback;

import com.lzf.flyingsocks.management.global.ResponseObject;
import com.lzf.flyingsocks.management.global.util.ResponseContext;
import com.lzf.flyingsocks.management.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/callback")
public class ServerCallbackController {

    @Autowired
    private UserService userService;

    /**
     * 用户发起连接时
     */
    @PostMapping("connect")
    public ResponseObject<Void> onConnect(@RequestParam("username") String username,
                                          @RequestParam("password") String password) {
        if (StringUtils.isAnyBlank(username, password)) {
            ResponseContext.setStatusCode(400);
            return ResponseObject.build(400, "用户名或密码不能为空");
        }

        boolean verify = userService.verify(username, password);
        if (!verify) {
            ResponseContext.setStatusCode(403);
            return ResponseObject.build(403, "用户名或密码不正确");
        }


        return ResponseObject.success();
    }

    /**
     * 用户断开连接时
     */
    @PostMapping("disconnect")
    public ResponseObject<Void> onDisconnect() {
        return null;
    }

}
