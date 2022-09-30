package cn.veasion.project.service;

import cn.veasion.project.model.SysLogVO;

/**
 * SysLogService
 *
 * @author luozhuowei
 * @date 2022/9/30
 */
public interface SysLogService {

    void asyncSaveLog(SysLogVO logVO);

}
