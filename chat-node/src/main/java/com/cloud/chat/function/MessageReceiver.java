package com.cloud.chat.function;

import com.cloud.chat.controller.ChatUIController;
import com.cloud.chat.dto.ChatMessage;
import com.cloud.chat.dto.VectorClock;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

public class MessageReceiver extends Thread {
    private final int port;
    private VectorClock vectorClock;
    private final PriorityBlockingQueue<ChatMessage> messageQueue = new PriorityBlockingQueue<>(100,
            Comparator.comparing(ChatMessage::vectorClockToComparableString));
    private final Set<String> receivedIds = new HashSet<>();
    private ChatUIController uiController;

    public MessageReceiver(int port) {
        this.port = port;
    }

    public void setVectorClock(VectorClock vectorClock) {
        this.vectorClock = vectorClock;
    }

    public void setUiController(ChatUIController controller) {
        this.uiController = controller;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            // Set shared socket for sending ACK and messages
            MessageSender.setSharedSocket(socket);

            byte[] buffer = new byte[1024];
            System.out.println("Listening on port " + port + "...");

            // Message display thread
            new Thread(() -> {
                while (true) {
                    try {
                        ChatMessage msg = messageQueue.take();
                        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                        String formatted = "[" + time + "] " + msg.from + " : " + msg.content;

                        if (uiController != null) {
                            uiController.appendMessage(formatted);
                        } else {
                            System.out.println("\n" + formatted);
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();

            // Message receiving loop
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String raw = new String(packet.getData(), 0, packet.getLength());

                if (raw.startsWith("ACK|")) {
                    String ackId = raw.substring(4);
                    MessageSender.acknowledge(ackId);
                    continue;
                }

                ChatMessage message = ChatMessage.deserialize(raw);

                if (receivedIds.contains(message.messageId)) {
                    sendAck(socket, packet.getAddress(), packet.getPort(), message.messageId);
                    continue;
                }
                receivedIds.add(message.messageId);

                if (vectorClock != null) {
                    vectorClock.updateFrom(message.vectorClock);
                }

                messageQueue.offer(message);
                sendAck(socket, packet.getAddress(), packet.getPort(), message.messageId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendAck(DatagramSocket socket, InetAddress ip, int port, String messageId) {
        try {
            String ackMsg = "ACK|" + messageId;
            byte[] ackData = ackMsg.getBytes();
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, ip, port);
            socket.send(ackPacket);
            System.out.println("Sent ACK to " + ip + ":" + port + " -> " + messageId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
