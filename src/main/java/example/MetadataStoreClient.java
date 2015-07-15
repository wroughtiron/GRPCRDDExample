
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

import anything.protocode.MetadataStoreGrpc.MetadataStoreBlockingStub;
import anything.protocode.MetadataStoreGrpc;
import anything.protocode.Interface.Criteria;
import anything.protocode.Interface.Result;

import com.google.protobuf.ByteString;

import io.grpc.ChannelImpl;
import io.grpc.transport.netty.NegotiationType;
import io.grpc.transport.netty.NettyChannelBuilder;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access logs matching a given user request can be obtained from a remote server using MetadataStoreClient
 */
public class MetadataStoreClient {

    private static final Logger logger = Logger.getLogger(MetadataStoreClient.class.getName());

    private final ChannelImpl channel;
    private final MetadataStoreBlockingStub blockingStub;

    public MetadataStoreClient(String host, int port) {
        channel = NettyChannelBuilder.forAddress(host, port)
                .negotiationType(NegotiationType.PLAINTEXT)
                .build();
        blockingStub = MetadataStoreGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTerminated(5, TimeUnit.SECONDS);
    }

    public void query(ByteString minID, ByteString maxID, String keyRegex) {
        try {
            Object[] params = {minID.toString(), maxID.toString(), keyRegex};
            logger.log(Level.INFO, "*** Query minID: {0}, maxID: {1}, keyRegex: {2}", params);
            Criteria request = Criteria.newBuilder().setKeyRegex(keyRegex).setMinId(minID).setMaxId(maxID).build();
            Iterator<Result> results = blockingStub.query(request);
            StringBuilder responseLog = new StringBuilder("Result: ");
            while (results.hasNext()) {
                Result result = results.next();
                responseLog.append(result);
                System.out.println(result.getKey());
            }
            logger.log(Level.INFO, responseLog.toString());
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "RPC failed", e);
            System.out.println("Connection failed");
            throw e;
        }
    }

    /*returnQuery returns blocking stream call*/
    public Iterator returnQuery(ByteString minID, ByteString maxID, String keyRegex) {
        try {
            Object[] params = {minID.toString(), maxID.toString(), keyRegex};
            logger.log(Level.INFO, "*** Query minID: {0}, maxID: {1}, keyRegex: {2}", params);
            Criteria request = Criteria.newBuilder().setKeyRegex(keyRegex).setMinId(minID).setMaxId(maxID).build();
            Iterator<Result> results = blockingStub.query(request);
            return results;
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "RPC failed", e);
            throw e;
        }
    }

    public ByteString getUpperBound() {
        byte[] upper = new byte[16];
        for (int i = 0; i < 16; i++) {
            upper[i] = Byte.parseByte("-1");
        }
        return ByteString.copyFrom(upper);
    }

    public ByteString getLowerBound() {
        byte[] lower = new byte[16];
        for (int i = 0; i < 16; i++) {
            lower[i] = Byte.parseByte("0");
        }
        return ByteString.copyFrom(lower);
    }

    public static void main (String[] args) {
        MetadataStoreClient mdsc = new MetadataStoreClient("localhost", 12345);
        ByteString minID = mdsc.getLowerBound();
        ByteString maxID = mdsc.getUpperBound();
        mdsc.query(minID, maxID, ".*");
    }

}