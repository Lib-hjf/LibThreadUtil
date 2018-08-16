package org.hjf.thread;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import org.hjf.log.LogUtil;

/**
 * 链式任务，按添加顺序往下执行
 */
public class LinkTask {

    private Handler handler = new Handler(Looper.getMainLooper());
    /**
     * 上个 Task 的唯一标识
     */
    private String agoTaskTag = MeshTask.TOP_TASK_TAG;

    /**
     * “可交叉式多叉树结构” 异步任务
     */
    private MeshTask meshTask = new MeshTask();

    /**
     * 自增变量，用于Runnable name
     */
    private int runnableIndex = 0;

    public LinkTask() {
    }

    /**
     * 首次添加：上节点任务 =  {@link MeshTask#topPointTask} 自动生成的.
     * 后续添加：上节点任务 = 上次添加的任务
     */
    public LinkTask addRunnable(Runnable runnable) {
        String taskName = String.valueOf(++runnableIndex);
        meshTask.addRunnable(taskName, runnable, agoTaskTag);
        agoTaskTag = taskName;
        return this;
    }

    /**
     * 首次添加：上节点任务 =  {@link MeshTask#topPointTask} 自动生成的.
     * 后续添加：上节点任务 = 上次添加的任务
     */
    public LinkTask addRunnableInUIThread(final Runnable runnable) {
        String taskName = String.valueOf(++runnableIndex);
        RunPoint runPoint = new MainThreadRunPoint(taskName, runnable, handler);
        meshTask.addRunPoint(runPoint, agoTaskTag);
        agoTaskTag = taskName;
        return this;
    }


    /**
     * 执行
     */
    public void execute() {
        meshTask.execute();
    }


    /**
     * add ui thread function
     */
    private static class MainThreadRunPoint extends RunPoint {

        private Handler mainHandler;

        MainThreadRunPoint(@NonNull String runnableName, @NonNull Runnable runnable, @NonNull Handler handler) {
            super(runnableName, runnable);
            this.mainHandler = handler;
        }

        @Override
        protected void onRun() {
            setFlag(RunnableFlag.WAIT);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    LogUtil.v("MainThreadRunPoint{0} to run.", MainThreadRunPoint.super.getName());
                    MainThreadRunPoint.super.runnable.run();
                    LogUtil.v("MainThreadRunPoint{0} complete.", MainThreadRunPoint.super.getName());
                    MainThreadRunPoint.super.setFlag(RunnableFlag.NEXT);
                }
            });
        }

        @Override
        protected void destroy() {
            super.destroy();
            mainHandler = null;
        }
    }
}
