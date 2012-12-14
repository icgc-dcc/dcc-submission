package org.icgc.dcc.web.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.icgc.dcc.web.ServerErrorCode;
import org.icgc.dcc.web.ServerErrorResponseMessage;
import org.icgc.dcc.web.validator.InvalidNameException;

@Provider
public class InvalidNameExceptionMapper implements ExceptionMapper<InvalidNameException> {

  @Override
  public Response toResponse(InvalidNameException exception) {
    return Response.status(Status.BAD_REQUEST)
        .entity(new ServerErrorResponseMessage(ServerErrorCode.INVALID_NAME, exception.getMessage())).build();
  }

}
