package com.miaogu.service.impl

import com.baomidou.mybatisplus.core.conditions.Wrapper
import com.baomidou.mybatisplus.core.toolkit.Wrappers
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.miaogu.entity.Chat3Message
import com.miaogu.enums.RestoreStatus
import com.miaogu.mapper.Chat3MessageMapper
import com.miaogu.service.Chat3MessageService
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class Chat3MessageServiceImpl : ServiceImpl<Chat3MessageMapper, Chat3Message>(), Chat3MessageService {
    @CacheEvict(value = ["chat3Messages", "chat3MessagesCount"], allEntries = true) // 清空缓存
    override fun remove(queryWrapper: Wrapper<Chat3Message>): Boolean {
        return super<ServiceImpl>.remove(queryWrapper)
    }

    @Cacheable(value = ["chat3Messages"]) // 缓存聊天消息列表
    override fun list(): MutableList<Chat3Message> {
        return super<ServiceImpl>.list()
    }

    @CacheEvict(value = ["chat3Messages"], allEntries = true) // 清空缓存
    override fun save(entity: Chat3Message): Boolean {
        return super<ServiceImpl>.save(entity)
    }

    @Cacheable(value = ["chat3MessagesCount"], key = "'chat3MessagesCount'") // 缓存计数结果
    override fun count(queryWrapper: Wrapper<Chat3Message>): Long {
        return super<ServiceImpl>.count(queryWrapper)
    }

    // 带版本号的逻辑删除
    override fun clearWithVersion(username: String): Boolean {
        // 获取当前最大版本号
        val maxVersion = baseMapper.selectMaxDeleteVersion(username)
        val newVersion = maxVersion + 1

        // 批量更新版本号（等效逻辑删除）
        return update(
            Wrappers.update<Chat3Message>()
                .set("delete_version", newVersion)
                .eq("username", username)
                .eq("delete_version", 0) // 只操作未删除的记录
        )
    }

    // 版本回退恢复
    override fun restoreVersion(username: String): RestoreStatus {
        // 获取可恢复的最大版本号
        val maxVersion = baseMapper.selectMaxDeleteVersion(username)
        if (maxVersion <= 0) return RestoreStatus.NO_HISTORY
        // 恢复该版本记录
        return if (baseMapper.updateVersion(username, 0, maxVersion)) RestoreStatus.SUCCESS else RestoreStatus.UPDATE_FAILED
    }
}
