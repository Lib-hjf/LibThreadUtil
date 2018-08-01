package org.hjf.threadx;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.hjf.util.log.LogUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * 采用 装饰设计模式 对 {@link Task} 进行功能的添加
 * 外部只需使用 {@link Task} 就能实现更高级的功能
 * <p>
 * 增加功能：
 * 1. 唯一任务标识
 * 2. 下节点任务 对象 缓存
 * 3. 上节点任务 标识 缓存
 * 4. GC回收通知器，通知 {@link MeshTask} 回收已完成的任务
 */
class MeshPointTask implements Runnable {

    /**
     * 当前任务对象缓存
     */
    private Task task;

    /**
     * 所有的 上节点任务 标示
     */
    @Nullable
    private List<String> prevTaskNameList;
    /**
     * 所有的 下节点任务 标示
     */
    @Nullable
    private List<String> nextTaskNameList;
    /**
     * 回收器，是 MeshTask 中回收器打的引用
     */
    private OnTaskCompleteListener onTaskCompleteListener;

    /**
     * 构造方法
     */
    MeshPointTask(@NonNull Task task) {
        this.task = task;
    }

    /**
     * 获取任务名
     */
    @Nullable
    public String getName() {
        return this.task.getName();
    }

    /**
     * 设置任务回收器
     *
     * @param onTaskCompleteListener 任务回收器
     */
    void setOnTaskCompleteListener(OnTaskCompleteListener onTaskCompleteListener) {
        this.onTaskCompleteListener = onTaskCompleteListener;
    }

    /**
     * 设置上级节点
     *
     * @param prevTaskTagList 所有上级节点的任务标示
     */
    void setPrevTaskNameList(@Nullable List<String> prevTaskTagList) {
        this.prevTaskNameList = prevTaskTagList;
    }
    /**
     * 移除上节点任务标识，表示上节点任务已经完成
     *
     * @return true: 所有上级节点任务都完成了，Task可运行
     */
    boolean removePrevTask(String taskName) {
        if (this.prevTaskNameList == null || this.prevTaskNameList.isEmpty()){
            return true;
        }
        this.prevTaskNameList.remove(taskName);
        return prevTaskNameList.isEmpty();
    }

    /**
     * 添加下级任务
     *
     * @param taskName 下节点任务名
     */
    void addNextPointTask(String taskName) {
        if (nextTaskNameList == null) {
            nextTaskNameList = new LinkedList<>();
        }
        nextTaskNameList.add(taskName);
    }

    /**
     * 获取所有下级节点任务标识
     */
    @Nullable
    List<String> getNextTaskNameList() {
        return this.nextTaskNameList;
    }


    @Override
    public void run() {

        do {
            LogUtil.v("Task【" + task.getName() + "】 to run.");
            // 提交任务去运行
            this.task.run();

            // 等待状态，每 0.1 秒检擦是否等待结束
            while (task.getFlag() == TaskFlag.WAIT) {
                LogUtil.v("Task【" + task.getName() + "】 wait ...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } while (this.task.getFlag() == TaskFlag.AGAIN);
        LogUtil.v("Task【" + task.getName() + "】 complete.");

        // 调起 Task Complete 回掉
        // 通知任务组 激活NextTask & 回收当前Task
        if (this.onTaskCompleteListener != null) {
            this.onTaskCompleteListener.onTaskComplete(task.getName());
        }
    }

    void destroy() {

        if (this.onTaskCompleteListener != null) {
            this.onTaskCompleteListener = null;
        }

        if (nextTaskNameList != null) {
            nextTaskNameList.clear();
            nextTaskNameList = null;
        }

        if (prevTaskNameList != null) {
            prevTaskNameList.clear();
            prevTaskNameList = null;
        }
    }

}
