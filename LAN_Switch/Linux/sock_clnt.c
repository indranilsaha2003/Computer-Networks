#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>

#define NSTRS       1500        /* No. of strings */
#define PORT        2222        /* Server port */
#define ITERATIONS  100         /* Set finite number of iterations */

char strs[NSTRS] = "";
char mystring[50] = "This string is fifty bytes long----------------\n";

/* Get time difference in milliseconds */
double get_time_diff(struct timeval start, struct timeval end) {
    return (end.tv_sec - start.tv_sec) * 1000.0 + (end.tv_usec - start.tv_usec) / 1000.0;
}

int main(int argc, char *argv[]) {
    if (argc != 2) {
        fprintf(stderr, "Usage: %s <server_ip>\n", argv[0]);
        exit(1);
    }

    int client_fd;
    struct sockaddr_in server;
    struct timeval start, end;

    /* Create socket */
    if ((client_fd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        perror("client: socket");
        exit(1);
    }

    /* Set server details */
    server.sin_family = AF_INET;
    server.sin_port = htons(PORT);
    
    if (inet_pton(AF_INET, argv[1], &server.sin_addr) <= 0) {
        perror("client: invalid address");
        close(client_fd);
        exit(1);
    }

    /* Connect to the server */
    if (connect(client_fd, (struct sockaddr *)&server, sizeof(server)) < 0) {
        perror("client: connect");
        close(client_fd);
        exit(1);
    }

    /* Prepare large message by concatenating the string multiple times */
    for (int i = 0; i < 30; i++) {
        strcat(strs, mystring);
    }

    printf("Connected to server %s on port %d...\n", argv[1], PORT);

    /* Send and receive messages */
    for (int i = 0; i < ITERATIONS; i++) {
        gettimeofday(&start, NULL);  // Start time
        send(client_fd, strs, sizeof(strs), 0);
        recv(client_fd, strs, sizeof(strs), 0);
        gettimeofday(&end, NULL);    // End time

        double time_spent = get_time_diff(start, end);
        printf("Time spent: %.3f ms\n", time_spent);
    }

    printf("Communication finished.\n");

    close(client_fd);
    return 0;
}
