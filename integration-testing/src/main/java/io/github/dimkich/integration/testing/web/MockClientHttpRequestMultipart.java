package io.github.dimkich.integration.testing.web;

import dev.baecher.multipart.StreamingMultipartParser;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;

public class MockClientHttpRequestMultipart extends MockClientHttpRequest {
    private final HttpMethod httpMethod;
    private final URI uri;
    private final ClientHttpRequest originalRequest;
    private final MockMvc mockMvc;

    public MockClientHttpRequestMultipart(HttpMethod httpMethod, URI uri, ClientHttpRequest originalRequest,
                                          MockMvc mockMvc) {
        super(httpMethod, uri);
        this.httpMethod = httpMethod;
        this.uri = uri;
        this.originalRequest = originalRequest;
        this.mockMvc = mockMvc;
    }

    @Override
    @SneakyThrows
    public ClientHttpResponse executeInternal() {
        MediaType contentType = getHeaders().getContentType();
        if (contentType != null && contentType.getType().equals("multipart")
                && contentType.getSubtype().equals("form-data")) {
            StreamingMultipartParser parser = new StreamingMultipartParser(new ByteArrayInputStream(getBodyAsBytes()));
            MockMultipartFile file;
            MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(httpMethod, uri);
            builder.headers(getHeaders());

            while (parser.hasNext()) {
                StreamingMultipartParser.Part part = parser.next();
                file = new MockMultipartFile(part.getHeaders().getName(), part.getHeaders().getFilename(),
                        part.getHeaders().getHeaderValue("content-type"), part.getInputStream());
                builder.file(file);
            }
            MockHttpServletResponse response = mockMvc.perform(builder).andReturn().getResponse();
            HttpStatusCode status = HttpStatusCode.valueOf(response.getStatus());
            byte[] body = response.getContentAsByteArray();
            MockClientHttpResponse clientResponse = new MockClientHttpResponse(body, status);
            clientResponse.getHeaders().putAll(getResponseHeaders(response));
            return clientResponse;
        }

        originalRequest.getBody().write(getBodyAsBytes());
        originalRequest.getHeaders().addAll(getHeaders());
        return originalRequest.execute();
    }

    private HttpHeaders getResponseHeaders(MockHttpServletResponse response) {
        HttpHeaders headers = new HttpHeaders();
        for (String name : response.getHeaderNames()) {
            List<String> values = response.getHeaders(name);
            for (String value : values) {
                headers.add(name, value);
            }
        }
        return headers;
    }
}
