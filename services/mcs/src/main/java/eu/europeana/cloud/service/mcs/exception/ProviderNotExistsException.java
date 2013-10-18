package eu.europeana.cloud.service.mcs.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * RecordNotExistsException
 */
public class ProviderNotExistsException extends WebApplicationException {

    public ProviderNotExistsException() {
        super(Response.Status.PRECONDITION_FAILED);
    }
}