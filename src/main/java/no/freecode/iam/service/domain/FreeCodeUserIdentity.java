package no.obos.iam.service.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * A class representing the identity of a OBOS User - backed by LDAP scheme
 * User: totto
 * Date: 1/11/11
 * Time: 8:20 AM
 */
public class FreeCodeUserIdentity implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(FreeCodeUserIdentity.class);

    private String uid;
    private String brukernavn;
    private String firstName;
    private String lastName;
    private String personRef;
    private String email;
    private String cellPhone;

    public boolean validate() {
        if (uid == null || uid.length() < 2) {
            logger.error("UID {} not valid", uid);
            return false;
        }
        if (brukernavn == null || brukernavn.length() < 3) {
            logger.error("brukernavn {} not valid", brukernavn);
            return false;
        }
        if (firstName == null || firstName.length() < 2) {
            logger.error("firstName {} not valid", firstName);
            return false;
        }
        if (lastName == null || lastName.length() < 2) {
            logger.error("lastName {} not valid", lastName);
            return false;
        }
        return true;
    }

    public String getPersonRef() {
        return personRef;
    }

    public void setPersonRef(String personRef) {
        this.personRef = personRef;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getBrukernavn() {
        return brukernavn;

    }

    public void setBrukernavn(String brukernavn) {
        this.brukernavn = brukernavn;
    }

    public String getPersonName() {
        return firstName + ' ' + lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCellPhone() {
        return cellPhone;
    }

    public void setCellPhone(String cellPhone) {
        this.cellPhone = cellPhone;
    }

    @Override
    public String toString() {
        return "FreeCodeUserIdentity{" +
                "uid='" + uid + '\'' +
                ", brukernavn='" + brukernavn + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", personRef='" + personRef + '\'' +
                ", email='" + email + '\'' +
                ", cellPhone='" + cellPhone + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FreeCodeUserIdentity that = (FreeCodeUserIdentity) o;

        if (uid != null ? !uid.equals(that.uid) : that.uid != null) {
            return false;
        }
        if (brukernavn != null ? !brukernavn.equals(that.brukernavn) : that.brukernavn != null) {
            return false;
        }
        if (cellPhone != null ? !cellPhone.equals(that.cellPhone) : that.cellPhone != null) {
            return false;
        }
        if (email != null ? !email.equals(that.email) : that.email != null) {
            return false;
        }
        if (firstName != null ? !firstName.equals(that.firstName) : that.firstName != null) {
            return false;
        }
        if (lastName != null ? !lastName.equals(that.lastName) : that.lastName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = uid != null ? uid.hashCode() : 0;
        result = 31 * result + (brukernavn != null ? brukernavn.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (cellPhone != null ? cellPhone.hashCode() : 0);
        return result;
    }

    private static final long serialVersionUID = 1;


}
