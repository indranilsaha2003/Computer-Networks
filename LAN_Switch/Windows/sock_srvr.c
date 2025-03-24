#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <winsock2.h>
#include <ws2tcpip.h>      // For inet_pton()
#include <windows.h>        // For Sleep() instead of usleep()

#define PORT 2222
#define NSTRS 1500

char strs[NSTRS] = "";
char mystring[50] = "This string is fifty bytes long----------------\n";

int main() {
    WSADATA wsa;
    SOCKET server_socket, client_socket;
    struct sockaddr_in server, client;
    int client_len = sizeof(client);

    // Initialize Winsock
    if (WSAStartup(MAKEWORD(2, 2), &wsa) != 0) {
        printf("Failed. Error Code: %d\n", WSAGetLastError());
        return 1;
    }

    // Create socket
    if ((server_socket = socket(AF_INET, SOCK_STREAM, 0)) == INVALID_SOCKET) {
        printf("Socket creation failed. Error: %d\n", WSAGetLastError());
        return 1;
    }

    server.sin_family = AF_INET;
    server.sin_addr.s_addr = INADDR_ANY;
    server.sin_port = htons(PORT);

    // Bind
    if (bind(server_socket, (struct sockaddr *)&server, sizeof(server)) == SOCKET_ERROR) {
        printf("Bind failed. Error: %d\n", WSAGetLastError());
        return 1;
    }

    listen(server_socket, 5);
    printf("Server listening on port %d...\n", PORT);

    for (int i = 0; i < 30; i++) {
        strcat(strs, mystring);
    }

    while ((client_socket = accept(server_socket, (struct sockaddr *)&client, &client_len)) != INVALID_SOCKET) {
        printf("Connection accepted\n");

        for (int i = 0; i < 1000; i++) {
            send(client_socket, strs, sizeof(strs), 0);
            Sleep(1);  // Sleep for 1ms (Windows API, replaces usleep)
        }

        closesocket(client_socket);
    }

    closesocket(server_socket);
    WSACleanup();
    return 0;
}
