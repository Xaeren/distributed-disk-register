@Override
public void getMessage(GetRequest request, StreamObserver<GetResponse> responseObserver) {

    int messageId = request.getMessageId();
    String text = storage.load(messageId);

    // 1️⃣ Eğer mesaj bu node’da yoksa → lider diğer üyelerden alacak
    if (text == null && registry.isLeader(self)) {

        List<NodeInfo> nodesWithMessage = registry.nodesWithMessage(messageId);

        for (NodeInfo node : nodesWithMessage) {
            if (node.getId().equals(self.getId())) continue; // kendini atla
            try {
                FamilyServiceGrpc.FamilyServiceBlockingStub stub = registry.stubFor(node);
                GetResponse res = stub.getMessage(request);
                text = res.getText();
                if (text != null) {
                    System.out.println("📦 Retrieved message " + messageId + " from node " + node.getId());
                    break;
                }
            } catch (Exception e) {
                System.out.println("❌ Node " + node.getId() + " unreachable");
            }
        }
    }

    GetResponse response = GetResponse.newBuilder()
            .setMessageId(messageId)
            .setText(text == null ? "" : text)
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
}
