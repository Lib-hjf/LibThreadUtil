package org.hjf.threadx;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.hjf.threadx.TaskFlag.AGAIN;
import static org.hjf.threadx.TaskFlag.NEXT;
import static org.hjf.threadx.TaskFlag.WAIT;

/**
 * 注解：TaskFlag，
 * 任务组 {@link MeshTask} {@link LinkTask}等当 {@link Task#run()} 代码块执行完成后，下一步如何动作的标识.
 */
@IntDef({
        NEXT,
        WAIT,
        AGAIN
})
@Retention(RetentionPolicy.SOURCE)
public @interface TaskFlag {
    /* 重新执行任务  */
    int AGAIN = 1;
    /* 等待下次标记  */
    int WAIT = 2;
    /* 执行下一个任务  */
    int NEXT = 3;
}
