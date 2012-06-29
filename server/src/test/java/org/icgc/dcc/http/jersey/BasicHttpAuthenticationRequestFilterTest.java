package org.icgc.dcc.http.jersey;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.icgc.dcc.security.UsernamePasswordAuthenticator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.common.net.HttpHeaders;

public class BasicHttpAuthenticationRequestFilterTest {

  private Request mockRequest;

  private MultivaluedMap<String, String> mockHeaders;

  private ContainerRequestContext mockContext;

  private ResponseBuilder mockBuilder;

  private BasicHttpAuthenticationRequestFilter basicHttpAuthenticationRequestFilter;

  private UsernamePasswordAuthenticator usernamePasswordAuthenticator;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {

    // Create a few mock instances
    this.mockRequest = mock(Request.class);
    this.mockHeaders = mock(MultivaluedMap.class);
    this.mockContext = mock(ContainerRequestContext.class);
    this.mockBuilder = mock(ResponseBuilder.class);
    this.usernamePasswordAuthenticator = mock(UsernamePasswordAuthenticator.class);

    this.basicHttpAuthenticationRequestFilter =
        new BasicHttpAuthenticationRequestFilter(this.usernamePasswordAuthenticator);

    // Create some behaviour
    when(this.mockContext.getRequest()).thenReturn(this.mockRequest);
    when(this.mockContext.getHeaders()).thenReturn(this.mockHeaders);

    when(this.usernamePasswordAuthenticator.authenticate("brett", "brettspasswd".toCharArray(), "")).thenReturn(true);
  }

  @Test
  public void test_preMatchFilter_handlesCorrectAuthorizationHeader() throws IOException {

    when(this.mockHeaders.getFirst(HttpHeaders.AUTHORIZATION))//
        .thenReturn("X-DCC-Auth YnJldHQ6YnJldHRzcGFzc3dk"); // encodes "brett:brettspasswd" in base64
    // (generate using: $ echo -n "brett:brettspasswd" | base64)
    this.runFilter();
    // Make sure there was a login attempt and capture the token
    // Assert username and password match
    verify(this.usernamePasswordAuthenticator).authenticate("brett", "brettspasswd".toCharArray(), "");
    verify(this.mockContext, Mockito.never()).abortWith(any(Response.class));
  }

  @Test
  public void test_preMatchFilter_handlesIncorrectAuthorizationHeader() throws IOException {

    when(this.mockHeaders.getFirst(HttpHeaders.AUTHORIZATION))//
        .thenReturn("X-DCC-Auth YnJldHQ6Tk9UYnJldHRzcGFzc3dk"); // encodes "brett:brettspasswd" in base64
    // (generate using: $ echo -n "brett:NOTbrettspasswd" | base64)
    this.runFilter();

    verify(this.usernamePasswordAuthenticator).authenticate("brett", "NOTbrettspasswd".toCharArray(), "");

    ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
    verify(this.mockContext).abortWith(response.capture());
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getValue().getStatus());
    assertEquals("X-DCC-Auth realm=\"DCC\"", response.getValue().getHeader(HttpHeaders.WWW_AUTHENTICATE));
  }

  @Test
  public void test_preMatchFilter_handlesMissingHeader() throws IOException {

    // This test is testing that the header is absent
    when(this.mockHeaders.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);

    this.runFilter();

    ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
    verify(this.mockContext).abortWith(response.capture());
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getValue().getStatus());
    assertEquals("X-DCC-Auth realm=\"DCC\"", response.getValue().getHeader(HttpHeaders.WWW_AUTHENTICATE));
  }

  @Test
  public void test_preMatchFilter_handlesMalformedAuthorizationHeader1() throws IOException {
    testMalformedHeader("Basi YnJldHQ6YnJldHRzcGFzc3dkCg==");
  }

  @Test
  public void test_preMatchFilter_handlesMalformedAuthorizationHeader2() throws IOException {
    testMalformedHeader("X-DCC-Auth YnJldHQvYnJldHRzcGFzc3dkCg==");
  }

  @Test
  public void test_preMatchFilter_handlesMalformedAuthorizationHeader3() throws IOException {
    testMalformedHeader("X-DCC-Auth");
  }

  /**
   * Common test fixture for a malformed header
   */
  private void testMalformedHeader(String malformed) throws IOException {
    when(this.mockHeaders.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(malformed);
    this.runFilter();

    ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
    verify(this.mockContext).abortWith(response.capture());
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getValue().getStatus());
  }

  // Exercise the code we're testing
  private void runFilter() throws IOException {
    this.basicHttpAuthenticationRequestFilter.filter(this.mockContext);
  }
}
