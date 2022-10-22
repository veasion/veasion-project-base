package cn.veasion.project.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.IdUtil;

/**
 * IdGenUtils
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
public class IdGenUtils {

    private static long workerId; // 机器ID（根据IP生成）
    private static long datacenterId = 1; // 数据中心ID（机房ID）
    private static Snowflake snowflake = IdUtil.createSnowflake(workerId, datacenterId);

    static {
        try {
            workerId = NetUtil.ipv4ToLong(NetUtil.getLocalhostStr());
        } catch (Exception e) {
            workerId = NetUtil.getLocalhostStr().hashCode();
        }
    }

    /**
     * 修改数据中心ID
     *
     * @param datacenterId 数据中心ID（机房ID），默认 1
     */
    public static void setDatacenterId(long datacenterId) {
        IdGenUtils.datacenterId = datacenterId;
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

}
