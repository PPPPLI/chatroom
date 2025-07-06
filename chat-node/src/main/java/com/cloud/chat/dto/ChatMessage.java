package com.cloud.chat.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChatMessage {

    public String messageId;
    public String type;
    public String from;
    public String to;
    public String content;
    public Map<String, Integer> vectorClock;

    public ChatMessage(String type, String from, String to, String content, Map<String, Integer> vectorClock) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.content = content;
        this.vectorClock = vectorClock;
        this.messageId = UUID.randomUUID().toString();
    }

    public ChatMessage(String type, String from, String to, String content, Map<String, Integer> vectorClock, String messageId) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.content = content;
        this.vectorClock = vectorClock;
        this.messageId = messageId;
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append("|").append(from).append("|")
                .append(to == null ? "" : to).append("|").append(content).append("|");
        for (Map.Entry<String, Integer> entry : vectorClock.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
        }
        sb.append("|").append(messageId);
        return sb.toString();
    }

    public static ChatMessage deserialize(String raw) {
        String[] parts = raw.split("\\|", 6);
        Map<String, Integer> vc = new HashMap<>();
        if (parts.length >= 5 && !parts[4].isEmpty()) {
            String[] entries = parts[4].split(",");
            for (String entry : entries) {
                if (!entry.isEmpty()) {
                    String[] kv = entry.split("=");
                    vc.put(kv[0], Integer.parseInt(kv[1]));
                }
            }
        }
        String messageId = parts.length == 6 ? parts[5] : UUID.randomUUID().toString();
        return new ChatMessage(parts[0], parts[1], parts[2].isEmpty() ? null : parts[2], parts[3], vc, messageId);
    }

    public String vectorClockToComparableString() {
        return vectorClock.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(";"));
    }
}

