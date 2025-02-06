package com.miaogu.controller

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.miaogu.annotation.RequireJwt
import com.miaogu.entity.DeepSeekMessage
import com.miaogu.response.ApiResponse
import com.miaogu.response.DeepSeekResponse
import com.miaogu.service.DeepSeekService
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
@RestController
@RequestMapping("/1004")
@RequireJwt
class DeepSeekController(
    private val deepSeekService: DeepSeekService,
    private val redisTemplate: RedisTemplate<String, String>
) {
    val username: String?
        get() = redisTemplate.opsForValue().get("username")
    @Value("\${deepseek.api.key}")
    private lateinit var deepSeekApiKey: String
    private val restTemplate = RestTemplate()
    @PostMapping("/send")
    fun sendMessage(@RequestBody message: DeepSeekMessage): ApiResponse<String> {
        val deepSeekMessages = mutableListOf(DeepSeekMessage(role = "system", content = "你是一个AI助手，我叫${username}。请根据用户输入的指令进行回答."))
        deepSeekMessages.addAll(deepSeekService.list(QueryWrapper<DeepSeekMessage>().eq("username", username)))
        deepSeekMessages.add(DeepSeekMessage(role = "user", content = message.content))
        // 发送请求到 DeepSeek API
        val request = mapOf<String, Any?>(
            "model" to "deepseek-chat",
            "messages" to deepSeekMessages,
            "stream" to false
        )

        // 设置请求头
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Authorization", "Bearer $deepSeekApiKey")
        }

        // 调用 DeepSeek API
        val response = restTemplate.postForEntity(
            "https://api.deepseek.com/chat/completions",
            HttpEntity(request, headers),
            DeepSeekResponse::class.java
        )

        // 处理响应
        return if (response.statusCode.is2xxSuccessful && response.body != null) {
            val aiResponse = response.body!!.choices.firstOrNull()?.message?.content ?: "No response"
            deepSeekService.save(DeepSeekMessage(role = "user", content = message.content, username = username, time= message.time))
            deepSeekService.save(DeepSeekMessage(role = "assistant", content = aiResponse, username = username, time= message.time))
            ApiResponse(HttpStatus.OK, "Success", aiResponse)
        } else {
            ApiResponse(response.statusCode.value(), "Failed to send message to DeepSeek API")
        }
    }
}
