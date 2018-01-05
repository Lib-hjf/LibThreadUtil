package org.hjf.threadx;

import android.support.annotation.NonNull;

import org.hjf.liblogx.LogUtil;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Runnable 默认在后台线程 {@link android.os.Process#THREAD_PRIORITY_BACKGROUND} 运行
 * 一个线程池大了影响资源，小了影响系统吞吐量。一般综合考虑CPU数量，内存的大小，并发请求的数量考虑，按需调整。
 * 使用线程池：可复用、减少创建销毁次数，减轻GC回收器压力，避免消耗过多内存，一个线程大约需要1MB；
 * 还可以控制并发数、执行定时等自定义要求
 */
class TaskThreadPoolExecutor extends ThreadPoolExecutor {

    private static TaskThreadPoolExecutor pauseThreadPoolExecutor;

    /**
     * @param corePoolSize    核心线程数，即使空闲也存活。
     *                        【核心线程:3】，当前【线程池线程数:1】且空闲，来新的任务后会新建线程而不是复用。
     *                        核心线程在allowCoreThreadTimeout被设置为true时会超时退出
     *                        建议根据根据确定
     * @param maximumPoolSize 最大线程数。
     *                        当线程数大于或等于核心线程，且任务队列已满时，线程池会创建新的线程，直到线程数量达到maxPoolSize。
     *                        如果线程数已等于maxPoolSize，且任务队列已满，则已超出线程池的处理能力，线程池会拒绝处理任务而抛出异常。
     *                        设置大一点
     * @param keepAliveTime   保持活性时间。多余的空闲线程在超过这个时间后没有任务会被销毁
     * @param unit            keepAliveTime 的单位
     * @param workQueue       任务队列。线程池功能的不同归根到底还是内部的 BlockingQueue 实现不同。队列长度设置过大，会导致任务响应时间过长，
     *                        栗子：PriorityBlockingQueue 优先级队列 【队列容量：11，根据有限度调用】
     *                        若将队列长度设置为Integer.MAX_VALUE，导致线程数量永远为corePoolSize 不增加，当任务数量陡增时，任务响应时间也将随之陡增。
     *                        注意不要使用：new LinkedBlockingQueue() (队列容量：this(Integer.MAX_VALUE))
     *                        另：BlockingQueue ，自动实现了多线程环境中的等待与唤醒。共提供五种：
     *                        -- ArrayBlockingQueue ：生产者和消费者共用一个锁，不能真正并行执行
     *                        -- LinkedBlockingQueue ：  生产者和消费者各自单独有锁，能真正并行执行
     *                        -- DelayQueue ：延时获取队列元素，插入数据（生产者）不会阻塞，获取数据操作（消费者）才会阻塞
     *                        -- PriorityBlockingQueue ：优先级阻塞队列，任务需实现 Compator 接口。永不阻塞生产者，只有队列无数据时阻塞消费者。
     *                        -- SynchronousQueue ：无缓冲队列。
     * @param threadFactory   线程工厂，按需调整。比如设置为后台线程
     * @param handler         拒绝策略。在线程池已关闭的情况下，或人物太多导致最大线程数并且任务队列已饱和，会拒绝execute提交的任务，默认是抛出 RejectedExecutionHandler异常
     */
    private TaskThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    static TaskThreadPoolExecutor getInstance() {
        if (pauseThreadPoolExecutor == null) {
            synchronized (TaskThreadPoolExecutor.class) {
                if (pauseThreadPoolExecutor == null) {
                    // Java 虚拟机 可用的 CPU 数量
                    int availableProcessors = Runtime.getRuntime().availableProcessors();
                    pauseThreadPoolExecutor = new TaskThreadPoolExecutor(
                            // 核心线程数，即使空闲也存活
                            availableProcessors + 1,
                            // 最大线程数
                            availableProcessors * 2 + 1,
                            // 保持活性时间，非核心线程对象空闲时间超过后，会回收内存
                            45,
                            // 保持活性时间单位
                            TimeUnit.SECONDS,
                            // 任务队列模式，这里是有限度队列，
                            new PriorityBlockingQueue<Runnable>(availableProcessors * 2),
                            // 线程工厂类
                            Executors.defaultThreadFactory(),
                            // 拒绝策略，使用默认的抛出异常
                            new AbortPolicy()
                    );
                }
            }
        }
        return TaskThreadPoolExecutor.pauseThreadPoolExecutor;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {

        // 默认在后台线程中运行
//        int defaultThreadPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND;
        int defaultThreadPriority = 5;

        /*if (r instanceof Task) {
            Task task = (Task) r;
            defaultThreadPriority = ((Task) r).getLevel();
            LogUtil.v("Thread[" + t.getName() + "] perform task[" + task.getName() + "].");
        } else {
            LogUtil.v("Thread[" + t.getName() + "] perform runnable.");
        }*/

        // Thread 是复用的，减少调度次数
        if (t.getPriority() != defaultThreadPriority) {
            LogUtil.v(t.getName() + " priority 【" + t.getPriority() + "】 ===> " + "【" + defaultThreadPriority + "】");
            android.os.Process.setThreadPriority(defaultThreadPriority);
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        // 若执行的是自定义的 Task，获取 Task 指定的 ThreadPriority
        /*if (r instanceof Task) {
            Task task = (Task) r;
            LogUtil.v("Task[" + task.getName() + "] complete.");
        } else {
            LogUtil.v("Runnable complete.");
        }*/
    }

    @Override
    protected void terminated() {
        LogUtil.v("TaskThreadPoolExecutor terminated.");
    }


    /**
     * @param isNow 是否立马销毁线程池
     */
    static void destroy(boolean isNow) {
        if (pauseThreadPoolExecutor != null) {
            synchronized (TaskThreadPoolExecutor.class) {
                if (pauseThreadPoolExecutor != null) {
                    // 立马销毁会返回未执行的任务
                    if (isNow) {
                        pauseThreadPoolExecutor.shutdownNow();
                    }
                    // 等待执行所有任务后自动销毁
                    else {
                        pauseThreadPoolExecutor.shutdown();
                    }
                    pauseThreadPoolExecutor = null;
                }
            }
        }
    }


    /**
     * ThreadPoolExecutor 的 PriorityBlockingQueue 支持问题
     * 由于{@link TaskThreadPoolExecutor}} 采用了 PriorityBlockingQueue 优先级队列，所以提交的 Runnable 需要实现 Comparable 接口
     * 但是，你还是会发现报错：FutureTask cannot be cast to java.lang.Comparable
     * 是因为调用 {@link TaskThreadPoolExecutor#submit(Runnable)} 方法的时候
     * 会调用 {@link ThreadPoolExecutor#newTaskFor(Callable)} 方法将 Runnable 转换为 FutureTask，然而 FutureTask 没有并实现 Comparable
     * <p>
     * 拓展：FutureTask 也是个 Runnable，运行我们指定的 Runnable 的流程如下
     * FutureTask.run() --> Callable.call() --> MyRunnable.run()
     * 1. FutureTask 是我们使用 ThreadPool.submit() --> AbstractExecutorService.newTaskFor(runnable, null) 进行包装的一个 Task，此方法复写了
     * 2. Callable 是在 FutureTask(Runnable myRunnable, V result) 构造方法中调用 Executors.callable(myRunnable, result) 合成的
     * 实现类是 RunnableAdapter
     * 3. RunnableAdapter.run() 中 进行了 myRunnable.run() 的操作
     * <p>
     * 解决办法：继承 {@link FutureTask}类并实现{@link Comparable}接口，重写 {@link ThreadPoolExecutor#newTaskFor(Callable)} 方法
     * <p>
     * 总结：慎用 ThreadPool.submit()，使用 ThreadPool.execute()。因为后续还会遇到以下问题（正对本项目类）：
     * 1. ComparableFutureTask cannot cast to Task. 报错地址：{@link PriorityBlockingQueue}类的 siftUpComparable() 方法
     * 原因推测：对比排序时转换类别错误
     */
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new ComparableFutureTask<>(callable);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new ComparableFutureTask<>(runnable, value);
    }

    private class ComparableFutureTask<V> extends FutureTask<V> implements Comparable<ComparableFutureTask<V>> {

        private Object object;


        ComparableFutureTask(Callable<V> callable) {
            super(callable);
            this.object = callable;
        }

        ComparableFutureTask(Runnable runnable, V result) {
            this(Executors.callable(runnable, result));
        }

        @Override
        public int compareTo(@NonNull ComparableFutureTask<V> other) {
            if (this == other) {
                return 0;
            }
            // 比较优先度。只关注我们定义的任务 Task
            if (this.object != null) {
                if (this.object instanceof Task && other.object instanceof Task) {
                    return ((Task) this.object).compareTo((Task) other.object);
                }
            }
            return 0;
        }
    }
}