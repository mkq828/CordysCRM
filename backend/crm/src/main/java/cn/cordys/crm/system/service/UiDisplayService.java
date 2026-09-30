package cn.cordys.crm.system.service;

import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.crm.system.dto.request.UiDisplaySaveRequest;
import cn.cordys.crm.system.dto.request.UploadTransferRequest;
import cn.cordys.crm.system.dto.response.UiDisplayParamResponse;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 界面设置服务：登录页 / 平台主页的图标、logo、背景、slogan 等展示配置。
 * <p>
 * 参数按租户隔离存储于 sys_parameter，key 形如 {@code ui.loginLogo.{orgId}}；
 * 文件类型参数走附件体系（临时转存为正式附件），参数值存附件 ID。
 */
@Service
public class UiDisplayService {

    private static final String UI_PREFIX = "ui.";

    @Resource
    private BaseMapper<Parameter> parameterMapper;
    @Resource
    private BaseMapper<Attachment> attachmentMapper;
    @Resource
    private AttachmentService attachmentService;

    /**
     * 查询当前租户的界面配置。
     *
     * @param orgId 组织ID
     */
    public List<UiDisplayParamResponse> info(String orgId) {
        // 登录页在未登录状态下读取，无组织上下文，回退到平台默认组织
        if (StringUtils.isBlank(orgId)) {
            orgId = OrganizationContext.DEFAULT_ORGANIZATION_ID;
        }
        String suffix = "." + orgId;
        List<UiDisplayParamResponse> result = new ArrayList<>();
        List<Parameter> all = parameterMapper.selectAll(null);
        if (all == null) {
            return result;
        }
        for (Parameter parameter : all) {
            String paramKey = parameter.getParamKey();
            if (StringUtils.isBlank(paramKey) || !paramKey.startsWith(UI_PREFIX) || !paramKey.endsWith(suffix)) {
                continue;
            }
            String key = paramKey.substring(UI_PREFIX.length(), paramKey.length() - suffix.length());
            UiDisplayParamResponse response = new UiDisplayParamResponse();
            response.setParamKey(UI_PREFIX + key);
            response.setParamValue(parameter.getParamValue());
            response.setType(parameter.getType());
            // 文件类型回显真实文件名
            if ("file".equals(parameter.getType()) && StringUtils.isNotBlank(parameter.getParamValue())) {
                Attachment attachment = attachmentMapper.selectByPrimaryKey(parameter.getParamValue());
                response.setFileName(attachment == null ? null : attachment.getName());
            }
            result.add(response);
        }
        return result;
    }

    /**
     * 保存界面配置。
     *
     * @param items      保存项（前端只传有变化的 file 项 + 全部 text 项）
     * @param files      新上传的文件（文件名带 {@code ui.xxx,} 前缀标识归属参数）
     * @param operatorId 操作人
     */
    public void save(List<UiDisplaySaveRequest> items, List<MultipartFile> files, String operatorId) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<String, MultipartFile> fileMap = new HashMap<>();
        if (files != null) {
            for (MultipartFile file : files) {
                String name = file.getOriginalFilename();
                if (StringUtils.isNotBlank(name)) {
                    int index = name.indexOf(',');
                    if (index > 0) {
                        fileMap.put(name.substring(0, index), file);
                    }
                }
            }
        }

        for (UiDisplaySaveRequest item : items) {
            String storageKey = storageKey(item.getParamKey(), item.getOrganizationId());
            if ("file".equals(item.getType())) {
                if (Boolean.TRUE.equals(item.getHasFile())) {
                    MultipartFile file = fileMap.get(item.getParamKey());
                    if (file != null) {
                        MultipartFile upload = rename(file, item.getFileName());
                        String tempId = attachmentService.uploadTemp(List.of(upload)).get(0);
                        // 以该参数为 resourceId 转存为正式附件（替换旧的）
                        attachmentService.processTemp(
                                new UploadTransferRequest(item.getOrganizationId(), storageKey, operatorId, List.of(tempId)));
                        setParam(storageKey, tempId, "file");
                    }
                } else if (Boolean.TRUE.equals(item.getOriginal())) {
                    removeParam(storageKey);
                }
            } else {
                setParam(storageKey, item.getParamValue(), "text");
            }
        }
    }

    /**
     * 预览文件类型参数（如图标、logo）。
     */
    public ResponseEntity<org.springframework.core.io.Resource> preview(String paramKey, String orgId) {
        if (StringUtils.isBlank(orgId)) {
            orgId = OrganizationContext.DEFAULT_ORGANIZATION_ID;
        }
        Parameter parameter = parameterMapper.selectByPrimaryKey(storageKey(paramKey, orgId));
        if (parameter == null || StringUtils.isBlank(parameter.getParamValue())) {
            return null;
        }
        return attachmentService.getResource(parameter.getParamValue());
    }

    private String storageKey(String paramKey, String orgId) {
        String key = paramKey;
        if (key != null && key.startsWith(UI_PREFIX)) {
            key = key.substring(UI_PREFIX.length());
        }
        return UI_PREFIX + key + "." + orgId;
    }

    /**
     * 用真实文件名重新包装文件，去掉前端 {@code ui.xxx,} 前缀。
     */
    private MultipartFile rename(MultipartFile file, String realName) {
        String name = realName;
        if (StringUtils.isBlank(name)) {
            String original = file.getOriginalFilename();
            if (StringUtils.isNotBlank(original)) {
                int index = original.indexOf(',');
                if (index > 0) {
                    name = original.substring(index + 1);
                }
            }
        }
        final String fileName = StringUtils.isBlank(name) ? file.getOriginalFilename() : name;
        return new MultipartFile() {
            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getName() {
                return file.getName();
            }

            @Override
            public String getContentType() {
                return file.getContentType();
            }

            @Override
            public boolean isEmpty() {
                return file.isEmpty();
            }

            @Override
            public long getSize() {
                return file.getSize();
            }

            @Override
            public byte[] getBytes() throws IOException {
                return file.getBytes();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return file.getInputStream();
            }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                file.transferTo(dest);
            }
        };
    }

    private void setParam(String key, String value, String type) {
        parameterMapper.deleteByPrimaryKey(key);
        Parameter parameter = new Parameter();
        parameter.setParamKey(key);
        parameter.setParamValue(value);
        parameter.setType(type);
        parameterMapper.insert(parameter);
    }

    private void removeParam(String key) {
        Parameter parameter = parameterMapper.selectByPrimaryKey(key);
        if (parameter != null && StringUtils.isNotBlank(parameter.getParamValue())) {
            attachmentService.delete(parameter.getParamValue());
        }
        parameterMapper.deleteByPrimaryKey(key);
    }
}
