package com.huixing.fontal.job;

import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PreCacheJob {
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient; // 注入 Redisson 客户端

    //第一步：获取我们的主要的用户
    private List<Long> mainUserId = Arrays.asList(10001L,10002L,10003L,10004L);

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
                for (Long userId : mainUserId) {
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


}
