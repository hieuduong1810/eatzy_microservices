package com.eatzy.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.servlet.http.HttpServletResponse;
import com.eatzy.common.dto.RestResponse;
import com.eatzy.common.annotation.ApiMessage;

/**
 * Shared ResponseBodyAdvice that wraps all successful JSON responses 
 * into RestResponse{statusCode, data, message}.
 * 
 * Auto-activated in any service that has spring-boot-starter-web on classpath.
 * Skips: Swagger/OpenAPI endpoints, error responses (4xx/5xx), String and Resource bodies.
 */
@ControllerAdvice
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
public class FormatRestResponse implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        int status = servletResponse.getStatus();

        RestResponse<Object> res = new RestResponse<Object>();
        res.setStatusCode(status);

        if (!MediaType.APPLICATION_JSON.equals(selectedContentType)) {
            return body;
        }

        if (body instanceof String || body instanceof Resource) {
            return body;
        }

        String path = request.getURI().getPath();
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return body;
        }

        if (status >= 400) {
            return body;
        } else {
            res.setData(body);
            ApiMessage message = returnType.getMethodAnnotation(ApiMessage.class);
            res.setMessage(message != null ? message.value() : "CALL API SUCCESS");
        }

        return res;
    }
}
