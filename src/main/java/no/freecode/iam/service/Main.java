package no.obos.iam.service;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import no.obos.iam.service.config.AppConfig;
import no.obos.iam.service.config.ImportModule;
import no.obos.iam.service.config.UserIdentityBackendModule;
import no.obos.iam.service.dataimport.PstyrImporter;
import no.obos.iam.service.helper.SecurityTokenHelper;
import no.obos.iam.service.security.SecurityFilter;
import org.apache.lucene.store.Directory;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private EmbeddedADS ads;
    private HttpServer httpServer;
    private int webappPort;
    private final Injector injector;

    public Main() throws IOException {
        injector = Guice.createInjector(new UserIdentityBackendModule());
    }

    public Injector getInjector() {
        return injector;
    }

    public void startServer() throws Exception {
        logger.info("Starting UserIdentityBackend");

        ServletHandler servletHandler = new ServletHandler();
        servletHandler.setContextPath("/uib");
        servletHandler.addInitParameter("com.sun.jersey.config.property.packages", "no.obos.iam.service.resource,no.obos.iam.service.view");
        servletHandler.addInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        servletHandler.setProperty(ServletHandler.LOAD_ON_STARTUP, "1");

        GuiceFilter filter = new GuiceFilter();
        servletHandler.addFilter(filter, "guiceFilter", null);

        SecurityFilter securityFilter = new SecurityFilter(injector.getInstance(SecurityTokenHelper.class));
        HashMap<String, String> initParams = new HashMap<String, String>(1);
        initParams.put(SecurityFilter.SECURED_PATHS_PARAM, "/useradmin");
        initParams.put(SecurityFilter.REQUIRED_ROLE_PARAM, "Brukeradmin");
        servletHandler.addFilter(securityFilter, "SecurityFilter", initParams);

        GuiceContainer container = new GuiceContainer(injector);
        servletHandler.setServletInstance(container);

        webappPort = Integer.valueOf(AppConfig.appConfig.getProperty("service.port"));
        URI baseUri = UriBuilder.fromUri("http://localhost").port(webappPort).build();
        httpServer = GrizzlyServerFactory.createHttpServer(baseUri, servletHandler);
        logger.info("Jersey webapp started on port {}", webappPort);
    }

    public int getPort() {
        return webappPort;
    }

    private static void deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        path.delete();
    }

    private static boolean shouldImportUsers() {
        String dburl = AppConfig.appConfig.getProperty("roledb.jdbc.url");
        String dbpath = dburl.substring(dburl.lastIndexOf(':')+1) + ".script";
        File dbfile = new File(dbpath);
        System.out.println("Sjekker " + dbfile.getAbsolutePath());
        boolean shouldImport = !dbfile.exists();
        if (shouldImport) {
            logger.info("DB-filer ikke funnet, oppretter og importerer");
        }
        return shouldImport;
    }

    public void importData() {
        Injector injector = Guice.createInjector(new ImportModule());
        PstyrImporter pstyrImporter = injector.getInstance(PstyrImporter.class);
        Directory index = injector.getInstance(Directory.class);
        pstyrImporter.importer(AppConfig.appConfig.getProperty("userimportsource"), index);
    }

    public void startEmbeddedDS() throws Exception {
        String ldappath = AppConfig.appConfig.getProperty("ldap.embedded.directory");
        int ldapport = Integer.valueOf(AppConfig.appConfig.getProperty("ldap.embedded.port"));
        ads = new EmbeddedADS(ldappath);
        ads.startServer(ldapport);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }

    public static void main(String[] args) {
        try {
            Main server = new Main();
            String startEmbeddedDS = AppConfig.appConfig.getProperty("ldap.embedded");
            logger.info("Embedded apacheDS is " + startEmbeddedDS);
            boolean importUsers = shouldImportUsers();
            if(importUsers) {
                deleteDirectory(new File("hsqldb"));
                deleteDirectory(new File(AppConfig.appConfig.getProperty("ldap.embedded.directory")));
                deleteDirectory(new File(AppConfig.appConfig.getProperty("lucene.directory")));
            }
            if("enabled".equals(startEmbeddedDS)) {
                server.startEmbeddedDS();
            }
            if(importUsers) {
                server.importData();
            }
            server.startServer();
            if(!"enabled".equals(startEmbeddedDS)) {
                try {
                    // wait forever...
                    Thread.currentThread().join();
                } catch (InterruptedException ie) {
                    logger.warn(ie.getLocalizedMessage(), ie);
                }
                server.stop();
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            System.exit(1);
        }

    }

    public void stop() {
        if(httpServer != null) {
            httpServer.stop();
        }
        if(ads != null) {
            ads.stopServer();
        }
    }

}
