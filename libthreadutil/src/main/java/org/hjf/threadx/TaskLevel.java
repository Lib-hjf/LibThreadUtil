package org.hjf.threadx;


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import static org.hjf.threadx.TaskLevel.MIN;
import static org.hjf.threadx.TaskLevel.LOW;
import static org.hjf.threadx.TaskLevel.DEFAULT;
import static org.hjf.threadx.TaskLevel.HIGH;
import static org.hjf.threadx.TaskLevel.MAX;

/**
 * 注解：RunnableLevel，表明任务的【优先等级】，谷歌不推荐使用枚举
 */

@IntDef({
        MIN,
        LOW,
        DEFAULT,
        HIGH,
        MAX
})
@Retention(RetentionPolicy.SOURCE)
public @interface TaskLevel {
    /* [00] 最低等级 */
    int MIN = 0;
    /* [10] 较低等级 */
    int LOW = 10;
    /* [20] 默认等级 */
    int DEFAULT = 20;
    /* [30] 较高等级 */
    int HIGH = 30;
    /* [40] 最高等级 */
    int MAX = 40;
}
