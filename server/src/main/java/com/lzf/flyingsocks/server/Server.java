package com.lzf.flyingsocks.server;

import com.lzf.flyingsocks.Component;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.Environment;
import com.lzf.flyingsocks.VoidComponent;

public interface Server extends Component<VoidComponent>, Environment {

    /**
     * 服务器版本
     */
    String VERSION = "v2.0";

    /**
     * @return 服务器配置信息
     */
    ServerConfig getServerConfig();

    /**
     * @return 服务器配置管理器
     */
    ConfigManager<?> getConfigManager();

    /**
     * @return 用户数据库
     */
    UserDatabase getUserDatabase();

}
