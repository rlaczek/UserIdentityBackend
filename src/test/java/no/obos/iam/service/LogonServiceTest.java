package no.obos.iam.service;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.header.MediaTypes;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import static org.junit.Assert.*;

import no.obos.iam.service.config.AppConfig;
import no.obos.iam.service.prestyr.PstyrImporterTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;

public class LogonServiceTest {
    private static URI baseUri;
    Client restClient;
    private static Main uib;

    @BeforeClass
    public static void init() throws Exception {
        PstyrImporterTest.deleteDirectory(new File("/tmp/ssotest/"));
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_JUNIT);
        uib = new Main();
        uib.startEmbeddedDS();
        uib.importData();
        uib.startServer();
        baseUri = UriBuilder.fromUri("http://localhost/uib/").port(uib.getPort()).build();
    }

    @AfterClass
    public static void cleanup() {
        uib.stop();
        PstyrImporterTest.deleteDirectory(new File("/tmp/ssotest/"));
    }

    @Before
    public void initRun() throws Exception {
        restClient = Client.create();
    }

    @Test
    public void welcome() {
        WebResource webResource = restClient.resource(baseUri);
        String s = webResource.get(String.class);
        assertTrue(s.contains("OBOS"));
        assertTrue(s.contains("<FORM"));
        assertFalse(s.contains("backtrace"));
    }

    /**
     * Test if a WADL document is available at the relative path
     * "application.wadl".
     */
    @Test
    public void testApplicationWadl() {
        WebResource webResource = restClient.resource(baseUri);
        String serviceWadl = webResource.path("application.wadl").
                accept(MediaTypes.WADL).get(String.class);
//        System.out.println("WADL:"+serviceWadl);
        assertTrue(serviceWadl.length() > 60);
    }

    @Test
    public void formLogonOK() throws IOException {
        WebResource webResource = restClient.resource(baseUri);
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("username", "bentelongva@hotmail.com");
        formData.add("password", "061073");
        ClientResponse response = webResource.path("logon").type("application/x-www-form-urlencoded").post(ClientResponse.class, formData);
        String responseBody = response.getEntity(String.class);
//        System.out.println(responseBody);
        assertTrue(responseBody.contains("Logon ok"));
        assertTrue(responseBody.contains("bentelongva@hotmail.com"));
    }

    @Test
    public void formLogonFail() throws IOException {
        WebResource webResource = restClient.resource(baseUri);
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("username", "bentelongva@hotmail.com");
        formData.add("password", "vrangt");
        ClientResponse response = webResource.path("logon").type("application/x-www-form-urlencoded").post(ClientResponse.class, formData);
        String responseBody = response.getEntity(String.class);
//        System.out.println(responseBody);

        assertTrue(responseBody.contains("failed"));
        assertFalse(responseBody.contains("obosUser"));
    }

    @Test
    public void XMLLogonOK() throws IOException {
        WebResource webResource = restClient.resource(baseUri);
        String payload = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><authgreier><auth><dilldall>dilldall</dilldall><user><username>bentelongva@hotmail.com</username><coffee>yes please</coffee><password>061073</password></user></auth></authgreier>";
        ClientResponse response = webResource.path("logon").type("application/xml").post(ClientResponse.class, payload);
        String responseXML = response.getEntity(String.class);
//        System.out.println(responseXML);

        assertTrue(responseXML.contains("obosUser"));
        assertTrue(responseXML.contains("Styrerommet"));
    }

    @Test
    public void XMLLogonFail() throws IOException {
        WebResource webResource = restClient.resource(baseUri);
        String payload = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><authgreier><auth><dilldall>dilldall</dilldall><user><username>bentelongva@hotmail.com</username><coffee>yes please</coffee><password>vrangt</password></user></auth></authgreier>";
        ClientResponse response = webResource.path("logon").type("application/xml").post(ClientResponse.class, payload);
        String responseXML = response.getEntity(String.class);
//        System.out.println(responseXML);

        assertTrue(responseXML.contains("logonFailed"));
        assertFalse(responseXML.contains("obosUser"));
    }

}
