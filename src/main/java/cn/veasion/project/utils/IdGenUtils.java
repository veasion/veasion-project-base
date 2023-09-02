package cn.veasion.project.utils;

/**
 * IdGenUtils
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
public class IdGenUtils {

    private static long workerId; // 机器ID（根据IP生成）
    private static long datacenterId = 1; // 数据中心ID（机房ID）
    private static Snowflake snowflake;

    static {
        try {
            workerId = ipv4ToLong(StringUtils.getLocalIp()) % 31;
        } catch (Exception e) {
            workerId = StringUtils.getLocalIp().hashCode() % 31;
        }
        snowflake = new Snowflake(workerId, datacenterId);
    }

    /**
     * 修改数据中心ID
     *
     * @param datacenterId 数据中心ID（机房ID），默认 1
     */
    public static void setDatacenterId(long datacenterId) {
        IdGenUtils.datacenterId = datacenterId;
        snowflake = new Snowflake(workerId, datacenterId);
    }

    /**
     * 雪花算法生成唯一ID
     */
    public static String genId() {
        return snowflake.nextIdStr();
    }

    public static long genLongId() {
        return snowflake.nextId();
    }

    private static long ipv4ToLong(String strIP) {
        if (strIP != null && strIP.contains(".")) {
            long[] ip = new long[4];
            int position1 = strIP.indexOf(".");
            int position2 = strIP.indexOf(".", position1 + 1);
            int position3 = strIP.indexOf(".", position2 + 1);
            ip[0] = Long.parseLong(strIP.substring(0, position1));
            ip[1] = Long.parseLong(strIP.substring(position1 + 1, position2));
            ip[2] = Long.parseLong(strIP.substring(position2 + 1, position3));
            ip[3] = Long.parseLong(strIP.substring(position3 + 1));
            return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
        } else {
            return 0L;
        }
    }

}
