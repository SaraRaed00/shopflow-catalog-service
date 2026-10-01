package com.shopflow.catalog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ReservationRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ReservationRateLimitFilter.class);
    private final StringRedisTemplate redisTemplate;
    private final int limit;
    private final long windowSeconds;

    public ReservationRateLimitFilter(StringRedisTemplate redisTemplate, @Value("${ratelimit.reservations.limit}") int limit,@Value("${ratelimit.reservations.window-seconds}") long windowSeconds){
        this.redisTemplate = redisTemplate;
        this.limit = limit;
        this.windowSeconds = windowSeconds;
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException{
        boolean isReservationCreate = "POST".equals(request.getMethod())&& "/api/v1/reservations".equals(request.getRequestURI());

        if(!isReservationCreate){ //  not a reservation
            chain.doFilter(request,response); // request continue normally
            return;
        }
        long timeOfWindowStart = System.currentTimeMillis()/1000/windowSeconds; // if the request is in the same min it will produce the same num.
        String RedisKey = "ratelimit:v1:reservations:" + timeOfWindowStart;

        try {
            Long count = redisTemplate.opsForValue().increment(RedisKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(RedisKey, Duration.ofSeconds(windowSeconds)); // set a TTL on the key, telling Redis delete this automatically in 60 seconds
            }
            if (count != null && count > limit) {
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(windowSeconds));
                response.setContentType("application/json");
                response.getWriter().write("""
                        {"code":"RESERVATION_RATE_LIMIT_EXCEEDED","message":"Too many reservation attempts, try again later"}""");
                return;
            }
        } catch (Exception ex) {
            log.warn("Reservation rate limiter unavailable, allowing request: {}", ex.getMessage());
        }

        chain.doFilter(request, response);
    }

}





