/*
 * Copyright 2011 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

//
// UNDONE:
//   * POST, DELETE
//   * Response schema
//   * Namespaces
//       - Path fragments
//

package com.splunk;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import junit.framework.TestCase;
import org.junit.*;
import static org.junit.Assert.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.splunk.*;
import com.splunk.http.*;
import com.splunk.sdk.Program;

public class ServiceTest extends TestCase {
    Program program = new Program();

    public ServiceTest() {}

    void checkResponse(ResponseMessage response) {
        assertEquals(response.getStatus(), 200);
        try {
            Document root = parse(response);
            // UNDONE: Check basic structure of ATOM response
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    Service connect() throws IOException {
        return new Service(
            program.host, program.port, program.scheme)
                .login(program.username, program.password);
    }

    // Returns the response content as an XML DOM.
    public Document parse(ResponseMessage response) 
        throws IOException, SAXException 
    {
        try {
            InputStream content = response.getContent();
            DocumentBuilderFactory factory = 
                DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new InputStreamReader(content));
            return builder.parse(inputSource);
        }
        catch (ParserConfigurationException e) {
            // Convert an obscure exception to runtime
            throw new RuntimeException(e.getMessage());
        }
    }

    @Before public void setUp() {
        this.program.init(); // Pick up .splunkrc settings
    }

    @Test public void testLogin() throws IOException {
        Service service = new Service(
            program.host, program.port, program.scheme);
        service.login(program.username, program.password);
    }

    @Test public void testLoginFail() throws IOException {
        Service service = new Service(
            program.host, program.port, program.scheme);

        try {
            // Swap username & password so we know this will fail
            service.login(program.password, program.username);
            fail("Login was expected to fail");
        }
        catch (Exception e) {}
    }

    @Test public void testLogout() throws IOException {
        Service service = connect();

        // Logged in, request should succeed
        ResponseMessage response = service.get("/services");
        checkResponse(response);

        // Logged out, the request should fail
        service.logout();
        try {
            service.get("/services");
            fail("Expected request to fail");
        }
        catch (Exception e) {}
    }

    // Make a few simple requests and make sure the results look ok.
    @Test public void testGet() throws IOException {
        Service service = connect();

        String[] paths = { "/", "/services", "/services/search/jobs" };
        for (String path : paths) {
            ResponseMessage response = service.get(path);
            checkResponse(response);
        }
    }
}
