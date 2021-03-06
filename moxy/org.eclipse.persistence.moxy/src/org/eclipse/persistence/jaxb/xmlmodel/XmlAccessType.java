/*******************************************************************************
 * Copyright (c) 1998, 2015 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * dmccann - June 29/2009 - 2.0 - Initial implementation
 ******************************************************************************/
package org.eclipse.persistence.jaxb.xmlmodel;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for xml-access-type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;simpleType name="xml-access-type"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="FIELD"/&gt;
 *     &lt;enumeration value="NONE"/&gt;
 *     &lt;enumeration value="PROPERTY"/&gt;
 *     &lt;enumeration value="PUBLIC_MEMBER"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 *
 */
@XmlType(name = "xml-access-type")
@XmlEnum
public enum XmlAccessType {

    FIELD,
    NONE,
    PROPERTY,
    PUBLIC_MEMBER;

    public String value() {
        return name();
    }

    public static XmlAccessType fromValue(String v) {
        return valueOf(v);
    }

}
