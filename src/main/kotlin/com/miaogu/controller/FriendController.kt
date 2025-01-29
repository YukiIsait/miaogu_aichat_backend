package com.miaogu.controller

import com.miaogu.annotation.RequireJwt
import com.miaogu.entity.Friend
import com.miaogu.response.R
import com.miaogu.service.FriendService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/friend")
class FriendController(
    private val friendService: FriendService
) {
    @RequireJwt // 需要进行 JWT 验证
    @PostMapping("/friendList")
    fun friendList(): R<MutableList<Friend>> {
        val data = friendService.list() // 使用IService的list()方法获取所有消息
        return R.success(data)
    }
}
