package com.example.family;

import family.*;
import io.grpc.stub.StreamObserver;

import java.util.List;

/**
 * Temel sistem:
 * - Gelen mesajı diske yazar
 * - Lider ise mesajı T=2 üyeye gönderir
 * - Üyeler gelen mesajı diske yazar
 */
public class FamilyServiceImpl extends FamilyServiceGrpc.FamilyServiceImplBase {

    private final NodeRegistry registry;
    private final NodeInfo self;
    private final DiskStorage storage;

    // ŞİMDİLİK SABİT: T = 2  (sonra config'ten okuyacağız)
    private static final int TOLERANCE = 2;

    public FamilyServiceImpl(NodeRegistry registry, NodeInfo self) {
        this.registry = registry;
        this.self = self;
        this.storage = new DiskStorage(self.getId());

        this.registry.add(self);
        System.out.println("Node started: " + self.getId());
    }

    // === ÜYE AİLEYE KATILIR ===
    @Override
    public void join(NodeInfo request, StreamObserver<FamilyView> responseObserver) {
        registry.add(request);

        FamilyView view = FamilyView.newBuilder()
                .addAllMembers(registry.snapshot())
                .build();

        responseObserver.onNext(view);
        responseObserver.onCompleted();
    }

    // === AİLE GÖRÜNÜMÜ ===
    @Override
    public void getFamily(Empty request, StreamObserver<FamilyView> responseObserver) {
        FamilyView view = FamilyView.newBuilder()
                .addAllMembers(registry.snapshot())
                .build();

        responseObserver.onNext(view);
        responseObserver.onCompleted();
    }

    // === ASIL İŞ BURADA ===
    // Liderden veya başka bir üyeden mesaj gelir
    @Override
    public void receiveChat(ChatMessage request, StreamObserver<Empty> responseObserver) {

        System.out.println("📩 Message received on " + self.getId()
                + " | msgId=" + request.getMessageId());

        // 1️⃣ HER NODE GELEN MESAJI DİSKE YAZAR
        storage.save(request);

        // 2️⃣ SADECE LİDER İSE → MESAJI DİĞER ÜYELERE YAY
        if (registry.isLeader(self)) {

            System.out.println("👑 I am leader, broadcasting message "
                    + request.getMessageId());

            List<NodeInfo> targets = registry.getOtherNodes(self, TOLERANCE);

            for (NodeInfo node : targets) {
                try {
                    FamilyServiceGrpc.FamilyServiceBlockingStub stub =
                            registry.stubFor(node);

                    stub.receiveChat(request);

                    System.out.println("  ✔ Sent to " + node.getId());
                } catch (Exception e) {
                    System.out.println("  ❌ Failed to send to " + node.getId());
                }
            }
        }

        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}
