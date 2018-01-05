package org.hjf.threadx;

/**
 * MeshTask 任务回收器
 * Task 完成后，断掉 Task 对象在内存中的引用链
 */
interface OnTaskCompleteListener {

    void onTaskComplete(String taskTag);
}
