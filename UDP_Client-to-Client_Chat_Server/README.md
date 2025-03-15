# UDP Client-to-Client Chat System  

A simple UDP-based chat system where multiple clients communicate through a central server while maintaining UDP characteristics. It includes an optional discovery server, acknowledgment (ACK) for reliability, and retransmission for lost messages. The system ensures message ordering using sequence numbers and enables real-time peer-to-peer communication.  


## Steps to run:
1. Run the DiscoveryServer
2. Run the UDPServer
3. Run the UDPClient(in multiple instances)
