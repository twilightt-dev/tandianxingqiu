package com.dianping.constant;

public class RedisConstants {
    /**
     * 登录时redis里存验证码的key
     */
    public static final String LOGIN_CODE_KEY = "login:code:";

    /**
     * 注册时redis里存验证码的key
     */
    public static final String REGISTER_CODE_KEY = "register:code:";
    /**
     * redis里存验证码的过期时间
     */
    public static final Long CODE_TTL = 2L;
    /**
     * 同一手机号、同一用途验证码的最短重发间隔。
     */
    public static final String CODE_SEND_COOLDOWN_KEY = "code:cooldown:";
    public static final Long CODE_SEND_COOLDOWN_TTL = 60L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
    public static final String UPLOAD_OWNER_KEY = "upload:owner:";
    public static final Long UPLOAD_OWNER_TTL = 24L;
}
