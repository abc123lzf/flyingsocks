echo "OpenSSL Tools"
openssl req -new -nodes -x509 -keyout ca.key -out ca.crt
openssl genrsa -out server.key 1024
::openssl genrsa -out client.key 1024
openssl req -new -key server.key -out server.csr
::openssl req -new -key client.key -out client.csr
openssl x509 -req -days 3650 -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt
::openssl x509 -req -days 3650 -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out client.crt
openssl pkcs8 -topk8 -in server.key -out pkcs8_server.key -nocrypt
::openssl pkcs8 -topk8 -in client.key -out pkcs8_client.key -nocrypt
pause