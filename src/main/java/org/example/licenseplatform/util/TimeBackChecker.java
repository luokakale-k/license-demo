package org.example.licenseplatform.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TimeBackChecker {

    private static final String CHECK_DIR = System.getProperty("user.home") + "/.license_record/";
    private static final String CHECK_FILE = CHECK_DIR + "last_check_time";

    /**
     * 校验是否存在时间回拨行为
     * 若当前系统时间 < 上次记录的时间，则视为时间回拨
     * @throws RuntimeException 若检测到回拨行为
     */
    public static void validateSystemTime() {
        long now = System.currentTimeMillis();

        try {
            File dir = new File(CHECK_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(CHECK_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(CHECK_FILE)));
                long lastTime = Long.parseLong(content.trim());

                if (now < lastTime) {
                    throw new RuntimeException("检测到系统时间回拨，License 校验失败");
                }
            }

            // 更新当前时间为最新
            Files.write(Paths.get(CHECK_FILE), String.valueOf(now).getBytes());
        } catch (IOException e) {
            throw new RuntimeException("时间回拨检测异常：" + e.getMessage(), e);
        }
    }
}
