package org.icgc.dcc.http.jersey;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.RequestHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.FilterContext;

import org.testng.annotations.Test;

import com.google.common.net.HttpHeaders;

public class BasicHttpAuthenticationRequestFilterTest {

  @Test(groups = { "unit" })
  public void test_preMatchFilter_handlesMissingHeader() throws IOException {
    // Create a few mock instances
    Request mockRequest = mock(Request.class);
    RequestHeaders mockHeaders = mock(RequestHeaders.class);
    FilterContext mockContext = mock(FilterContext.class);
    ResponseBuilder mockBuilder = mock(ResponseBuilder.class);

    // Create some behaviour
    when(mockContext.getRequest()).thenReturn(mockRequest);
    when(mockRequest.getHeaders()).thenReturn(mockHeaders);

    // This test is testing that the header is absent
    when(mockHeaders.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

    // Mocking the ResponseBuilder is kinda painful.
    when(mockContext.createResponse()).thenReturn(mockBuilder);
    when(mockBuilder.status(any(Response.Status.class))).thenReturn(mockBuilder);
    when(mockBuilder.header(anyString(), any())).thenReturn(mockBuilder);

    BasicHttpAuthenticationRequestFilter filter = new BasicHttpAuthenticationRequestFilter();
    // Exercise the code we're testing
    filter.preMatchFilter(mockContext);

    // Make some assertions
    verify(mockBuilder).status(Response.Status.UNAUTHORIZED);
    verify(mockBuilder).header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"DCC\"");
  }

}
