package no.obos.iam.service;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import no.obos.iam.service.config.AppConfig;
import no.obos.iam.service.mail.MockMail;
import no.obos.iam.service.prestyr.PstyrImporterTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.net.URI;

import static org.junit.Assert.*;

public class UserAdminTest {
    private static WebResource baseResource;
    private static WebResource logonResource;
    private static Main uib;

    @BeforeClass
    public static void init() throws Exception {
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_JUNIT);
        PstyrImporterTest.deleteDirectory(new File("/tmp/ssotest/"));
        uib = new Main();
        uib.startEmbeddedDS();
        uib.importData();
        uib.startServer();
        URI baseUri = UriBuilder.fromUri("http://localhost/uib/useradmin/").port(uib.getPort()).build();
        URI logonUri = UriBuilder.fromUri("http://localhost/uib/").port(uib.getPort()).build();
        //String usertoken = "usrtk1";
        baseResource = Client.create().resource(baseUri)/*.path(usertoken + '/')*/;
        logonResource = Client.create().resource(logonUri);
    }

    @AfterClass
    public static void teardown() throws Exception {
        uib.stop();
        PstyrImporterTest.deleteDirectory(new File("/tmp/ssotest/"));
    }

    @Test
    public void getuser() {
        WebResource webResource = baseResource.path("users/bartek.milewski@gmail.com");
        String s = webResource.get(String.class);
        System.out.println(s);
        assertTrue(s.contains("\"firstName\":\"BARTOSZ\""));
    }

    @Test
    public void getuserapps() {
        WebResource webResource = baseResource.path("users/bentelongva@hotmail.com/applications");
        String s = webResource.get(String.class);
        System.out.println(s);
        assertFalse(s.contains("\"firstName\":\"BENTE\""));
        assertTrue(s.contains("\"appId\" : \"201\""));
        assertTrue(s.contains("\"appId\" : \"101\""));
        assertTrue(s.contains("\"hasRoles\" : true"));
        assertTrue(s.contains("\"hasRoles\" : false"));
    }

    @Test
    public void getuserroles() {
        WebResource webResource = baseResource.path("users/bentelongva@hotmail.com/201/");
        String s = webResource.get(String.class);
        System.out.println(s);
        assertFalse(s.contains("\"firstName\":\"BENTE\""));
        assertTrue(s.contains("\"appId\": \"201\""));
    }

    @Test
    public void adduserrole() {
        WebResource webResource = baseResource.path("users/bentelongva@hotmail.com/50");
        webResource.path("/add").type("application/json").post(String.class, "{\"orgID\": \"0005\",\n" +
                "        \"roleName\": \"KK\",\n" +
                "        \"roleValue\": \"test\"}");
        String s = webResource.get(String.class);
        assertTrue(s.contains("KK"));
        WebResource webResource2 = baseResource.path("users/bentelongva@hotmail.com");
        s = webResource2.get(String.class);
//        System.out.println("Roller etter: " + s);
        assertTrue(s.contains("KK"));
    }

    @Test
    public void adduserroleNoJson() {
        WebResource webResource = baseResource.path("users/bentelongva@hotmail.com/201");
        try {
            String s = webResource.path("/add").type("application/json").post(String.class, "");
            System.out.println(s);
            fail("Expected 400, got " + s);
        } catch (UniformInterfaceException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void adduserroleBadJson() {
        WebResource webResource = baseResource.path("users/bentelongva@hotmail.com/201");
        try {
            String s = webResource.path("/add").type("application/json").post(String.class, "{ dilldall }");
            System.out.println(s);
            fail("Expected 400, got " + s);
        } catch (UniformInterfaceException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void addDefaultUserrole() {
        WebResource webResource = baseResource.path("users/bentelongva@hotmail.com/1");
        webResource.path("/adddefaultrole").get(String.class);
        String s = webResource.get(String.class);
//        System.out.println(s);
        assertTrue(s.contains("Brukeradmin"));
        WebResource webResource2 = baseResource.path("users/bentelongva@hotmail.com");
        s = webResource2.get(String.class);
//        System.out.println("Roller etter: " + s);
        assertTrue(s.contains("Brukeradmin"));
    }

    @Test
    public void addExistingUserrole() {
        WebResource webResource = baseResource.path("users/bentelongva@hotmail.com/201");
        try {
            String s = webResource.path("/add").type("application/json").post(String.class, "{\"orgID\": \"0001\",\n" +
                    "        \"roleName\": \"VM\",\n" +
                    "        \"roleValue\": \"test\"}");
            System.out.println(s);
            fail("Expected 409, got " + s);
        } catch (UniformInterfaceException e) {
            assertEquals(409, e.getResponse().getStatus());
        }
    }

    @Test
    public void deleteuserrole() {
        WebResource webResource = baseResource.path("users/stig.roger@sjoenden.net/201");
        String s = webResource.get(String.class);
        assertTrue(s.contains("VM"));
        webResource.path("/delete").type("application/json").post(String.class, "{\"orgID\": \"0001\", \"roleName\": \"VM\"}");
        s = webResource.get(String.class);
        assertFalse(s.contains("VM"));
        WebResource webResource2 = baseResource.path("users/stig.roger@sjoenden.net");
        s = webResource2.get(String.class);
        assertFalse(s.contains("VM"));
    }

    @Test
    public void deleteAllAppuserroles() {
        WebResource webResource = baseResource.path("users/wenche@bladcentralen.no/201");
        String s = webResource.get(String.class);
        assertTrue(s.contains("VM"));
        webResource.path("/deleteall").get(String.class);
        WebResource webResource2 = baseResource.path("users/wenche@bladcentralen.no");
        s = webResource2.get(String.class);
        assertFalse(s.contains("VM"));
    }

    @Test
    public void modifyuserrole() {
        WebResource webResource = baseResource.path("users/c.roberg@gmail.com/201");
        String s = webResource.get(String.class);
        System.out.println(s);
        assertTrue(s.contains("VM"));
        webResource.type("application/json").put(String.class, "{\"orgID\": \"0070\", \"roleName\": \"VM\", \"roleValue\" : \"flott\"}");
        s = webResource.get(String.class);
        System.out.println(s);
        assertTrue(s.contains("flott"));
    }

    @Test
    public void userexists() {
        WebResource webResource = baseResource.path("users/bentelongva@hotmail.com/exists");
        String s = webResource.get(String.class);
        assertTrue(s.contains("true"));
    }

    @Test
    public void usernotexists() {
        WebResource webResource = baseResource.path("users/eggbert@hotmail.com/exists");
        String s = webResource.get(String.class);
        assertTrue(s.contains("false"));
    }

    @Test
    public void getnonexistinguser() {
        WebResource webResource = baseResource.path("users/bantelonga@gmail.com");
        try {
            String s = webResource.get(String.class);
            fail("Expected 404, got " + s);
        } catch(UniformInterfaceException e) {
            assertEquals(404, e.getResponse().getStatus());
        }
    }

    @Test
    public void addUser() {
        String userjson = "{\n" +
                " \"personRef\":\"riffraff\",\n" +
                " \"brukernavn\":\"snyper\",\n" +
                " \"firstName\":\"Edmund\",\n" +
                " \"lastName\":\"Gøffse\",\n" +
                " \"email\":\"snyper@midget.orj\",\n" +
                " \"cellPhone\":\"12121212\"\n" +
                "}";
        WebResource webResource = baseResource.path("users/add");
        webResource.type("application/json").post(String.class, userjson);
        String s = baseResource.path("users/snyper").get(String.class);
        assertTrue(s.contains("snyper@midget.orj"));
        assertTrue(s.contains("Edmund"));
        s = baseResource.path("find/snyper").get(String.class);
        assertTrue(s.contains("snyper@midget.orj"));
        assertTrue(s.contains("Edmund"));
    }

    @Test
    public void resetAndChangePassword() {
        String userjson = "{\n" +
                " \"personRef\":\"123123123\",\n" +
                " \"brukernavn\":\"sneile\",\n" +
                " \"firstName\":\"Effert\",\n" +
                " \"lastName\":\"Huffse\",\n" +
                " \"email\":\"sneile@midget.orj\",\n" +
                " \"cellPhone\":\"21212121\"\n" +
                "}";
        WebResource webResource = baseResource.path("users/add");
        webResource.type("application/json").post(String.class, userjson);
        baseResource.path("users/sneile/resetpassword").type("application/json").get(String.class);
        String token = uib.getInjector().getInstance(MockMail.class).getToken("sneile");
        assertNotNull(token);

        ClientResponse response = baseResource.path("users/sneile/newpassword/" + token).type(MediaType.APPLICATION_JSON).post(ClientResponse.class,"{\"newpassword\":\"naLLert\"}");
        assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
        String payload = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><auth><username>sneile</username><password>naLLert</password></auth>";
        response = logonResource.path("logon").type("application/xml").post(ClientResponse.class, payload);
        assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
        String identity = response.getEntity(String.class);
        assertTrue(identity.contains("identity"));
        assertTrue(identity.contains("sneile"));
    }

    @Test
    public void newUserFromEmail() {
        String userjson = "{\n" +
                " \"personRef\":\"43234321\",\n" +
                " \"brukernavn\":\"gvarvar@midget.orc\",\n" +
                " \"firstName\":\"Gvarveig\",\n" +
                " \"lastName\":\"Neskle\",\n" +
                " \"email\":\"gvarvar@midget.orc\",\n" +
                " \"cellPhone\":\"43434343\"\n" +
                "}";
        WebResource webResource = baseResource.path("users/add");
        webResource.type("application/json").post(String.class, userjson);
        baseResource.path("users/gvarvar@midget.orc/resetpassword").type("application/json").get(String.class);
        String token = uib.getInjector().getInstance(MockMail.class).getToken("gvarvar@midget.orc");
        assertNotNull(token);

        String newUserPayload = "{\"newpassword\":\"zePelin32\", \"newusername\":\"gvarnes\"}";
        ClientResponse response = baseResource.path("users/gvarvar@midget.orc/newuser/" + token).type(MediaType.APPLICATION_JSON).post(ClientResponse.class, newUserPayload);
        assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
        String payload = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><auth><username>gvarnes</username><password>zePelin32</password></auth>";
        response = logonResource.path("logon").type("application/xml").post(ClientResponse.class, payload);
        assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
        String identity = response.getEntity(String.class);
        System.out.println(identity);
        assertTrue(identity.contains("identity"));
        assertTrue(identity.contains("gvarnes"));
    }

    @Test
    public void modifyUser() {
        String userjson = "{\n" +
                " \"personRef\":\"1231312\",\n" +
                " \"brukernavn\":\"siqula\",\n" +
                " \"firstName\":\"Hoytahl\",\n" +
                " \"lastName\":\"Gøffse\",\n" +
                " \"email\":\"siqula@midget.orj\",\n" +
                " \"cellPhone\":\"12121212\"\n" +
                "}";
        String updateduserjson = "{\n" +
                " \"personRef\":\"1231312\",\n" +
                " \"brukernavn\":\"siqula\",\n" +
                " \"firstName\":\"Harald\",\n" +
                " \"lastName\":\"Gøffse\",\n" +
                " \"email\":\"siqula@midget.orj\",\n" +
                " \"cellPhone\":\"35353535\"\n" +
                "}";
        baseResource.path("users/add").type("application/json").post(String.class, userjson);
        String s = baseResource.path("users/siqula").get(String.class);
        assertTrue(s.contains("siqula@midget.orj"));
        assertTrue(s.contains("Hoytahl"));
        assertTrue(s.contains("12121212"));

        baseResource.path("users/siqula").type("application/json").put(String.class, updateduserjson);
        s = baseResource.path("users/siqula").get(String.class);
        assertTrue(s.contains("siqula@midget.orj"));
        assertTrue(s.contains("Harald"));
        assertTrue(s.contains("35353535"));
    }

    @Test
    public void find() {
        WebResource webResource = baseResource.path("find/bente");
        String s = webResource.get(String.class);
        assertTrue(s.contains("\"firstName\":\"BENTE\""));
    }

    @Test
    public void deleteUser() {
        WebResource webResource = baseResource.path("users/frustaalstrom@gmail.com");
        String s = webResource.get(String.class);
        assertTrue(s.contains("frustaalstrom"));
        webResource.path("/delete").get(String.class);
        try {
            s = webResource.get(String.class);
            fail("Expected 404, got " + s);
        } catch(UniformInterfaceException e) {
            assertEquals(404, e.getResponse().getStatus());
        }
    }

    @Test
    public void deleteUserNotFound() {
        WebResource webResource = baseResource.path("users/dededede@hotmail.com/delete");
        try {
            String s = webResource.get(String.class);
            fail("Expected 404, got " + s);
        } catch(UniformInterfaceException e) {
            assertEquals(404, e.getResponse().getStatus());
        }

    }


}
