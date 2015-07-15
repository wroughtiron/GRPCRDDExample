
//  Copyright 2015 Roy Ferguson
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.


package example;

import anything.protocode.Interface;
import anything.protocode.Interface.Result;
import anything.protocode.MetadataStoreGrpc;

import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;
import io.grpc.ServerImpl;
import io.grpc.transport.netty.NettyServerBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Access logs are stored in results data set stored as ArrayList, read in from a text file
 * Results have a uniformly distributed 128-bit ByteString ID, time of user request, user request (Key) and user IP address (Value) as fields
 * Connection to client returns grpc blocking stream
 */
public class MetadataStoreServer {
    private static final Logger logger = Logger.getLogger(MetadataStoreServer.class.getName());

    private final int port;
    private final Collection<Result> results;
    private ServerImpl grpcServer;

    public MetadataStoreServer(int port, String resultFile) {
        this.port = port;
        results = parseResults(resultFile);
    }

    private Collection<Interface.Result> parseResults(String filename) {
        File file = new File(filename);
        Collection<Interface.Result> results = new ArrayList<Interface.Result>();
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                Interface.Result result = Interface.Result
                        .newBuilder()
                        .setId(hashLine(line))
                        .setTime(Long.parseLong(line.substring(line.indexOf('[') + 1, line.indexOf('[') + 12).replaceAll(":", "")))
                        .setKey(line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")))
                        .setValue(line.substring(0, line.indexOf(" "))).build();
                results.add(result);
            }
            return results;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ByteString hashLine(String line) {
        return ByteString.copyFrom(Hashing.murmur3_128().hashBytes(line.getBytes()).asBytes());
    }

    public void start() throws IOException {
        grpcServer = NettyServerBuilder.forPort(port)
                .addService(MetadataStoreGrpc.bindService(new MetadataStoreIMPL(results)))
                .build().start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                MetadataStoreServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() {
        if (grpcServer != null) {
            grpcServer.shutdown();
        }
    }

    public static void main(String[] args) throws Exception {
        MetadataStoreServer server = new MetadataStoreServer(12345, args[0]);
        server.start();
    }
}
