package no.obos.iam.service.dataimport;

import com.google.inject.Inject;
import no.obos.iam.service.domain.FreeCodeUserIdentity;
import no.obos.iam.service.domain.UserPropertyAndRole;
import no.obos.iam.service.helper.LDAPHelper;
import no.obos.iam.service.helper.StringCleaner;
import no.obos.iam.service.repository.UserPropertyAndRoleRepository;
import no.obos.iam.service.search.Indexer;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.io.*;
import java.sql.SQLException;
import java.util.*;

public class PstyrImporter {
    private static final Logger logger = LoggerFactory.getLogger(PstyrImporter.class);

    public static final List<String> invoiceRoles = Arrays.asList("FM", "VF", "SM", "VA", "AND");
    public static final String APPID_INVOICE = "101";
    public static final String APPID_STYREROMMET = "201";
    public static final String APPID_BRUKERADMIN = "1";
    public static final String APPID_OBOSPERSONKUNDE = "50";

    private static final int BORETTSLAGSNR = 0;
//    private static final int MEDLEMSNR = 1;
    private static final int ROLLEKODE = 2;
    private static final int ETTERNAVN = 3;
    private static final int FORNAVN = 4;
//    private static final int ADRESSE = 5;
//    private static final int POSTSTED = 6;
//    private static final int POSTNR = 7;
//    private static final int TELEFON_KVELD = 8;
//    private static final int TELEFON_HJEMME = 9;
//    private static final int STYRET_REKKEFOLGE = 10;
    private static final int FODSELSDATO = 12;
    private static final int FODSELSNR = 13;
    private static final int TELEFON_MOBIL = 15;
    private static final int EPOST = 16;

    private static final int STYRE_AAR_FRA = 17;//?
    private static final int STYRE_AAR_TIL = 18;//?
//    private static final int STYRE_DATO_FRA = 19;//?
//    private static final int STYRE_DATO_TIL = 20;//?

    private static final StringCleaner stringCleaner = new StringCleaner();
    private final Set<String> userswithoutemail = new HashSet<String>();

    @Inject private LDAPHelper ldapHelper;
    @Inject private QueryRunner queryRunner;
    @Inject private UserPropertyAndRoleRepository repo;

    public void importer(String userImportSource, Directory index) {
        System.out.println("Importerer fra " + userImportSource);
        initDB();
        BufferedReader reader = null;
        try {
            InputStream classpathStream = PstyrImporter.class.getClassLoader().getResourceAsStream(userImportSource);
            System.out.println("classpathStream=" + classpathStream);
            reader = new BufferedReader(new InputStreamReader(classpathStream, "ISO-8859-1"));
            final Set<String> idsAddedToLdap = new HashSet<String>();
            Indexer indexer = new Indexer(index);
            final IndexWriter indexWriter = indexer.getWriter();
            String line;
            while ((line = reader.readLine()) != null) {
                final String[] columns = line.split(",");
                String personnummer = getString(columns, FODSELSDATO) + getString(columns, FODSELSNR);
                if(personnummer.length() == 11) {
                    FreeCodeUserIdentity user;
                    if(!idsAddedToLdap.contains(personnummer)) {
                        idsAddedToLdap.add(personnummer);
                        user = createUserIdentity(columns, personnummer);
                        if(user != null) {
                            String password = getString(columns, FODSELSDATO);
                            ldapHelper.addOBOSUserIdentity(user, password);
                            indexer.addToIndex(indexWriter, user);
                        }
                    } else {
                        user = ldapHelper.getUserinfo(createUsername(columns));
                    }
                    if(user != null) {
                        importToRole(columns, user);
                    }
                }
            }
//            legginnSuperbruker();
            legginnbrukeradmin(); //for test!
            indexWriter.optimize();
            indexWriter.close();
            logger.debug("{} users imported", idsAddedToLdap.size());
            logger.debug("{} users without email, not imported.", userswithoutemail.size());

        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warn("Error closing stream", e);
                }
            }
        }
    }

    private void legginnbrukeradmin() {
        FreeCodeUserIdentity freeCodeUserIdentity = new FreeCodeUserIdentity();
        freeCodeUserIdentity.setFirstName("Bruker");
        freeCodeUserIdentity.setLastName("Admin");
        freeCodeUserIdentity.setEmail("brukeradmin@nomail.qw");
        freeCodeUserIdentity.setCellPhone("11211211");
        freeCodeUserIdentity.setUid("badm");
        freeCodeUserIdentity.setBrukernavn("badm");
        try {
            ldapHelper.addOBOSUserIdentity(freeCodeUserIdentity, "mdab");
            UserPropertyAndRole userPropertyAndRole = new UserPropertyAndRole();
            userPropertyAndRole.setUid(freeCodeUserIdentity.getUid());
            userPropertyAndRole.setAppId("1");
            userPropertyAndRole.setOrgId("9999");
            userPropertyAndRole.setRoleName("Brukeradmin");
            repo.addUserPropertyAndRole(userPropertyAndRole);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
        }

    }

    private FreeCodeUserIdentity createUserIdentity(String[] columns, String personnummer) throws NamingException {
        String brukernavn = createUsername(columns);
        if(brukernavn != null && brukernavn.length() > 5) {
            String personref = PersonrefHelper.createPersonref(personnummer);
            String uid = UUID.randomUUID().toString();

            FreeCodeUserIdentity freeCodeUserIdentity = new FreeCodeUserIdentity();
            freeCodeUserIdentity.setFirstName(getString(columns, FORNAVN));
            freeCodeUserIdentity.setLastName(getString(columns, ETTERNAVN));
            freeCodeUserIdentity.setEmail(getString(columns, EPOST));
            String cellphone = getString(columns, TELEFON_MOBIL);
            freeCodeUserIdentity.setCellPhone(cellphone.length() > 2 ? cellphone : "");
            freeCodeUserIdentity.setPersonRef(personref);
            freeCodeUserIdentity.setUid(uid);
            freeCodeUserIdentity.setBrukernavn(brukernavn);
            return freeCodeUserIdentity;
        } else {
            userswithoutemail.add(getString(columns, FORNAVN) + " " + getString(columns, ETTERNAVN) + "," + personnummer);
        }
        return null;
    }

    private void importToRole(String[] columns, FreeCodeUserIdentity user) {
        String rolle = getString(columns, ROLLEKODE);
        UserPropertyAndRole userPropertyAndRole = new UserPropertyAndRole();
        userPropertyAndRole.setUid(user.getUid());
        if(invoiceRoles.contains(rolle)) {
            userPropertyAndRole.setAppId(APPID_INVOICE);
//            if(rolle.equals("SM") || rolle.equals("VM")) {
//                rolle += getString(columns, STYRET_REKKEFOLGE);
//            }
        } else {
            userPropertyAndRole.setAppId(APPID_STYREROMMET);
        }
        String orgid = getString(columns, BORETTSLAGSNR);
        orgid = "0000".substring(orgid.length()) + orgid;
        userPropertyAndRole.setOrgId(orgid);
        userPropertyAndRole.setRoleName(rolle);
        String fra = getString(columns, STYRE_AAR_FRA);
        String til = getString(columns, STYRE_AAR_TIL);
        if(fra != null && !"0".equals(fra)) {
            userPropertyAndRole.setRoleValue(fra + " - " + til);
        }
        repo.addUserPropertyAndRole(userPropertyAndRole);
    }

    private String createUsername(String[] columns) {
        return getString(columns, EPOST);
    }

    private String getString(String[] columns, int field) {
        return stringCleaner.cleanString(columns[field]);
    }

    private void initDB() {
        logger.info("Oppretter tabeller");
        try {
            queryRunner.update("CREATE TABLE UserRoles (" +
                    "  ID INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY," +
                    "  UserID char(36) NOT NULL," +
                    "  AppID varchar(32)," +
                    "  OrganizationId varchar(32)," +
                    "  RoleName varchar(32)," +
                    "  RoleValues varchar(256)" +
                    ")");
            queryRunner.update("CREATE TABLE Applications (" +
                    "  ID varchar(32)," +
                    "  Name varchar(128)," +
                    "  DefaultRole varchar(30) default null," +
                    "  DefaultOrgid varchar(30) default null" +
                    ")");
            queryRunner.update("CREATE TABLE Organization (" +
                    "  ID varchar(32)," +
                    "  Name varchar(128)" +
                    ")");
            queryRunner.update("CREATE TABLE Roles (" +
                    "  ID char(32)," +
                    "  Name varchar(128)" +
                    ")");
            queryRunner.update("CREATE TABLE AUDITLOG (" +
                    "  ID INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY," +
                    "  userid varchar(36)," +
                    "  timestamp varchar(20)," +
                    "  action varchar(32)," +
                    "  field varchar(32)," +
                    "  value varchar(256)" +
                    ")");

            logger.info("Legger inn grunndata");
            queryRunner.update("INSERT INTO Applications values ('" + APPID_STYREROMMET + "', 'Styrerommet', 'StyrerommetTilgang', '9999')");
            queryRunner.update("INSERT INTO Applications values ('" + APPID_INVOICE + "', 'Invoice', 'InvoiceBruker', '9999')");
            queryRunner.update("INSERT INTO Applications values ('" + APPID_BRUKERADMIN + "', 'Brukeradmin', 'Brukeradmin', '9999')");
            queryRunner.update("INSERT INTO Applications values ('" + APPID_OBOSPERSONKUNDE + "', 'OBOS Personkunde', 'Kunde', '9999')");

            importerSelskaper();
        } catch (SQLException e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }

    private void importerSelskaper() {
        BufferedReader reader = null;
        try {
            InputStream classpathStream = getClass().getClassLoader().getResourceAsStream("selskaper.csv");
            InputStreamReader isr = new InputStreamReader(classpathStream, "UTF-8");
            reader = new BufferedReader(isr);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                queryRunner.update("INSERT INTO Organization values (?, ?)", columns[0], columns[1]);
            }
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
        } catch (SQLException e) {
            logger.error(e.getLocalizedMessage(), e);
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warn("Error closing stream", e);
                }
            }
        }
    }

    public void setRepo(UserPropertyAndRoleRepository repo) {
        this.repo = repo;
    }

    public void setLdapHelper(LDAPHelper ldapHelper) {
        this.ldapHelper = ldapHelper;
    }

    public void setQueryRunner(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }
}
