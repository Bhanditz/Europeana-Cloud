package eu.europeana.cloud.service.mcs.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * RecordNotExistsException
 */
public class CannotDeletePersistentRepresentationVersion extends WebApplicationException {

    public CannotDeletePersistentRepresentationVersion() {
        super(Response.Status.METHOD_NOT_ALLOWED);
    }
}