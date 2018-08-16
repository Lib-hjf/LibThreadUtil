package org.hjf.thread;

import android.support.annotation.IntDef;

import org.hjf.threadx.LinkTask;
import org.hjf.threadx.MeshTask;
import org.hjf.threadx.Task;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.hjf.thread.RunnableFlag.AGAIN;
import static org.hjf.thread.RunnableFlag.NEXT;
import static org.hjf.thread.RunnableFlag.WAIT;

/**
 * 注解：RunnableFlag，
 * 任务组 {@link MeshTask} {@link LinkTask}等当 {@link Task#run()} 代码块执行完成后，下一步如何动作的标识.
 */
@IntDef({
        NEXT,
        WAIT,
        AGAIN
})
@Retention(RetentionPolicy.SOURCE)
public @interface RunnableFlag {
    /* 重新执行任务  */
    int AGAIN = 1;
    /* 等待下次标记  */
    int WAIT = 2;
    /* 执行下一个任务  */
    int NEXT = 3;
}
