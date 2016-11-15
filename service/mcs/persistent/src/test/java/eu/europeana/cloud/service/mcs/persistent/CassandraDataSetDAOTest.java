package eu.europeana.cloud.service.mcs.persistent;

import eu.europeana.cloud.common.model.DataSet;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.service.mcs.persistent.cassandra.CassandraDataSetDAO;
import me.prettyprint.cassandra.utils.TimeUUIDUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(value = {"classpath:/spiedServicesTestContext.xml"})
public class CassandraDataSetDAOTest extends CassandraTestBase {

    @Autowired
    private CassandraDataSetDAO dataSetDAO;

    private static final String SAMPLE_PROVIDER_NAME = "Provider_1";
    private static final String SAMPLE_DATASET_ID = "Sample_ds_id_1";
    private static final String SAMPLE_REP_NAME_1 = "Sample_rep_1";
    private static final String SAMPLE_REP_NAME_2 = "Sample_rep_2";
    private static final String SAMPLE_REP_NAME_3 = "Sample_rep_3";
    private static final String SAMPLE_REVISION_ID = "Revision_1";
    private static final String SAMPLE_REVISION_ID2 = "Revision_2";
    private static final String SAMPLE_CLOUD_ID = "Cloud_1";
    private static final String SAMPLE_CLOUD_ID2 = "Cloud_2";
    private static final String SAMPLE_CLOUD_ID3 = "Cloud_3";
    private static final UUID SAMPLE_VERSION_ID = TimeUUIDUtils.getTimeUUID(new java.util.Date().getTime());

    @Test
    public void newRepresentationNameShouldBeAdded() {
        dataSetDAO.addDataSetsRepresentationName(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REP_NAME_1);
        Set<String> repNames = dataSetDAO.getAllRepresentationsNamesForDataSet(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID);
        Assert.assertTrue(repNames.size() == 1);
        Assert.assertTrue(repNames.contains(SAMPLE_REP_NAME_1));
    }


    @Test
    public void representationNameShouldBeRemovedFromDB() {
        dataSetDAO.addDataSetsRepresentationName(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REP_NAME_1);
        dataSetDAO.addDataSetsRepresentationName(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REP_NAME_2);
        //
        dataSetDAO.removeRepresentationNameForDataSet(SAMPLE_REP_NAME_1, SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID);
        Set<String> repNames = dataSetDAO.getAllRepresentationsNamesForDataSet(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID);
        //
        Assert.assertTrue(repNames.size() == 1);
        Assert.assertTrue(repNames.contains(SAMPLE_REP_NAME_2));
        Assert.assertFalse(repNames.contains(SAMPLE_REP_NAME_1));
    }

    @Test
    public void allRepresentationsNamesForDataSetShouldBeRemoved() {
        dataSetDAO.addDataSetsRepresentationName(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REP_NAME_1);
        dataSetDAO.addDataSetsRepresentationName(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REP_NAME_2);
        dataSetDAO.addDataSetsRepresentationName(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REP_NAME_3);
        Set<String> repNames = dataSetDAO.getAllRepresentationsNamesForDataSet(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID);
        Assert.assertTrue(repNames.size() == 3);
        //
        dataSetDAO.removeAllRepresentationsNamesForDataSet(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID);
        repNames = dataSetDAO.getAllRepresentationsNamesForDataSet(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID);
        //
        Assert.assertTrue(repNames.size() == 0);
    }

    @Test
    public void shouldListAllRepresentationsNamesForGivenDataSet() {
        dataSetDAO.addDataSetsRepresentationName(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REP_NAME_1);
        dataSetDAO.addDataSetsRepresentationName(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REP_NAME_3);
        Set<String> representations = dataSetDAO.getAllRepresentationsNamesForDataSet(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID);
        Assert.assertTrue(representations.contains(SAMPLE_REP_NAME_1));
        Assert.assertFalse(representations.contains(SAMPLE_REP_NAME_2));
        Assert.assertTrue(representations.contains(SAMPLE_REP_NAME_3));
    }

    @Test
    public void shouldListAllCloudIdForGivenRevisionAndDataset(){
        //given
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID);
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID2);
        //assigned to different revision
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID2, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID3);

        //when
        List<String> cloudIds = dataSetDAO.getDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1, 2);

        //then
        assertThat(cloudIds,hasItems(SAMPLE_CLOUD_ID,SAMPLE_CLOUD_ID2));
        assertThat(cloudIds, not(hasItems(SAMPLE_CLOUD_ID3)));
    }

    @Test
    public void shouldListAllCloudIdForGivenRevisionAndDatasetWithLimit(){
        //given
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID);
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID2);
        //assigned to different revision
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID2, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID3);

        //when
        List<String> cloudIds = dataSetDAO.getDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1, 1);

        //then
        assertThat(cloudIds,hasItems(SAMPLE_CLOUD_ID));
        assertThat(cloudIds, not(hasItems(SAMPLE_CLOUD_ID2,SAMPLE_CLOUD_ID3)));
    }

    @Test
    public void shouldListAllCloudIdForGivenRevisionAndDatasetWithPagination(){
        //given
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID);
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID2);
        //assigned to different revision
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID2, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID3);

        //when
        List<String> cloudIds = dataSetDAO.getDataSetsRevisionWithPagination(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID2, 1);

        //then
        assertThat(cloudIds,hasItems(SAMPLE_CLOUD_ID2));
        assertThat(cloudIds, not(hasItems(SAMPLE_CLOUD_ID,SAMPLE_CLOUD_ID3)));
    }

    @Test
    public void shouldListAllCloudIdForGivenRevisionForSecondRevision(){
        //given
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID);
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID2);
        //assigned to different revision
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID2, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID3);

        //when
        List<String> cloudIds = dataSetDAO.getDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID,
                SAMPLE_REVISION_ID2, SAMPLE_REP_NAME_1, 3);

        //then
        assertThat(cloudIds,hasItems(SAMPLE_CLOUD_ID3));
        assertThat(cloudIds, not(hasItems(SAMPLE_CLOUD_ID,SAMPLE_CLOUD_ID2)));
    }

    @Test
    public void shouldRemoveRevisionFromDataSet(){
        //given
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID);
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID2);

        //when
        dataSetDAO.removeDataSetsRevision(SAMPLE_PROVIDER_NAME,SAMPLE_DATASET_ID,SAMPLE_REVISION_ID,
                SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID);

        //then
        List<String> cloudIds = dataSetDAO.getDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID,
                SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1, 3);
        assertThat(cloudIds,hasItems(SAMPLE_CLOUD_ID2));
        assertThat(cloudIds, not(hasItems(SAMPLE_CLOUD_ID)));
    }

    @Test
    public void shouldRemoveRevisionFromDataSetSecondRevision(){
        //given
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID);
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID2);

        //when
        dataSetDAO.removeDataSetsRevision(SAMPLE_PROVIDER_NAME,SAMPLE_DATASET_ID,SAMPLE_REVISION_ID,
                SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID2);

        //then
        List<String> cloudIds = dataSetDAO.getDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID,
                SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1, 3);
        assertThat(cloudIds,hasItems(SAMPLE_CLOUD_ID));
        assertThat(cloudIds, not(hasItems(SAMPLE_CLOUD_ID2)));
    }

    @Test
    public void shouldRemoveAssigmentsOnRemoveWholeDataSet(){
        //given
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID);
        dataSetDAO.addDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID, SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1,SAMPLE_CLOUD_ID2);

        //when
        dataSetDAO.deleteDataSet(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID);

        //then
        List<String> cloudIds = dataSetDAO.getDataSetsRevision(SAMPLE_PROVIDER_NAME, SAMPLE_DATASET_ID,
                SAMPLE_REVISION_ID, SAMPLE_REP_NAME_1, 3);
        assertThat(cloudIds, not(hasItems(SAMPLE_CLOUD_ID, SAMPLE_CLOUD_ID2)));
    }

    @Test
    public void shouldAddLatestRevisionForRepresentation(){
        //given
        DataSet dataSet = new DataSet();
        dataSet.setId("sampleDataSetID");
        dataSet.setProviderId("sampleProvider");
        Representation representation = new Representation();
        representation.setCloudId("sampleCloudID");
        representation.setRepresentationName("sampleRepresentationName");
        representation.setVersion("123ef902-fdd1-11e5-993a-fa163e8d4ae3");

        Revision revision = new Revision();
        revision.setRevisionProviderId("sampleProvider");
        revision.setRevisionName("sampleRevision");
        //when
        dataSetDAO.addLatestRevisionForDatasetAssignment(dataSet, representation, revision);
        //then
        String versionId = dataSetDAO.getVersionOfLatestRevisionForDatasetAssignment(dataSet, representation, revision);
        Assert.assertNotNull(versionId);
        Assert.assertTrue(versionId.equals("123ef902-fdd1-11e5-993a-fa163e8d4ae3"));
    }


    @Test
    public void shouldRemoveLatestRevisionForRepresentation(){
        //given
        DataSet dataSet = new DataSet();
        dataSet.setId("sampleDataSetID");
        dataSet.setProviderId("sampleProvider");
        Representation representation = new Representation();
        representation.setCloudId("sampleCloudID");
        representation.setRepresentationName("sampleRepresentationName");
        representation.setVersion("123ef902-fdd1-11e5-993a-fa163e8d4ae3");

        Revision revision = new Revision();
        revision.setRevisionProviderId("sampleProvider");
        revision.setRevisionName("sampleRevision");
        dataSetDAO.addLatestRevisionForDatasetAssignment(dataSet, representation, revision);
        //when
        dataSetDAO.removeLatestRevisionForDatasetAssignment(dataSet, representation, revision);
        //then
        String versionId = dataSetDAO.getVersionOfLatestRevisionForDatasetAssignment(dataSet,representation,revision);
        Assert.assertNull(versionId);
    }

    @Test
    public void shouldUpdateLatestRevisionForRepresentation(){
        //given
        DataSet dataSet = new DataSet();
        dataSet.setId("sampleDataSetID");
        dataSet.setProviderId("sampleProvider");
        Representation representation = new Representation();
        representation.setCloudId("sampleCloudID");
        representation.setRepresentationName("sampleRepresentationName");
        representation.setVersion("123ef902-fdd1-11e5-993a-fa163e8d4ae3");

        Revision revision = new Revision();
        revision.setRevisionProviderId("sampleProvider");
        revision.setRevisionName("sampleRevision");
        dataSetDAO.addLatestRevisionForDatasetAssignment(dataSet, representation, revision);
        //when
        representation.setVersion("123ef902-fdd1-11e5-993a-fa163e8d4ae4");
        dataSetDAO.addLatestRevisionForDatasetAssignment(dataSet, representation, revision);
        //then
        String versionId = dataSetDAO.getVersionOfLatestRevisionForDatasetAssignment(dataSet, representation, revision);
        Assert.assertTrue(versionId.equals("123ef902-fdd1-11e5-993a-fa163e8d4ae4"));
    }
}
