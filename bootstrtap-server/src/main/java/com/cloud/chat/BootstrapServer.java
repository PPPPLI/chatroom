package com.cloud.chat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BootstrapServer {
    private static final int PORT = 4444;
    private static final Map<String, Long> peers = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 15000; // Consider a peer offline if inactive for more than 15 seconds

    public static void main(String[] args) {
        System.out.println("Bootstrap server is running on port " + PORT);

        // Periodic cleanup thread to remove inactive peers
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                    long now = System.currentTimeMillis();
                    peers.entrySet().removeIf(entry -> now - entry.getValue() > TIMEOUT_MS);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();

        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());
                if (msg.startsWith("JOIN|")) {
                    String newPeer = msg.split("\\|")[1];

                    if (!peers.containsKey(newPeer)) {
                        System.out.println("New peer registered: " + newPeer);
                    }

                    if (peers.size() >= 10 && !peers.containsKey(newPeer)) {
                        String response = "FULL|The network is full. A maximum of 10 nodes is allowed.";
                        byte[] responseData = response.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(
                                responseData, responseData.length,
                                packet.getAddress(), packet.getPort()
                        );
                        socket.send(responsePacket);
                        continue;
                    }

                    peers.put(newPeer, System.currentTimeMillis());

                    String peersList = String.join(",", peers.keySet());
                    String response = "PEERS|" + peersList;
                    byte[] responseData = response.getBytes();

                    DatagramPacket responsePacket = new DatagramPacket(
                            responseData, responseData.length,
                            packet.getAddress(), packet.getPort()
                    );
                    socket.send(responsePacket);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
