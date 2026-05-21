package com.internpilot.constant;

public final class RedisKeyConstants {

    public static final String AUTH_CAPTCHA_PREFIX = "internpilot:auth:captcha:";
    public static final String AUTH_CAPTCHA_COOLDOWN_PREFIX = "internpilot:auth:captcha:cooldown:";
    public static final String AUTH_CAPTCHA_FAIL_PREFIX = "internpilot:auth:captcha:fail:";
    public static final String AUTH_CAPTCHA_DAILY_PREFIX = "internpilot:auth:captcha:daily:";

    public static final String AI_ANALYSIS_TASK_PREFIX = "internpilot:ai:analysis:task:";
    public static final String AI_ANALYSIS_RESULT_PREFIX = "internpilot:ai:analysis:result:";
    public static final String AI_CACHE_PREFIX = "internpilot:ai:cache:";

    public static final String USER_PERMISSION_PREFIX = "internpilot:user:permission:";

    private RedisKeyConstants() {
    }

    public static String captcha(String sceneCode, String target) {
        return AUTH_CAPTCHA_PREFIX + sceneCode + ":" + target;
    }

    public static String captchaCooldown(String sceneCode, String target) {
        return AUTH_CAPTCHA_COOLDOWN_PREFIX + sceneCode + ":" + target;
    }

    public static String captchaFail(String sceneCode, String target) {
        return AUTH_CAPTCHA_FAIL_PREFIX + sceneCode + ":" + target;
    }

    public static String captchaDaily(String target) {
        return AUTH_CAPTCHA_DAILY_PREFIX + target;
    }

    public static String analysisTask(String taskNo) {
        return AI_ANALYSIS_TASK_PREFIX + taskNo;
    }
}
