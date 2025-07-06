package com.cloud.chat.dto;


import java.util.HashMap;
import java.util.Map;

public class VectorClock {
    private final Map<String, Integer> clock = new HashMap<>();
    private final String selfId;

    public VectorClock(String selfId) {
        this.selfId = selfId;
        clock.put(selfId, 0);
    }

    public synchronized void increment() {
        clock.put(selfId, clock.getOrDefault(selfId, 0) + 1);
    }

    public synchronized void updateFrom(Map<String, Integer> receivedClock) {
        for (Map.Entry<String, Integer> entry : receivedClock.entrySet()) {
            String node = entry.getKey();
            int value = entry.getValue();
            int current = clock.getOrDefault(node, 0);
            clock.put(node, Math.max(current, value));
        }
        increment();
    }

    public synchronized Map<String, Integer> getClock() {
        return new HashMap<>(clock);
    }
}
