package org.hjf.threadx;

import android.os.Handler;
import android.os.Looper;

import org.hjf.util.log.LogUtil;

/**
 * Task ，自定义的最小调度资源
 * 、
 * 注： {@link TaskThreadPoolExecutor#beforeExecute(Thread, Runnable)} 设定了
 * Task 运行在  {@link android.os.Process#THREAD_PRIORITY_BACKGROUND} 级别
 * <p>
 * 功能：
 * 1. 实现指定任务工作线程
 * 2. 与 {@link TaskThreadPoolExecutor} 实现任务优先级控制
 * 3. 增加 ResultFlag 表示是否完成目标和控制接下来的动作
 */
public abstract class UITask extends Task {

    private Handler handler = new Handler(Looper.getMainLooper());

    public UITask() {
        super();
    }

    @Override
    public void run() {
        LogUtil.v("UITask dispatched to UI thread, current UITask marked as   [TaskFlag.WAIT]   state.");
        setFlag(TaskFlag.WAIT);
        // 提交 UI 任务
        handler.post(new Runnable() {
            @Override
            public void run() {
                LogUtil.v("UITask start execution.");
                runOnUiThread();
                LogUtil.v("UITask complete, and marked as   [TaskFlag.NEXT]   state");
                setFlag(TaskFlag.NEXT);
            }
        });
    }

    public abstract void runOnUiThread();
}
