package eu.europeana.cloud.service.uis.rest;

import com.qmino.miredot.annotations.ReturnType;
import eu.europeana.cloud.common.exceptions.ProviderDoesNotExistException;
import eu.europeana.cloud.common.model.CloudId;
import eu.europeana.cloud.common.response.ResultSlice;
import eu.europeana.cloud.common.web.UISParamConstants;
import eu.europeana.cloud.service.aas.authentication.SpringUserUtils;
import eu.europeana.cloud.service.uis.ACLServiceWrapper;
import eu.europeana.cloud.service.uis.UniqueIdentifierService;
import eu.europeana.cloud.service.uis.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Implementation of the Unique Identifier Service.
 *
 * @author Yorgos.Mamakis@ kb.nl
 * @since Oct 17, 2013
 */
@Component
@Path("/cloudIds")
@Scope("request")
public class UniqueIdentifierResource {

    @Autowired
    private UniqueIdentifierService uniqueIdentifierService;

    @Autowired
    private DataProviderResource dataProviderResource;

    private static final String CLOUDID = "cloudId";

    @Autowired
    private ACLServiceWrapper aclWrapper;


    private final String CLOUD_ID_CLASS_NAME = CloudId.class.getName();

    private static final Logger LOGGER = LoggerFactory.getLogger(UniqueIdentifierResource.class);


    /**
     * Invokes the generation of a cloud identifier using the provider
     * identifier and a record identifier.
     * <p>
     * <br/>
     * <br/>
     * <div style='border-left: solid 5px #999999; border-radius: 10px; padding:
     * 6px;'> <strong>Required permissions:</strong>
     * <ul>
     * <li>Authenticated user</li>
     * </ul>
     * </div>
     *
     * @param providerId <strong>REQUIRED</strong> identifier of data-provider for
     *                   which new cloud identifier will be created.
     * @param localId    record identifier which will be binded to the newly created
     *                   cloud identifier. If not provided, random value will be
     *                   generated.
     * @return The newly created CloudId
     * @throws DatabaseConnectionException   database error
     * @throws RecordExistsException         Record already exists in repository
     * @throws ProviderDoesNotExistException Supplied Data-provider does not exist
     * @throws RecordDatasetEmptyException   dataset is empty
     * @throws CloudIdDoesNotExistException  cloud identifier does not exist
     * @throws CloudIdAlreadyExistException  Cloud identifier was created previously
     * @summary Cloud identifier generation
     */
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ReturnType("eu.europeana.cloud.common.model.CloudId")
    @PreAuthorize("isAuthenticated()")
    public Response createCloudId(@QueryParam(UISParamConstants.Q_PROVIDER) String providerId,
                                  @QueryParam(UISParamConstants.Q_RECORD_ID) String localId)
            throws DatabaseConnectionException, RecordExistsException, ProviderDoesNotExistException,
            RecordDatasetEmptyException, CloudIdDoesNotExistException, CloudIdAlreadyExistException {

        final CloudId cId = (localId != null) ? (uniqueIdentifierService.createCloudId(providerId, localId))
                : (uniqueIdentifierService.createCloudId(providerId));

        final Response response = Response.ok().entity(cId).build();

        // CloudId created => let's assign permissions to the owner
        String creatorName = SpringUserUtils.getUsername();

        if (creatorName != null) {

            ObjectIdentity cloudIdIdentity = new ObjectIdentityImpl(CLOUD_ID_CLASS_NAME, cId.getId());
            MutableAcl cloudIdAcl = aclWrapper.getAcl(creatorName, cloudIdIdentity);
            aclWrapper.updateAcl(cloudIdAcl);
        }
        dataProviderResource.grantPermissionsToLocalId(cId, providerId);
        return response;
    }

    /**
     * Retrieves cloud identifier based on given provider identifier and record
     * identifier
     *
     * @param providerId <strong>REQUIRED</strong> provider identifier
     * @param recordId   <strong>REQUIRED</strong> record identifier
     * @return Cloud identifier associated with given provider identifier and
     * record identifier
     * @throws DatabaseConnectionException   database error
     * @throws RecordDoesNotExistException   record does not exist
     * @throws ProviderDoesNotExistException provider does not exist
     * @throws RecordDatasetEmptyException   dataset is empty
     * @summary Cloud identifier retrieval
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ReturnType("eu.europeana.cloud.common.model.CloudId")
    public Response getCloudId(@QueryParam(UISParamConstants.Q_PROVIDER) String providerId,
                               @QueryParam(UISParamConstants.Q_RECORD_ID) String recordId)
            throws DatabaseConnectionException, RecordDoesNotExistException, ProviderDoesNotExistException,
            RecordDatasetEmptyException {
        return Response.ok(uniqueIdentifierService.getCloudId(providerId, recordId)).build();
    }


    /**
     * Retrieves list of record Identifiers associated with the cloud
     * identifier. Result is returned in slices which contain fixed amount of
     * results and reference (token) to next slice of results.
     *
     * @param cloudId <strong>REQUIRED</strong> cloud identifier for which list of
     *                all record identifiers will be retrieved
     * @return The list of record identifiers bound to given provider identifier
     * @throws DatabaseConnectionException   database error
     * @throws CloudIdDoesNotExistException  cloud identifier does not exist
     * @throws ProviderDoesNotExistException provider does not exist
     * @throws RecordDatasetEmptyException   datset is empty
     * @summary List of record identifiers retrieval
     */
    @GET
    @Path("{" + CLOUDID + "}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @ReturnType("eu.europeana.cloud.common.response.ResultSlice<eu.europeana.cloud.common.model.CloudId>")
    public Response getLocalIds(@PathParam(CLOUDID) String cloudId)
            throws DatabaseConnectionException, CloudIdDoesNotExistException, ProviderDoesNotExistException,
            RecordDatasetEmptyException {
        ResultSlice<CloudId> pList = new ResultSlice<>();
        pList.setResults(uniqueIdentifierService.getLocalIdsByCloudId(cloudId));
        return Response.ok(pList).build();
    }


    /**
     * Remove a cloud identifier and all the associations to its record
     * identifiers
     * <p>
     * <br/>
     * <br/>
     * <div style='border-left: solid 5px #999999; border-radius: 10px; padding:
     * 6px;'> <strong>Required permissions:</strong>
     * <ul>
     * <li>Admin role</li>
     * </ul>
     * </div>
     *
     * @param cloudId <strong>REQUIRED</strong> cloud identifier which will be
     *                removed
     * @return Empty response with http status code indicating whether the
     * operation was successful or not
     * @throws DatabaseConnectionException   database error
     * @throws CloudIdDoesNotExistException  cloud identifier does not exist
     * @throws ProviderDoesNotExistException provider does not exist
     * @throws RecordIdDoesNotExistException record identifier does not exist
     * @summary Cloud identifier removal
     */
    @DELETE
    @Path("{" + CLOUDID + "}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Response deleteCloudId(@PathParam(CLOUDID) String cloudId)
            throws DatabaseConnectionException, CloudIdDoesNotExistException, ProviderDoesNotExistException,
            RecordIdDoesNotExistException {

        // usuwanie cloudId local id tworznego przy tworznie clouId
        //sprawdzić co się stanie po dodaniu mapowania wiele razy i uunięcu cloudId
        //dopisac

        List<CloudId> removedCloudIds = uniqueIdentifierService.deleteCloudId(cloudId);
        for (CloudId cId : removedCloudIds) {
            dataProviderResource.deleteLocalIdAcl(cId.getLocalId().getRecordId(), cId.getLocalId().getProviderId());
        }

        // let's delete the permissions as well
        ObjectIdentity cloudIdentity = new ObjectIdentityImpl(CLOUD_ID_CLASS_NAME, cloudId);
        aclWrapper.deleteAcl(cloudIdentity, false);
        return Response.ok("CloudId marked as deleted").build();
    }
}
