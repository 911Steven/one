package com.lcw.one.file.rest;

import com.lcw.one.file.service.SysFileEOService;
import com.lcw.one.file.store.FileStoreFactory;
import com.lcw.one.sys.entity.SysFileEO;
import com.lcw.one.util.exception.OneBaseException;
import com.lcw.one.util.http.ResponseMessage;
import com.lcw.one.util.http.Result;
import com.lcw.one.util.utils.cipher.Encodes;
import com.lcw.one.util.utils.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping(value = "/${restPath}/sys/file")
@Api(description = "系统 - 文件")
public class SysFileDownloadRestController {

    private static final Logger logger = LoggerFactory.getLogger(SysFileDownloadRestController.class);

    @Autowired
    private SysFileEOService sysFileEOService;

    @Autowired
    private FileStoreFactory fileStoreFactory;

    @ApiOperation("获取文件信息")
    @GetMapping("/{fileId}")
    public ResponseMessage<SysFileEO> getById(@PathVariable String fileId) {
        return Result.success(sysFileEOService.get(fileId));
    }

    @ApiOperation("下载文件(内部)")
    @GetMapping("/{fileId}/download")
    public void downFile(HttpServletResponse response, @PathVariable String fileId, String fileName) {
        if (StringUtils.isEmpty(fileId)) {
            throw new OneBaseException("FileId不能为空");
        }

        SysFileEO sysFileEO = sysFileEOService.get(fileId);
        if (sysFileEO == null) {
            throw new OneBaseException("FileId[" + fileId + "]不存在");
        }

        try {
            if (StringUtils.isEmpty(fileName)) {
                fileName = sysFileEO.getFileName();
            }
            response.setHeader("Content-Disposition", "attachment; filename=" + Encodes.urlEncode(fileName + "." + sysFileEO.getFileType()));
            response.setContentType(sysFileEO.getContentType());

            fileStoreFactory.instance(sysFileEO.getStoreType()).loadFile(sysFileEO.getSavePath(), response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new OneBaseException("下载文件失败，请重试");
        }
    }

    @ApiOperation("下载文件(外部)")
    @GetMapping("/{fileId}/download-ext")
    public void downFile(HttpServletResponse response, @PathVariable String fileId) {
        SysFileEO sysFileEO = sysFileEOService.get(fileId);
        if (sysFileEO != null && sysFileEO.getPermissionType() == 1) {
            downFile(response, fileId, null);
        } else {
            throw new OneBaseException("文件不存在");
        }
    }

}
