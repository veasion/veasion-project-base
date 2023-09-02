package cn.veasion.project.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * DateUtils
 *
 * @author luozhuowei
 */
public class DateUtils {

    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static String format(LocalDateTime localDateTime, String format) {
        if (localDateTime != null && !StringUtils.isBlank(format)) {
            DateTimeFormatter df = DateTimeFormatter.ofPattern(format);
            return localDateTime.format(df);
        } else {
            return null;
        }
    }

    public static String format(Date date, String format) {
        if (date != null && !StringUtils.isBlank(format)) {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return format(date, sdf);
        } else {
            return null;
        }
    }

    public static String format(Date date, DateFormat format) {
        return null != format && null != date ? format.format(date) : null;
    }

    public static String format(Date date, DateTimeFormatter format) {
        return null != format && null != date ? format.format(date.toInstant()) : null;
    }

    public static String formatDateTime(Date date) {
        return new SimpleDateFormat(DATE_TIME_PATTERN).format(date);
    }

    public static String formatDate(Date date) {
        return null == date ? null : new SimpleDateFormat(DATE_PATTERN).format(date);
    }

    public static Date parseDateTime(String dateStr) {
        return parse(dateStr, DATE_TIME_PATTERN);
    }

    public static Date parseDate(String dateStr) {
        return parse(dateStr, DATE_PATTERN);
    }

    public static Date parse(String dateStr, String dateFormat) {
        try {
            return new SimpleDateFormat(dateFormat).parse(dateStr);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static Date addDays(Date date, int days) {
        if (date == null) {
            return null;
        }
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        instance.add(Calendar.DAY_OF_MONTH, days);
        return instance.getTime();
    }

    public static Date addHour(Date date, int hour) {
        if (date == null) {
            return null;
        }
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        instance.add(Calendar.HOUR_OF_DAY, hour);
        return instance.getTime();
    }

    public static String getStartDate(Date date) {
        if (date == null) {
            return null;
        }
        return DateUtils.format(date, "yyyy-MM-dd") + " 00:00:00";
    }

    public static String getStartDate(String date) {
        if (date == null) {
            return null;
        }
        if (date.contains(":")) {
            return date.split(" ")[0] + " 00:00:00";
        } else {
            return date + " 00:00:00";
        }
    }

    public static Date modifyHHmmss(Date date, int hour, int minute, int second) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        instance.set(Calendar.HOUR_OF_DAY, hour);
        instance.set(Calendar.MINUTE, minute);
        instance.set(Calendar.SECOND, second);
        instance.set(Calendar.MILLISECOND, 0);
        return instance.getTime();
    }

    public static String getEndDate(Date date) {
        if (date == null) {
            return null;
        }
        return DateUtils.format(date, "yyyy-MM-dd") + " 23:59:59";
    }

    public static String getEndDate(String date) {
        if (date == null) {
            return null;
        }
        if (date.contains(":")) {
            return date.split(" ")[0] + " 23:59:59";
        } else {
            return date + " 23:59:59";
        }
    }

    public static Date simpleParse(String date) {
        if (date == null) {
            return null;
        }
        if (!date.contains("-") && date.matches("\\d+")) {
            return new Date(Long.parseLong(date));
        }
        return date.contains(":") ? parse(date, "yyyy-MM-dd HH:mm:ss") : parse(date, "yyyy-MM-dd");
    }

    public static String dateStr(Date date, String defFormat, boolean defWeek) {
        if (date == null) {
            return null;
        }
        Date today = new Date();
        String dateStr = format(date, "yyyy-MM-dd");
        if (dateStr.equals(format(today, "yyyy-MM-dd"))) {
            return "今天";
        } else if (date.getTime() < today.getTime() &&
                dateStr.equals(format(addDays(today, -1), "yyyy-MM-dd"))) {
            return "昨天";
        } else if (date.getTime() > today.getTime() &&
                dateStr.equals(format(addDays(today, 1), "yyyy-MM-dd"))) {
            return "明天";
        } else if (defWeek) {
            return format(date, defFormat) + " " + week(date);
        } else {
            return format(date, defFormat);
        }
    }

    public static String getDurationStr(long duration) {
        if (duration < 3600_000) {
            return (int) (duration / 60_000) + "分" + (int) (duration % 60_000 / 1000) + "秒";
        }
        if (duration < 86400000) {
            return (int) (duration / 3600_000) + "小时" + (int) (duration % 3600_000 / 60_000) + "分";
        }
        return (int) (duration / 86400000) + "天" + (int) (duration % 86400000 / 3600_000) + "小时" + (int) (duration % 3600_000 / 60_000) + "分";
    }

    public static String week(Date date) {
        switch (weekOfDay(date)) {
            case 1:
                return "周一";
            case 2:
                return "周二";
            case 3:
                return "周三";
            case 4:
                return "周四";
            case 5:
                return "周五";
            case 6:
                return "周六";
            case 7:
                return "周日";
        }
        return null;
    }

    public static int weekOfDay(Date date) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        int week = instance.get(Calendar.DAY_OF_WEEK);
        week = week - 1;
        if (week == 0) {
            return 7;
        }
        return week;
    }

    public static int hour(Date date, boolean is24Hour) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        return instance.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
    }

    public static int minute(Date date) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        return instance.get(Calendar.MINUTE);
    }

    public static int days(String startDate, String endDate) {
        return days(simpleParse(startDate), simpleParse(endDate));
    }

    public static int days(Date startDate, Date endDate) {
        return (int) ((endDate.getTime() - startDate.getTime() - 1) / 86400000) + 1;
    }

}
