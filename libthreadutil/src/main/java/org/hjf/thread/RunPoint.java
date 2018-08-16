package org.hjf.thread;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.hjf.log.LogUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * Run point
 * 采用 装饰设计模式 对 {@link Runnable} 进行功能的添加
 * <p>
 * 增加功能：
 * 1. 唯一任务标识
 * 2. 下节点任务 对象 缓存
 * 3. 上节点任务 标识 缓存
 * 4. GC回收通知器，通知 {@link RunPoint} 回收已完成的任务
 */
class RunPoint implements Runnable {

    /**
     * 当前任务对象缓存
     */
    protected Runnable runnable;

    /**
     * 名称
     */
    private String name;

    /**
     * {@link Runnable#run()} 代码执行完成后接下来的动作策略
     */
    @RunnableFlag
    private int flag = RunnableFlag.NEXT;

    /**
     * 所有的 上节点任务 标示
     */
    @Nullable
    private List<String> preRunnableNameList;
    /**
     * 所有的 下节点任务 标示
     */
    @Nullable
    private List<String> nextRunnableNameList;

    private OnRunnableCompleteListener onrunnableCompleteListener;

    /**
     * 构造方法
     */
    RunPoint(@NonNull String runnableName, @NonNull Runnable runnable) {
        this.name = runnableName;
        this.runnable = runnable;
    }


    /**
     * 获取任务名
     */
    @Nullable
    public String getName() {
        return this.name;
    }

    void setFlag(@RunnableFlag int flag) {
        this.flag = flag;
    }

    /**
     * 设置任务回收器
     *
     * @param onRunnableCompleteListener 任务回收器
     */
    void setOnRunnableCompleteListener(OnRunnableCompleteListener onRunnableCompleteListener) {
        this.onrunnableCompleteListener = onRunnableCompleteListener;
    }

    /**
     * 设置上级节点
     *
     * @param preRunnableTagList 所有上级节点的任务标示
     */
    void setPreRunnableNameList(@Nullable List<String> preRunnableTagList) {
        this.preRunnableNameList = preRunnableTagList;
    }

    /**
     * 添加下个任务名称
     *
     * @param runnableName runnable name
     */
    void addNextRunnableName(String runnableName) {
        if (nextRunnableNameList == null) {
            nextRunnableNameList = new LinkedList<>();
        }
        nextRunnableNameList.add(runnableName);
    }

    /**
     * 获取所有下级节点任务标识
     */
    @Nullable
    List<String> getNextRunnableNameList() {
        return this.nextRunnableNameList;
    }

    /**
     * 移除上节点任务标识，表示上节点任务已经完成
     *
     * @return true: 所有上级节点任务都完成了，Task可运行
     */
    boolean removePreRunnableByName(String taskName) {
        if (this.preRunnableNameList == null || this.preRunnableNameList.isEmpty()) {
            return true;
        }
        this.preRunnableNameList.remove(taskName);
        return preRunnableNameList.isEmpty();
    }

    protected void destroy() {

        this.onrunnableCompleteListener = null;

        if (nextRunnableNameList != null) {
            nextRunnableNameList.clear();
            nextRunnableNameList = null;
        }

        if (preRunnableNameList != null) {
            preRunnableNameList.clear();
            preRunnableNameList = null;
        }
    }

    @Override
    public void run() {
        do {
            LogUtil.v("runnable【{0}】 to run.", this.name);
            onPreRun();
            LogUtil.v("runnable【{0}】 to run.", this.name);
            onRun();
            LogUtil.v("runnable【{0}】 run end. Current runnable flag is {1}", this.name, this.flag);
            onEndRun();

            // 等待状态，每 0.1 秒检擦是否等待结束
            while (this.flag == RunnableFlag.WAIT) {
                LogUtil.v("runnable【{0}】 wait ...", this.name);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } while (this.flag == RunnableFlag.AGAIN);

        LogUtil.v("runnable【{0}】 complete.", this.name);
        // 调起 runnable Complete 回掉
        // 通知任务组 激活NextRunnable & 回收当前Runnable
        if (this.onrunnableCompleteListener != null) {
            this.onrunnableCompleteListener.onRunnableCompleteListener(this.name);
        }
    }

    protected void onPreRun() {
        if (this.runnable == null) {
            LogUtil.v("RunPoint【{0}】 member runnable is null.", this.name);
            throw new RuntimeException("RunPoint【" + this.name + "】 member runnable is null.");
        }
    }

    protected void onRun() {
        this.runnable.run();
    }

    protected void onEndRun() {

    }

}
