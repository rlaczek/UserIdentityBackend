<html>
<head><title>Logon ok</title></head>
<body>
<b>Brukerinfo:</b><br>
<table style="border: 1px solid black;" border="1">
    <tr>
        <th>Brukernavn</th>
        <td>${it.identity.brukernavn}</td>
    </tr>
    <tr>
        <th>Fornavn</th>
        <td>${it.identity.firstName}</td>
    </tr>
    <tr>
        <th>Etternavn</th>
        <td>${it.identity.lastName}</td>
    </tr>
    <tr>
        <th>Mobil</th>
        <td>${it.identity.cellPhone!}</td>
    </tr>
    <tr>
        <th>Epost</th>
        <td>${it.identity.email!}</td>
    </tr>
    <tr>
        <th>Personref</th>
        <td>${it.identity.personRef!}</td>
    </tr>
    <tr>
        <th>Uid</th>
        <td>${it.identity.uid}</td>
    </tr>
</table>
<br>
<b>Roller:</b><br>
<table style="border: 1px solid black;" border="1">
<tr>
    <th>Applikasjon</th>
    <th>Organisasjon</th>
    <th>Rolle</th>
    <th>Rolleverdi</th>
<#list it.propsAndRoles as rolle>
    <tr>
        <td>${rolle.applicationName!} (${rolle.appId!})</td>
        <td>${rolle.organizationName!} (${rolle.orgId!})</td>
        <td>${rolle.roleName}</td>
        <td>${rolle.roleValue!}</td>
    </tr>
</#list>
</table>
</body>
</html>