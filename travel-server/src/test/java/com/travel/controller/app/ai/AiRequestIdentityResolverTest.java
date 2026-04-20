package com.travel.controller.app.ai;

import com.travel.util.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AI 请求身份解析器测试。
 */
class AiRequestIdentityResolverTest {

    @Test
    void shouldResolveBearerTokenAndUserIdentity() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        when(jwtUtils.getUserIdFromToken("user-token")).thenReturn(19L);

        AiRequestIdentityResolver resolver = new AiRequestIdentityResolver(jwtUtils);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer user-token");

        assertEquals("user-token", resolver.resolveToken(request));
        assertEquals(19L, resolver.resolveUserId(request));
    }

    @Test
    void shouldResolveAdminIdentityFromToken() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        when(jwtUtils.getAdminIdFromToken("admin-token")).thenReturn(7L);

        AiRequestIdentityResolver resolver = new AiRequestIdentityResolver(jwtUtils);

        assertEquals(7L, resolver.resolveAdminId("admin-token"));
    }

    @Test
    void shouldReturnNullWhenAuthorizationHeaderIsInvalid() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        AiRequestIdentityResolver resolver = new AiRequestIdentityResolver(jwtUtils);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Token invalid");

        assertNull(resolver.resolveToken(request));
        assertNull(resolver.resolveUserId(request));
        assertNull(resolver.resolveAdminId(request));
    }

    @Test
    void shouldResolveClientIpByForwardedHeadersFirst() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        AiRequestIdentityResolver resolver = new AiRequestIdentityResolver(jwtUtils);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");

        assertEquals("10.0.0.1", resolver.resolveClientIp(request));
    }

    @Test
    void shouldFallbackToRemoteAddressWhenProxyHeadersAreMissing() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        AiRequestIdentityResolver resolver = new AiRequestIdentityResolver(jwtUtils);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertEquals("127.0.0.1", resolver.resolveClientIp(request));
    }
}
