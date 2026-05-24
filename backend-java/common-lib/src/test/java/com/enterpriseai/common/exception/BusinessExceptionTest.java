package com.enterpriseai.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void shouldStoreCodeAndMessage() {
        BusinessException ex = new BusinessException(400, "Bad input");
        assertEquals(400, ex.getCode());
        assertEquals("Bad input", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void shouldAcceptCustomHttpStatus() {
        BusinessException ex = new BusinessException(503, "Down", HttpStatus.SERVICE_UNAVAILABLE);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
    }

    @Test
    void notFound_shouldBe404() {
        BusinessException ex = BusinessException.notFound("User", "123");
        assertEquals(404, ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("User"));
        assertTrue(ex.getMessage().contains("123"));
    }

    @Test
    void unauthorized_shouldBe401() {
        BusinessException ex = BusinessException.unauthorized("Token expired");
        assertEquals(401, ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void forbidden_shouldBe403() {
        BusinessException ex = BusinessException.forbidden("No permission");
        assertEquals(403, ex.getCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
    }

    @Test
    void badRequest_shouldBe400() {
        BusinessException ex = BusinessException.badRequest("Missing field");
        assertEquals(400, ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }
}
