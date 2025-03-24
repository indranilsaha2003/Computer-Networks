#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <winsock2.h>
#include <windows.h>
#include <unistd.h>   //  usleep for microsecond sleep

#define PORT 2222
#define NSTRS 1500
#define ITERATIONS 10
#define SLEEP_MICROSECONDS 500  // 0.5 ms sleep interval

char strs[NSTRS] = "";
char mystring[50] = "This string is fifty bytes long----------------\n";

int main(int argc, char *argv[]) {
    if (argc != 2) {
        printf("Usage: %s <server_ip>\n", argv[0]);
        return 1;
    }

    WSADATA wsa;
    SOCKET client_socket;
    struct sockaddr_in server;
    
    LARGE_INTEGER frequency, start, end;   // High-precision timer variables

    // Initialize Winsock
    WSAStartup(MAKEWORD(2, 2), &wsa);

    // Create socket
    client_socket = socket(AF_INET, SOCK_STREAM, 0);
    server.sin_family = AF_INET;
    server.sin_port = htons(PORT);
    server.sin_addr.s_addr = inet_addr(argv[1]);

    // Connect to server
    connect(client_socket, (struct sockaddr *)&server, sizeof(server));

    // Prepare the message
    for (int i = 0; i < 30; i++) {
        strcat(strs, mystring);
    }

    // Get the frequency of the high-precision timer
    QueryPerformanceFrequency(&frequency);

    //  Send data in iterations with microsecond sleep intervals
    for (int i = 0; i < ITERATIONS; i++) {
        QueryPerformanceCounter(&start);

        send(client_socket, strs, strlen(strs), 0);
        recv(client_socket, strs, sizeof(strs), 0);

        QueryPerformanceCounter(&end);

        // Convert to milliseconds
        double elapsed_time_ms = (double)(end.QuadPart - start.QuadPart) * 1000.0 / frequency.QuadPart;
        printf("Time spent: %.3f ms\n", elapsed_time_ms);

        // Sleep for 0.5 ms (500 microseconds)
        usleep(SLEEP_MICROSECONDS);
    }

    printf("Communication finished.\n");

    closesocket(client_socket);
    WSACleanup();
    return 0;
}
