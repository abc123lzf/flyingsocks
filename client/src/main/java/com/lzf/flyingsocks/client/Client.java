package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.Component;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.Environment;
import com.lzf.flyingsocks.VoidComponent;

public interface Client extends Component<VoidComponent>, Environment {

    String DEFAULT_COMPONENT_NAME = "flyingsocks-client";

    ConfigManager<?> getConfigManager();
}
