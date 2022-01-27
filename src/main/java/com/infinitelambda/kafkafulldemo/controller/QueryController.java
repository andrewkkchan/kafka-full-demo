package com.infinitelambda.kafkafulldemo.controller;

import com.infinitelambda.kafkafulldemo.state.StateHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QueryController {
    private final StateHolder stateHolder;
    @GetMapping("/query")
    public int query(){
        return stateHolder.getState();
    }
}
