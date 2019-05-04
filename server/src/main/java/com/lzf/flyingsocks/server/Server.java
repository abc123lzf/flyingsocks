package com.lzf.flyingsocks.server;

import com.lzf.flyingsocks.Component;
import com.lzf.flyingsocks.Environment;
import com.lzf.flyingsocks.VoidComponent;

public interface Server extends Component<VoidComponent>, Environment {

    ServerConfig getServerConfig();

}
