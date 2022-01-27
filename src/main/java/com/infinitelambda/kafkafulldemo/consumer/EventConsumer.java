package com.infinitelambda.kafkafulldemo.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventConsumer implements Runnable {

    private final org.apache.kafka.clients.consumer.Consumer<String, String> kafkaConsumer;
    private final KafkaTemplate<String, String> template;
    private final ExecutorService executorService;


    @PostConstruct
    public void init() {
        executorService.submit(this);
    }

    @PreDestroy
    public void destroy() {
        executorService.shutdown();
    }

    @Override
    public void run() {
        kafkaConsumer.subscribe(Collections.singleton("input-ops"), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {

            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {

            }
        });
        while (true) {
            ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofHours(1));
            consumerRecords.forEach(record -> {
                log.info("topic : {}", record.topic());
                log.info("partition : {}", record.partition());
                log.info("key : {}", record.key());
                log.info("value : {}", record.value());
                //processing & validate according to business rules
                //keeping some state in the memory
                //sending back results into output
                try {
                    template.send("output-result", record.key(), record.value());
                } catch (Exception e){
                    log.error("error found in producing", e);
                }
            });
            kafkaConsumer.commitSync();
        }
    }

}
