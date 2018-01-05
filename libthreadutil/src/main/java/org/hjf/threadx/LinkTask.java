package org.hjf.threadx;

/**
 * 链式任务，按添加顺序往下执行
 */
public class LinkTask {

    /**
     * 上个 Task 的唯一标识
     */
    private String agoTaskTag = MeshTask.TOP_TASK_TAG;

    /**
     * “可交叉式多叉树结构” 异步任务
     */
    private MeshTask meshTask = new MeshTask();

    private int taskIndex = 0;

    LinkTask() {
    }

    /**
     * 首次添加：上节点任务 =  {@link MeshTask#topPointTask} 自动生成的.
     * 后续添加：上节点任务 = 上次添加的任务
     */
    public LinkTask addTask(Task task) {
        String taskName = String.valueOf(++taskIndex);
        meshTask.addTask(taskName, task, agoTaskTag);
        agoTaskTag = taskName;
        return this;
    }

    public void execute() {
        meshTask.execute();
    }
}
