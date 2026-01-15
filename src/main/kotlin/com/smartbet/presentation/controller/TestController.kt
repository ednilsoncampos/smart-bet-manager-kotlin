package com.smartbet.presentation.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
class TestController {

    @PostMapping("/post")
    fun postTeste(): String {
        return "OK"
    }

    @GetMapping("/open")
    fun open(): String {
        return "OK"
    }
}
