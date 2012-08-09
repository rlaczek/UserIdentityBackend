<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<freeCodeUser>
    <identity>
        <brukernavn>${it.identity.brukernavn?xml}</brukernavn>
        <cellPhone>${it.identity.cellPhone!?xml}</cellPhone>
        <email>${it.identity.email!?xml}</email>
        <firstName>${it.identity.firstName?xml}</firstName>
        <lastName>${it.identity.lastName?xml}</lastName>
        <personRef>${it.identity.personRef!?xml}</personRef>
        <UID>${it.identity.uid?xml}</UID>
    </identity>
    <applications>
<#list it.propsAndRoles as rolle>
        <application>
            <appId>${rolle.appId?xml}</appId>
            <applicationName>${rolle.applicationName?xml}</applicationName>
            <orgID>${rolle.orgId?xml}</orgID>
            <organizationName>${rolle.organizationName?xml}</organizationName>
            <roleName>${rolle.roleName?xml}</roleName>
            <roleValue>${rolle.roleValue!?xml}</roleValue>
        </application>
</#list>
    </applications>
</freeCodeUser>