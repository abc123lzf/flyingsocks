### 配置说明

服务端配置保存在config目录中，包含以下文件 <br>
`config.properties`：基础配置，该文件中的内容会被加载到SystemProperties <br>
`server.json`：代理服务配置，包含端口、密码、加密方式等 <br>
`log4j.properties`：系统日志配置 <br>
`encrypt`：代理服务加密所需的文件、配置，子目录名必须与`server.json`包含的节点名(`name`)一一对应


### 服务启动

进入`bin`目录，执行`startup.sh` <br>
服务是否启动成功需要通过命令jps查看，如果jps输出中包含ServerBoot则说明启动成功  <br>
如果启动失败，执行`startup.sh -t`查看异常信息

### 服务停止

执行命令`stop.sh`即可。
