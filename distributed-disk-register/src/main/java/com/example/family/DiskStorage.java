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
            // Başlangıçta klasörü tarayıp index doldur
            loadIndex();
        } catch (IOException e) {
            throw new RuntimeException("DiskStorage init failed", e);
        }
    }

    /**
     * SET işlemi: Mesajı diske kaydeder
     */
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

    /**
     * GET işlemi: Mesajı diskten okur
     */
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

    /**
     * Node'un diskinde kaç mesaj olduğunu döndürür
     */
    public synchronized int messageCount() {
        return index.size();
    }

    /**
     * Node başlatıldığında disk klasörünü tarayıp index'i doldurur
     */
    private void loadIndex() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDir, "*.txt")) {
            for (Path path : stream) {
                String name = path.getFileName().toString();
                int id = Integer.parseInt(name.replace(".txt", ""));
                index.put(id, path);
            }
        }
    }
}
