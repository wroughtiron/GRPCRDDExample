
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
import anything.protocode.Interface.Criteria;
import anything.protocode.MetadataStoreGrpc;

import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;

import java.util.Collection;

/**
 * MetadataStoreIMPL filters through results data set on server side
 * Returns those which satisfy criteria in a blocking stream
 */
public class MetadataStoreIMPL implements MetadataStoreGrpc.MetadataStore {

    private final Collection<Result> results;

    MetadataStoreIMPL(Collection<Result> results) {
        this.results = results;
    }

    @Override
    public void query(Criteria request, StreamObserver<Interface.Result> responseObserver) {
        for (Result result : results) {
            if (largerOrEqual(result.getId(), request.getMinId())
                    && smallerOrEqual(result.getId(), request.getMaxId())
                    && result.getKey().matches(request.getKeyRegex())) {
                responseObserver.onValue(result);
            }
        }
        responseObserver.onCompleted();
    }

    /*Comparison done using little-Endian ordering*/
    private boolean largerOrEqual(ByteString a, ByteString b) {
        byte[] aArray = a.toByteArray();
        byte[] bArray = b.toByteArray();
        for (int i = 0; i < 16; i++) {
            if ((aArray[i] & 0xFF) > (bArray[i] & 0xFF)) {
                return true;
            }
            if ((aArray[i] & 0xFF) < (bArray[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    /*Comparison done using little-Endian ordering*/
    private boolean smallerOrEqual(ByteString a, ByteString b) {
        byte[] aArray = a.toByteArray();
        byte[] bArray = b.toByteArray();
        for (int i = 0; i < 16; i++) {
            if ((aArray[i] & 0xFF) < (bArray[i] & 0xFF)) {
                return true;
            }
            if ((aArray[i] & 0xFF) > (bArray[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }

}
