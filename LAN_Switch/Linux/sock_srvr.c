#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <time.h>

#define NSTRS       1500        /* No. of strings */
#define PORT        2222        /* Server port */
#define ITERATIONS  100         /* Set finite number of iterations */
#define SLEEP_NS    100000      /* Sleep time in nanoseconds (100 µs) */

char strs[NSTRS] = "";
char mystring[50] = "This string is fifty bytes long----------------\n";

/* Sleep function using nanosleep for sub-millisecond accuracy */
void sleep_microseconds(long microseconds) {
    struct timespec req, rem;
    req.tv_sec = microseconds / 1000000;                  // Convert to seconds
    req.tv_nsec = (microseconds % 1000000) * 1000;        // Convert to nanoseconds
    nanosleep(&req, &rem);
}

int main() {
    int server_fd, client_fd;
    struct sockaddr_in saun, fsaun;
    socklen_t fromlen = sizeof(fsaun);

    /* Create socket */
    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        perror("server: socket");
        exit(1);
    }

    /* Configure socket address */
    saun.sin_family = AF_INET;
    saun.sin_port = htons(PORT);
    saun.sin_addr.s_addr = INADDR_ANY;

    /* Allow port reuse to prevent "Address already in use" errors */
    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    /* Bind the socket */
    if (bind(server_fd, (struct sockaddr *)&saun, sizeof(saun)) < 0) {
        perror("server: bind");
        close(server_fd);
        exit(1);
    }

    /* Start listening for connections */
    if (listen(server_fd, 5) < 0) {
        perror("server: listen");
        close(server_fd);
        exit(1);
    }

    /* Prepare large message by concatenating the string multiple times */
    for (int i = 0; i < 30; i++) {
        strcat(strs, mystring);
    }

    printf("Server listening on port %d...\n", PORT);

    /* Accept and process client connections */
    client_fd = accept(server_fd, (struct sockaddr *)&fsaun, &fromlen);
    if (client_fd < 0) {
        perror("server: accept");
        close(server_fd);
        exit(1);
    }

    fprintf(stderr, "One connection initiated.\n");

    /* Send data with sleep for high-speed traffic */
    for (int i = 0; i < ITERATIONS; i++) {
        send(client_fd, strs, sizeof(strs), 0);
        sleep_microseconds(100);  // Sleep for 100 µs (0.1 ms)
    }

    printf("Communication finished.\n");

    close(client_fd);
    close(server_fd);
    return 0;
}
