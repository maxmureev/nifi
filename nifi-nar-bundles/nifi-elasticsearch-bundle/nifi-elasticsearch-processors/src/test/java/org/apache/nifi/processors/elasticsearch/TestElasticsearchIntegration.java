package org.apache.nifi.processors.elasticsearch;

import org.apache.nifi.processors.elasticsearch.docker.DockerServicePortType;
import org.apache.nifi.processors.elasticsearch.docker.ElasticsearchDockerInitializer;
import org.apache.nifi.processors.elasticsearch.docker.ElasticsearchNodesType;
import org.apache.nifi.processors.elasticsearch.docker.PreStartNetworkStatus;
import org.apache.nifi.provenance.ProvenanceEventRecord;
import org.apache.nifi.provenance.ProvenanceEventType;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.serialization.record.MockRecordParser;
import org.apache.nifi.serialization.record.RecordFieldType;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.*;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestElasticsearchIntegration extends ElasticsearchDockerInitializer{
    private TestRunner runner;
    private static byte[] docExample;
    private static String esUrl;
    private static String esUrlProxy;
    private static String proxyPort;
    private static String proxyAuthPort;
    private static Boolean networkExisted;
    private static String network;

    @BeforeClass
    public static void initializeContainers() throws Exception {
       PreStartNetworkStatus networkExistedBefore = initializeNetwork("elasticsearch_squid_nifi");
        network = networkExistedBefore.getNetworkName();
        logger.info("Docker network name - " + network);
        networkExisted = networkExistedBefore.isExistedBefore();
        EnumMap<ElasticsearchNodesType, String> elasticsearchServerHosts = getFreeHostsOnSubnet();
        logger.info("Elasticsearch cluster nodes ip addresses");
        String elasticsearchNodesIps = "";
        for(Map.Entry<ElasticsearchNodesType, String> entry : elasticsearchServerHosts.entrySet()) {
            elasticsearchNodesIps = elasticsearchNodesIps + "\n" + entry.getKey() + " - " + entry.getValue();
        }
        logger.info("Elasticsearch cluster nodes ip addresses:"+ elasticsearchNodesIps);
        EnumMap<DockerServicePortType, String> elasticsearchSquidDockerServicesPorts = getElasticsearchSquidFreePorts();
        esUrl = "http://127.0.0.1:" + elasticsearchSquidDockerServicesPorts.get(DockerServicePortType.ES01_SP);
        esUrlProxy = "http://" + elasticsearchServerHosts.get(ElasticsearchNodesType.ES_NODE_01_IP_ADDRESS) + ":9200";
        proxyPort = elasticsearchSquidDockerServicesPorts.get(DockerServicePortType.SQUID_SP);
        proxyAuthPort = elasticsearchSquidDockerServicesPorts.get(DockerServicePortType.SQUID_AUTH_SP);
        clearElasticsearchSquidDocker();
        startElasticsearchSquidDocker(elasticsearchSquidDockerServicesPorts, elasticsearchServerHosts, network);
    }


    @AfterClass
    public static  void clearContainers() throws Exception {
        logger.info("Waiting for docker containers to stop...");
        clearElasticsearchSquidDocker();
        if (!networkExisted){
            logger.info("Removing es_squid network ...");
            String closeNetworkCommand = "docker network rm " + network;
            runShellCommandWithLogs(closeNetworkCommand);
        }
    }


    @Before
    public void once() throws IOException {
        docExample = TestPutElasticsearchHttp.getDocExample();
    }

    @After
    public void teardown() {
        runner = null;
    }

    @Test
    public void testPutElasticSearchHttpRecordBasic() throws InitializationException {
        logger.info("Starting test " + new Object() {
        }.getClass().getEnclosingMethod().getName());
        runner = TestRunners.newTestRunner(new PutElasticsearchHttpRecord());
        MockRecordParser recordReader = new MockRecordParser();
        recordReader.addSchemaField("id", RecordFieldType.INT);
        recordReader.addSchemaField("name", RecordFieldType.STRING);
        recordReader.addSchemaField("code", RecordFieldType.INT);
        recordReader.addSchemaField("date", RecordFieldType.DATE);
        recordReader.addSchemaField("time", RecordFieldType.TIME);
        recordReader.addSchemaField("ts",RecordFieldType.TIMESTAMP);
        recordReader.addSchemaField("routing",RecordFieldType.STRING);

        runner.addControllerService("reader", recordReader);
        runner.enableControllerService(recordReader);
        runner.setProperty(PutElasticsearchHttpRecord.RECORD_READER, "reader");
        runner.setProperty(PutElasticsearchHttpRecord.ES_URL, esUrl);
        runner.setProperty(PutElasticsearchHttpRecord.INDEX, "doc");
        runner.setProperty(PutElasticsearchHttpRecord.TYPE, "status");
        runner.setProperty(PutElasticsearchHttpRecord.ID_RECORD_PATH, "/id");
        runner.setProperty(PutElasticsearchHttpRecord.ROUTING_RECORD_PATH,"/routing");
        runner.assertValid();
        Date date = new Date(new java.util.Date().getTime());
        Time time = new Time(System.currentTimeMillis());
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        recordReader.addRecord(0,"rec",100,date,time,timestamp,"user_0");

        runner.enqueue(new byte[0], new HashMap<String, String>() {{
            put("doc_id", "28039652140");
        }});

        runner.enqueue(new byte[0]);
        runner.run(1, true, true);
        runner.assertAllFlowFilesTransferred(PutElasticsearchHttpRecord.REL_SUCCESS, 1);
        List<ProvenanceEventRecord> provEvents = runner.getProvenanceEvents();
        assertNotNull(provEvents);
        assertEquals(1, provEvents.size());
        assertEquals(ProvenanceEventType.SEND, provEvents.get(0).getEventType());
    }

    @Test
    public void testPutElasticSearchHttpRecordBatch() throws InitializationException {
        logger.info("Starting test " + new Object() {
        }.getClass().getEnclosingMethod().getName());
        runner = TestRunners.newTestRunner(new PutElasticsearchHttpRecord());
        MockRecordParser recordReader = new MockRecordParser();
        recordReader.addSchemaField("id", RecordFieldType.INT);
        recordReader.addSchemaField("name", RecordFieldType.STRING);
        recordReader.addSchemaField("code", RecordFieldType.INT);
        recordReader.addSchemaField("date", RecordFieldType.DATE);
        recordReader.addSchemaField("time", RecordFieldType.TIME);
        recordReader.addSchemaField("ts",RecordFieldType.TIMESTAMP);
        recordReader.addSchemaField("routing",RecordFieldType.STRING);

        runner.addControllerService("reader", recordReader);
        runner.enableControllerService(recordReader);
        runner.setProperty(PutElasticsearchHttpRecord.RECORD_READER, "reader");
        runner.setProperty(PutElasticsearchHttpRecord.ES_URL, esUrl);
        runner.setProperty(PutElasticsearchHttpRecord.INDEX, "doc");
        runner.setProperty(PutElasticsearchHttpRecord.TYPE, "status");
        runner.setProperty(PutElasticsearchHttpRecord.ID_RECORD_PATH, "/id");
        runner.setProperty(PutElasticsearchHttpRecord.ROUTING_RECORD_PATH,"/routing");
        runner.assertValid();


        for (int i = 1; i < 101; i++) {
            Date date = new Date(new java.util.Date().getTime());
            Time time = new Time(System.currentTimeMillis());
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            recordReader.addRecord(i,"rec"+i,100+i,date,time,timestamp,"user_"+i);
            String newStrId = Integer.toString(i);
            runner.enqueue(new byte[0], new HashMap<String, String>() {{
                put("doc_id", newStrId);
            }});
            runner.run(1,true,true);
        }
        runner.assertAllFlowFilesTransferred(PutElasticsearchHttpRecord.REL_SUCCESS, 100);
    }
    @Test
    public void testPutElasticSearchHttpBasic() throws IOException {
        logger.info("Starting test " + new Object() {
        }.getClass().getEnclosingMethod().getName());
        final TestRunner  runner = TestRunners.newTestRunner(new PutElasticsearchHttp());
        byte[] docExample = TestPutElasticsearchHttp.getDocExample();
        runner.setProperty(AbstractElasticsearchHttpProcessor.ES_URL, esUrl);
        runner.setProperty(PutElasticsearchHttp.INDEX, "doc");
        runner.setProperty(PutElasticsearchHttp.BATCH_SIZE, "1");
        runner.setProperty(PutElasticsearchHttp.TYPE, "status");
        runner.setProperty(PutElasticsearchHttp.ID_ATTRIBUTE, "doc_id");
        runner.setProperty(PutElasticsearchHttp.ROUTING_ATTRIBUTE, "doc_routing");
        runner.assertValid();

        runner.enqueue(docExample, new HashMap<String, String>() {{
            put("doc_id", "28039652140");
            put("doc_routing", "new_user");
        }});

        runner.enqueue(docExample);
        runner.run(1, true, true);
        runner.assertAllFlowFilesTransferred(PutElasticsearchHttp.REL_SUCCESS, 1);
    }

    @Test
    public void testPutElasticSearchHttpBatch() throws IOException {
        logger.info("Starting test " + new Object() {
        }.getClass().getEnclosingMethod().getName());
        final TestRunner  runner = TestRunners.newTestRunner(new PutElasticsearchHttp());
        byte[] docExample = TestPutElasticsearchHttp.getDocExample();
        runner.setProperty(AbstractElasticsearchHttpProcessor.ES_URL, esUrl);
        runner.setProperty(PutElasticsearchHttp.INDEX, "doc");
        runner.setProperty(PutElasticsearchHttp.BATCH_SIZE, "100");
        runner.setProperty(PutElasticsearchHttp.TYPE, "status");
        runner.setProperty(PutElasticsearchHttp.ID_ATTRIBUTE, "doc_id");
        runner.setProperty(PutElasticsearchHttp.ROUTING_ATTRIBUTE, "doc_routing");

        runner.assertValid();

        for (int i = 0; i < 100; i++) {
            long newId = 28039652140L + i;
            final String newStrId = Long.toString(newId);
            int finalI = i;
            runner.enqueue(docExample, new HashMap<String, String>() {{
                put("doc_id", newStrId);
                put("doc_routing", "new_user" + finalI);
            }});
        }
        runner.run();
        runner.assertAllFlowFilesTransferred(PutElasticsearchHttp.REL_SUCCESS, 100);
    }
    @Test
    public void testFetchElasticsearchBasic() throws IOException {
        logger.info("Starting test " + new Object() {
        }.getClass().getEnclosingMethod().getName());
        final TestRunner runner = TestRunners.newTestRunner(new FetchElasticsearchHttp());
        testPutElasticSearchHttpBasic();
        //Local Cluster - Mac pulled from brew
        runner.setProperty(AbstractElasticsearchHttpProcessor.ES_URL, esUrl);
        runner.setProperty(FetchElasticsearchHttp.INDEX, "doc");
        runner.setProperty(FetchElasticsearchHttp.TYPE, "status");
        runner.setProperty(FetchElasticsearchHttp.DOC_ID, "${doc_id}");
        runner.assertValid();

        runner.enqueue(docExample, new HashMap<String, String>() {{
            put("doc_id", "28039652140");
        }});

        runner.enqueue(docExample);
        runner.run(1, true, true);
        runner.assertAllFlowFilesTransferred(FetchElasticsearchHttp.REL_SUCCESS, 1);
    }

    @Test
    public void testFetchElasticsearchBatch() throws IOException {
        logger.info("Starting test " + new Object() {
        }.getClass().getEnclosingMethod().getName());
        runner = TestRunners.newTestRunner(new FetchElasticsearchHttp());
        testPutElasticSearchHttpBatch();
        //Local Cluster - Mac pulled from brew
        runner.setProperty(AbstractElasticsearchHttpProcessor.ES_URL, esUrl);
        runner.setProperty(FetchElasticsearchHttp.INDEX, "doc");
        runner.setProperty(FetchElasticsearchHttp.TYPE, "status");
        runner.setProperty(FetchElasticsearchHttp.DOC_ID, "${doc_id}");
        runner.assertValid();

        for (int i = 0; i < 100; i++) {
            long newId = 28039652140L + i;
            final String newStrId = Long.toString(newId);
            runner.enqueue(docExample, new HashMap<String, String>() {{
                put("doc_id", newStrId);
            }});
        }
        runner.run(100);
        runner.assertAllFlowFilesTransferred(FetchElasticsearchHttp.REL_SUCCESS, 100);
    }
    @Test
    public void testPutElasticSearchBasicBehindProxy() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new PutElasticsearchHttp());
        byte[] docExample = TestPutElasticsearchHttp.getDocExample();
        runner.setValidateExpressionUsage(false);

        runner.setProperty(PutElasticsearchHttp.INDEX, "doc");
        runner.setProperty(PutElasticsearchHttp.BATCH_SIZE, "1");
        runner.setProperty(PutElasticsearchHttp.TYPE, "status");
        runner.setProperty(PutElasticsearchHttp.ID_ATTRIBUTE, "doc_id");

        runner.setProperty(PutElasticsearchHttp.PROXY_HOST, "localhost");
        runner.setProperty(PutElasticsearchHttp.PROXY_PORT, proxyPort);
        runner.setProperty(PutElasticsearchHttp.ES_URL, esUrlProxy);
        runner.assertValid();

        runner.enqueue(docExample, new HashMap<String, String>() {{
            put("doc_id", "28039652140");
        }});

        runner.enqueue(docExample);
        runner.run(1, true, true);
        runner.assertAllFlowFilesTransferred(PutElasticsearchHttp.REL_SUCCESS, 1);
    }

    @Test
    public void testFetchElasticsearchBasicBehindProxy() throws IOException {
        testPutElasticSearchBasicBehindProxy();
        runner = TestRunners.newTestRunner(new FetchElasticsearchHttp());
        runner.setValidateExpressionUsage(true);

        runner.setProperty(FetchElasticsearchHttp.INDEX, "doc");
        runner.setProperty(FetchElasticsearchHttp.TYPE, "status");
        runner.setProperty(FetchElasticsearchHttp.DOC_ID, "${doc_id}");

        runner.setProperty(FetchElasticsearchHttp.PROXY_HOST, "localhost");
        runner.setProperty(FetchElasticsearchHttp.PROXY_PORT, proxyPort);
        runner.setProperty(FetchElasticsearchHttp.ES_URL, esUrlProxy);

        runner.assertValid();

        runner.enqueue(docExample, new HashMap<String, String>() {{
            put("doc_id", "28039652140");
        }});

        runner.enqueue(docExample);
        runner.run(1, true, true);
        runner.assertAllFlowFilesTransferred(FetchElasticsearchHttp.REL_SUCCESS, 1);
    }
    @Test
    public void testPutElasticSearchBasicBehindAuthenticatedProxy() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(new PutElasticsearchHttp());
        byte[] docExample = TestPutElasticsearchHttp.getDocExample();
        runner.setValidateExpressionUsage(false);

        runner.setProperty(PutElasticsearchHttp.INDEX, "doc");
        runner.setProperty(PutElasticsearchHttp.BATCH_SIZE, "1");
        runner.setProperty(PutElasticsearchHttp.TYPE, "status");
        runner.setProperty(PutElasticsearchHttp.ID_ATTRIBUTE, "doc_id");
        runner.setProperty(PutElasticsearchHttp.ROUTING_ATTRIBUTE, "new_user");

        runner.setProperty(PutElasticsearchHttp.PROXY_HOST, "localhost");
        runner.setProperty(PutElasticsearchHttp.PROXY_PORT, proxyAuthPort);
        runner.setProperty(PutElasticsearchHttp.PROXY_USERNAME, "proxy-squid");
        runner.setProperty(PutElasticsearchHttp.PROXY_PASSWORD, "changeme");
        runner.setProperty(PutElasticsearchHttp.ES_URL, esUrlProxy);


        runner.assertValid();

        runner.enqueue(docExample, new HashMap<String, String>() {{
            put("doc_id", "28039652140");
        }});

        runner.enqueue(docExample);
        runner.run(1, true, true);
        runner.assertAllFlowFilesTransferred(PutElasticsearchHttp.REL_SUCCESS, 1);
    }

    @Test
    public void testFetchElasticsearchBasicBehindAuthenticatedProxy() throws IOException {
        testPutElasticSearchBasicBehindAuthenticatedProxy();
        runner = TestRunners.newTestRunner(new FetchElasticsearchHttp());
        runner.setValidateExpressionUsage(true);

        runner.setProperty(FetchElasticsearchHttp.INDEX, "doc");
        runner.setProperty(FetchElasticsearchHttp.TYPE, "status");
        runner.setProperty(FetchElasticsearchHttp.DOC_ID, "${doc_id}");

        runner.setProperty(FetchElasticsearchHttp.PROXY_HOST, "localhost");
        runner.setProperty(FetchElasticsearchHttp.PROXY_PORT, proxyAuthPort);
        runner.setProperty(FetchElasticsearchHttp.PROXY_USERNAME, "proxy-squid");
        runner.setProperty(FetchElasticsearchHttp.PROXY_PASSWORD, "changeme");
        runner.setProperty(FetchElasticsearchHttp.ES_URL, esUrlProxy);

        runner.assertValid();

        runner.enqueue(docExample, new HashMap<String, String>() {{
            put("doc_id", "28039652140");
        }});

        runner.enqueue(docExample);
        runner.run(1, true, true);
        runner.assertAllFlowFilesTransferred(FetchElasticsearchHttp.REL_SUCCESS, 1);
    }
}
