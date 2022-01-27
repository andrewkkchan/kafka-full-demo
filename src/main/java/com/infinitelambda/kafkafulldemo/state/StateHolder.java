package com.infinitelambda.kafkafulldemo.state;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class StateHolder {
    private int state;
}
