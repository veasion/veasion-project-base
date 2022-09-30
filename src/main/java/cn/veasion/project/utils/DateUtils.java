package cn.veasion.project.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;

import java.util.Calendar;
import java.util.Date;

/**
 * DateUtils
 *
 * @author luozhuowei
 */
public class DateUtils extends DateUtil {

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

    public static String week(Date date) {
        Week week = dayOfWeekEnum(date);
        switch (week) {
            case SUNDAY:
                return "周日";
            case MONDAY:
                return "周一";
            case TUESDAY:
                return "周二";
            case WEDNESDAY:
                return "周三";
            case THURSDAY:
                return "周四";
            case FRIDAY:
                return "周五";
            case SATURDAY:
                return "周六";
        }
        return null;
    }

    public static int days(String startDate, String endDate) {
        return days(simpleParse(startDate), simpleParse(endDate));
    }

    public static int days(Date startDate, Date endDate) {
        return (int) ((endDate.getTime() - startDate.getTime() - 1) / 86400000) + 1;
    }

}
