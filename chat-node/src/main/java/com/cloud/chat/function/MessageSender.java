package com.cloud.chat.function;

import com.cloud.chat.dto.ChatMessage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageSender {
    private static final Map<String, Boolean> ackMap = new ConcurrentHashMap<>();
    private static final ExecutorService resendExecutor = Executors.newCachedThreadPool();
    private static DatagramSocket sharedSocket = null;

    public static void setSharedSocket(DatagramSocket socket) {
        sharedSocket = socket;
    }

    public static void sendMessage(ChatMessage message, String ip, int port) {
        if (sharedSocket == null) {
            throw new IllegalStateException("Shared socket not initialized. Call setSharedSocket() first.");
        }

        String messageId = message.messageId;
        ackMap.put(messageId, false);

        resendExecutor.execute(() -> {
            try {
                int maxRetries = 5;
                int attempts = 0;

                while (attempts < maxRetries && !Boolean.TRUE.equals(ackMap.get(messageId))) {
                    byte[] data = message.serialize().getBytes();
                    InetAddress address = InetAddress.getByName(ip);
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                    sharedSocket.send(packet);

                    attempts++;
                    Thread.sleep(2000);
                }

                if (!Boolean.TRUE.equals(ackMap.get(messageId))) {
                    System.out.println("Message " + messageId + " was not acknowledged. Aborting retries.");
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ackMap.remove(messageId);
            }
        });
    }

    public static void acknowledge(String messageId) {
        ackMap.put(messageId, true);
        System.out.println("Received ACK for message " + messageId);
    }
}
