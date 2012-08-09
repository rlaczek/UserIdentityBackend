package no.obos.iam.service.resource;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.api.view.Viewable;
import no.obos.iam.service.audit.ActionPerformed;
import no.obos.iam.service.domain.*;
import no.obos.iam.service.domain.Application;
import no.obos.iam.service.helper.LDAPHelper;
import no.obos.iam.service.helper.LdapAuthenticatorImpl;
import no.obos.iam.service.mail.MockMail;
import no.obos.iam.service.repository.AuditLogRepository;
import no.obos.iam.service.repository.BackendConfigDataRepository;
import no.obos.iam.service.repository.UserPropertyAndRoleRepository;
import no.obos.iam.service.search.Indexer;
import no.obos.iam.service.search.Search;
import no.obos.iam.service.security.Authentication;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Grensesnitt for brukeradministrasjon.
 */
@Path("/useradmin/")//{usertokenid}/")
public class UserAdminResource {
    private static final Logger logger = LoggerFactory.getLogger(UserAdminResource.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    @Inject private UserPropertyAndRoleRepository userPropertyAndRoleRepository;
    @Inject private BackendConfigDataRepository backendConfigDataRepository;
    @Inject private LDAPHelper ldapHelper;
    @Inject @Named("external") private LdapAuthenticatorImpl externalLdapAuthenticator;
    @Inject private Search search;
    @Inject private Indexer indexer;
    @Inject private AuditLogRepository auditLogRepository;
    @Inject private PasswordGenerator passwordGenerator;
    @Inject private MockMail mailSender;

    @Context
    private UriInfo uriInfo;

    //////////////// Users

    /**
     * Find users.
     * @param query User query.
     * @return json response.
     */
    @GET @Path("/find/{q}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response find(@PathParam("q") String query) {
        logger.debug("query: {}", query);
        List<FreeCodeUserIdentity> result = search.search(query);

        HashMap<String, Object> model = new HashMap<String, Object>(2);
        model.put("users", result);
        model.put("userbaseurl", uriInfo.getBaseUri());
        return Response.ok(new Viewable("/useradmin/users.json.ftl", model)).build();
    }

    /**
     * Get user details.
     * @param username Username
     * @return user details and roles.
     */
    @Path("/users/{username}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("username") String username) {
        FreeCodeUserIdentity freeCodeUserIdentity;
        try {
            freeCodeUserIdentity = ldapHelper.getUserinfo(username);
            logger.debug("fant {}", freeCodeUserIdentity);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if(freeCodeUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        FreeCodeUser freeCodeUser = new FreeCodeUser(freeCodeUserIdentity, userPropertyAndRoleRepository.getUserPropertyAndRoles(freeCodeUserIdentity.getUid()));
        HashMap<String, Object> model = new HashMap<String, Object>(2);
        model.put("user", freeCodeUser);
        model.put("userbaseurl", uriInfo.getBaseUri());
        return Response.ok(new Viewable("/useradmin/user.json.ftl", model)).build();
    }

    @POST @Path("/users/add")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUser(String userJson) {
        logger.debug("Adduser: {}", userJson);
        try {
            JSONObject jsonobj = new JSONObject(userJson);
            String username = jsonobj.getString("brukernavn");
            FreeCodeUserIdentity freeCodeUserIdentity = ldapHelper.getUserinfo(username);
            logger.debug("fant {}", freeCodeUserIdentity);
            if(freeCodeUserIdentity != null) {
                logger.info("adduser: bruker {} finnes fra før", username);
                return Response.status(Response.Status.NOT_ACCEPTABLE).build();
            }
            freeCodeUserIdentity = new FreeCodeUserIdentity();
            freeCodeUserIdentity.setBrukernavn(username);
            freeCodeUserIdentity.setFirstName(jsonobj.getString("firstName"));
            freeCodeUserIdentity.setLastName(jsonobj.getString("lastName"));
            freeCodeUserIdentity.setEmail(jsonobj.getString("email"));
            freeCodeUserIdentity.setCellPhone(jsonobj.getString("cellPhone"));
            freeCodeUserIdentity.setPersonRef(jsonobj.getString("personRef"));
            freeCodeUserIdentity.setUid(UUID.randomUUID().toString());
            logger.debug("Ny bruker: {}", freeCodeUserIdentity);
            ldapHelper.addOBOSUserIdentity(freeCodeUserIdentity, passwordGenerator.generate());
            indexer.addToIndex(freeCodeUserIdentity);
            audit(ActionPerformed.ADDED, "user", freeCodeUserIdentity.toString());
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (JSONException e) {
            logger.error("Bad json", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok().build();
    }

    private void audit(String action, String what, String value) {
        String user = Authentication.getAuthenticatedUser().getName();
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(user, now, action, what, value);
        auditLogRepository.store(actionPerformed);

    }

    @Path("/users/{username}/exists")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response exists(@PathParam("username") String username) {
        logger.debug("does {} exist?");
        try {
            FreeCodeUserIdentity id = ldapHelper.getUserinfo(username);
            String result = (id != null) ? "{\"exists\" : true}" : "{\"exists\" : false}";
            logger.debug("exists: " + result);
            return Response.ok(result).build();
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET @Path("/users/{username}/delete")
    public Response deleteUser(@PathParam("username") String username) {
        try {
            FreeCodeUserIdentity user = ldapHelper.getUserinfo(username);
            if(user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }
            ldapHelper.deleteUser(username);
            String uid = user.getUid();
            userPropertyAndRoleRepository.deleteUser(uid);
            indexer.removeFromIndex(uid);
            audit(ActionPerformed.DELETED, "user", "uid=" + uid + ", username=" + username);
            return Response.ok().build();
        } catch (NamingException e) {
            logger.error("deleteUser failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT @Path("/users/{username}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyUser(@PathParam("username") String username, String userJson) {
        logger.debug("modifyUser: ", userJson);
        try {
            FreeCodeUserIdentity freeCodeUserIdentity = ldapHelper.getUserinfo(username);
            if(freeCodeUserIdentity == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }

            logger.debug("fant {}", freeCodeUserIdentity);
            try {
                JSONObject jsonobj = new JSONObject(userJson);
                freeCodeUserIdentity.setFirstName(jsonobj.getString("firstName"));
                freeCodeUserIdentity.setLastName(jsonobj.getString("lastName"));
                freeCodeUserIdentity.setEmail(jsonobj.getString("email"));
                freeCodeUserIdentity.setCellPhone(jsonobj.getString("cellPhone"));
                freeCodeUserIdentity.setPersonRef(jsonobj.getString("personRef"));
                freeCodeUserIdentity.setBrukernavn(jsonobj.getString("brukernavn"));
            } catch (JSONException e) {
                logger.error("Bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            logger.debug("Endret bruker: {}", freeCodeUserIdentity);
            ldapHelper.updateUser(username, freeCodeUserIdentity);
            indexer.update(freeCodeUserIdentity);
            audit(ActionPerformed.MODIFIED, "user", freeCodeUserIdentity.toString());
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }

    @GET @Path("/users/{username}/resetpassword")
    public Response resetPassword(@PathParam("username") String username) {
        logger.info("Reset password for user {}", username);
        try {
            FreeCodeUserIdentity user = ldapHelper.getUserinfo(username);
            if(user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }
            String newPassword = passwordGenerator.generate();
            ChangePasswordToken changePasswordToken = new ChangePasswordToken(username, newPassword);
            String salt = passwordGenerator.generate();
            ldapHelper.setTempPassword(username, newPassword, salt);
            String token = changePasswordToken.generateTokenString(salt.getBytes());
            mailSender.sendpasswordmail(user.getEmail(), username, token);
            audit(ActionPerformed.MODIFIED, "password", user.getUid());
            logger.info("Password reset " + token);
            logger.debug("salt=" + salt);
            return Response.ok().build();
        } catch (NamingException e) {
            logger.error("resetPassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST @Path("/users/{username}/newpassword/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePassword(@PathParam("username") String username, @PathParam("token") String token, String passwordJson) {
        logger.info("Endrer passord for {}: {}", username, passwordJson);
        try {
            FreeCodeUserIdentity user = ldapHelper.getUserinfo(username);
            if(user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }
            byte[] salt = ldapHelper.getSalt(username).getBytes();
            logger.debug("salt=" + new String(salt));
            ChangePasswordToken changePasswordToken = ChangePasswordToken.fromTokenString(token, salt);
            logger.info("Passwordtoken for {} ok.", username);
            boolean ok = externalLdapAuthenticator.authWithTemp(username, changePasswordToken.getPassword());
            if(!ok) {
                logger.info("Authentication failed while changing password for user {}", username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            try {
                JSONObject jsonobj = new JSONObject(passwordJson);
                String newpassword = jsonobj.getString("newpassword");
                ldapHelper.changePassword(username, newpassword);
                audit(ActionPerformed.MODIFIED, "password", user.getUid());
            } catch (JSONException e) {
                logger.error("Bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            logger.error("changePassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (NamingException e) {
            logger.error("changePassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST @Path("/users/{username}/newuser/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response newUser(@PathParam("username") String username, @PathParam("token") String token, String passwordJson) {
        logger.info("Endrer data for ny bruker {}: {}", username, passwordJson);
        try {
            FreeCodeUserIdentity user = ldapHelper.getUserinfo(username);
            if(user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }
            byte[] salt = ldapHelper.getSalt(username).getBytes();
            logger.debug("salt=" + new String(salt));
            ChangePasswordToken changePasswordToken = ChangePasswordToken.fromTokenString(token, salt);
            logger.info("Passwordtoken for {} ok.", username);
            boolean ok = externalLdapAuthenticator.authWithTemp(username, changePasswordToken.getPassword());
            if(!ok) {
                logger.info("Authentication failed while changing password for user {}", username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            try {
                JSONObject jsonobj = new JSONObject(passwordJson);
                String newpassword = jsonobj.getString("newpassword");
                String newusername = jsonobj.getString("newusername");
                if(!username.equals(newusername)) {
                    FreeCodeUserIdentity newidexists = ldapHelper.getUserinfo(newusername);
                    if(newidexists != null) {
                        return Response.status(Response.Status.BAD_REQUEST).entity("Username already exists").build();
                    }
                    user.setBrukernavn(newusername);
                    ldapHelper.updateUser(username, user);
                }
                ldapHelper.changePassword(newusername, newpassword);
                audit(ActionPerformed.MODIFIED, "password", user.getUid());
            } catch (JSONException e) {
                logger.error("Bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            logger.info("Nye brukerdata lagret");
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            logger.error("newUser failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (NamingException e) {
            logger.error("newUser failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /////////// Roles

    /**
     * Lister alle applikasjoner, samt angir om brukeren har noen roller her.
     * @param username user id
     * @return app-liste.
     */
    @Path("/users/{username}/applications")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response getApplications(@PathParam("username") String username) {
        FreeCodeUserIdentity freeCodeUserIdentity;
        try {
            freeCodeUserIdentity = ldapHelper.getUserinfo(username);
            logger.debug("fant {}", freeCodeUserIdentity);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if(freeCodeUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        FreeCodeUser freeCodeUser = new FreeCodeUser(freeCodeUserIdentity, userPropertyAndRoleRepository.getUserPropertyAndRoles(freeCodeUserIdentity.getUid()));
        List<Application> allApps = backendConfigDataRepository.getApplications();
        Set<String> myApps = new HashSet<String>();
        for (UserPropertyAndRole role : freeCodeUser.getPropsAndRoles()) {
            myApps.add(role.getAppId());
        }

        HashMap<String, Object> model = new HashMap<String, Object>(3);
        model.put("allApps", allApps);
        model.put("myApps", myApps);
        return Response.ok(new Viewable("/useradmin/userapps.json.ftl", model)).build();
    }

    @Path("/users/{username}/{appid}")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response getUserRoles(@PathParam("username") String username, @PathParam("appid") String appid) {
        FreeCodeUserIdentity freeCodeUserIdentity;
        try {
            freeCodeUserIdentity = ldapHelper.getUserinfo(username);
            logger.debug("fant {}", freeCodeUserIdentity);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if(freeCodeUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        FreeCodeUser freeCodeUser = new FreeCodeUser(freeCodeUserIdentity, userPropertyAndRoleRepository.getUserPropertyAndRoles(freeCodeUserIdentity.getUid()));
        List<UserPropertyAndRole> rolesForApp = new ArrayList<UserPropertyAndRole>();
        for (UserPropertyAndRole role : freeCodeUser.getPropsAndRoles()) {
            if(role.getAppId().equals(appid)) {
                rolesForApp.add(role);
            }
        }
        HashMap<String, Object> model = new HashMap<String, Object>(2);
        model.put("roller", rolesForApp);
        return Response.ok(new Viewable("/useradmin/roles.json.ftl", model)).build();
    }

    @POST @Path("/users/{username}/{appid}/add")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUserRole(@PathParam("username") String username, @PathParam("appid") String appid, String jsonrole) {
        logger.debug("legg til rolle for uid={}, appid={}, rollejson={}", new String[]{ username, appid, jsonrole});
        if(jsonrole == null || jsonrole.trim().length() == 0) {
            logger.warn("Empty json payload");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        FreeCodeUserIdentity freeCodeUserIdentity;
        try {
            freeCodeUserIdentity = ldapHelper.getUserinfo(username);
            logger.debug("fant {}", freeCodeUserIdentity);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if(freeCodeUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        UserPropertyAndRole role = new UserPropertyAndRole();
        role.setUid(freeCodeUserIdentity.getUid());
        role.setAppId(appid);
        try {
            JSONObject jsonobj = new JSONObject(jsonrole);
            role.setOrgId(jsonobj.getString("orgID"));
            role.setRoleName(jsonobj.getString("roleName"));
            role.setRoleValue(jsonobj.getString("roleValue"));
        } catch (JSONException e) {
            logger.error("Bad json", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        logger.debug("Role: {}", role);
//        if(appid.equals(PstyrImporter.APPID_INVOICE) && !PstyrImporter.invoiceRoles.contains(role.getRoleName())) {
//            logger.warn("Ugyldig rolle for invoice");
//            return Response.status(Response.Status.CONFLICT).build();
//        }
//        if(!appid.equals(PstyrImporter.APPID_INVOICE) && PstyrImporter.invoiceRoles.contains(role.getRoleName())) {
//            logger.warn("App og rolle matcher ikke");
//            return Response.status(Response.Status.CONFLICT).build();
//        }
        String uid = freeCodeUserIdentity.getUid();
        List<UserPropertyAndRole> existingRoles = userPropertyAndRoleRepository.getUserPropertyAndRoles(uid);
        for (UserPropertyAndRole existingRole : existingRoles) {
            if(existingRole.getAppId().equals(appid) && existingRole.getOrgId().equals(role.getOrgId()) && existingRole.getRoleName().equals(role.getRoleName())) {
                logger.warn("App og rolle finnes fra før");
                return Response.status(Response.Status.CONFLICT).build();
            }
        }
        userPropertyAndRoleRepository.addUserPropertyAndRole(role);
        audit(ActionPerformed.ADDED, "role", "uid=" + uid + ", username=" + username + ", appid=" + appid + ", role=" + jsonrole);
        return Response.ok().build();
    }

    @GET @Path("/users/{username}/{appid}/adddefaultrole")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addDefaultRole(@PathParam("username") String username, @PathParam("appid") String appid) {
        logger.debug("legg til default rolle for {}:{}", username, appid);
        FreeCodeUserIdentity freeCodeUserIdentity;
        try {
            freeCodeUserIdentity = ldapHelper.getUserinfo(username);
            logger.debug("fant {}", freeCodeUserIdentity);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if(freeCodeUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }

        Application app = backendConfigDataRepository.getApplication(appid);
        if(app == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"app not found\"}'").build();
        }
        if(app.getDefaultrole() == null) {
            return Response.status(Response.Status.CONFLICT).entity("{\"error\":\"app has no default role\"}'").build();
        }
        String orgName = backendConfigDataRepository.getOrgname(app.getDefaultOrgid());
        UserPropertyAndRole role = new UserPropertyAndRole();
        role.setUid(freeCodeUserIdentity.getUid());
        role.setAppId(appid);
        role.setApplicationName(app.getName());
        role.setOrgId(app.getDefaultOrgid());
        role.setOrganizationName(orgName);
        role.setRoleName(app.getDefaultrole());
        logger.debug("Role: {}", role);
        List<UserPropertyAndRole> existingRoles = userPropertyAndRoleRepository.getUserPropertyAndRoles(freeCodeUserIdentity.getUid());
        for (UserPropertyAndRole existingRole : existingRoles) {
            if(existingRole.getAppId().equals(appid) && existingRole.getOrgId().equals(role.getOrgId()) && existingRole.getRoleName().equals(role.getRoleName())) {
                logger.warn("App og rolle finnes fra før");
                return Response.status(Response.Status.CONFLICT).build();
            }
        }

        userPropertyAndRoleRepository.addUserPropertyAndRole(role);
        audit(ActionPerformed.ADDED, "role", "uid=" + freeCodeUserIdentity.getUid() + ", appid=" + appid + ", role=" + role);

        HashMap<String, Object> model = new HashMap<String, Object>(2);
        model.put("rolle", role);
        return Response.ok(new Viewable("/useradmin/role.json.ftl", model)).build();
    }

    @POST @Path("/users/{username}/{appid}/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteUserRole(@PathParam("username") String username, @PathParam("appid") String appid, String jsonrole) {
        logger.debug("Fjern rolle for {} i app {}: {}", new String[]{username, appid, jsonrole});
        FreeCodeUserIdentity freeCodeUserIdentity;
        try {
            freeCodeUserIdentity = ldapHelper.getUserinfo(username);
            logger.debug("fant bruker: {}", freeCodeUserIdentity);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if(freeCodeUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        try {
            JSONObject jsonobj = new JSONObject(jsonrole);
            String orgid = jsonobj.getString("orgID");
            String rolename = jsonobj.getString("roleName");
            String uid = freeCodeUserIdentity.getUid();
            userPropertyAndRoleRepository.deleteUserRole(uid, appid, orgid, rolename);
            audit(ActionPerformed.DELETED, "role", "uid=" + uid + ", appid=" + appid + ", role=" + jsonrole);
        } catch (JSONException e) {
            logger.error("Bad json", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok().build();
    }

    @GET @Path("/users/{username}/{appid}/deleteall")
    public Response deleteAllUserRolesForApp(@PathParam("username") String username, @PathParam("appid") String appid) {
        logger.debug("Fjern alle roller for {}: {}", username, appid);
        FreeCodeUserIdentity freeCodeUserIdentity;
        try {
            freeCodeUserIdentity = ldapHelper.getUserinfo(username);
            logger.debug("fant {}", freeCodeUserIdentity);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if(freeCodeUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        String uid = freeCodeUserIdentity.getUid();
        userPropertyAndRoleRepository.deleteUserAppRoles(uid, appid);
        audit(ActionPerformed.DELETED, "role", "uid=" + uid + ", appid=" + appid + ", roles=all");
        return Response.ok().build();
    }


    @PUT @Path("/users/{username}/{appid}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyRoleValue(@PathParam("username") String username, @PathParam("appid") String appid, String jsonrole) {
        FreeCodeUserIdentity freeCodeUserIdentity;
        try {
            freeCodeUserIdentity = ldapHelper.getUserinfo(username);
            logger.debug("fant {}", freeCodeUserIdentity);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        if(freeCodeUserIdentity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
        }
        try {
            JSONObject jsonobj = new JSONObject(jsonrole);
            String orgid = jsonobj.getString("orgID");
            String rolename = jsonobj.getString("roleName");
            String rolevalue = jsonobj.getString("roleValue");
            String uid = freeCodeUserIdentity.getUid();
            userPropertyAndRoleRepository.updateUserRoleValue(uid, appid, orgid, rolename, rolevalue);
            audit(ActionPerformed.MODIFIED, "role", "uid=" + uid + ", appid=" + appid + ", role=" + jsonrole);
        } catch (JSONException e) {
            logger.error("bad json", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.ok().build();
    }

}
