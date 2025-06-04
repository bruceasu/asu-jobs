package me.asu.jobs;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static me.asu.jobs.SimpleLog.*;
import static me.asu.jobs.Streams.getAtList;
import static me.asu.jobs.Streams.getCronList;

public class QuartzManager {
    static Scheduler scheduler;

    static {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // reload 的实现
    public static void reload() throws Exception {
        List<String> cronLines = getCronList();
        List<String> atLines = getAtList();

        QuartzManager.reload(cronLines, atLines);
        info("Crontab reloaded.");
    }


    public static void reload(List<String> cronList, List<String> atList) throws Exception {
        scheduler.clear();
        int count = 0;
        for (String line : cronList) {
            debug("[cron] ", line);

            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            //  秒 分 时 日 月 星期 命令 参数里列表
            String[] parts = line.split("\\s+", 7);
            if (parts.length != 7) {
                error("Invalid cron expression: ", line);
                continue;
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                builder.append(parts[i]).append(" ");
            }
            builder.setLength(builder.length() - 1);
            String cron = builder.toString();
            builder.setLength(0);
            for (int i = 6; i < parts.length; i++) {
                builder.append(parts[i]).append(" ");
            }
            builder.setLength(builder.length() - 1);
            String cmd = builder.toString();

            JobDetail job = JobBuilder.newJob(ShellJob.class)
                    .withIdentity("job" + count++, "group1")
                    .usingJobData("cmd", cmd)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                    .build();
            scheduler.scheduleJob(job, trigger);
        }

        for (String line : atList) {
            debug("[at]", line);
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+", 2);
            LocalDateTime dt = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            if (dt.isBefore(LocalDateTime.now())) {
                debug("[at] " + dt + " is expired.");
                continue;
            }
            String cmd = parts[1];
            JobDetail job = JobBuilder.newJob(ShellJob.class)
                    .withIdentity("job" + count++, "group1")
                    .usingJobData("cmd", cmd)
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .startAt(java.util.Date.from(dt.atZone(ZoneId.systemDefault()).toInstant()))
                    .build();
            scheduler.scheduleJob(job, trigger);
        }
    }

    public static class ShellJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            String cmd = context.getJobDetail().getJobDataMap().getString("cmd");
            try {
                debug("Run: " + cmd);
                Process proc = Runtime.getRuntime().exec(cmd);
                proc.waitFor();
            } catch (Exception e) {
               error(e.getMessage());
            }
        }
    }
}
