#! /bin/bash
######################################################
#         flyingsocks v2.0   快速安装脚本
#######################################################
echo "flyingsocks服务器快速安装"

FS_SYS_TYPE=-1

command -v yum >/dev/null 2>&1 || {
    FS_SYS_TYPE=0
}

command -v apt-get >/dev/null 2>&1 || {
    FS_SYS_TYPE=1
}

if [ FS_SYS_TYPE == -1 ]; then
    echo "未知的操作系统类型，请手动安装"
    exit 1;
elif [ $FS_SYS_TYPE == 0 ]; then
    command -v wget >/dev/null 2>&1 || { yum install -y wget; }
    command -v openssl >/dev/null 2>&1 || { yum install -y openssl; }
    command -v unzip >/dev/null 2>&1 || { yum install -y unzip zip;}
else
    command -v wget >/dev/null 2>&1 || { apt-get install -y wget; }
    command -v openssl >/dev/null 2>&1 || { apt-get install -y openssl; }
    command -v unzip >/dev/null 2>&1 || { apt-get install -y unzip zip;}
fi

echo "服务器配置"
read -p "输入端口号：" FS_PORT
read -p "输入证书端口号：" FS_CERT_PORT
read -p "输入最大客户端连接数：" FS_MAX_CLI
read -p "输入密码：" FS_PWD

mkdir /etc/flyingsocks-server
cd /etc/flyingsocks-server
touch config.json
echo "[{ \"name\": \"default\"," "\"port\":$FS_PORT,"  "\"cert-port\":$FS_CERT_PORT,"  "\"max-client\": $FS_MAX_CLI," "\"encrypt\": \"OpenSSL\"," \
        "\"auth-type\": \"simple\"," "\"password\": \"$FS_PWD\"" "}]" > config.json

cd /tmp
wget https://raw.githubusercontent.com/abc123lzf/flyingsocks/v2.0/download/flyingsocks-server-v2.0.zip
unzip flyingsocks-server-v2.0.zip -d /usr
cd /usr/flyingsocks-server-v2.0/conf/encrypt

openssl req -new -nodes -x509 -keyout private.key -out ca.crt

cd /usr/flyingsocks-server-v2.0/bin
chmod 770 startup.sh
./startup.sh -daemon

cd ..

echo "Install path: /usr/flyingsocks-server-2.0"
echo "Done"
