package ru.mtuci.demo.controller;

import org.springframework.web.bind.annotation.*;
import ru.mtuci.demo.controller.model.Demo;

@RestController
@RequestMapping("/")
public class DemoController {

    @GetMapping("/hello")
    public String sayHello(@RequestParam String str){
        return str;
    }

    @PostMapping
    public Demo getDemo(@RequestBody Demo demo){
        return demo;
    }
}
