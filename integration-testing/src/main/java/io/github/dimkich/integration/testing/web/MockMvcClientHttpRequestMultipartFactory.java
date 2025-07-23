package io.github.dimkich.integration.testing.web;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

public class MockMvcClientHttpRequestMultipartFactory extends MockMvcClientHttpRequestFactory {
    private final MockMvc mockMvc;

    public MockMvcClientHttpRequestMultipartFactory(MockMvc mockMvc) {
        super(mockMvc);
        this.mockMvc = mockMvc;
    }

    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
        ClientHttpRequest request = super.createRequest(uri, httpMethod);
        return new MockClientHttpRequestMultipart(httpMethod, uri, request, mockMvc);
    }
}
