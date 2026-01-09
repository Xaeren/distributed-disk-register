package com.example.family;

import family.NodeInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry {

    private final Map<String, NodeInfo> nodes = new ConcurrentHashMap<>();
    private final Map<Integer, Set<String>> messageLocations = new ConcurrentHashMap<>();

    public void add(NodeInfo node) {
        nodes.put(node.getId(), node);
    }

    public boolean isLeader(NodeInfo node) {
        return !nodes.isEmpty() && node.getId().equals(getLeaderId());
    }

    public String getLeaderId() {
        // En düşük ID'li node lider olsun
        return nodes.keySet().stream().min(String::compareTo).orElse(null);
    }

    public List<NodeInfo> snapshot() {
        return new ArrayList<>(nodes.values());
    }

    public List<NodeInfo> getOtherNodes(NodeInfo self, int count) {
        List<NodeInfo> others = new ArrayList<>();
        for (NodeInfo n : nodes.values()) {
            if (!n.getId().equals(self.getId())) {
                others.add(n);
            }
        }
        return others.subList(0, Math.min(count, others.size()));
    }

    public void addMessageLocation(int messageId, String nodeId) {
        messageLocations.computeIfAbsent(messageId, k -> new HashSet<>()).add(nodeId);
    }

    public List<NodeInfo> nodesWithMessage(int messageId) {
        Set<String> nodeIds = messageLocations.getOrDefault(messageId, new HashSet<>());
        List<NodeInfo> result = new ArrayList<>();
        for (String id : nodeIds) {
            NodeInfo node = nodes.get(id);
            if (node != null) result.add(node);
        }
        return result;
    }

    // Stub oluşturmayı şimdilik boş bırakabiliriz, NodeMain ile dolduracağız
    public family.FamilyServiceGrpc.FamilyServiceBlockingStub stubFor(NodeInfo node) {
        return null;
    }
}
