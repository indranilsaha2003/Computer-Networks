# Steps to run:

## Compile
gcc -o server sock_srvr.c
gcc -o client sock_clnt.c
(add "-lws2_32" at the end of every command if on Windows)

## Run Server
./server

## Run Client
./client 127.0.0.1
