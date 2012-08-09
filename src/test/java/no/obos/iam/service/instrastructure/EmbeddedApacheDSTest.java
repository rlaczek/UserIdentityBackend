package no.obos.iam.service.instrastructure;


import junit.framework.TestCase;
import no.obos.iam.service.EmbeddedADS;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class EmbeddedApacheDSTest extends TestCase {
    private EmbeddedADS ads;
    public void setUp() throws Exception {
        File workDir = new File(System.getProperty("user.home") + "/pstyr/ldap");
        workDir.mkdirs();
        // Create the server
        ads = new EmbeddedADS(workDir);

        // optionally we can start a server too
        ads.startServer(10368);
        super.setUp();
        Thread.sleep(2000);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        ads.stopServer();
    }

    public void testProcess() throws Exception {
/**        LDAPHelper ldap = new LDAPHelper();
        //System.in.read();
        try {
            ldap.addUser("ostroy", "Roy", "Ost", "myPW");
        } catch (NamingException e) {

        }*/
        Thread.sleep(30);
    }
}
