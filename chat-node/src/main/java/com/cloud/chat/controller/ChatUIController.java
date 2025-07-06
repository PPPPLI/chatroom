package com.cloud.chat.controller;

import com.cloud.chat.dto.ChatMessage;
import com.cloud.chat.dto.VectorClock;
import com.cloud.chat.function.MessageReceiver;
import com.cloud.chat.function.MessageSender;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatUIController {
    @FXML
    private TextArea messageArea;

    @FXML
    private TextField inputField;

    @FXML
    private Button sendButton;

    @FXML
    private Label nodeCountLabel;

    @FXML
    private ComboBox<String> peerComboBox;

    @FXML
    private Button refreshButton;

    private String selfId;
    private VectorClock vectorClock;
    private final List<InetSocketAddress> peers = new ArrayList<>();
    private final String bootstrapIp = "127.0.0.1";
    private final int bootstrapPort = 4444;
    private int listeningPort;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public void initialize() {
        try {
            listeningPort = 5000 + new Random().nextInt(1000);
            selfId = InetAddress.getLocalHost().getHostAddress() + ":" + listeningPort;
            vectorClock = new VectorClock(selfId);

            MessageReceiver receiver = new MessageReceiver(listeningPort);
            receiver.setVectorClock(vectorClock);
            receiver.setUiController(this);
            receiver.start();

            refreshPeers();

            new Thread(() -> {
                while (running.get()) {
                    try {
                        Thread.sleep(2000);
                        refreshPeers();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }

        sendButton.setOnAction(e -> sendMessage());
        refreshButton.setOnAction(e -> refreshPeers());
    }

    private void refreshPeers() {
        try (DatagramSocket socket = new DatagramSocket()) {
            String joinMessage = "JOIN|" + selfId;
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
                synchronized (peers) {
                    peers.clear();
                    List<String> onlineList = new ArrayList<>();
                    for (String entry : entries) {
                        if (!entry.equals(selfId)) {
                            String[] parts = entry.split(":");
                            InetSocketAddress socketAddr = new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
                            peers.add(socketAddr);
                            onlineList.add(entry);
                        }
                    }

                    String selected = peerComboBox.getValue();
                    onlineList.add(0, "[Broadcast to all]");

                    Platform.runLater(() -> {
                        peerComboBox.getItems().setAll(onlineList);
                        if (selected != null && onlineList.contains(selected)) {
                            peerComboBox.setValue(selected);
                        } else {
                            peerComboBox.setValue("[Broadcast to all]");
                        }
                        nodeCountLabel.setText("Online peers: " + peers.size());
                    });
                }
            } else if (raw.startsWith("FULL|")) {
                appendMessage("Join failed: " + raw.substring(5));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String msg = inputField.getText();
        if (msg.isEmpty()) return;
        vectorClock.increment();

        String target = peerComboBox.getValue();
        if (target != null && !target.isEmpty() && !target.equals("[Broadcast to all]")) {
            String[] parts = target.split(":");
            String ip = parts[0];
            int port = Integer.parseInt(parts[1]);
            ChatMessage chatMsg = new ChatMessage("private", selfId, target, msg, vectorClock.getClock());
            MessageSender.sendMessage(chatMsg, ip, port);
            appendMessage("[You → " + target + "] : " + msg);
        } else {
            ChatMessage chatMsg = new ChatMessage("broadcast", selfId, null, msg, vectorClock.getClock());
            synchronized (peers) {
                for (InetSocketAddress peer : peers) {
                    MessageSender.sendMessage(chatMsg, peer.getAddress().getHostAddress(), peer.getPort());
                }
            }
            appendMessage("[You] : " + msg);
        }

        inputField.clear();
    }

    public void appendMessage(String msg) {
        Platform.runLater(() -> {
            messageArea.appendText(msg + "\n");
        });
    }
}
