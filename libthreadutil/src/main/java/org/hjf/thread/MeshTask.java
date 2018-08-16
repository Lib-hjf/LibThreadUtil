package org.hjf.thread;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import org.hjf.log.LogUtil;
import org.hjf.threadx.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * “可交叉式多叉树结构” 异步进程，针对对处理后台线程的耗时操作：网络请求，数据库数据读取等
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
    private RunPoint topPointTask;

    /**
     * 记录添加过的任务节点
     */
    private HashMap<String, RunPoint> runPointCache = new HashMap<>();

    /**
     * 任务是否已开始运行，开始运行了就不能添加任务，不能二次使用 execute 方法
     */
    private boolean isStarted = false;

    /**
     * 任务回收器，目前实现任务完成清除
     */
    private OnRunnableCompleteListener onTaskCompleteListener = new OnRunnableCompleteListener() {

        @Override
        public void onRunnableCompleteListener(String taskTag) {
            synchronized (MeshTask.this) {
                // 任务完成，激活此任务的下节点任务，并回收此任务
                if (runPointCache != null && runPointCache.containsKey(taskTag)) {
                    RunPoint pointTask = runPointCache.get(taskTag);
                    // 激活下节点任务
                    notifyNextTask(pointTask);
                    // 回收Task
                    pointTask.destroy();
                    runPointCache.remove(taskTag);
                    LogUtil.v("GC Runnable {0}, remaining runnable num = {1}", taskTag, runPointCache.size());
                }
            }
        }
    };


    public MeshTask() {
        super();
        initTopRunnable();
    }

    /**
     * 初始化顶点Task，为多顶点的多叉树增加个共同顶点，变为一个顶点的多叉树
     */
    private void initTopRunnable() {
        topPointTask = new RunPoint(TOP_TASK_TAG, new Runnable() {
            @Override
            public void run() {
            }
        });
        topPointTask.setOnRunnableCompleteListener(onTaskCompleteListener);
        runPointCache.put(topPointTask.getName(), topPointTask);
    }


    /**
     * 添加（次顶点）节点任务
     * 层次仅低于默认创建的 TopPointTask{@link MeshTask#topPointTask} 之下
     *
     * @param runnableName runnable name
     * @param runnable     {@link java.lang.Runnable}
     */
    public MeshTask addRunnable(String runnableName, @NonNull Runnable runnable) {
        return this.addRunnable(runnableName, runnable, TOP_TASK_TAG);
    }

    /**
     * 添加节点任务
     *
     * @param runnableName     任务名
     * @param runnable         任务对象
     * @param preRunnableNames 此节点任务所有的上级节点任务
     */
    public MeshTask addRunnable(String runnableName, @NonNull Runnable runnable, String... preRunnableNames) {
        if (isStarted) {
            throw new RuntimeException("The MeshTask is already running, add new RunPoint is illegal operation.");
        }
        RunPoint runPoint = new RunPoint(runnableName, runnable);
        addRunPoint(runPoint, preRunnableNames);
        return MeshTask.this;
    }

    /**
     * put run point into cache
     *
     * @param runPoint         {@link RunPoint}
     * @param preRunnableNames front run point name
     */
    void addRunPoint(@NonNull RunPoint runPoint, String... preRunnableNames) {
        this.connectRunPoint(runPoint, preRunnableNames);
        this.runPointCache.put(runPoint.getName(), runPoint);
    }


    /**
     * connect front and back run point.
     *
     * @param runPoint         {@link RunPoint}
     * @param preRunnableNames front run point name
     */
    private void connectRunPoint(@NonNull RunPoint runPoint, String... preRunnableNames) {
        // Arrays.asList() 返回的是java.util.Arrays$ArrayList 不是 ArrayList，其对 add、remove 方法的默认实现就是抛出异常 UnsupportedOperationException
        List<String> preRunnableNameList = new ArrayList<>(Arrays.asList(preRunnableNames));
        // 建立前置任务与当前任务的连接关系
        for (String preRunnableName : preRunnableNameList) {
            RunPoint preRunnable = runPointCache.get(preRunnableName);
            if (preRunnable == null) {
                throw new IllegalArgumentException("PreRunnable " + preRunnableName + " not found.\n" +
                        "You must add the " + preRunnableName + " Runnable before adding the Runnable " + runPoint.getName() + "."
                );
            }
            preRunnable.addNextRunnableName(runPoint.getName());
        }
        // 当前任务设置所有上节点任务标识
        runPoint.setPreRunnableNameList(preRunnableNameList);
        // 设置任务完成回调
        runPoint.setOnRunnableCompleteListener(onTaskCompleteListener);
    }


    /**
     * 激活下节点任务
     *
     * @param meshRunnablePoint 已完成的 Task
     */
    private void notifyNextTask(@NonNull RunPoint meshRunnablePoint) {

        List<String> nextRunnableNameList = meshRunnablePoint.getNextRunnableNameList();
        if (nextRunnableNameList == null || nextRunnableNameList.isEmpty()) {
            return;
        }

        for (String nextRunnableName : nextRunnableNameList) {
            if (runPointCache != null && runPointCache.containsKey(nextRunnableName)) {
                LogUtil.v("Runnable[{0}] try notify Runnable[{1}]", meshRunnablePoint.getName(), nextRunnableName);
                RunPoint nextPoint = runPointCache.get(nextRunnableName);
                // 删除上级节点，如果所有上级节点都完成后，可运行
                if (nextPoint.removePreRunnableByName(meshRunnablePoint.getName())) {
                    LogUtil.v("Runnable[{0}] notify success.", meshRunnablePoint.getName());
                    ThreadPoolExecutor.getInstance().submit(nextPoint);
                }
                // 还有上级节点任务在进行，不能运行
                else {
                    LogUtil.v("Runnable[{0}] notify failed", meshRunnablePoint.getName());
                }
            }
        }
    }

    @WorkerThread
    public void execute() {
        MeshTask.this.isStarted = true;
        ThreadPoolExecutor.getInstance().submit(MeshTask.this.topPointTask);
    }
}
