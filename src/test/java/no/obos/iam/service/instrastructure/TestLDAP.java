package no.obos.iam.service.instrastructure;

import junit.framework.TestCase;
import no.obos.iam.service.EmbeddedADS;

import javax.naming.*;
import javax.naming.directory.*;
import java.io.File;
import java.util.*;

public class TestLDAP extends TestCase {

    private EmbeddedADS ads;
    final static String ldapServerName = "localhost";
    final static String rootdn = "uid=admin,ou=system";
    final static String rootpass = "secret";
    final static String rootContext = "dc=external,dc=OBOS,dc=no";
    final static int serverPort = 10367;


    public void setUp() throws Exception {
        File workDir = new File("/tmp/testldap/ldap");
        workDir.mkdirs();
        // Create the server
        ads = new EmbeddedADS(workDir);
        ads.startServer(serverPort);
        super.setUp();
        Thread.sleep(1000);
    }

    @Override
    protected void tearDown() throws Exception {
        ads.stopServer();
        super.tearDown();
    }

    public void testLDAPSimple() {
        // set up environment to access the server

        Properties env = new Properties();

        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + ldapServerName + ":" + serverPort + "/" + rootContext);
        env.put(Context.SECURITY_PRINCIPAL, rootdn);
        env.put(Context.SECURITY_CREDENTIALS, rootpass);

        try {
            // obtain initial directory context using the environment
            DirContext ctx = new InitialDirContext(env);

            // create some random number to add to the directory
            Integer i = new Integer(new Random().nextInt());

//            System.out.println("Adding " + i + " to directory...");
            ctx.bind("cn=myRandomInt", i);

            i = new Integer(98765);
//            System.out.println("i is now: " + i);

            i = (Integer) ctx.lookup("cn=myRandomInt");
//            System.out.println("Retrieved i from directory with value: " + i);
        } catch (NameAlreadyBoundException nabe) {
            System.err.println("value has already been bound!");
        } catch (NameNotFoundException nnfe) {
            System.err.println("Name not found!");

        } catch (Exception e) {
            System.err.println(e);
            assertFalse("Unsuccessful LDAP simple test",false);
        }
    }
}
