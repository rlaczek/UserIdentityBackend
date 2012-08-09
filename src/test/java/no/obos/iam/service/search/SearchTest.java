package no.obos.iam.service.search;

import no.obos.iam.service.domain.FreeCodeUserIdentity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SearchTest {
    @Test
    public void search() throws IOException {
        Directory index = new RAMDirectory();

        Indexer indexer = new Indexer(index);
        List<FreeCodeUserIdentity> users = new ArrayList<FreeCodeUserIdentity>(){{
            add(createUser("bentelonga@hotmail.com", "Bente", "Longva", "bentelongva@hotmail.com", "bentelongva@hotmail.com"));
            add(createUser("bråodd", "Oddvar", "Brå", "staven@hotmail.com", "staven@hotmail.com"));
            add(createUser("noenas@gogo.go", "Nasse", "Nøff", "noenas@gogo.go", "noenas@gogo.go"));
            add(createUser("leif@nassegroupt.com", "Oddvar", "Bjørn", "leif@nassegroupt.com", "leif@nassegroupt.com"));
            add(createUser("leif@leif.com", "Bjørn", "Brå", "leif@leif.com", "leif@leif.com"));
        }};
        indexer.addToIndex(users);
        Search search = new Search(index);
        List<FreeCodeUserIdentity> result = search.search("Oddvar");
        assertEquals(2, result.size());
        result = search.search("Bjørn");
        assertEquals(2, result.size());
        result = search.search("leif@nassegroupt.com");
        assertEquals(1, result.size());
        result = search.search("Pølser");
        assertEquals(0, result.size());
    }

    @Test
    public void removeuser() throws IOException {
        RAMDirectory index = new RAMDirectory();

        Indexer indexer = new Indexer(index);
        List<FreeCodeUserIdentity> users = new ArrayList<FreeCodeUserIdentity>(){{
            add(createUser("bentelonga@hotmail.com", "Bente", "Longva", "bentelongva@hotmail.com", "bentelongva@hotmail.com"));
            add(createUser("bråodd", "Oddvar", "Brå", "staven@hotmail.com", "staven@hotmail.com"));
            add(createUser("noenas@gogo.go", "Nasse", "Nøff", "noenas@gogo.go", "noenas@gogo.go"));
            add(createUser("leif@nassegroupt.com", "Oddvar", "Bjørn", "leif@nassegroupt.com", "leif@nassegroupt.com"));
            add(createUser("leif@leif.com", "Bjørn", "Brå", "leif@leif.com", "leif@leif.com"));
        }};
        indexer.addToIndex(users);
        Search search = new Search(index);
        List<FreeCodeUserIdentity> result = search.search("Oddvar");
        assertEquals(2, result.size());
        indexer.removeFromIndex("staven@hotmail.com");
        result = search.search("Oddvar");
        assertEquals(1, result.size());
    }

    @Test
    public void modifyuser() throws IOException {
        RAMDirectory index = new RAMDirectory();

        Indexer indexer = new Indexer(index);
        List<FreeCodeUserIdentity> users = new ArrayList<FreeCodeUserIdentity>(){{
            add(createUser("bentelonga@hotmail.com", "Bente", "Longva", "bentelongva@hotmail.com", "bentelongva@hotmail.com"));
            add(createUser("bråodd", "Oddvar", "Brå", "staven@hotmail.com", "staven@hotmail.com"));
            add(createUser("noenas@gogo.go", "Nasse", "Nøff", "noenas@gogo.go", "noenas@gogo.go"));
            add(createUser("leif@nassegroupt.com", "Oddvar", "Bjørn", "leif@nassegroupt.com", "leif@nassegroupt.com"));
            add(createUser("leif@leif.com", "Bjørn", "Brå", "leif@leif.com", "leif@leif.com"));
        }};
        indexer.addToIndex(users);
        Search search = new Search(index);
        List<FreeCodeUserIdentity> result = search.search("Oddvar");
        assertEquals(2, result.size());
        result = search.search("Oddaivars");
        assertEquals(0, result.size());
        indexer.update(createUser("bråodd", "Oddaivars", "Brå", "staven@hotmail.com", "staven@hotmail.com"));
        result = search.search("Oddvar");
        assertEquals(1, result.size());
        result = search.search("Oddaivars");
        assertEquals(1, result.size());
    }

    @Test
    public void wildcardsearch() throws IOException {
        Directory index = new RAMDirectory();

        Indexer indexer = new Indexer(index);
        List<FreeCodeUserIdentity> users = new ArrayList<FreeCodeUserIdentity>(){{
            add(createUser("bentelonga@hotmail.com", "Bente", "Longva", "bentelongva@hotmail.com", "bentelongva@hotmail.com"));
            add(createUser("bråodd", "Oddvar", "Brå", "staven@hotmail.com", "staven@hotmail.com"));
            add(createUser("noenas@gogo.go", "Nasse", "Nøff", "noenas@gogo.go", "noenas@gogo.go"));
            add(createUser("leif@nassegroupt.com", "Oddvar", "Bjørn", "leif@nassegroupt.com", "leif@nassegroupt.com"));
            add(createUser("leif@leif.com", "Bjørn", "Brå", "leif@leif.com", "leif@leif.com"));
        }};
        indexer.addToIndex(users);
        Search search = new Search(index);
        List<FreeCodeUserIdentity> result = search.search("bjør nøf");
        assertEquals(3, result.size());
        result = search.search("bjørn_nøff");
        assertEquals(3, result.size());
        result = search.search("leif@");
        assertEquals(2, result.size());
    }

    @Test
    public void weights() throws IOException {
        Directory index = new RAMDirectory();

        Indexer indexer = new Indexer(index);
        List<FreeCodeUserIdentity> users = new ArrayList<FreeCodeUserIdentity>(){{
            add(createUser("bentelonga@hotmail.com", "Bente", "Longva", "bentelongva@hotmail.com", "bentelongva@hotmail.com"));
            add(createUser("bråodd", "Oddvar", "Leifke", "staven@hotmail.com", "staven@hotmail.com"));
            add(createUser("bjørn@gogo.go", "Nasse", "Nøff", "noenas@gogo.go", "noenas@gogo.go"));
            add(createUser("leif@nassegroupt.com", "Loif", "Bjørn", "leif@nassegroupt.com", "leif@nassegroupt.com"));
            add(createUser("lomar@bassegroupt.com", "Leif", "Bjørn", "ldede@nassegroupt.com", "leedde@nassegroupt.com"));
            add(createUser("gtr@leif.com", "Bjørn", "Leif", "tgr@leif.com", "grtgr@leif.com"));
        }};
        indexer.addToIndex(users);
        Search search = new Search(index);
        List<FreeCodeUserIdentity> result = search.search("Leif");
        assertEquals(4, result.size());
        assertEquals("lomar@bassegroupt.com", result.get(0).getBrukernavn());
        assertEquals("gtr@leif.com", result.get(1).getBrukernavn());
        assertEquals("leif@nassegroupt.com", result.get(2).getBrukernavn());
        assertEquals("bråodd", result.get(3).getBrukernavn());
    }


    private static FreeCodeUserIdentity createUser(String brukernavn, String fornavn, String etternavn, String email, String uid) {
        FreeCodeUserIdentity user1 = new FreeCodeUserIdentity();
        user1.setBrukernavn(brukernavn);
        user1.setFirstName(fornavn);
        user1.setLastName(etternavn);
        user1.setEmail(email);
        user1.setUid(uid);
	    user1.setPersonRef("r"+uid);
        return user1;
    }

}
