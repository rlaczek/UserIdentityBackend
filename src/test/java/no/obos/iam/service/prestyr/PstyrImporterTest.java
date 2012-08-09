package no.obos.iam.service.prestyr;

import junit.framework.TestCase;
import no.obos.iam.service.EmbeddedADS;
import no.obos.iam.service.dataimport.PstyrImporter;
import no.obos.iam.service.domain.FreeCodeUserIdentity;
import no.obos.iam.service.domain.UserPropertyAndRole;
import no.obos.iam.service.helper.LDAPHelper;
import no.obos.iam.service.repository.BackendConfigDataRepository;
import no.obos.iam.service.repository.UserPropertyAndRoleRepository;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

import java.io.File;
import java.util.List;

public class PstyrImporterTest extends TestCase{
    private final static String basepath = "/tmp/pstyrimp/";
    private final static String dbpath = basepath + "hsqldb/roles";
    private final static String ldappath = basepath + "hsqldb/ldap/";
    private final static int LDAP_PORT = 10935;

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    static public boolean deleteDirectory(File path) {
        if( path.exists() ) {
          File[] files = path.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return( path.delete() );
    }

    public void testImport() throws Exception {
        deleteDirectory(new File(basepath));
        Directory index = new NIOFSDirectory(new File(basepath + "lucene"));

        File ldapdir = new File(ldappath);
        ldapdir.mkdirs();
        EmbeddedADS ads = new EmbeddedADS(ldappath);
        ads.startServer(LDAP_PORT);
        PstyrImporter pstyrImporter = new PstyrImporter();
        LDAPHelper ldapHelper = new LDAPHelper("ldap://localhost:10935/dc=external,dc=OBOS,dc=no", "uid=admin,ou=system", "secret", "initials");
        pstyrImporter.setLdapHelper(ldapHelper);
        UserPropertyAndRoleRepository userPropertyAndRoleRepository = new UserPropertyAndRoleRepository();
        pstyrImporter.setRepo(userPropertyAndRoleRepository);
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:file:" + dbpath);
        QueryRunner queryRunner = new QueryRunner(dataSource);
        pstyrImporter.setQueryRunner(queryRunner);
        userPropertyAndRoleRepository.setQueryRunner(queryRunner);
        BackendConfigDataRepository bcdr = new BackendConfigDataRepository();
        bcdr.setQueryRunner(queryRunner);
        userPropertyAndRoleRepository.setBackendConfigDataRepository(bcdr);

        pstyrImporter.importer("testxstyr.csv", index);
        FreeCodeUserIdentity freeCodeUserIdentity = ldapHelper.getUserinfo("bentelongva@hotmail.com");
        assertNotNull(freeCodeUserIdentity);
        assertEquals("bentelongva@hotmail.com", freeCodeUserIdentity.getBrukernavn());
        assertEquals("bentelongva@hotmail.com", freeCodeUserIdentity.getEmail());
        assertNotNull(freeCodeUserIdentity.getPersonRef());
        assertNotNull(freeCodeUserIdentity.getUid());
        assertNotNull(freeCodeUserIdentity.getFirstName());
        assertNotNull(freeCodeUserIdentity.getLastName());
        List<UserPropertyAndRole> roles = userPropertyAndRoleRepository.getUserPropertyAndRoles(freeCodeUserIdentity.getUid());
        assertNotNull(roles);
        assertEquals(1, roles.size());
        UserPropertyAndRole role = roles.get(0);
        assertEquals(freeCodeUserIdentity.getUid(), role.getUid());
        assertEquals("0001", role.getOrgId());
        assertEquals("VM", role.getRoleName());
        ads.stopServer();
    }
}
