package com.example.family;

import family.ChatMessage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class DiskStorage {

    private final Path storageDir;
    private final Map<Integer, Path> index = new HashMap<>();

    public DiskStorage(String nodeId) {
        this.storageDir = Paths.get("data", nodeId);
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new RuntimeException("DiskStorage init failed", e);
        }
    }

    // SET için: mesajı diske yaz
    public synchronized void save(ChatMessage msg) {
        try {
            Path file = storageDir.resolve(msg.getMessageId() + ".txt");

            try (BufferedWriter writer = Files.newBufferedWriter(
                    file,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                writer.write(msg.getText());
            }

            index.put(msg.getMessageId(), file);

        } catch (IOException e) {
            throw new RuntimeException("Failed to save message " + msg.getMessageId(), e);
        }
    }

    // GET için: mesajı diskten oku
    public synchronized String load(int messageId) {
        try {
            Path file = index.get(messageId);
            if (file == null || !Files.exists(file)) {
                return null;
            }
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public synchronized int messageCount() {
        return index.size();
    }
}
