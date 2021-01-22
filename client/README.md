
### GraalVM Native Image编译
```
native-image.cmd -H:+ReportExceptionStackTraces ^
--initialize-at-run-time=org.slf4j,io.netty,com.lzf.flyingsocks.client.gui ^
--no-fallback ^
-jar flyingsocks-client-3.0-SNAPSHOT-jar-with-dependencies.jar
```
修改ICON：
```
RCEDIT64 /I flyingsocks-client-3.0-SNAPSHOT-jar-with-dependencies.exe ../icon.ico
```