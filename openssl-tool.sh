#! /bin/bash
if command -v openssl > /dev/null 2>&1; then
    echo "openssl exists, ready to build cert"
else
    yum install -y openssl openssl-devel
fi

if [ ! -n "$1" ]; then
    CERT_PATH = ./cert
    if [ ! -d "./cert" ]; then
        mkdir cert
    fi
else
    CERT_PATH=$1
fi


cd $CERT_PATH

openssl req -new -nodes -x509 -keyout ca.key -out ca.crt
openssl genrsa -out server.key 1024
openssl req -new -key server.key -out server.csr
openssl x509 -req -days 3650 -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt
openssl pkcs8 -topk8 -in server.key -out pkcs8_server.key -nocrypt
unset CERT_PATH
cd ..
echo "Done"