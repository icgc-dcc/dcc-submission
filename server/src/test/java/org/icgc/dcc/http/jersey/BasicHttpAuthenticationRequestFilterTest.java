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

import org.icgc.dcc.security.UsernamePasswordAuthenticator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.net.HttpHeaders;

public class BasicHttpAuthenticationRequestFilterTest {

  private Request mockRequest;

  private RequestHeaders mockHeaders;

  private FilterContext mockContext;

  private ResponseBuilder mockBuilder;

  private BasicHttpAuthenticationRequestFilter basicHttpAuthenticationRequestFilter;

  private UsernamePasswordAuthenticator usernamePasswordAuthenticator;

  @Before
  public void setUp() {

    // Create a few mock instances
    this.mockRequest = mock(Request.class);
    this.mockHeaders = mock(RequestHeaders.class);
    this.mockContext = mock(FilterContext.class);
    this.mockBuilder = mock(ResponseBuilder.class);
    this.usernamePasswordAuthenticator = mock(UsernamePasswordAuthenticator.class);

    this.basicHttpAuthenticationRequestFilter =
        new BasicHttpAuthenticationRequestFilter(this.usernamePasswordAuthenticator);

    // Create some behaviour
    when(this.mockContext.getRequest()).thenReturn(this.mockRequest);
    when(this.mockRequest.getHeaders()).thenReturn(this.mockHeaders);
  }

  @Test
  public void test_preMatchFilter_handlesCorrectAuthorizationHeader() throws IOException {

    when(this.mockHeaders.getHeader(HttpHeaders.AUTHORIZATION))//
        .thenReturn("Basic YnJldHQ6YnJldHRzcGFzc3dk"); // encodes "brett:brettspasswd" in base64
                                                       // (generate using: $ echo -n "brett:brettspasswd" | base64)
                                                       // this.prepareOkResponse();
    this.runFilter();
    // Make sure there was a login attempt and capture the token
    // Assert username and password match
    verify(this.usernamePasswordAuthenticator).authenticate("brett", "brettspasswd".toCharArray(), "");
    verify(this.mockContext, Mockito.never()).setResponse(any(Response.class));
  }

  @Test
  public void test_preMatchFilter_handlesMissingHeader() throws IOException {

    // This test is testing that the header is absent
    when(this.mockHeaders.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

    this.prepareAnyResponse();
    this.runFilter();

    // Make some assertions
    verify(this.mockBuilder).status(Response.Status.UNAUTHORIZED);
    verify(this.mockBuilder).header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"DCC\"");
  }

  @Test
  public void test_preMatchFilter_handlesMalformedAuthorizationHeader1() throws IOException {
    testMalformedHeader("Basi YnJldHQ6YnJldHRzcGFzc3dkCg==");
  }

  @Test
  public void test_preMatchFilter_handlesMalformedAuthorizationHeader2() throws IOException {
    testMalformedHeader("Basic YnJldHQvYnJldHRzcGFzc3dkCg==");
  }

  @Test
  public void test_preMatchFilter_handlesMalformedAuthorizationHeader3() throws IOException {
    testMalformedHeader("Basic");
  }

  /**
   * Common test fixture for a malformed header
   */
  private void testMalformedHeader(String malformed) throws IOException {
    when(this.mockHeaders.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(malformed);
    this.prepareAnyResponse();
    this.runFilter();
    verify(this.mockBuilder).status(Response.Status.BAD_REQUEST);
  }

  // Mocking the ResponseBuilder is kinda painful.
  private void prepareAnyResponse() {
    when(this.mockContext.createResponse()).thenReturn(this.mockBuilder);
    when(this.mockBuilder.status(any(Response.Status.class))).thenReturn(this.mockBuilder);
    when(this.mockBuilder.header(anyString(), any())).thenReturn(this.mockBuilder);
  }

  // Exercise the code we're testing
  private void runFilter() throws IOException {
    this.basicHttpAuthenticationRequestFilter.preMatchFilter(this.mockContext);
  }
}
