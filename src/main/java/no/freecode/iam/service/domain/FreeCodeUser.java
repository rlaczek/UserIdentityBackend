package no.obos.iam.service.domain;

import java.util.ArrayList;
import java.util.List;

public class FreeCodeUser {
    private FreeCodeUserIdentity identity = null;
    private List<UserPropertyAndRole> propsandroles = new ArrayList<UserPropertyAndRole>();

    public FreeCodeUser(FreeCodeUserIdentity identity, List<UserPropertyAndRole> propsandroles) {
        this.identity = identity;
        this.propsandroles = propsandroles;
    }

    public FreeCodeUserIdentity getIdentity() {
        return identity;
    }

    public void setIdentity(FreeCodeUserIdentity identity) {
        this.identity = identity;
    }

    public List<UserPropertyAndRole> getPropsAndRoles() {
        return propsandroles;
    }

    public void addPropsAndRoles(UserPropertyAndRole propsandrole) {
        this.propsandroles.add(propsandrole);
    }

    public void setPropsAndRoles(List<UserPropertyAndRole> propsandroles) {
        this.propsandroles = propsandroles;
    }
}
