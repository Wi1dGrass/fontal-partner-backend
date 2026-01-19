package com.huixing.fontal.service;

import cn.hutool.core.date.StopWatch;
import com.google.gson.Gson;
import com.huixing.fontal.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;


@SpringBootTest
public class UserServiceTest {
    @Resource
    private UserService userService;

    // 自定义线程池：根据 CPU 核心数调整，这里设定核心 16 线程，最大 32 线程
    private final ExecutorService executorService = new ThreadPoolExecutor(
            16, 32, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10000));

    @Test
    void doInsertUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final int TOTAL_NUM = 100000; // 总插入量
        final int BATCH_SIZE = 5000;  // 每批次大小
        List<CompletableFuture<Void>> futureList = new ArrayList<>();

        // 定义标签库
        List<String> tagPool = Arrays.asList("Java", "Python", "C++", "网络安全", "Web",
                "软件工程", "嵌入式", "数据分析", "AI", "算法");
        Gson gson = new Gson();

        // 分组处理
        for (int i = 0; i < (TOTAL_NUM / BATCH_SIZE); i++) {
            List<User> userList = Collections.synchronizedList(new ArrayList<>());

            // 构建一批次数据
            for (int j = 0; j < BATCH_SIZE; j++) {
                User user = new User();
                user.setUsername("TestUser_" + i + "_" + j);
                user.setUserAccount("account_" + i + "_" + j);
                user.setUserAvatarUrl("https://picsum.photos/200");
                user.setGender(0);
                user.setUserPassword("12345678");
                user.setEmail("test@qq.com");
                user.setUserStatus(0);
                user.setUserRole(0);

                // 随机选择标签 (1 到 全部)
                Collections.shuffle(tagPool);
                int randomNum = ThreadLocalRandom.current().nextInt(1, tagPool.size() + 1);
                List<String> randomTags = tagPool.subList(0, randomNum);
                user.setTags(gson.toJson(randomTags));

                userList.add(user);
            }

            // 异步批量执行
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                userService.saveBatch(userList, BATCH_SIZE);
            }, executorService);

            futureList.add(future);
        }

        // 等待任务全部结束
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();

        stopWatch.stop();
        System.out.println("批量插入耗时：" + stopWatch.getTotalTimeMillis() + "ms");
    }
}
