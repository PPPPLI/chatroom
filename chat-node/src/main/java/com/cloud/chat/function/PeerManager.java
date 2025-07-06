package com.cloud.chat.function;

import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class PeerManager {
    private final List<InetSocketAddress> peers = new ArrayList<>();
    private final String bootstrapIp;
    private final int bootstrapPort;
    private final int selfPort;
    private final String selfAddress;

    public PeerManager(String bootstrapIp, int bootstrapPort, int selfPort) {
        this.bootstrapIp = bootstrapIp;
        this.bootstrapPort = bootstrapPort;
        this.selfPort = selfPort;
        this.selfAddress = getSelfAddress();
        refreshPeers();
    }

    private String getSelfAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress() + ":" + selfPort;
        } catch (Exception e) {
            e.printStackTrace();
            return "localhost:" + selfPort;
        }
    }

    // Request peer list from bootstrap server and update current list
    public synchronized void refreshPeers() {
        try (DatagramSocket socket = new DatagramSocket()) {
            String joinMessage = "JOIN|" + selfAddress;
            byte[] requestData = joinMessage.getBytes();
            InetAddress address = InetAddress.getByName(bootstrapIp);
            DatagramPacket request = new DatagramPacket(requestData, requestData.length, address, bootstrapPort);
            socket.send(request);

            byte[] buffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            String raw = new String(response.getData(), 0, response.getLength());

            if (raw.startsWith("PEERS|")) {
                String[] entries = raw.substring(6).split(",");
                peers.clear();
                for (String entry : entries) {
                    if (!entry.equals(selfAddress)) {
                        String[] parts = entry.split(":");
                        peers.add(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
                    }
                }
            } else if (raw.startsWith("FULL|")) {
                System.out.println("Join failed: " + raw.substring(5));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized List<InetSocketAddress> getPeers() {
        return new ArrayList<>(peers);
    }
}
