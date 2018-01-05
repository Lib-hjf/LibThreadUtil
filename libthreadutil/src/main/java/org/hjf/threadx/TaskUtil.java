package org.hjf.threadx;


/**
 * 任务管理类
 */
public class TaskUtil {

    /**
     * 执行 Runnable
     */
    public static void execute(Runnable runnable) {
        TaskThreadPoolExecutor.getInstance().execute(runnable);
    }

    /**
     * 执行任务，在 {@link android.os.Process#THREAD_PRIORITY_BACKGROUND} 线程中运行
     */
    public static void execute(Task task) {
        task.setLevel(TaskLevel.DEFAULT);
        TaskThreadPoolExecutor.getInstance().execute(task);
    }

    /**
     * 执行任务，在 {@link android.os.Process#THREAD_PRIORITY_BACKGROUND} 线程中运行
     *
     * @param level 任务在线程池中的优先度
     * @param task       任务 对象
     */
    public static void execute(@TaskLevel int level, Task task) {
        task.setLevel(level);
        TaskThreadPoolExecutor.getInstance().execute(task);
    }

    public static LinkTask createLinkTask(){
        return new LinkTask();
    }

    /**
     * 创建 “可交叉式多叉树结构” 异步任务对象
     * 使用场景举列：
     * A ------------> E【A】 ----> F【E&D】
     * --> D【A&C】 --
     * B --> C【B】 --
     * <p>
     * 【？】 为当前任务开始执行的前置条件，栗子：任务（D）必须等任务 A 和 C 完成后才能够执行
     *
     * @return “可交叉式多叉树结构” 任务对象/
     */
    public static MeshTask createMeshTask() {
        return new MeshTask();
    }

    /**
     * 关闭线程池，当退出程序时
     *
     * @param isNow 是否立马关闭，抛弃队列中的任务
     */
    public static void closeTaskPool(boolean isNow) {
        TaskThreadPoolExecutor.destroy(isNow);
    }

}
