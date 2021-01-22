/*
 * Copyright (c) 2019 abc123lzf <abc123lzf@126.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lzf.flyingsocks.server.db;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lzf.flyingsocks.AbstractConfig;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.ConfigEvent;
import com.lzf.flyingsocks.ConfigEventListener;
import com.lzf.flyingsocks.ConfigInitializationException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.server.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 轻量级用户数据库实现：基于文本文件实现
 */
public class TextUserDatabase extends AbstractConfig implements UserDatabase {
    private static final Logger log = LoggerFactory.getLogger("TextUserDatabase");

    public static final String NAME = "userdatabase.text";

    private final Map<String, UserGroupImpl> groupMap = new ConcurrentHashMap<>(8);

    /**
     * 用户组对象
     */
    private static final class UserGroupImpl implements UserGroup {
        private final String name;
        private final ConcurrentMap<String, String> userMap = new ConcurrentHashMap<>(8);

        public UserGroupImpl(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }
    }

    public TextUserDatabase(ConfigManager<?> configManager) {
        super(configManager, NAME);
    }

    @Override
    protected void initInternal() throws ConfigInitializationException {
        ServerConfig cfg = configManager.getConfig(ServerConfig.NAME, ServerConfig.class);
        if (cfg != null) {
            doInitial(cfg);
        } else {
            configManager.registerConfigEventListener(new ConfigEventListener() {
                @Override
                public void configEvent(ConfigEvent event) {
                    if (event.getEvent().equals(Config.REGISTER_EVENT) && event.getSource() instanceof ServerConfig) {
                        try {
                            doInitial((ServerConfig) event.getSource());
                        } catch (ConfigInitializationException e) {
                            log.error("Load UserDatabase occur a exception", e);
                        }
                        configManager.removeConfigEventListener(this);
                    }
                }
            });
        }
    }


    private void doInitial(ServerConfig cfg) throws ConfigInitializationException {
        try (InputStream is = new FileInputStream(cfg.getLocation().toFile())) {
            byte[] b = new byte[512000];
            int len = is.read(b);
            String json = new String(b, 0, len, StandardCharsets.UTF_8);

            JSONArray arr = JSON.parseArray(json);
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                UserGroupImpl group = new UserGroupImpl(obj.getString("group"));

                JSONArray userArr = obj.getJSONArray("user");
                for (int j = 0; j < userArr.size(); j++) {
                    JSONObject user = userArr.getJSONObject(j);
                    group.userMap.put(user.getString("user"), user.getString("pass"));
                }

                if (groupMap.containsKey(group.name))
                    log.warn("Group name '{}' already exists", group.name);
                groupMap.put(group.name, group);
            }

        } catch (IOException ignore) {
            log.info("Can not open user db file user.json");
        } catch (Exception e) {
            throw new ConfigInitializationException(e);
        }
    }


    @Override
    public boolean doAuth(String group, String username, String password) {
        UserGroupImpl ug = groupMap.get(group);
        if (ug == null)
            return false;

        return (password == null ? "" : password).equals(ug.userMap.get(username));
    }

    @Override
    public boolean register(String group, String username, String password) {
        UserGroupImpl ug = groupMap.get(group);
        if (ug == null)
            return false;

        if (ug.userMap.containsKey(username))
            return false;

        ug.userMap.put(username, password);
        return true;
    }

    @Override
    public boolean delete(String group, String username) {
        UserGroupImpl ug = groupMap.get(group);
        if (ug == null)
            return false;

        return ug.userMap.remove(username) != null;
    }

    @Override
    public boolean changePassword(String group, String username, String newPassword) {
        UserGroupImpl ug = groupMap.get(group);
        if (ug == null)
            return false;

        if (!ug.userMap.containsKey(username))
            return false;

        ug.userMap.put(username, newPassword);
        return true;
    }
}
