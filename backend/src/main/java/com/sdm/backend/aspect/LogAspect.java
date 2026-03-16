package com.sdm.backend.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdm.backend.annotation.Log;
import com.sdm.backend.entity.OperationLog;
import com.sdm.backend.service.OperationLogService;
import com.sdm.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Around("@annotation(com.sdm.backend.annotation.Log)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperationLog operationLog = new OperationLog();
        operationLog.setOperationTime(LocalDateTime.now());

        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                try {
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    String username = jwtUtil.getUsernameFromToken(token);
                    String role = jwtUtil.getRoleFromToken(token);
                    operationLog.setUserId(userId);
                    operationLog.setUsername(username);
                    operationLog.setRole(role);
                } catch (Exception e) {
                }
            }

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Log logAnnotation = signature.getMethod().getAnnotation(Log.class);
            operationLog.setModule(logAnnotation.module());
            operationLog.setOperation(logAnnotation.operation());
            operationLog.setDescription(logAnnotation.description());

            operationLog.setRequestMethod(request.getMethod());
            operationLog.setRequestUrl(request.getRequestURI());
            operationLog.setIpAddress(getIpAddress(request));
            operationLog.setUserAgent(request.getHeader("User-Agent"));

            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                try {
                    operationLog.setRequestParams(objectMapper.writeValueAsString(args));
                } catch (Exception e) {
                    operationLog.setRequestParams("[]");
                }
            }

            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            operationLog.setExecutionTime(executionTime);
            operationLog.setStatus("SUCCESS");
            operationLogService.save(operationLog);
            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            operationLog.setExecutionTime(executionTime);
            operationLog.setStatus("FAILED");
            operationLog.setErrorMessage(e.getMessage());
            operationLogService.save(operationLog);
            throw e;
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        // 将 IPv6 本地回环地址转换为 IPv4 格式
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}
