package eu.europeana.cloud.service.mcs.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * RecordNotExistsException
 */
public class VersionNotExistsException extends WebApplicationException {

    public VersionNotExistsException() {
        super(Response.Status.NOT_FOUND);
    }
}