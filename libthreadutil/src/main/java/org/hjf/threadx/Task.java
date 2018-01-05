package org.hjf.threadx;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


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
public abstract class Task implements Runnable, Comparable<Task> {

    /**
     * 任务名字
     */
    private String name = "Undefined";

    /**
     * 任务优先等级
     */
    @TaskLevel
    private int level;

    /**
     * 执行结果标记，{@link TaskFlag#AGAIN} 表示重新执行
     */
    @TaskFlag
    private int flag = TaskFlag.NEXT;

    public Task() {
        this(TaskLevel.DEFAULT);
    }

    public Task(@TaskLevel int level) {
        this.level = level;
    }


    /**
     * 线程池中的 Task 优先度比较
     */
    @Override
    public int compareTo(@NonNull Task other) {
        int myPriority = getLevel();
        int otherPriority = other.getLevel();
        return myPriority < otherPriority ? 1 /* low priority */ : myPriority > otherPriority ? -1 /* high priority */ : 0;
    }

    /**
     * 设置任务名
     */
    public void setName(String taskName) {
        this.name = taskName;
    }

    /**
     * 获取任务名
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * 设置 Task 的标志
     */
    public void setFlag(@TaskFlag int resultFlag) {
        this.flag = resultFlag;
    }

    /**
     * 获取标志
     */
    @TaskFlag
    int getFlag() {
        return this.flag;
    }

    /**
     * 设置任务等级
     */
    void setLevel(@TaskLevel int mPreferenceLevel) {
        this.level = mPreferenceLevel;
    }

    /**
     * 获取任务在线程池中的优先度等级
     */
    public int getLevel() {
        return this.level;
    }
}
