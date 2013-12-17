package eu.europeana.cloud.service.mcs.persistent;

import eu.europeana.cloud.client.uis.rest.CloudException;
import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.common.model.CloudId;
import eu.europeana.cloud.service.mcs.persistent.exception.SystemException;
import eu.europeana.cloud.service.uis.exception.CloudIdDoesNotExistException;
import java.util.Iterator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

public class UISClientHandlerImpl implements UISClientHandler {

    @Autowired
    private UISClient uisClient;


    /**
     * Checks if given cloudId exist in Unique Identifiers Service. Throws SystemException in case of UIS error.
     * 
     * @param cloudId
     *            cloud id
     * @return true if cloudId exists in UIS, false otherwise
     */
    @Override
    public boolean recordExistInUIS(String cloudId) {
        boolean result = false;
        try {
            List<CloudId> records = uisClient.getRecordId(cloudId);
            if (records == null) {
                throw new IllegalStateException("UIS returned null");
            }
            if (records.isEmpty()) {
                throw new IllegalStateException("UIS returned empty list");
            }

            Iterator<CloudId> iterator = records.iterator();
            while (iterator.hasNext()) {
                CloudId ci = iterator.next();
                if (ci.getId().equals(cloudId)) {
                    result = true;
                    break;
                }
            }
            if (result == false) {
                throw new IllegalStateException(String.format("Cloud id %s not on the list returned by UIS", cloudId));
            }
        } catch (CloudException ex) {
            if (ex.getCause() instanceof CloudIdDoesNotExistException) {
                result = false;
            } else {
                throw new SystemException(ex);
            }
        }
        return result;
    }
}