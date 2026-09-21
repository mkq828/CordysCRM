package cn.cordys.config;

import cn.cordys.common.context.OrganizationContextWebFilter;
import cn.cordys.common.context.TraceIdWebFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * @Author: jianxing
 * @CreateTime: 2025-01-08  17:40
 */
@Configuration
public class OrganizationContextConfig {

    /**
     * 注册 OrganizationContextWebFilter 过滤器
     *
     * @return
     */
    @Bean
    public FilterRegistrationBean<OrganizationContextWebFilter> tenantContextWebFilter() {
        FilterRegistrationBean<OrganizationContextWebFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new OrganizationContextWebFilter());
        return registrationBean;
    }

    /**
     * 注册 TraceIdWebFilter 过滤器，用于生成/透传 traceId 并写入 MDC
     */
    @Bean
    public FilterRegistrationBean<TraceIdWebFilter> traceIdWebFilter() {
        FilterRegistrationBean<TraceIdWebFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TraceIdWebFilter());
        // traceId 需在 Shiro 等认证过滤器之前生成，否则 401/404 等请求拿不到 X-Trace-Id
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }
}
