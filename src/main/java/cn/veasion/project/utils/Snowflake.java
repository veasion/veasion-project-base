package cn.veasion.project.utils;

import java.io.Serializable;
import java.util.Date;

/**
 * Snowflake
 *
 * @author luozhuowei
 * @date 2023/8/30
 */
public class Snowflake implements Serializable {

    private final long twepoch;
    private final long workerIdBits;
    private final long dataCenterIdBits;
    private final long maxWorkerId;
    private final long maxDataCenterId;
    private final long sequenceBits;
    private final long workerIdShift;
    private final long dataCenterIdShift;
    private final long timestampLeftShift;
    private final long sequenceMask;
    private final long workerId;
    private final long dataCenterId;
    private long sequence;
    private long lastTimestamp;

    public Snowflake(long workerId, long dataCenterId) {
        this((Date) null, workerId, dataCenterId);
    }

    public Snowflake(Date epochDate, long workerId, long dataCenterId) {
        this.workerIdBits = 5L;
        this.dataCenterIdBits = 5L;
        this.maxWorkerId = 31L;
        this.maxDataCenterId = 31L;
        this.sequenceBits = 12L;
        this.workerIdShift = 12L;
        this.dataCenterIdShift = 17L;
        this.timestampLeftShift = 22L;
        this.sequenceMask = 4095L;
        this.sequence = 0L;
        this.lastTimestamp = -1L;
        if (null != epochDate) {
            this.twepoch = epochDate.getTime();
        } else {
            this.twepoch = 1288834974657L;
        }

        if (workerId <= 31L && workerId >= 0L) {
            if (dataCenterId <= 31L && dataCenterId >= 0L) {
                this.workerId = workerId;
                this.dataCenterId = dataCenterId;
            } else {
                throw new IllegalArgumentException("datacenter Id can't be greater than 31 or less than 0");
            }
        } else {
            throw new IllegalArgumentException("worker Id can't be greater than 31 or less than 0");
        }
    }

    public long getWorkerId(long id) {
        return id >> 12 & 31L;
    }

    public long getDataCenterId(long id) {
        return id >> 17 & 31L;
    }

    public long getGenerateDateTime(long id) {
        return (id >> 22 & 2199023255551L) + this.twepoch;
    }

    public synchronized long nextId() {
        long timestamp = this.genTime();
        if (timestamp < this.lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards. Refusing to generate id for " + (this.lastTimestamp - timestamp) + "ms");
        } else {
            if (this.lastTimestamp == timestamp) {
                this.sequence = this.sequence + 1L & 4095L;
                if (this.sequence == 0L) {
                    timestamp = this.tilNextMillis(this.lastTimestamp);
                }
            } else {
                this.sequence = 0L;
            }

            this.lastTimestamp = timestamp;
            return timestamp - this.twepoch << 22 | this.dataCenterId << 17 | this.workerId << 12 | this.sequence;
        }
    }

    public String nextIdStr() {
        return Long.toString(this.nextId());
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp;
        for (timestamp = this.genTime(); timestamp <= lastTimestamp; timestamp = this.genTime()) {
        }
        return timestamp;
    }

    private long genTime() {
        return System.currentTimeMillis();
    }

}