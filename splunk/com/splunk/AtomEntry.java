/*
 * Copyright 2012 Splunk, Inc.
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

package com.splunk;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The {@code AtomEntry} class represents an Atom {@code <entry>} element.
 */
public class AtomEntry extends AtomObject {
    /** The value of the Atom entry's {@code <published>} element. */
    public String published;

    /** The value of the Atom entry's {@code <content>} element. */
    public Record content;

    /**
     * Creates a new {@code AtomEntry} instance.
     *
     * @return A new {@code AtomEntry} instance.                  ––
     */
    static AtomEntry create() {
        return new AtomEntry();
    }

    /**
     * Creates a new {@code AtomEntry} instance based on a given stream.
     * A few endpoints, such as {@code search/jobs/{sid}},
     * return an Atom {@code <entry>} element as the root of the response.
     *
     * @param input The input stream.
     * @return An {@code AtomEntry} instance representing the parsed stream.
     */
    public static AtomEntry parse(InputStream input) {
        Element root = Xml.parse(input).getDocumentElement();
        String rname = root.getTagName();
        String xmlns = root.getAttribute("xmlns");
        if (!rname.equals("entry") ||
            !xmlns.equals("http://www.w3.org/2005/Atom"))
            throw new RuntimeException("Unrecognized format");
        return AtomEntry.parse(root);
    }

    /**
     * Creates a new {@code AtomEntry} instance based on a given XML element.
     *
     * @param element The XML element.
     * @return An {@code AtomEntry} instance representing the parsed element.
     */
    static AtomEntry parse(Element element) {
        AtomEntry entry = AtomEntry.create();
        entry.load(element);
        return entry;
    }

    /**
     * Initializes the current instance with a given XML element.
     *
     * @param element The XML element.
     */
    @Override void init(Element element) {
        String name = element.getTagName();
        if (name.equals("published")) {
            this.published = element.getTextContent().trim();
        }
        else if (name.equals("content")) {
            this.content = parseContent(element);
        }
        else {
            super.init(element);
        }
    }

    /**
     * Returns a filtered list of child XML element nodes. This helper
     * function makes it easier to retrieve only the element children
     * of a given XML element.
     *
     * @param element The XML element.
     * @return A list of child element nodes.
     */
    static ArrayList<Element> getChildElements(Element element) {
        ArrayList<Element> result = new ArrayList<Element>();
        for (Node child = element.getFirstChild();
             child != null;
             child = child.getNextSibling())
            if (child.getNodeType() == Node.ELEMENT_NODE)
                result.add((Element)child);
        return result;
    }

    /**
     * Parses the {@code <content>} element of an Atom entry.
     *
     * @param element The XML element to parse.
     * @return A {@code Record} object containing the parsed values.
     */
    Record parseContent(Element element) {
        assert(element.getTagName().equals("content"));

        Record content = null;

        List<Element> children = getChildElements(element);

        int count = children.size();

        // Expect content to be empty or a single <dict> element
        assert(count == 0 || count == 1);

        if (count == 1) {
            Element child = children.get(0);
            content = parseDict(child);
        }

        return content;
    }

    /**
     * Parses a {@code <dict>} content element and returns a {@code Record}
     * object containing the parsed values.
     *
     * @param element The {@code <dict>} element to parse.
     * @return A {@code Record} object containing the parsed values.
     */
    Record parseDict(Element element) {
        assert(element.getTagName().equals("s:dict"));

        if (!element.hasChildNodes()) return null;

        List<Element> children = getChildElements(element);

        int count = children.size();
        if (count == 0) return null;

        Record result = new Record();
        for (Element child : children) {
            assert(child.getTagName().equals("s:key"));
            String key = child.getAttribute("name");
            Object value = parseValue(child);
            if (value != null) result.put(key, value);
        }
        return result;
    }

    /**
     * Parse a {@code <list>} element and return a {@code List} object
     * containing the parsed values.
     *
     * @param element The {@code <list>} element to parse.
     * @return A {@code List} object containing the parsed values.
     */
    List parseList(Element element) {
        assert(element.getTagName().equals("s:list"));

        if (!element.hasChildNodes()) return null;

        List<Element> children = getChildElements(element);

        int count = children.size();
        if (count == 0) return null;

        List result = new ArrayList(count);
        for (Element child : children) {
            assert(child.getTagName().equals("s:item"));
            Object value = parseValue(child);
            if (value != null) result.add(value);
        }
        return result;
    }

    /**
     * Parses the value content of a dict/key or a list/item element. The value
     * is either text, a {@code <dict>} element, or a {@code <list>} element.
     *
     * @param element The XML element containing the values to parse.
     * @return An object containing the parsed values. If the source was a text
     * value, the object is a {@code String}. If the source was a {@code <dict>}
     * element, the object is a {@code Record}. If the source was a
     * {@code <list>} element, the object is a {@code List} object.
     */
    Object parseValue(Element element) {
        String name = element.getTagName();

        assert(name.equals("s:key") || name.equals("s:item"));

        if (!element.hasChildNodes()) return null;

        List<Element> children = getChildElements(element);

        int count = children.size();

        // If no element children, then it must be a text value
        if (count == 0) return element.getTextContent();

        // If its not a text value, then expect a single child element.
        assert(children.size() == 1);

        Element child = children.get(0);

        name = child.getTagName();

        if (name.equals("s:dict"))
            return parseDict(child);

        if (name.equals("s:list"))
            return parseList(child);

        assert(false); // Unreached
        return null;
    }
}
