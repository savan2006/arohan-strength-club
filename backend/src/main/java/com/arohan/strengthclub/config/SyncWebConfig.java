package com.arohan.strengthclub.config;

import com.arohan.strengthclub.service.SyncEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SyncWebConfig implements WebMvcConfigurer {
    private final SyncEventService syncEvents;

    public SyncWebConfig(SyncEventService syncEvents) { this.syncEvents = syncEvents; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                    Object handler, Exception ex) {
                try {
                    syncEvents.captureSuccessfulMutation(request, response.getStatus(), ex);
                } catch (Exception ignored) {
                    // Event tracking is best-effort and must not affect a completed request.
                }
            }
        }).addPathPatterns("/api/**");
    }
}
