/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.addons.ldap_query;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.ow2.proactive.addons.ldap_query.model.ErrorResponse;
import org.ow2.proactive.addons.ldap_query.model.LDAPResponse;
import org.ow2.proactive.addons.ldap_query.model.Response;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.ObjectMapper;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ LDAPConnectionUtility.class })
public class LDAPClientTest {
    private LDAPClient ldapClient;

    private ObjectMapper mapper = new ObjectMapper();

    @Mock
    private DirContext ldapConnection;

    private String ldapUrl = "ldap://localhost:389";

    private String ldapDnBase = "dc=yourOrganization,dc=com";

    private String ldapSearchBase = "dc=sophia";

    private String ldapSearchFilter = "(objectclass=*)";

    private String ldapSelectedAttributes = "attributeName1,attributeName2";

    private String ldapUsername = "cn=admin,ou=users";

    private String ldapPassword = "adminPassword";

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testLDAPClientConstructFromMaps() {
        Map mapVariables = new HashMap();
        mapVariables.put("ldapUrl", ldapUrl);
        mapVariables.put("ldapDnBase", ldapDnBase);
        mapVariables.put("ldapSearchBase", ldapSearchBase);
        mapVariables.put("ldapSearchFilter", ldapSearchFilter);
        mapVariables.put("ldapSelectedAttributes", ldapSelectedAttributes);
        Map mapCredentials = new HashMap();
        mapCredentials.put("ldapUsername", ldapUsername);
        mapCredentials.put("ldapPassword", ldapPassword);
        ldapClient = new LDAPClient(mapVariables, mapCredentials);

        assertThat(ldapClient.allLDAPClientParameters.get(LDAPClient.ARG_URL), is(ldapUrl));
        assertThat(ldapClient.allLDAPClientParameters.get(LDAPClient.ARG_SEARCH_BASE), is(ldapSearchBase));
        assertThat(ldapClient.allLDAPClientParameters.get(LDAPClient.ARG_SEARCH_FILTER), is(ldapSearchFilter));
        assertThat(ldapClient.allLDAPClientParameters.get(LDAPClient.ARG_SELECTED_ATTRIBUTES),
                   is(ldapSelectedAttributes));
        assertThat(ldapClient.allLDAPClientParameters.get(LDAPClient.ARG_USERNAME), is(ldapUsername));
        assertThat(ldapClient.allLDAPClientParameters.get(LDAPClient.ARG_PASSWORD), is(ldapPassword));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLDAPClientMissedArgumentException() {
        Map mapVariables = new HashMap();
        Map mapCredentials = new HashMap();
        new LDAPClient(mapVariables, mapCredentials);
    }

    @Test
    public void testOkResultSearchQueryLDAP() throws NamingException, IOException {
        PowerMockito.mockStatic(LDAPConnectionUtility.class);
        try {
            when(LDAPConnectionUtility.connect(ldapUrl, ldapUsername, ldapPassword)).thenReturn(ldapConnection);
        } catch (NamingException e) {
            e.printStackTrace();
        }
        ldapClient = new LDAPClient(ldapUrl,
                                    ldapUsername,
                                    ldapPassword,
                                    ldapSearchBase,
                                    ldapSearchFilter,
                                    ldapSelectedAttributes);

        NamingEnumeration results = mock(NamingEnumeration.class);
        when(ldapConnection.search(anyString(), anyString(), any(SearchControls.class))).thenReturn(results);
        when(results.hasMore()).thenReturn(false);

        String jsonResponse = ldapClient.searchQueryLDAP();
        PowerMockito.verifyStatic(Mockito.times(1));
        Response response = mapper.readValue(jsonResponse, LDAPResponse.class);
        assertThat(response.getStatus(), is("Ok"));
    }

    @Test
    public void testErrorResultSearchQueryLDAP() throws NamingException, IOException {
        ldapClient = new LDAPClient(ldapUrl,
                                    ldapUsername,
                                    ldapPassword,
                                    ldapSearchBase,
                                    ldapSearchFilter,
                                    ldapSelectedAttributes);
        String jsonResponse = ldapClient.searchQueryLDAP();
        Response response = mapper.readValue(jsonResponse, ErrorResponse.class);
        assertThat(response.getStatus(), is("Error"));
    }

}
