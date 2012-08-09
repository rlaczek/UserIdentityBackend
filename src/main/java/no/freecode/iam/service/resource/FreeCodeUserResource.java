package no.obos.iam.service.resource;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.api.view.Viewable;
import no.obos.iam.service.domain.FreeCodeUser;
import no.obos.iam.service.domain.FreeCodeUserIdentity;
import no.obos.iam.service.helper.LdapAuthenticatorImpl;
import no.obos.iam.service.repository.UserPropertyAndRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for å autentisere bruker og hente FreeCodeUser med oversikt over applikasjoner, selskaper og roller.
 */
@Path("/")
public class FreeCodeUserResource {
    private static final Logger logger = LoggerFactory.getLogger(FreeCodeUserResource.class);
    @Inject @Named("internal") private LdapAuthenticatorImpl internalLdapAuthenticator;
    @Inject @Named("external") private LdapAuthenticatorImpl externalLdapAuthenticator;
    @Inject private UserPropertyAndRoleRepository userPropertyAndRoleRepositor;

    private Map<String, Object> welcomeModel;

    public FreeCodeUserResource() {
        createWelcomeModel();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response info() {
        return Response.ok(new Viewable("/welcome", welcomeModel)).build();
    }

    /**
     * Authentication using XML. XML must contain an element with name username, and an element with name password.
     * @param input XML input stream.
     * @return XML-encoded identity and role information, or a LogonFailed element if authentication failed.
     */
    @Path("logon")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response authenticateUser(InputStream input) {
        logger.debug("authenticateUser XML");
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document dDoc = builder.parse(input);
            XPath xPath = XPathFactory.newInstance().newXPath();
            String username = (String) xPath.evaluate("//username", dDoc, XPathConstants.STRING);
            String password = (String) xPath.evaluate("//password", dDoc, XPathConstants.STRING);
            logger.debug(username + ":" + password);
            FreeCodeUserIdentity id = null;
            if(username != null && password != null) {
                id = externalLdapAuthenticator.auth(username, password);
//                if(id == null) {
//                    System.out.println("Prøver intern ldap");
//                    id = internalLdapAuthenticator.auth(username, password);
//                }
            }
            if(id == null)  {
                logger.info("Authentication failed for user {}", username);
                return Response.status(Response.Status.FORBIDDEN).entity(new Viewable("/logonFailed.xml.ftl")).build();
            }
            FreeCodeUser freeCodeUser = new FreeCodeUser(id, userPropertyAndRoleRepositor.getUserPropertyAndRoles(id.getUid()));
            logger.info("Authentication ok for user {}", username);
            return Response.ok(new Viewable("/user.xml.ftl", freeCodeUser)).build();
        } catch (ParserConfigurationException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, check error logs</error>").build();
        } catch (SAXException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, check error logs</error>").build();
        } catch (XPathExpressionException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, check error logs</error>").build();
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, check error logs</error>").build();
        }
    }

    /**
     * Form/html-based authentication.
     * @param username Username to be authenticated.
     * @param password Users password.
     * @return XML-encoded identity and role information, or a LogonFailed element if authentication failed.
     */
    @Path("logon")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response authenticateUserForm(@FormParam("username") String username, @FormParam("password") String password) {
        logger.debug("authenticateUser form: user=" + username + ", password=" + password);
        FreeCodeUserIdentity id = null;
        if(username != null && password != null) {
            id = externalLdapAuthenticator.auth(username, password);
//            if(id == null) {
//                System.out.println("Prøver intern ldap");
//                id = internalLdapAuthenticator.auth(username, password);
//            }
        } else {
            logger.warn("Missing user or password");
        }
        if(id == null) {
            return Response.ok(new Viewable("/logonFailed.ftl")).build();
        }
        FreeCodeUser freeCodeUser = new FreeCodeUser(id, userPropertyAndRoleRepositor.getUserPropertyAndRoles(id.getUid()));
        return Response.ok(new Viewable("/user.ftl", freeCodeUser)).build();
    }

    private void createWelcomeModel() {
        welcomeModel = new HashMap<String, Object>(1);
        try {
            final String hostname = java.net.InetAddress.getLocalHost().getHostName();
            welcomeModel.put("hostname", hostname);
        } catch (UnknownHostException e) {
            logger.warn(e.getLocalizedMessage(), e);
        }
    }
}
