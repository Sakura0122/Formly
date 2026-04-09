package com.sakura.formly.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "测试")
@RestController
@RequestMapping("/test")
public class TestController {

    @RequestMapping()
    public String test() {
        return "test";
    }
}
