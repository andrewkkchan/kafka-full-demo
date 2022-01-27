package com.infinitelambda.kafkafulldemo.consumer;

import com.infinitelambda.kafkafulldemo.exception.BusinessRuleValidationError;
import com.infinitelambda.kafkafulldemo.state.StateHolder;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventConsumer implements Runnable {

    private final org.apache.kafka.clients.consumer.Consumer<String, String> kafkaConsumer;
    private final KafkaTemplate<String, String> template;
    private final ExecutorService executorService;
    private final StateHolder stateHolder;
    private final HashSet<String> processedKeys = new HashSet<>();


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
                //seek to the very beginning because this consumer is in memory only
                for (TopicPartition topicPartition : collection) {
                    kafkaConsumer.seek(topicPartition, 0);
                }

            }
        });
        while (true) {
            ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofHours(1));
            consumerRecords.forEach(record -> {
                try {
                    log.info("topic : {}", record.topic());
                    log.info("partition : {}", record.partition());
                    log.info("key : {}", record.key());
                    log.info("value : {}", record.value());

                    //processing & validate according to business rules
                    if (processedKeys.contains(record.key())) {
                        throw new BusinessRuleValidationError("Duplicate message");
                    } else {
                        processedKeys.add(record.key());
                    }

                    String value = record.value();
                    if (value == null || value.isEmpty() || value.equals("q")) {
                        throw new BusinessRuleValidationError("Value empty or quit");
                    }
                    char operator = value.charAt(0);
                    int operand = Integer.parseInt(value.substring(1));

                    if (operand <= 0) {
                        // do not take non negative operand
                        throw new BusinessRuleValidationError("No negative operand");
                    }

                    if (operator == '+') {
                        stateHolder.setState(stateHolder.getState() + operand);
                    } else if (operator == '-') {
                        if (operand > stateHolder.getState()) {
                            throw new BusinessRuleValidationError("Negative result disallowed");
                        }
                        stateHolder.setState(stateHolder.getState() - operand);

                    } else if (operator == '*') {
                        stateHolder.setState(stateHolder.getState() * operand);
                    } else if (operator == '/') {
                        stateHolder.setState(stateHolder.getState() / operand);
                    } else {
                        //not valid operator
                        throw new BusinessRuleValidationError("Not valid operator");
                    }
                    //keeping some state in the memory
                    //sending back results into output
                    try {
                        template.send("output-result", record.key(), String.valueOf(stateHolder.getState()));
                    } catch (Exception e) {
                        log.error("error found in producing", e);
                    }
                } catch (BusinessRuleValidationError e) {
                    try {
                        template.send("output-result", record.key(), e.getMessage());
                    } catch (Exception pe) {
                        log.error("error found in producing", pe);
                    }
                }
            });
            kafkaConsumer.commitSync();
        }
    }

}
