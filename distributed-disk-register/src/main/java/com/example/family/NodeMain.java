package com.example.family;

import family.NodeInfo;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class NodeMain {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java NodeMain <nodeId> <port>");
            return;
        }

        String nodeId = args[0];
        int port = Integer.parseInt(args[1]);

        NodeInfo self = NodeInfo.newBuilder()
                .setId(nodeId)
                .setHost("localhost")
                .setPort(port)
                .build();

        NodeRegistry registry = new NodeRegistry();
        FamilyServiceImpl service = new FamilyServiceImpl(registry, self);

        Server server = ServerBuilder.forPort(port)
                .addService(service)
                .build();

        server.start();
        System.out.println("Node " + nodeId + " started at port " + port);

        server.awaitTermination();
    }
}
