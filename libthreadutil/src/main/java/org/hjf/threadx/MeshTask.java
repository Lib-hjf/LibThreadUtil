package org.hjf.threadx;

import android.support.annotation.NonNull;

import org.hjf.liblogx.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * “可交叉式多叉树结构” 异步任务，针对对处理后台线程的耗时操作：网络请求，数据库数据读取等
 * <p>
 * 使用场景：
 * <p>
 * A ------------> E【A】 ----> F【E&D】
 * --> D【A&C】 --
 * B --> C【B】 --
 * <p>
 * 【？】 为当前任务开始执行的前置条件，栗子：任务（D）必须等任务 A 和 C 完成后才能够执行
 * <p>
 * 如：从网络请求数据，本地数据库加载数据。合并后显示，先后显示都是可以的
 * <p>
 * <p>
 * 注意：与 ChainTask 不同的是： MeshTask 执行结束后立马销毁，不用等待此网状所有节点任务都完成任务；
 * 而 ChainTask 是等待链状任务中所有的任务完成后自己在销毁。
 * <p>
 * <p>
 * 支持：PointTask完成后自动销毁
 * 支持：失败重新运行，在 {@link Task#run()} 方法最后进行标记 {@link Task#setFlag(int)}
 * 支持：暂停、继续、取消等操作。 TODO -- 可以利用 allPointTask
 * 支持：网络任务断网保留，网络状态变化后重新拉取。 TODO -- 可以利用 allPointTask
 */
public final class MeshTask {

    static final String TOP_TASK_TAG = "TopPointTaskByAutoCreate";

    /**
     * 自动创建顶点任务，进行初始任务的分发
     */
    MeshPointTask topPointTask;

    /**
     * 记录添加过的任务节点
     */
    private HashMap<String, MeshPointTask> taskCache;

    /**
     * 任务是否已开始运行，开始运行了就不能添加任务，不能二次使用 execute 方法
     */
    private boolean isStarted = false;

    /**
     * 任务回收器，目前实现任务完成清除
     */
    private OnTaskCompleteListener onTaskCompleteListener = new OnTaskCompleteListener() {

        @Override
        public void onTaskComplete(String taskTag) {
            synchronized (MeshTask.this) {
                // 任务完成，激活此任务的下节点任务，并回收此任务
                if (taskCache != null && taskCache.containsKey(taskTag)) {
                    MeshPointTask pointTask = taskCache.get(taskTag);
                    // 激活下节点任务
                    notifyNextTask(pointTask);
                    // 回收Task
                    pointTask.destroy();
                    taskCache.remove(taskTag);
                    LogUtil.v("GC Task【" + taskTag + "】，remaining tasks： " + taskCache.size());
                }
            }
        }
    };


    MeshTask() {
        super();
        taskCache = new HashMap<>();
        initTopTask();
    }

    /**
     * 添加（次顶点）节点任务
     * 层次仅低于默认创建的 TopPointTask{@link MeshTask#topPointTask} 之下
     *
     * @param taskName 任务名
     * @param task 任务对象
     */
    public MeshTask addTask(String taskName, Task task) {
        return addTask(taskName, task, TOP_TASK_TAG);
    }

    /**
     * 添加节点任务
     *
     * @param taskName 任务名
     * @param sourceTask   任务对象
     * @param prevTaskTags 此节点任务所有的上级节点任务
     */
    public MeshTask addTask(String taskName, @NonNull Task sourceTask, String... prevTaskTags) {
        if (isStarted) {
            throw new RuntimeException("The MeshTask is already running, add new MeshPointTask is illegal operation.");
        }
        sourceTask.setName(taskName);
        // 包装 WrapTask
        MeshPointTask warpTask = new MeshPointTask(sourceTask);
        // 建立上、下级节点任务关系
        // Arrays.asList() 返回的是java.util.Arrays$ArrayList 不是 ArrayList，其对 add、remove 方法的默认实现就是抛出异常 UnsupportedOperationException
        List<String> prevTaskTagList = new ArrayList<>(Arrays.asList(prevTaskTags));
        // 已添加 上节点任务 添加 下节点任务标识
        for (String prevTaskTag : prevTaskTagList) {
            MeshPointTask prevTask = taskCache.get(prevTaskTag);
            if (prevTask == null) {
                throw new IllegalArgumentException(
                        "No pre task for " + prevTaskTag + " was found.\n" +
                                "You must add the " + prevTaskTag + " task before adding the " + warpTask.getName() + " task."
                );
            }
            prevTask.addNextPointTask(warpTask.getName());
        }
        // 当前任务设置所有上节点任务标识
        warpTask.setPrevTaskNameList(prevTaskTagList);
        // 设置任务完成回掉
        warpTask.setOnTaskCompleteListener(onTaskCompleteListener);
        // 添加进入任务缓冲
        taskCache.put(warpTask.getName(), warpTask);
        return MeshTask.this;
    }

    /**
     * 初始化顶点Task，为多顶点的多叉树增加个共同顶点，变为一个顶点的多叉树
     */
    private void initTopTask() {
        Task task = new Task() {
            @Override
            public void run() {
                setFlag(TaskFlag.NEXT);
            }
        };
        task.setName(TOP_TASK_TAG);
        topPointTask = new MeshPointTask(task);
        topPointTask.setOnTaskCompleteListener(onTaskCompleteListener);
        taskCache.put(topPointTask.getName(), topPointTask);
    }

    /**
     * 执行任务组
     */
    public void execute() {
        if (isStarted) {
            return;
        }
        isStarted = true;
        LogUtil.v("\n\n#############################");
        LogUtil.v("MeshTask 任务组启动 TopTask");
        TaskThreadPoolExecutor.getInstance().execute(topPointTask);
    }

    /**
     * 激活下节点任务
     *
     * @param task 已完成的 Task
     */
    private void notifyNextTask(@NonNull MeshPointTask task) {

        List<String> nextTaskNameList = task.getNextTaskNameList();
        if (nextTaskNameList == null || nextTaskNameList.isEmpty()) {
            return;
        }

        for (String nextTaskName : nextTaskNameList) {
            if (taskCache != null && taskCache.containsKey(nextTaskName)) {
                LogUtil.v("Task【" + task.getName() + "】 try notify Task【" + nextTaskName + "】");
                MeshPointTask nextPointTask = taskCache.get(nextTaskName);
                // 删除上级节点，如果所有上级节点都完成后，可运行
                if (nextPointTask.removePrevTask(task.getName())) {
                    TaskThreadPoolExecutor.getInstance().execute(nextPointTask);
                    LogUtil.v("Task【" + task.getName() + "】 notify success.");
                }
                // 还有上级节点任务在进行，不能运行
                else {
                    LogUtil.v("Task【" + task.getName() + "】 notify failed");
                }
            }
        }
    }
}
