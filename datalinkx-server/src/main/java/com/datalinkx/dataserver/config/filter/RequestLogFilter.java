package com.datalinkx.dataserver.config.filter;


import com.datalinkx.common.constants.MetaConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;



@Slf4j
@Component
public class RequestLogFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String traceId = MDC.get(MetaConstants.CommonConstant.TRACE_ID);
        if (!StringUtils.hasLength(traceId)) {
            traceId = getCommonVariable(request, MetaConstants.CommonConstant.TRACE_ID);
            if (!StringUtils.hasLength(traceId)) {
                traceId = UUID.randomUUID().toString();
            }
        }
        MDC.put(MetaConstants.CommonConstant.TRACE_ID, traceId);

        request.setAttribute("reqStartTime", System.currentTimeMillis());
        request.setAttribute("traceId", traceId);
        String realIp = request.getHeader("X-Real-Ip");

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            long elasTime = System.currentTimeMillis() - (long) request.getAttribute("reqStartTime");
            afterRequestLog(request, response, realIp, elasTime);
            MDC.clear();
        }
    }

    public String getCommonVariable(HttpServletRequest req, String name) {
        String value = req.getHeader(name);
        if (!StringUtils.hasLength(value)) {
            value = req.getParameter(name);
        }
        return value;
    }
    
    private void afterRequestLog(HttpServletRequest request, HttpServletResponse response, String realIp, long elasTime) {
        log.info("uri={}||http_method={}||real_ip={}||request_time={}||status={}||elapse={}", request.getRequestURI(), request.getMethod(),
                realIp, request.getAttribute("reqStartTime"), response.getStatus(), elasTime);
    }
    
    @Override
    public void destroy() {
    }
}