package com.huixing.fontal.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.service.TeamMatchService;
import com.huixing.fontal.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PreCacheJob {
    @Resource
    private UserService userService;

    @Resource
    private TeamMatchService teamMatchService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient; // 注入 Redisson 客户端

    /**
     * 第一步：获取我们的主要的用户
     * 获取前4个正常状态的用户ID用于预热缓存
     */
    private List<Long> getMainUserIds() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id"); // 只查询ID字段，提高性能
        queryWrapper.eq("userStatus", 0); // 状态为0表示正常
        queryWrapper.eq("isDelete", 0); // 未删除
        queryWrapper.orderByAsc("id"); // 按ID升序排序
        queryWrapper.last("LIMIT 4"); // 限制返回4条
        List<User> userList = userService.list(queryWrapper);
        return userList.stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    /**
     * 每天凌晨预热，使用分布式锁保证多台机器只有一台执行
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void doPreCacheMainUser() {
        // 1. 定义锁的名称，确保唯一性
        RLock lock = redissonClient.getLock("fontal:precachejob:docache:lock");

        try {
            /**
             * 2. 尝试获取锁
             * waitTime: 0 -> 拿不到锁直接放弃，不等待
             * leaseTime: 30, TimeUnit.SECONDS -> 锁过期时间（防止执行到一半死机导致死锁）
             */
            if (lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                log.info("get lock: " + Thread.currentThread().getId());

                // 执行核心预热逻辑
                List<Long> mainUserIdList = getMainUserIds();
                if (mainUserIdList.isEmpty()) {
                    log.warn("未找到主要用户，跳过预热");
                    return;
                }
                for (Long userId : mainUserIdList) {
                    User mainUser = userService.getById(userId);
                    if (mainUser == null) continue;

                    String cacheKey = userService.redisFormat(userId);
                    ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

                    // 这里注意：定时任务里尽量不要直接调用 Controller，建议逻辑下沉到 Service
                    List<User> userList = userService.computeMatchUsers(mainUser);

                    List<User> safetyUserList = userList.stream()
                            .map(userService::getSafetyUser)
                            .collect(Collectors.toList());

                    valueOperations.set(cacheKey, safetyUserList, 24, TimeUnit.HOURS);
                    log.info("预热成功，用户ID: {}", userId);
                }
            }
        } catch (InterruptedException e) {
            log.error("doPreCacheMainUser error", e);
        } finally {
            // 3. 【非常重要】释放锁时，先判断是否是当前线程持有的锁
            if (lock.isHeldByCurrentThread()) {
                log.info("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    /**
     * 每小时预热热门队伍
     * 预热前20个热门队伍到缓存
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void preCacheHotTeams() {
        RLock lock = redissonClient.getLock("fontal:precachejob:precacheHotTeams:lock");

        try {
            if (lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                log.info("开始预热热门队伍缓存");
                long startTime = System.currentTimeMillis();
                
                // 调用队伍匹配服务获取热门队伍
                int limit = 20;
                teamMatchService.getHotTeams(limit);
                
                log.info("预热热门队伍缓存成功，耗时: {}ms", System.currentTimeMillis() - startTime);
            }
        } catch (InterruptedException e) {
            log.error("preCacheHotTeams error", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 每小时预热最新队伍
     * 预热前20个最新队伍到缓存
     */
    @Scheduled(cron = "0 30 * * * ?")
    public void preCacheNewTeams() {
        RLock lock = redissonClient.getLock("fontal:precachejob:precacheNewTeams:lock");

        try {
            if (lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                log.info("开始预热最新队伍缓存");
                long startTime = System.currentTimeMillis();
                
                // 调用队伍匹配服务获取最新队伍
                int limit = 20;
                teamMatchService.getNewTeams(limit);
                
                log.info("预热最新队伍缓存成功，耗时: {}ms", System.currentTimeMillis() - startTime);
            }
        } catch (InterruptedException e) {
            log.error("preCacheNewTeams error", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
