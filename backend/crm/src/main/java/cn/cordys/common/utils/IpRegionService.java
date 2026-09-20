package cn.cordys.common.utils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * IP 归属地解析服务。基于 ip2region 离线库，将客户端 IP 解析为城市（如「广州市」）。
 * <p>
 * 仅加载 IPv4 库（约 11MB，BufferCache 全量进内存，线程安全）；IPv6 登录会返回 null。
 */
@Slf4j
@Component
public class IpRegionService {

    private static final String IP2REGION_DB = "ip2region/ip2region_v4.xdb";

    private Ip2Region ip2Region;

    @PostConstruct
    public void init() {
        try (InputStream in = new ClassPathResource(IP2REGION_DB).getInputStream()) {
            Config config = Config.custom()
                    .setCachePolicy(Config.BufferCache)
                    .setXdbInputStream(in)
                    .asV4();
            this.ip2Region = Ip2Region.create(config, null);
        } catch (Exception e) {
            log.error("加载 ip2region 数据失败，登录城市解析不可用", e);
            this.ip2Region = null;
        }
    }

    /**
     * 解析 IP 所在城市。返回例如「广州市」；库未加载、IP 非法或无法定位时返回 null。
     */
    public String searchCity(String ip) {
        if (ip2Region == null || StringUtils.isBlank(ip)) {
            return null;
        }
        try {
            String region = ip2Region.search(ip.trim());
            if (StringUtils.isBlank(region)) {
                return null;
            }
            // 格式：国家|省份|城市|ISP|国家码，例如「中国|广东省|广州市|电信|CN」
            String[] parts = region.split("\\|", -1);
            String city = parts.length >= 3 ? parts[2] : null;
            if (isUsableCity(city)) {
                return city;
            }
            String province = parts.length >= 2 ? parts[1] : null;
            if (isUsableCity(province)) {
                return province;
            }
            return null;
        } catch (Exception e) {
            log.warn("解析 IP 城市失败: {}", ip, e);
            return null;
        }
    }

    /**
     * 是否为可展示的城市名。ip2region 对保留/内网地址返回「Reserved」「保留地址」等占位值，
     * 这些不应作为登录城市展示，统一按无法定位处理。
     */
    private boolean isUsableCity(String value) {
        if (StringUtils.isBlank(value) || "0".equals(value)) {
            return false;
        }
        return !"reserved".equalsIgnoreCase(value)
                && !"保留地址".equals(value)
                && !"内网IP".equals(value)
                && !"局域网".equals(value);
    }

    @PreDestroy
    public void destroy() {
        if (ip2Region != null) {
            try {
                ip2Region.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
