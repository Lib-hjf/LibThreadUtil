package org.hjf.threadx;


import android.app.ActivityManager;
import android.content.Context;
import android.os.Looper;
import android.support.annotation.Nullable;

/**
 * Created by huangjf on 2017/1/22.
 * <p>
 * THREAD_PRIORITY_AUDIO //标准音乐播放使用的线程优先级
 * THREAD_PRIORITY_BACKGROUND //标准后台程序
 * THREAD_PRIORITY_DEFAULT // 默认应用的优先级
 * THREAD_PRIORITY_DISPLAY //标准显示系统优先级，主要是改善UI的刷新
 * THREAD_PRIORITY_FOREGROUND //标准前台线程优先级
 * THREAD_PRIORITY_LESS_FAVORABLE //低于favorable
 * THREAD_PRIORITY_LOWEST //有效的线程最低的优先级
 * THREAD_PRIORITY_MORE_FAVORABLE //高于favorable
 * THREAD_PRIORITY_URGENT_AUDIO //标准较重要音频播放优先级
 * THREAD_PRIORITY_URGENT_DISPLAY //标准较重要显示优先级，对于输入事件同样适用。
 */

public final class ThreadUtil {

    /**
     * 首选，Android 自带的API，拥有公共定义的优先事项组，对线程调度影响显著。
     * priority：【-20， 19】，高优先级 -> 低优先级.
     * <p>
     * z注意：使用这个设置线程级别后，利用java原生的 Thread.getTaskLevel() 无法准确获取线程级别
     * <p>
     * 而 java.lang.Thread 中 {@link Thread#setPriority(int)} 的 priority：【1， 10】，低优先级 -> 高优先级.
     */
    static void setToBackground() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
    }

    /**
     * 判断当前线程是否是主线程
     */
    public static boolean isMainThread() {
        return Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId();
    }

    /**
     * 获取当前进程名
     */
    @Nullable
    public static String getCurrProgressName(Context context){
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : activityManager.getRunningAppProcesses()) {
            if (processInfo.pid == android.os.Process.myPid()){
                return processInfo.processName;
            }
        }
        return null;
    }
}
