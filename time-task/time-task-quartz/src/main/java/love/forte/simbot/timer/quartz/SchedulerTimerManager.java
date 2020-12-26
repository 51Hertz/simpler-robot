/*
 *
 *  * Copyright (c) 2020. ForteScarlet All rights reserved.
 *  * Project  component-onebot
 *  * File     QuartzMutableTimerManager.java
 *  *
 *  * You can contact the author through the following channels:
 *  * github https://github.com/ForteScarlet
 *  * gitee  https://gitee.com/ForteScarlet
 *  * email  ForteScarlet@163.com
 *  * QQ     1149159218
 *  *
 *  *
 *
 */

package love.forte.simbot.timer.quartz;

import love.forte.common.ioc.DependBeanFactory;
import love.forte.simbot.LogAble;
import love.forte.simbot.exception.ExceptionProcessor;
import love.forte.simbot.timer.*;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 {@link org.quartz.Scheduler} 的定时任务管理器。
 * @author ForteScarlet
 */
public class SchedulerTimerManager implements MutableTimerManager {

    private final Scheduler scheduler;
    private final DependBeanFactory dependBeanFactory;
    private final ExceptionProcessor exceptionProcessor;

    static final String TASK_KEY = "task";
    static final String LOG_KEY = "logger";
    static final String B_F_KEY = "dependBeanFactory";
    static final String E_P_KEY = "exceptionProcessor";
    static final String GROUP = "simbot-task";

    public SchedulerTimerManager(Scheduler scheduler, DependBeanFactory dependBeanFactory, ExceptionProcessor exceptionProcessor) {
        this.scheduler = scheduler;
        this.dependBeanFactory = dependBeanFactory;
        this.exceptionProcessor = exceptionProcessor;
    }

    private final WeakHashMap<Task, Job> weakTaskJob = new WeakHashMap<>(8);


    /**
     * 添加/注册一个 task。首次任务立即执行。
     *
     * @param task task
     * @return 是否添加成功。如果失败，
     * @throws IllegalArgumentException 如果ID已经存在。
     * @throws IllegalStateException    {@link Task#cycle()} 解析错误。
     * @throws TimerException 添加到调度器失败。
     */
    @Override
    public boolean addTask(Task task) {
        return addTask(task, 0);
    }

    /**
     * 添加/注册一个 task，并延迟指定时间后执行。
     *
     * @param task  task
     * @param delay 延迟时间。
     * @return 是否添加成功。
     * @throws IllegalArgumentException 如果ID已经存在。
     * @throws IllegalStateException    {@link Task#cycle()} 解析错误。
     * @throws TimerException 添加到调度器失败。
     */
    @Override
    public boolean addTask(Task task, long delay) {

        // 已经存在此task
        if (weakTaskJob.containsKey(task)) {
            throw new IllegalArgumentException("Duplicate task " + task);
        }

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(TASK_KEY, task);
        jobDataMap.put(B_F_KEY, dependBeanFactory);
        jobDataMap.put(E_P_KEY, exceptionProcessor);

        Logger logger;
        if (task instanceof LogAble) {
            logger = ((LogAble) task).getLog();
        } else {
            logger = LoggerFactory.getLogger("love.forte.simbot.timer[" + task.id() + "]");
        }

        jobDataMap.put(LOG_KEY, logger);

        JobDetail job = JobBuilder.newJob(QuartzJob.class)
                .setJobData(jobDataMap)
                .withIdentity(task.id(), GROUP)
                .withDescription(task.name()).build();

        // 判断是否为周期时间
        CycleType cycleType = task.cycleType();

        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                .forJob(job)
                .withIdentity("tri_" + job.getKey().getName(), job.getKey().getGroup());

        if (delay > 0) {
            triggerBuilder.startAt(new Date(System.currentTimeMillis() + delay));
        } else {
            triggerBuilder.startNow();
        }

        Trigger trigger;
        long repeat;

        switch (cycleType) {
            case FIXED:
                long millFixed;
                if (task instanceof FixedTask) {
                    FixedTask fixedTask = (FixedTask) task;
                    millFixed = fixedTask.timeUnit().toMillis(fixedTask.duration());
                } else {
                    millFixed = Long.parseLong(task.cycle());
                }

                SimpleScheduleBuilder fixedScheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(millFixed);

                repeat = task.repeat();
                if (repeat > 0) {
                    fixedScheduleBuilder.withRepeatCount((int) repeat);
                } else {
                    fixedScheduleBuilder.repeatForever();
                }
                trigger = triggerBuilder.withSchedule(fixedScheduleBuilder).build();

                break;
            case CRON:
                String cron;
                if (task instanceof CronTask) {
                    CronTask cronTask = (CronTask) task;
                    cron = cronTask.cron();
                } else {
                    cron = task.cycle();
                }

                CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
                trigger = triggerBuilder.withSchedule(cronScheduleBuilder).build();

                break;


            default: throw new IllegalStateException("未知异常-schedulerTimerManager");
        }


        try {
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            throw new TimerException("", e);
        }

        return true;
    }

    /**
     * 移除/停止一个任务。
     * 移除的同时会尝试终止此任务之后的执行。
     *
     * @param id ID
     * @return 如果存在任务，返回被终止的任务，否则返回 {@code null}。
     */
    @Override
    public Task removeTask(String id) {
        return null;
    }

    /**
     * 获取当前定时任务中的任务列表。
     *
     * @return 当前已注册的定时任务列表。
     */
    @Override
    public Collection<? extends Task> taskList() {
        return null;
    }

    /**
     * 根据ID获取一个对应的task实例。
     *
     * @param id task id
     * @return 如果存在，返回task实例，否则得到 {@code null}。
     */
    @Override
    public Task getTask(String id) {
        return null;
    }
}