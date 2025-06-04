package me.asu.jobs;

import me.asu.http.Application;
import me.asu.httpclient.SimpleHttpClient;
import me.asu.httpclient.SimpleHttpResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.asu.jobs.SimpleLog.error;
import static me.asu.jobs.SimpleLog.info;
import static me.asu.jobs.Streams.*;

public class Crontab {
    // 修改为服务端地址和端口 (通过配置)
    // 1. 当前目录
    // 2. $home/.config/crontab
    // 3. $home/.config/at
    // 4. $home/.config/cron.properties
    static final String start_cmd = "http://localhost:%d/reload";
    static final String start_stop = "http://localhost:%d/stop";
    static final Map<String, String> WEEK_MAP = new HashMap<>();
    static final Map<String, String> WEEK_FULL = new HashMap<>();
    static final Set<String> WEEKEND = new HashSet<>(Arrays.asList("1", "7"));
    static final Set<String> WORKDAY = new HashSet<>(Arrays.asList("2", "3", "4", "5", "6"));
    static {
        // Quartz: 1=Sun 2=Mon 3=Tue 4=Wed 5=Thu 6=Fri 7=Sat
        WEEK_MAP.put("sun", "1");
        WEEK_FULL.put("sunday", "1");
        WEEK_MAP.put("mon", "2");
        WEEK_FULL.put("monday", "2");
        WEEK_MAP.put("tue", "3");
        WEEK_FULL.put("tuesday", "3");
        WEEK_MAP.put("wed", "4");
        WEEK_FULL.put("wednesday", "4");
        WEEK_MAP.put("thu", "5");
        WEEK_FULL.put("thursday", "5");
        WEEK_MAP.put("fri", "6");
        WEEK_FULL.put("friday", "6");
        WEEK_MAP.put("sat", "7");
        WEEK_FULL.put("saturday", "7");
    }

    public static void main(String[] args) throws Exception {
        if (args != null && args.length > 0) {
            if ("help".equalsIgnoreCase(args[0])) {
                usage();
                System.exit(0);
            } else if ("list".equalsIgnoreCase(args[0]) || "ls".equalsIgnoreCase(args[0])) {
                list();
            } else if ("edit".equalsIgnoreCase(args[0]) || "edit".equalsIgnoreCase(args[0])) {
                edit();
            } else if ("reload".equalsIgnoreCase(args[0])) {
                reload();
            } else if ("stop".equalsIgnoreCase(args[0])) {
                stop();
            } else if ("add".equalsIgnoreCase(args[0])){
                parseAddCommand(Arrays.asList(args));
            } else if ("every".equalsIgnoreCase(args[0])){
                parseCronCommand(Arrays.asList(args));
            } else if ("at".equalsIgnoreCase(args[0])){
                parseAtCommand(Arrays.asList(args));
            } else if ("start".equalsIgnoreCase(args[0])) {
                start(args);
            } else {
                usage();
                System.exit(1);
            }

        } else {
            usage();
            System.exit(1);
        }
    }

    private static void start(String[] args) throws Exception {
        GUITools.createTrayIcon("alarm_icon.png", "Crontab Alarm");
        QuartzManager.reload();
        if (args != null && args.length > 0) {
            for (String arg : args) {
                if (arg.equals("--verbose") || arg.equals("-v")) {
                    SimpleLog.VERBOSE = true;
                    break;
                }
            }
        }

        int port = getWebServerPort();
        Application server = new Application(port);
        server.addRoute("/reload", (req, rsp) -> {
            try {
                QuartzManager.reload();
                rsp.send(200, "OK");
            } catch (Exception e) {
                rsp.send(500, "FAIL!\n" + e.getMessage());
            }
            return 0;
        }, "POST");
        server.addRoute("/stop", (req, rsp) -> {
            try {
                System.out.println("Stopping server...");
                rsp.send(200, "OK");
                rsp.getBody().flush();
            } finally {
                System.exit(0);
            }
            return 0;
        }, "POST");
        server.run();
    }

    private static void edit() {
//        SukProcess sukProcess = new SukProcess("cmd","/c","start " + Streams.CRONTAB_PATH);
        String editor = getEditor();
        SukProcess sukProcess = new SukProcess(editor, Streams.CRONTAB_PATH.toString());
//        SukProcess sukProcess = new SukProcess("D:\\green\\Notepad4\\notepad4.exe", Streams.CRONTAB_PATH.toString());
        sukProcess.run();
//        sukProcess.getExitCode();

    }

    private static void reload() {
        int port = getWebServerPort();
        final SimpleHttpClient simpleHttpClient = SimpleHttpClient.create(String.format(start_cmd, port));
        try {
            SimpleHttpResponse resp = simpleHttpClient.post().send();
            info("<<< ", resp.getContent());
        } catch (Exception e) {
            error("<<< ", e.getMessage());
        }
        // 使用 okjson 解析并美化 JSON 响应
        //JobResponse parsedResponse = OKJSON.stringToObject(response, JobResponse.class, 0);
        //System.out.println("响应: " + OKJSON.objectToString(parsedResponse, OKJSON.OPTIONS_PRETTY_FORMAT_ENABLE));
    }

    private static void stop() {
        int port = getWebServerPort();
        final SimpleHttpClient simpleHttpClient = SimpleHttpClient.create(String.format(start_stop, port));
        try {
            SimpleHttpResponse resp = simpleHttpClient.post().send();
            info("<<< ", resp.getContent());
        } catch (Exception e) {
            error("<<< ", e.getMessage());
        }
        // 使用 okjson 解析并美化 JSON 响应
        //JobResponse parsedResponse = OKJSON.stringToObject(response, JobResponse.class, 0);
        //System.out.println("响应: " + OKJSON.objectToString(parsedResponse, OKJSON.OPTIONS_PRETTY_FORMAT_ENABLE));
    }

    static void list() throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append("===================================================\n");
        List<String> cronList = getCronList();
        if (cronList != null) {
            sb.append("Cron:\n");
            sb.append("---------------------------------------------------\n");
            for (int i = 0; i < cronList.size(); i++) {
                String cron = cronList.get(i);
                sb.append(i + 1).append(".").append(cron).append("\n");
            }
            sb.append("\n\n");
        }


        List<String> atList = getAtList();
        if (atList != null) {
            sb.append("At:\n");
            sb.append("---------------------------------------------------\n");
            for (int i = 0; i < atList.size(); i++) {
                String cron = atList.get(i);
                sb.append(i + 1).append(".").append(cron).append("\n");
            }
        }
        sb.append("===================================================\n");
        System.out.println( sb.toString());
    }


    static void usage() {
        System.out.println("Usage: start | stop | reload | list | edit | help | add | every | at [arguments]");
    }

    /**
     * 解析 add 命令。
     * add CRON-EXPRESSION CMD-LIST
     */
    static void parseAddCommand(List<String> input) {
        if (input == null || input.size() < 2) {
            error("add CRON-EXPRESSION CMD-LIST ");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < input.size(); i++) {
            builder.append(input.get(i));
        }
        builder.append("\n");
        try {
            appendToCron(builder.toString());
            reload();
        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    static void parseCronCommand(List<String> input)  {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < input.size(); i++) {
                sb.append(input.get(i)).append(' ');
            }
            String cron = parseCron(sb.toString());
            appendToCron(cron + "\n");
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    static void parseAtCommand(List<String> input)  {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < input.size(); i++) {
                sb.append(input.get(i)).append(' ');
            }
            String at = parseAt(sb.toString());
            appendToAt(at + "\n");
        } catch (Exception e) {
          error(e.getMessage());
        }
    }

    // 支持简写和全拼
    static String parseWeekday(String s) {
        s = s.trim().toLowerCase();
        if (WEEK_MAP.containsKey(s)) return WEEK_MAP.get(s);
        if (WEEK_FULL.containsKey(s)) return WEEK_FULL.get(s);
        throw new IllegalArgumentException("非法星期: " + s);
    }

    // 用于at ... on ...场景，返回day数字列表
    static List<Integer> parseDayExprToDayList(String expr) {
        expr = expr.trim().toLowerCase();
        expr = expr.replaceAll("\\s+", "");
        if ("workday".equals(expr)) return Arrays.asList(2, 3, 4, 5, 6);
        if ("weekend".equals(expr)) return Arrays.asList(1, 7);
        List<Integer> days = new ArrayList<>();
        for (String part : expr.split(",")) {
            if (part.contains("-")) {
                String[] range = part.split("-");
                int from = Integer.parseInt(parseWeekday(range[0]));
                int to = Integer.parseInt(parseWeekday(range[1]));
                for (int i = from; i <= to; i++) days.add(i);
            } else {
                days.add(Integer.parseInt(parseWeekday(part)));
            }
        }
        return days;
    }

    // 将 Mon,Wed 转 2,4；Mon-Thu 转 2-5；支持简写和全拼
    static String parseDayExprToCron(String dayExpr) {
        dayExpr = dayExpr.trim().toLowerCase();
        dayExpr = dayExpr.replaceAll("\\s+", "");
        if ("workday".equals(dayExpr)) return "2-6";
        if ("weekend".equals(dayExpr)) return "1,7";
        String[] segs = dayExpr.split(",");
        List<String> out = new ArrayList<>();
        for (String seg : segs) {
            if (seg.contains("-")) {
                String[] range = seg.split("-");
                int from = Integer.parseInt(parseWeekday(range[0]));
                int to = Integer.parseInt(parseWeekday(range[1]));
                out.add(from + "-" + to);
            } else {
                out.add(parseWeekday(seg));
            }
        }
        return String.join(",", out);
    }


    static int parseMonthStr(String month) {
        String m = month.trim().toLowerCase();
        //        String[] mArr = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
        //        for (int i = 0; i < mArr.length; i++) {
        //            if (m.equals(mArr[i])) return i + 1;
        //        }
        // 支持全拼
        switch (m) {
            case "january":
            case "jan":
                return 1;
            case "feb":
            case "february":
                return 2;
            case "march":
            case "mar":
                return 3;
            case "apr":
            case "april":
                return 4;
            case "jun":
            case "june":
                return 6;
            case "jul":
            case "july":
                return 7;
            case "aug":
            case "august":
                return 8;
            case "sep":
            case "september":
                return 9;
            case "oct":
            case "october":
                return 10;
            case "nov":
            case "november":
                return 11;
            case "dec":
            case "december":
                return 12;
        }
        throw new IllegalArgumentException("未知月份: " + month);
    }

    /**
     * <table>
     *  <thead><tr><td>优先级</td><td>格式示例（最特殊→最泛化）</td><td>正则说明</td><td>备注/示例</td></tr></thead>
     *  <tbody>
     *      <tr>
     *        <td>1</td><td>at HH:mm on MM-dd CMD</td><td>at (\d{1,2}):(\d{2}) on (\d{2})-(\d{2}) ...	</td><td>at 08:00 on 12-31 echo hello</td>
     *      </tr>
     *      <tr>
     *          <td>2</td><td>at HH:mm on MMM dd CMD</td><td>at (\d{1,2}):(\d{2}) on ([a-zA-Z]{3,9}) (\d{1,2}) ...</td><td>at 08:00 on Dec 31 echo hello</td>
     *      </tr>
     *      <tr>
     *          </td><td>3</td><td>at HH:mm on weekend CMD</td><td>at (\d{1,2}):(\d{2}) on weekend ...</td><td>at 08:00 on weekend echo hello</td>
     *      </tr>
     *      <tr>
     *          </td><td>3</td><td>at HH:mm on workday CMD</td><td>at (\d{1,2}):(\d{2}) on workday ...</td><td>at 08:00 on workday echo hello</td>
     *      </tr>
     *      <tr>
     *          <td>4</td><td>at HH:mm on Mon-Thu CMD</td><td>at (\d{1,2}):(\d{2}) on ([a-zA-Z,\\-]+) ...</td><td>at 08:00 on Mon-Fri echo hello</td>
     *      </tr>
     *     <tr>
     *          <td>5</td><td>at HH:mm CMD</td><td>at (\d{1,2}):(\d{2}) ...</td><td>at 08:00 echo hello (仅时间)</td>
     *     </tr>
     *  </tbody>
     * </table>
     */
    public static String parseAt(String input) {
        input = input.trim();
        // -------- 1)  at HH:mm on MM-dd CMD (顺延到明年) --------
        {
            Pattern p2b = Pattern.compile("^at\\s+(\\d{1,2}):(\\d{2})\\s+on\\s+(\\d{1,2})[-/.](\\d{1,2})\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher m2b = p2b.matcher(input);
            if (m2b.matches()) {
                int hour = Integer.parseInt(m2b.group(1));
                int min = Integer.parseInt(m2b.group(2));
                int mm = Integer.parseInt(m2b.group(3));
                int dd = Integer.parseInt(m2b.group(4));
                String cmd = m2b.group(5).trim();

                LocalDate today = LocalDate.now();
                int year = today.getYear();
                LocalDate targetDate = LocalDate.of(year, mm, dd);
                LocalDateTime dt = targetDate.atTime(hour, min);
                if (dt.isBefore(LocalDateTime.now())) {
                    // 顺延到明年
                    dt = LocalDate.of(year + 1, mm, dd).atTime(hour, min);
                }
                return dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " " + cmd;
            }
        }
        // -------- 2) at HH:mm on MMM dd CMD (顺延到明年) --------
        {
            Pattern p2c = Pattern.compile("^at\\s+(\\d{1,2}):(\\d{2})\\s+on\\s+([a-zA-Z]{3,9})\\s+(\\d{1,2})\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher m2c = p2c.matcher(input);
            if (m2c.matches()) {
                int hour = Integer.parseInt(m2c.group(1));
                int min = Integer.parseInt(m2c.group(2));
                String monthStr = m2c.group(3);
                int dayOfMonth = Integer.parseInt(m2c.group(4));
                String cmd = m2c.group(5).trim();

                int mm = parseMonthStr(monthStr);
                LocalDate today = LocalDate.now();
                int year = today.getYear();
                LocalDate targetDate = LocalDate.of(year, mm, dayOfMonth);
                LocalDateTime dt = targetDate.atTime(hour, min);
                if (dt.isBefore(LocalDateTime.now())) {
                    // 顺延到明年
                    dt = LocalDate.of(year + 1, mm, dayOfMonth).atTime(hour, min);
                }
                return dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " " + cmd;
            }
        }

        // -------- 3) at HH:mm on <weekday list> CMD --------
        {
            Pattern p4 = Pattern.compile("^at\\s+(\\d{1,2}):(\\d{2})\\s+on\\s+([a-zA-Z]{3,9})\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher m4 = p4.matcher(input);
            if (m4.matches()) {
                int hour = Integer.parseInt(m4.group(1));
                int min = Integer.parseInt(m4.group(2));
                String dayExpr = m4.group(3);
                String cmd = m4.group(4).trim();
                List<Integer> target = parseDayExprToDayList(dayExpr);
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime next = null;
                for (int i = 0; i < 7; i++) {
                    LocalDateTime candidate = now.toLocalDate().plusDays(i).atTime(hour, min);
                    int day = candidate.getDayOfWeek().getValue() % 7 + 1;
                    if (candidate.isAfter(now) && target.contains(day)) {
                        next = candidate;
                        break;
                    }
                }
                if (next == null) throw new IllegalArgumentException("找不到未来一周的目标日期");
                return next.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " " + cmd;
            }
        }

        // -------- 4) at HH:mm CMD --------
        {
            Pattern p1 = Pattern.compile("^at\\s+(\\d{1,2}):(\\d{2})\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher m1 = p1.matcher(input);
            if (m1.matches()) {
                int hour = Integer.parseInt(m1.group(1));
                int min = Integer.parseInt(m1.group(2));
                String cmd = m1.group(3).trim();
                LocalDate today = LocalDate.now();
                LocalDateTime dt = today.atTime(hour, min);
                if (dt.isBefore(LocalDateTime.now())) dt = dt.plusDays(1);
                return dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " " + cmd;
            }
        }

        throw new IllegalArgumentException("Unsupported command: " + input);
    }

    /**
     * Cron 命令解析正则优先级表
     * 优先级越高，正则在代码中应越靠前检查。
     * <table>
     *  <thead><tr><td>优先级</td><td>格式示例（最特殊→最泛化）</td><td>正则说明</td><td>备注/示例</td></tr></thead>
     *  <tbody>
     *      <tr>
     *          <td>6</td><td>every month on Mon:1 at HH:mm CMD</td><td>every month on ([a-zA-Z]+):(\d|L) at (\d{1,2}):(\d{2}) ...</td><td>every month on Mon:1 at 9:30 echo hello</td>
     *      </tr>
     *      <tr>
     *          <td>7</td><td>every month on Last at HH:mm CMD</td><td>every month on last at (\d{1,2}):(\d{2}) ...</td><td>every month on Last at 09:00 ...</td>
     *      </tr>
     *      <tr>
     *          <td>7</td><td>every month on First at HH:mm CMD</td><td>every month on first at (\d{1,2}):(\d{2}) ...</td><td>every month on First at 09:00 ...</td>
     *      </tr>
     *      <tr>
     *          <td>8</td><td>every month on DD at HH:mm CMD</td><td>every month on (\d{1,2}) at (\d{1,2}):(\d{2}) ...</td><td>every month on 5 at 09:00 ...</td>
     *      </tr>
     *      <tr>
     *          <td>9</td><td>every weekend at HH:mm CMD</td><td>every weekend at (\d{1,2}):(\d{2}) ...</td><td>every weekend at 09:00 ...</td>
     *      </tr>
     *      <tr>
     *          <td>9</td><td>every workday at HH:mm CMD</td><td>every workday at (\d{1,2}):(\d{2}) ...</td><td>every workday at 09:00 ...</td>
     *      </tr>
     *      <tr>
     *          <td>10</td><td>every Mon,Wed at HH:mm CMD  every Mon-Thu at HH:mm CMD</td><td>every ([a-zA-Z,\\-]+) at (\d{1,2}):(\d{2}) ...</td><td>every Mon,Wed at 09:00 ...</td>
     *      </tr>
     *      <tr>
     *          <td>10</td><td>every Mon at HH:mm CMD</td><td>every ([a-zA-Z]+) at (\d{1,2}):(\d{2}) ...</td><td>every Mon at 09:00 ...</td>
     *      </tr>
     *      <tr>
     *          <td>11</td><td>every day at HH:mm CMD</td><td>every day at (\d{1,2}):(\d{2}) ...</td><td>every day at 09:00 ...</td>
     *      </tr>
     *      <tr>
     *          <td>12</td><td>every N hours CMD</td><td>every (\d{1,2}) hours? ...</td><td>every 3 hours echo hi</td>
     *      </tr>
     *      <tr>
     *          <td>13</td><td>every hour CMD</td><td>every hour ...</td><td>every hour echo hi</td>
     *     </tr>
     *     <tr>
     *          <td>14</td><td>every N minutes CMD</td><td>every (\d{1,2}) minutes? ...</td><td>every 5 minutes echo hi</td>
     *     </tr>
     *     <tr>
     *          <td>15</td><td>every minute CMD</td><td>every minute ...</td><td>every minute echo hi</td>
     *     </tr>
     *  </tbody>
     * </table>
     */
    public static String parseCron(String input) {
        input = input.trim();
        // -------- 6) every month on Mon:1 at HH:mm CMD (第一个周一) --------
        {
            Pattern p16 = Pattern.compile("^every\\s+month\\s+on\\s+([a-zA-Z]+):(\\d|L)\\s+at\\s+(\\d{1,2}):(\\d{2})\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher m16 = p16.matcher(input);
            if (m16.matches()) {
                String weekday = m16.group(1).toLowerCase();
                String which = m16.group(2).toUpperCase(); // 1/2/L
                int hour = Integer.parseInt(m16.group(3));
                int min = Integer.parseInt(m16.group(4));
                String weekNum;
                if ("l".equals(which)) {
                    weekNum = "L";
                } else {
                    weekNum = "#" + which;
                }
                String weekdayNum = parseWeekday(weekday);
                String dow = weekNum.equals("L") ? (weekdayNum + "L") : (weekdayNum + weekNum);
                return String.format("0 %d %d ? * %s %s", min, hour, dow, m16.group(5).trim());
            }
        }

        // -------- 7) every month on Last at HH:mm CMD --------
        {
            Pattern p15 = Pattern.compile("^every\\s+month\\s+on\\s+last\\s+at\\s+(\\d{1,2}):(\\d{2})\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher m15 = p15.matcher(input);
            if (m15.matches()) {
                int hour = Integer.parseInt(m15.group(1));
                int min = Integer.parseInt(m15.group(2));
                return String.format("0 %d %d L * ? %s", min, hour, m15.group(3).trim());
            }
        }

        // -------- 7)  every month on First at HH:mm CMD --------
        {
            Pattern p14 = Pattern.compile("^every\\s+month\\s+on\\s+first\\s+at\\s+(\\d{1,2}):(\\d{2})\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher m14 = p14.matcher(input);
            if (m14.matches()) {
                int hour = Integer.parseInt(m14.group(1));
                int min = Integer.parseInt(m14.group(2));
                return String.format("0 %d %d 1 * ? %s", min, hour, m14.group(3).trim());
            }
        }

        // -------- 8) every month on DD at HH:mm CMD --------
        {
            Pattern p13 = Pattern.compile("^every\\s+month\\s+on\\s+(\\d{1,2})\\s+at\\s+(\\d{1,2}):(\\d{2})\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher m13 = p13.matcher(input);
            if (m13.matches()) {
                int dd = Integer.parseInt(m13.group(1));
                int hour = Integer.parseInt(m13.group(2));
                int min = Integer.parseInt(m13.group(3));
                return String.format("0 %d %d %d * ? %s", min, hour, dd, m13.group(4).trim());
            }
        }

        // -------- 9) every day at HH:mm CMD --------
        {
            Pattern p9 = Pattern.compile("^every\\s+day\\s+at\\s+(\\d{1,2}):(\\d{2})\\s+(.+)$", Pattern.CASE_INSENSITIVE);

            Matcher m9 = p9.matcher(input);
            if (m9.matches()) {
                int hour = Integer.parseInt(m9.group(1));
                int min = Integer.parseInt(m9.group(2));
                return String.format("0 %d %d * * ? %s", min, hour, m9.group(3).trim());
            }
        }

        // -------- 10) every weekend at HH:mm CMD --------
        {
            Pattern p11 = Pattern.compile("^every\\s+weekend\\s+at\\s+(\\d{1,2}):(\\d{2})\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher m11 = p11.matcher(input);
            if (m11.matches()) {
                int hour = Integer.parseInt(m11.group(1));
                int min = Integer.parseInt(m11.group(2));
                return String.format("0 %d %d ? * 1,7 %s", min, hour, m11.group(3).trim());
            }
        }

        // -------- 10) every Workday at HH:mm CMD --------
        {
            Pattern p12 = Pattern.compile("^every\\s+workday\\s+at\\s+(\\d{1,2}):(\\d{2})\\s+(.+)$", Pattern.CASE_INSENSITIVE);

            Matcher m12 = p12.matcher(input);
            if (m12.matches()) {
                int hour = Integer.parseInt(m12.group(1));
                int min = Integer.parseInt(m12.group(2));
                return String.format("0 %d %d ? * 2-6 %s", min, hour, m12.group(3).trim());
            }
        }

        // -------- 11) every (Mon|Mon,Wed|Mon-Thu) at HH:mm CMD --------
        {
            Pattern p10 = Pattern.compile("^every\\s+(\\b(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)(?:day)?(?:-(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)(?:day)?)?\\b)\\s+at\\s+(\\d{1,2}):(\\d{2})\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher m10 = p10.matcher(input);
            if (m10.matches()) {
                String dayExpr = m10.group(1);
                int hour = Integer.parseInt(m10.group(2));
                int min = Integer.parseInt(m10.group(3));
                String days = parseDayExprToCron(dayExpr);
                return String.format("0 %d %d ? * %s %s", min, hour, days, m10.group(4).trim());
            }
        }

        // -------- 12) every N hours CMD --------
        {
            Pattern p8 = Pattern.compile("^every\\s+(\\d{1,2})\\s+hours?\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher m8 = p8.matcher(input);
            if (m8.matches()) {
                int n = Integer.parseInt(m8.group(1));
                return String.format("0 0 0/%d * * ? %s", n, m8.group(2).trim());
            }
        }

        // -------- 13) every hour CMD --------
        {
            Pattern p7 = Pattern.compile("^every\\s+hour\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher m7 = p7.matcher(input);
            if (m7.matches()) {
                return  "0 0 * * * ? " + m7.group(1).trim();
            }
        }

        // --------  14) every N minutes CMD --------
        {
            Pattern p6 = Pattern.compile("^every\\s+(\\d{1,2})\\s+minutes?\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher m6 = p6.matcher(input);
            if (m6.matches()) {
                int n = Integer.parseInt(m6.group(1));
                return String.format("0 0/%d * * * ? %s", n, m6.group(2).trim());
            }
        }

        // -------- 15)  every minute CMD --------
        {
            Pattern p5 = Pattern.compile("^every\\s+minute\\s+(.+)$", Pattern.CASE_INSENSITIVE);
            Matcher m5 = p5.matcher(input);
            if (m5.matches()) {
                return "0 * * * * ? " + m5.group(1).trim();
            }
        }

        throw new IllegalArgumentException("Unsupported command: " + input);
    }

}
