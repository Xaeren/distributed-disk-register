package com.example.family;

import family.ChatMessage;
import family.Empty;
import family.FamilyServiceGrpc;
import family.FamilyView;
import family.NodeInfo;
import io.grpc.stub.StreamObserver;

import java.util.List;


public class FamilyServiceImpl extends FamilyServiceGrpc.FamilyServiceImplBase {

    private final NodeRegistry registry;
    private final NodeInfo self;
    private final DiskStorage storage;

    public FamilyServiceImpl(NodeRegistry registry, NodeInfo self) {
        this.registry = registry;
        this.self = self;
        this.registry.add(self);
        this.storage = new DiskStorage(self.getNodeId());
    }

    // Yeni node join olursa
    @Override
    public void join(NodeInfo request, StreamObserver<FamilyView> responseObserver) {
        registry.add(request);

        FamilyView view = FamilyView.newBuilder()
                .addAllMembers(registry.snapshot())
                .build();

        responseObserver.onNext(view);
        responseObserver.onCompleted();
    }

    // Ailenin snapshot'unu döndür
    @Override
    public void getFamily(Empty request, StreamObserver<FamilyView> responseObserver) {
        FamilyView view = FamilyView.newBuilder()
                .addAllMembers(registry.snapshot())
                .build();

        responseObserver.onNext(view);
        responseObserver.onCompleted();
    }

    // İstemciden gelen mesaj (SET)
    @Override
    public void sendMessage(ChatMessage request, StreamObserver<Empty> responseObserver) {
        // 1️⃣ Mesajı diske kaydet
        storage.save(request);

        // 2️⃣ Eğer lider isek diğer üyelere broadcast et
        if (self.getIsLeader()) {
            int tolerance = registry.getTolerance();
            registry.broadcast(request, tolerance, self.getNodeId());
        }

        // 3️⃣ OK yanıtı döndür
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    // GET mesajı (crash toleranslı)
    @Override
    public void getMessage(ChatMessage request, StreamObserver<ChatMessage> responseObserver) {
        int messageId = request.getMessageId();
        String text = storage.load(messageId);

        // Eğer mesaj yoksa diğer üyelere sor (lider node için)
        if (text == null && self.getIsLeader()) {
            List<NodeInfo> members = registry.snapshot();
            for (NodeInfo member : members) {
                if (member.getNodeId().equals(self.getNodeId())) continue;
                try {
                    FamilyServiceGrpc.FamilyServiceBlockingStub stub = registry.stubFor(member);
                    ChatMessage remote = stub.getMessage(ChatMessage.newBuilder().setMessageId(messageId).build());
                    text = remote.getText();
                    if (text != null && !text.isEmpty()) {
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Failed to fetch message from " + member.getNodeId());
                }
            }
        }

        // Yanıt oluştur
        ChatMessage reply = ChatMessage.newBuilder()
                .setMessageId(messageId)
                .setText(text != null ? text : "")
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    // Diğer üyelerden broadcast mesajı geldiğinde
    @Override
    public void receiveChat(ChatMessage request, StreamObserver<Empty> responseObserver) {
        System.out.println("💬 Incoming message:");
        System.out.println("  From: " + request.getFromHost() + ":" + request.getFromPort());
        System.out.println("  Text: " + request.getText());
        System.out.println("  Timestamp: " + request.getTimestamp());
        System.out.println("--------------------------------------");

        // Mesajı diske kaydet
        storage.save(request);

        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}
