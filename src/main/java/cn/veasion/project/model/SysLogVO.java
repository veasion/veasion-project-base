package cn.veasion.project.model;

import java.util.Date;

/**
 * SysLogVO
 *
 * @author luozhuowei
 * @date 2022/9/30
 */
public class SysLogVO implements ICompanyId {

    /**
     * 操作描述
     */
    private String description;
    /**
     * 日志级别类型
     */
    private String logType;
    /**
     * 类和方法
     */
    private String method;
    /**
     * 请求参数
     */
    private String params;
    /**
     * 请求IP
     */
    private String requestIp;
    /**
     * 耗时（毫秒）
     */
    private Long time;
    /**
     * 操作用户
     */
    private String username;
    /**
     * IP地址
     */
    private String address;
    /**
     * 浏览器
     */
    private String browser;
    /**
     * 异常信息
     */
    private String exceptionDetail;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 公司ID
     */
    private Long companyId;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public void setRequestIp(String requestIp) {
        this.requestIp = requestIp;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getExceptionDetail() {
        return exceptionDetail;
    }

    public void setExceptionDetail(String exceptionDetail) {
        this.exceptionDetail = exceptionDetail;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
}
