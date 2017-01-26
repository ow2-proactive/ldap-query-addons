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
package org.ow2.proactive.addons.ldap_query;/*
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.ow2.proactive.addons.ldap_query.model.ErrorResponse;
import org.ow2.proactive.addons.ldap_query.model.LDAPResponse;
import org.ow2.proactive.addons.ldap_query.model.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@NoArgsConstructor
public class LDAPClient {

    private static final String REGEX_LIST_SEPARATOR = ",\\s?";

    /*
     * Define name of variables that can be passed to the LDAP Query task
     */

    public static final String ARG_URL = "ldapUrl";

    public static final String ARG_USERNAME = "ldapUsername";

    public static final String ARG_PASSWORD = "ldapPassword";

    public static final String ARG_SEARCH_BASE = "ldapSearchBase";

    public static final String ARG_SEARCH_FILTER = "ldapSearchFilter";

    public static final String ARG_SELECTED_ATTRIBUTES = "ldapSelectedAttributes";

    protected String ldapUrl;

    protected String ldapUsername;

    protected String ldapPassword;

    protected String ldapSearchBase;

    protected String ldapSearchFilter;

    protected String ldapSelectedAttributes;

    protected DirContext ldapConnection;

    public LDAPClient(String ldapUrl, String ldapUsername, String ldapPassword, String ldapSearchBase,
            String ldapSearchFilter, String ldapSelectedAttributes) {
        this.ldapUrl = ldapUrl;
        this.ldapUsername = ldapUsername;
        this.ldapPassword = ldapPassword;
        this.ldapSearchBase = ldapSearchBase;
        this.ldapSearchFilter = ldapSearchFilter;
        this.ldapSelectedAttributes = ldapSelectedAttributes;
    }

    public LDAPClient(Map<String, Serializable> actualTaskVariables, Map<String, Serializable> credentials) {
        List<String> taskVariablesList = new LinkedList<>();
        taskVariablesList.add(ARG_URL);
        taskVariablesList.add(ARG_USERNAME);
        taskVariablesList.add(ARG_PASSWORD);
        taskVariablesList.add(ARG_SEARCH_BASE);
        taskVariablesList.add(ARG_SEARCH_FILTER);
        taskVariablesList.add(ARG_SELECTED_ATTRIBUTES);

        Map mapWithVariables;
        for (String variableName : taskVariablesList) {
            if (actualTaskVariables.containsKey(variableName)) {
                mapWithVariables = actualTaskVariables;
            } else {
                mapWithVariables = credentials;
            }
            setLdapClientFields(mapWithVariables, variableName);
        }
    }

    private void setLdapClientFields(Map<String, Serializable> taskVariablesMap, String variableName) {
        if (!taskVariablesMap.containsKey(variableName)) {
            throw new IllegalArgumentException("The missed argument for LDAPClient, variable name: " + variableName);
        }
        String varValue = getAsString(taskVariablesMap, variableName);
        try {
            this.getClass().getDeclaredField(variableName).set(this, varValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("The variable name is wrong:" + variableName, e);
        }
    }

    private String getAsString(Map<String, Serializable> map, String argFrom) {
        return (String) map.get(argFrom);
    }

    private String[] splitAttributes(String attrList) {
        String[] splittedAttr = new String[0];
        if (attrList != null && !attrList.trim().isEmpty()) {
            splittedAttr = attrList.split(REGEX_LIST_SEPARATOR);

        }
        return splittedAttr;
    }

    public String searchQueryLDAP() {
        NamingEnumeration results = null;
        ObjectMapper mapper = new ObjectMapper();
        Response response;
        boolean allAttrs = false;
        String resultOutput = new String();
        List attributesList = new LinkedList();
        HashSet<String> attrReturn = new HashSet();

        String[] attributesToReturn = splitAttributes(ldapSelectedAttributes);

        if (attributesToReturn.length == 0) {
            allAttrs = true;
        } else {
            attrReturn = new HashSet<>(Arrays.asList(attributesToReturn));
        }

        try {
            ldapConnection = LDAPConnectionUtility.connect(ldapUrl, ldapUsername, ldapPassword);
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            results = ldapConnection.search(ldapSearchBase, ldapSearchFilter, controls);

            //iterate throw all attributes in the result of search query
            while (results.hasMore()) {
                SearchResult searchResult = (SearchResult) results.next();
                Attributes attributes = searchResult.getAttributes();
                Map<String, String> attrMap = new HashMap();
                for (NamingEnumeration ae = attributes.getAll(); ae.hasMore();) {
                    Attribute attr = (Attribute) ae.next();
                    String attrId = attr.getID();
                    if ((!allAttrs && attrReturn.contains(attrId)) || allAttrs) {
                        attrMap.put(attrId, attr.get().toString());
                    }
                }
                if (!attrMap.isEmpty()) {
                    attributesList.add(attrMap);
                }
            }
            response = new LDAPResponse("Ok", attributesList);
        } catch (Exception e) {
            response = new ErrorResponse("Error", e.toString());
        } finally {
            if (results != null) {
                try {
                    results.close();
                } catch (Exception e) {
                }
            }
            if (ldapConnection != null) {
                try {
                    ldapConnection.close();
                } catch (Exception e) {
                }
            }
        }
        try {
            resultOutput = mapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return resultOutput;
    }
}
