package com.project.lumina.client.util;

import android.content.Context;

/**
 * HashCat的空壳实现版本。
 * 该版本移除了对 native 库的依赖和所有的签名校验逻辑。
 * 所有方法都返回一个默认的、表示“成功”或“无害”的值。
 */
public class HashCat {
    private static HashCat instance;

    // 不再加载 native-lib 库
    // static {
    //     System.loadLibrary("native-lib");
    // }

    /**
     * 获取单例实例。
     */
    public static synchronized HashCat getInstance() {
        if (instance == null) {
            instance = new HashCat();
        }
        return instance;
    }

    /**
     * 私有构造函数，防止外部实例化。
     */
    private HashCat() {
        // 构造函数为空
    }

    /**
     * 原生方法的空壳实现。直接返回一个空字符串。
     *
     * @param context Context对象
     * @return 返回一个空字符串
     */
    public String getSignaturesSha1(Context context) {
        return "";
    }

    /**
     * 原生方法的空壳实现。直接返回 true，表示校验通过。
     *
     * @param context Context对象
     * @return 始终返回 true
     */
    public boolean checkSha1(Context context) {
        return true;
    }

    /**
     * 原生方法的空壳实现。直接返回一个空字符串。
     *
     * @param context Context对象
     * @param userId  用户ID
     * @return 返回一个空字符串
     */
    public String getToken(Context context, String userId) {
        return "";
    }

    /**
     * 初始化校验逻辑的空壳实现。
     * 此方法现在不执行任何检查，并始终返回 true，以确保应用正常继续运行。
     *
     * @param context Context对象
     * @return 始终返回 true
     */
    public boolean LintHashInit(Context context) {
        // 直接返回true，跳过所有检查和应用终止逻辑。
        return true;
    }

    /**
     * 获取用户Token的空壳实现。
     *
     * @param context Context对象
     * @param userId  用户ID
     * @return 来自空壳getToken方法的空字符串
     */
    public String getTokenForUser(Context context, String userId) {
        return getToken(context, userId);
    }
    
    // getSha1Value 方法已被移除，因为它不再被任何地方调用。
}