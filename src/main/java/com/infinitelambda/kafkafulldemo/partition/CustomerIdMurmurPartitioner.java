package com.infinitelambda.kafkafulldemo.partition;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class CustomerIdMurmurPartitioner implements Partitioner {
    private static final BigInteger PARTITION_COUNT = BigInteger.valueOf(10);

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        if (key == null) {
            return 0;
        }
        int partition;
        HashFunction hashFunction = Hashing.murmur3_32();
        byte[] keyUtf8Bytes = key.toString().getBytes(StandardCharsets.UTF_8);
        HashCode digest = hashFunction.hashBytes(keyUtf8Bytes);
        BigInteger bi = new BigInteger(digest.asBytes());
        partition = bi.mod(PARTITION_COUNT).abs().intValue();
        return partition;
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
