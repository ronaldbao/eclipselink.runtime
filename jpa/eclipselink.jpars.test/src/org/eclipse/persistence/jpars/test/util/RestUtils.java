/*******************************************************************************
 * Copyright (c) 2011, 2015 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *      gonural - Initial implementation
 *      2014-09-01-2.6.0 Dmitry Kornilov
 *        - Fixes related to JPARS service versions
 ******************************************************************************/
package org.eclipse.persistence.jpars.test.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import org.eclipse.persistence.jpa.rs.MatrixParameters;
import org.eclipse.persistence.jpa.rs.PersistenceContext;
import org.eclipse.persistence.jpa.rs.QueryParameters;
import org.eclipse.persistence.jpa.rs.exceptions.ErrorResponse;
import org.eclipse.persistence.jpars.test.server.RestCallFailedException;

import javax.imageio.ImageIO;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.fail;

/**
 * Helper class with static utility methods used for REST service testing.
 *
 * @author gonural
 */
public class RestUtils {
    private static final Logger logger = Logger.getLogger("org.eclipse.persistence.jpars.test.server");

    private static final String APPLICATION_LOCATION = "/eclipselink.jpars.test/persistence/";
    private static final String SERVER_URI_BASE = "server.uri.base";
    public static final String JPA_RS_VERSION_STRING = "jpars.version.string";
    private static final String DEFAULT_SERVER_URI_BASE = "http://localhost:8080";
    private static final String JSON_REST_MESSAGE_FOLDER = "org/eclipse/persistence/jpars/test/restmessage/json/";
    private static final String XML_REST_MESSAGE_FOLDER = "org/eclipse/persistence/jpars/test/restmessage/xml/";
    private static final String IMAGE_FOLDER = "org/eclipse/persistence/jpars/test/image/";

    private static final Client client = Client.create();

    /**
     * Gets the server uri.
     *
     * @return the server uri
     * @throws URISyntaxException the URI syntax exception
     */
    public static URI getServerURI() throws URISyntaxException {
        String serverURIBase = System.getProperty(SERVER_URI_BASE, DEFAULT_SERVER_URI_BASE);
        return new URI(serverURIBase + APPLICATION_LOCATION);
    }

    /**
     * Gets the server uri.
     *
     * @return the server uri
     * @throws URISyntaxException the URI syntax exception
     */
    public static URI getServerURI(String versionString) throws URISyntaxException {
        String serverURIBase = System.getProperty(SERVER_URI_BASE, DEFAULT_SERVER_URI_BASE);
        String versionStringUrlFragment = versionString == null || versionString.equals("") ? "" : versionString + "/";
        return new URI(serverURIBase + APPLICATION_LOCATION + versionStringUrlFragment);
    }

    /**
     * Marshal.
     *
     * @param <T> the generic type
     * @param context the context
     * @param object the object
     * @param mediaType the media type
     * @return the string
     * @throws RestCallFailedException the rest call failed exception
     * @throws JAXBException the JAXB exception
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public static <T> String marshal(PersistenceContext context, Object object, MediaType mediaType) throws RestCallFailedException, JAXBException, UnsupportedEncodingException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        context.marshall(object, mediaType, os, true);
        return os.toString("UTF-8");
    }

    /**
     * Unmarshal.
     *
     * @param <T> the generic type
     * @param context the context
     * @param msg the msg
     * @param type the type
     * @param mediaType the media type
     * @return T the generic type
     * @throws JAXBException the JAXB exception
     */
    @SuppressWarnings("unchecked")
    public static <T> T unmarshal(PersistenceContext context, String msg, String type, MediaType mediaType) throws JAXBException {
        T resultObject = null;
        resultObject = (T) context.unmarshalEntity(type, mediaType, new ByteArrayInputStream(msg.getBytes()));
        return resultObject;
    }

    /**
     * Rest read.
     *
     * @param <T> the generic type
     * @param context the context
     * @param id the id
     * @param type the type
     * @param resultClass the result class
     * @param tenantId the tenant id
     * @param outputMediaType the output media type
     * @return T the generic type
     * @throws RestCallFailedException the rest call failed exception
     * @throws URISyntaxException the URI syntax exception
     */
    @SuppressWarnings("unchecked")
    public static <T> T restRead(PersistenceContext context, Object id, String type, Class<T> resultClass, Map<String, String> tenantId, MediaType outputMediaType) throws RestCallFailedException, URISyntaxException {
        StringBuilder uri = new StringBuilder();
        uri.append(RestUtils.getServerURI(context.getVersion()) + context.getName());
        if (tenantId != null) {
            for (String key : tenantId.keySet()) {
                uri.append(";" + key + "=" + tenantId.get(key));
            }
        }
        uri.append("/entity/" + type + "/" + id);
        WebResource webResource = client.resource(uri.toString());
        ClientResponse response = webResource.accept(outputMediaType).get(ClientResponse.class);
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status != Status.OK) {
            restCallFailed(context, status, result, outputMediaType);
        }

        T resultObject = null;
        try {
            resultObject = (T) context.unmarshalEntity(type, outputMediaType, new ByteArrayInputStream(result.getBytes()));
        } catch (JAXBException e) {
            fail("Exception thrown unmarshalling: " + e);
        }
        return resultObject;
    }

    /**
     * Rest read.
     *
     * @param context the context
     * @param id the id
     * @param type the type
     * @param tenantId the tenant id
     * @param outputMediaType the output media type
     * @return the string
     * @throws RestCallFailedException the rest call failed exception
     * @throws URISyntaxException the URI syntax exception
     */
    public static String restRead(PersistenceContext context, Object id, String type, Map<String, String> tenantId, MediaType outputMediaType) throws RestCallFailedException, URISyntaxException {
        StringBuilder uri = new StringBuilder();
        uri.append(RestUtils.getServerURI(context.getVersion()) + context.getName());
        if (tenantId != null) {
            for (String key : tenantId.keySet()) {
                uri.append(";").append(key).append("=").append(tenantId.get(key));
            }
        }
        uri.append("/entity/").append(type).append("/").append(id);

        logger.info(uri.toString());

        WebResource webResource = client.resource(uri.toString());
        ClientResponse response = webResource.accept(outputMediaType).get(ClientResponse.class);
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status != Status.OK) {
            restCallFailed(context, status, result, outputMediaType);
        }
        return result;
    }

    /**
     * REST GET entity with hints.
     *
     * @param context persistent context
     * @param id entity ID
     * @param type entity type
     * @param hints hints list
     * @param outputMediaType media type
     * @return response in string
     */
    public static String restReadWithHints(PersistenceContext context, Object id, String type, Map<String, String> hints, MediaType outputMediaType) throws RestCallFailedException, URISyntaxException {
        StringBuilder uri = new StringBuilder();
        uri.append(RestUtils.getServerURI(context.getVersion())).append(context.getName()).append("/entity/").append(type).append("/").append(id);
        appendParametersAndHints(uri, null, hints);
        WebResource webResource = client.resource(uri.toString());
        ClientResponse response = webResource.accept(outputMediaType).get(ClientResponse.class);
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status != Status.OK) {
            restCallFailed(context, status, result, outputMediaType);
        }
        return result;
    }

    /**
     * Rest update.
     *
     * @param <T> the generic type
     * @param context the context
     * @param object the object
     * @param type the type
     * @param resultClass the result class
     * @param tenantId the tenant id
     * @param mediaType the media type
     * @param sendLinks the send links
     * @return T the generic type
     * @throws RestCallFailedException the rest call failed exception
     * @throws URISyntaxException the URI syntax exception
     */
    @SuppressWarnings("unchecked")
    public static <T> T restUpdate(PersistenceContext context, Object object, String type, Class<T> resultClass, Map<String, String> tenantId, MediaType mediaType, boolean sendLinks) throws RestCallFailedException, URISyntaxException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            if (sendLinks) {
                context.marshallEntity(object, mediaType, os);
            } else {
                context.marshall(object, mediaType, os, false);
            }
        } catch (JAXBException e) {
            fail("Exception thrown marshalling: " + e);
        }

        StringBuilder uri = new StringBuilder();
        uri.append(RestUtils.getServerURI(context.getVersion()) + context.getName());
        if (tenantId != null) {
            for (String key : tenantId.keySet()) {
                uri.append(";" + key + "=" + tenantId.get(key));
            }
        }
        uri.append("/entity/" + type);
        WebResource webResource = client.resource(uri.toString());
        ClientResponse response = webResource.type(mediaType).accept(mediaType).post(ClientResponse.class, os.toString());
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);

        if (status != Status.OK) {
            restCallFailed(context, status, result, mediaType);
        }

        T resultObject = null;
        try {
            resultObject = (T) context.unmarshalEntity(type, mediaType, new ByteArrayInputStream(result.getBytes()));
        } catch (JAXBException e) {
            fail("Exception thrown unmarshalling: " + e);
        }
        return resultObject;
    }

    /**
     * Rest update.
     *
     * @param context the context
     * @param message the message
     * @param type the type
     * @param tenantId the tenant id
     * @param mediaType the media type
     * @return the string
     * @throws RestCallFailedException the rest call failed exception
     * @throws URISyntaxException the URI syntax exception
     */
    public static String restUpdate(PersistenceContext context, String message, String type, Map<String, String> tenantId, MediaType mediaType) throws RestCallFailedException, URISyntaxException {
        StringBuilder uri = new StringBuilder();
        uri.append(RestUtils.getServerURI(context.getVersion()) + context.getName());
        if (tenantId != null) {
            for (String key : tenantId.keySet()) {
                uri.append(";" + key + "=" + tenantId.get(key));
            }
        }
        uri.append("/entity/" + type);
        WebResource webResource = client.resource(uri.toString());
        ClientResponse response = webResource.type(mediaType).accept(mediaType).post(ClientResponse.class, message);
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status != Status.OK) {
            restCallFailed(context, status, result, mediaType);
        }
        return result;
    }

    /**
     * Rest create.
     *
     * @param <T> the generic type
     * @param context the context
     * @param object the object
     * @param type the type
     * @param resultClass the result class
     * @param tenantId the tenant id
     * @param mediaType the media type
     * @param sendLinks the send links
     * @return T the generic type
     * @throws RestCallFailedException the rest call failed exception
     * @throws URISyntaxException the URI syntax exception
     */
    @SuppressWarnings("unchecked")
    public static <T> T restCreate(PersistenceContext context, Object object, String type, Class<T> resultClass, Map<String, String> tenantId, MediaType mediaType, boolean sendLinks) throws RestCallFailedException, URISyntaxException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            if (sendLinks) {
                context.marshall(object, mediaType, os, true);
            } else {
                context.marshall(object, mediaType, os, false);
            }
        } catch (JAXBException e) {
            fail("Exception thrown marshalling: " + e);
        }

        StringBuilder uri = new StringBuilder();
        uri.append(RestUtils.getServerURI(context.getVersion()) + context.getName());
        if (tenantId != null) {
            for (String key : tenantId.keySet()) {
                uri.append(";" + key + "=" + tenantId.get(key));
            }
        }
        uri.append("/entity/" + type);

        logger.info(os.toString());
        logger.info(uri.toString());

        WebResource webResource = client.resource(uri.toString());
        ClientResponse response = null;
        if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
            response = webResource.type(mediaType).accept("application/json;charset=UTF-8").put(ClientResponse.class, os.toString());
        } else {
            response = webResource.type(mediaType).accept(mediaType).put(ClientResponse.class, os.toString());
        }

        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);

        if (status != Status.CREATED) {
            restCallFailed(context, status, result, mediaType);
        }

        T resultObject = null;
        try {
            resultObject = (T) context.unmarshalEntity(type, mediaType, new ByteArrayInputStream(result.getBytes()));
        } catch (JAXBException e) {
            fail("Exception thrown unmarshalling: " + e);
        }
        return resultObject;
    }

    /**
     * Rest create with sequence.
     *
     * @param context the context
     * @param object the object
     * @param type the type
     * @param tenantId the tenant id
     * @param mediaType the media type
     * @throws URISyntaxException the URI syntax exception
     */
    public static void restCreateWithSequence(PersistenceContext context, String object, String type, Map<String, String> tenantId, MediaType mediaType) throws URISyntaxException {
        StringBuilder uri = new StringBuilder();
        uri.append(RestUtils.getServerURI(context.getVersion()) + context.getName());
        if (tenantId != null) {
            for (String key : tenantId.keySet()) {
                uri.append(";" + key + "=" + tenantId.get(key));
            }
        }
        uri.append("/entity/" + type);
        WebResource webResource = client.resource(uri.toString());
        ClientResponse response = webResource.type(mediaType).accept(mediaType).put(ClientResponse.class, object.toString());
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status == Status.BAD_REQUEST) {
            restCallFailed(context, status, result, mediaType);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Rest create.
     *
     * @param <T> the generic type
     * @param context the context
     * @param object the object
     * @param type the type
     * @param resultClass the result class
     * @param tenantId the tenant id
     * @param mediaType the media type
     * @return T the generic type
     * @throws URISyntaxException the URI syntax exception
     */
    @SuppressWarnings("unchecked")
    public static <T> T restCreate(PersistenceContext context, String object, String type, Class<T> resultClass, Map<String, String> tenantId, MediaType mediaType) throws URISyntaxException {
        StringBuilder uri = new StringBuilder();
        uri.append(RestUtils.getServerURI(context.getVersion()) + context.getName());
        if (tenantId != null) {
            for (String key : tenantId.keySet()) {
                uri.append(";" + key + "=" + tenantId.get(key));
            }
        }
        uri.append("/entity/" + type);
        WebResource webResource = client.resource(uri.toString());
        ClientResponse response = webResource.type(mediaType).accept(mediaType).put(ClientResponse.class, object);
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);

        if (status != Status.CREATED) {
            restCallFailed(context, status, result, mediaType);
        }

        T resultObject = null;
        try {
            resultObject = (T) context.unmarshalEntity(type, mediaType, new ByteArrayInputStream(result.getBytes()));
        } catch (JAXBException e) {
            fail("Exception thrown unmarshalling: " + e);
        }
        return resultObject;
    }

    /**
     * Rest delete.
     *
     * @param <T> the generic type
     * @param context the context
     * @param id the id
     * @param type the type
     * @param resultClass the result class
     * @param attribute the attribute
     * @param tenantId the tenant id
     * @param mediaType the media type
     * @throws RestCallFailedException the rest call failed exception
     * @throws URISyntaxException the URI syntax exception
     */
    public static <T> void restDelete(PersistenceContext context, Object id, String type, Class<T> resultClass, String attribute, Map<String, String> tenantId, MediaType mediaType) throws RestCallFailedException, URISyntaxException {
        StringBuilder uri = new StringBuilder();
        uri.append(RestUtils.getServerURI(context.getVersion()) + context.getName());
        if (tenantId != null) {
            for (String key : tenantId.keySet()) {
                uri.append(";" + key + "=" + tenantId.get(key));
            }
        }
        uri.append("/entity/" + type + "/" + id);
        if (attribute != null) {
            uri.append("/" + attribute);
        }

        WebResource webResource = client.resource(uri.toString());
        ClientResponse response = webResource.type(mediaType).accept(mediaType).delete(ClientResponse.class);
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status != Status.OK) {
            restCallFailed(context, status, result, mediaType);
        }
    }

    /**
     * Rest read by href.
     *
     * @param <T> the generic type
     * @param context the context
     * @param href the href
     * @param type the type
     * @param outputMediaType the output media type
     * @return T the generic type
     * @throws RestCallFailedException the rest call failed exception
     * @throws JAXBException the JAXB exception
     */
    @SuppressWarnings("unchecked")
    public static <T> T restReadByHref(PersistenceContext context, String href, String type, MediaType outputMediaType) throws RestCallFailedException, JAXBException {
        WebResource webResource = client.resource(href);
        ClientResponse response = webResource.accept(outputMediaType).get(
                ClientResponse.class);
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status != Status.OK) {
            restCallFailed(context, status, result, outputMediaType);
        }
        return (T) context.unmarshalEntity(type, outputMediaType, new ByteArrayInputStream(result.getBytes()));
    }

    /**
     * Rest named multi result query.
     *
     * @param context the context
     * @param queryName the query name
     * @param parameters the parameters
     * @param hints the hints
     * @param mediaType the media type
     * @return the string
     * @throws URISyntaxException the URI syntax exception
     */
    public static String restNamedMultiResultQuery(PersistenceContext context, String queryName, Map<String, Object> parameters, Map<String, String> hints, MediaType mediaType) throws URISyntaxException {
        StringBuilder resourceURL = new StringBuilder();
        resourceURL.append(RestUtils.getServerURI(context.getVersion()) + context.getName() + "/query/" + queryName);
        appendParametersAndHints(resourceURL, parameters, hints);
        WebResource webResource = client.resource(resourceURL.toString());
        ClientResponse response = webResource.accept(mediaType).get(ClientResponse.class);
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status != Status.OK) {
            restCallFailed(context, status, result, mediaType);
        }

        return result;
    }

    /**
     * Rest named single result query.
     *
     * @param context the context
     * @param queryName the query name
     * @param parameters the parameters
     * @param hints the hints
     * @param outputMediaType the output media type
     * @return the string
     * @throws URISyntaxException the URI syntax exception
     */
    public static String restNamedSingleResultQuery(PersistenceContext context, String queryName, Map<String, Object> parameters, Map<String, String> hints, MediaType outputMediaType) throws URISyntaxException {
        StringBuilder resourceURL = new StringBuilder();
        resourceURL.append(RestUtils.getServerURI(context.getVersion()) + context.getName() + "/singleResultQuery/" + queryName);
        appendParametersAndHints(resourceURL, parameters, hints);
        WebResource webResource = client.resource(resourceURL.toString());
        ClientResponse response = webResource.accept(outputMediaType).get(ClientResponse.class);
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status != Status.OK) {
            restCallFailed(context, status, result, outputMediaType);
        }

        return result;
    }

    /**
     * Rest named single result query in byte array.
     *
     * @param context the persistent context
     * @param queryName the query name
     * @param persistenceUnit the persistence unit
     * @param parameters the parameters
     * @param hints the hints
     * @param outputMediaType the output media type
     * @return the byte[]
     * @throws URISyntaxException the URI syntax exception
     */
    public static byte[] restNamedSingleResultQueryInByteArray(PersistenceContext context, String queryName, String persistenceUnit, Map<String, Object> parameters, Map<String, String> hints, MediaType outputMediaType) throws URISyntaxException {
        StringBuilder resourceURL = new StringBuilder();
        resourceURL.append(RestUtils.getServerURI(context.getVersion()) + persistenceUnit + "/singleResultQuery/" + queryName);
        appendParametersAndHints(resourceURL, parameters, hints);
        WebResource webResource = client.resource(resourceURL.toString());
        ClientResponse response = webResource.accept(outputMediaType).get(ClientResponse.class);
        Status status = response.getClientResponseStatus();
        if (status != Status.OK) {
            throw new RestCallFailedException(status);
        }

        ByteArrayOutputStream baos = null;

        if (outputMediaType == MediaType.APPLICATION_OCTET_STREAM_TYPE) {
            try {
                BufferedImage image = ImageIO.read(response.getEntityInputStream());
                baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                byte[] bytes = baos.toByteArray();
                return bytes;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (baos != null) {
                    try {
                        baos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Rest update relationship.
     *
     * @param <T> the generic type
     * @param context the context
     * @param objectId the object id
     * @param type the type
     * @param relationshipName the relationship name
     * @param newValue the new value
     * @param resultClass the result class
     * @param mediaType the media type
     * @return T the generic type
     * @throws RestCallFailedException the rest call failed exception
     * @throws URISyntaxException the URI syntax exception
     */
    @SuppressWarnings("unchecked")
    public static <T> T restUpdateRelationship(PersistenceContext context, String objectId, String type, String relationshipName, Object newValue, Class<T> resultClass, MediaType mediaType) throws RestCallFailedException, URISyntaxException {
        WebResource webResource = client.resource(RestUtils.getServerURI(context.getVersion()) + context.getName() + "/entity/" + type + "/" + objectId + "/" + relationshipName);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            context.marshallEntity(newValue, mediaType, os);

        } catch (JAXBException e) {
            fail("Exception thrown marshalling: " + e);
        }
        ClientResponse response = webResource.type(mediaType).accept(mediaType).post(ClientResponse.class, os.toString());
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);

        if (status != Status.OK) {
            restCallFailed(context, status, result, mediaType);
        }

        T resultObject = null;
        try {
            resultObject = (T) context.unmarshalEntity(type, mediaType, new ByteArrayInputStream(result.getBytes()));
        } catch (JAXBException e) {
            fail("Exception thrown unmarshalling: " + e);
        }
        return resultObject;
    }

    /**
     * Rest update bidirectional relationship.
     *
     * @param context the context
     * @param objectId the object id
     * @param type the type
     * @param relationshipName the relationship name
     * @param newValue the new value
     * @param mediaType the media type
     * @param partner the partner
     * @param sendLinks the send links
     * @return the string
     * @throws RestCallFailedException the rest call failed exception
     * @throws URISyntaxException the URI syntax exception
     * @throws JAXBException the JAXB exception
     */
    public static String restUpdateBidirectionalRelationship(PersistenceContext context, String objectId, String type, String relationshipName, Object newValue, MediaType mediaType, String partner, boolean sendLinks) throws RestCallFailedException, URISyntaxException, JAXBException {

        String url = RestUtils.getServerURI(context.getVersion()) + context.getName() + "/entity/" + type + "/"
                + objectId + "/" + relationshipName;
        if (partner != null) {
            url += ";" + MatrixParameters.JPARS_RELATIONSHIP_PARTNER + "=" + partner;
        }
        WebResource webResource = client.resource(url);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        context.marshall(newValue, mediaType, os, sendLinks);
        ClientResponse response = webResource.type(mediaType).accept(mediaType).post(ClientResponse.class, os.toString());
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status != Status.OK) {
            restCallFailed(context, status, result, mediaType);
        }

        return result;
    }

    /**
     * Rest remove bidirectional relationship.
     *
     * @param context the context
     * @param objectId the object id
     * @param type the type
     * @param relationshipName the relationship name
     * @param mediaType the media type
     * @param partner the partner
     * @param listItemId the list item id
     * @return the string
     * @throws RestCallFailedException the rest call failed exception
     * @throws URISyntaxException the URI syntax exception
     * @throws JAXBException the JAXB exception
     */
    public static String restRemoveBidirectionalRelationship(PersistenceContext context, String objectId, String type, String relationshipName, MediaType mediaType, String partner, String listItemId) throws RestCallFailedException, URISyntaxException, JAXBException {
        String url = RestUtils.getServerURI(context.getVersion()) + context.getName() + "/entity/" + type + "/" + objectId + "/" + relationshipName;
        if (partner != null) {
            url += "?" + QueryParameters.JPARS_RELATIONSHIP_PARTNER + "=" + partner;
            if (listItemId != null) {
                url += "&" + QueryParameters.JPARS_LIST_ITEM_ID + "=" + listItemId;
            }
        } else {
            if (listItemId != null) {
                url += "?" + QueryParameters.JPARS_LIST_ITEM_ID + "=" + listItemId;
            }
        }

        WebResource webResource = client.resource(url);
        ClientResponse response = webResource.type(mediaType).accept(mediaType).delete(ClientResponse.class);
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status != Status.OK) {
            restCallFailed(context, status, result, mediaType);
        }
        return result;
    }

    /**
     * Gets the JSON message.
     *
     * @param inputFile the input file
     * @return the JSON message
     */
    public static String getJSONMessage(String inputFile) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(JSON_REST_MESSAGE_FOLDER + inputFile);
        String msg = convertStreamToString(is);
        return msg;
    }

    /**
     * Gets the XML message.
     *
     * @param inputFile the input file
     * @return the XML message
     */
    public static String getXMLMessage(String inputFile) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(XML_REST_MESSAGE_FOLDER + inputFile);
        return convertStreamToString(is);
    }

    /**
     * Convert image to byte array.
     *
     * @param imageName the image name
     * @return the byte[]
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static byte[] convertImageToByteArray(String imageName) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(IMAGE_FOLDER + imageName);
        BufferedImage originalImage = ImageIO.read(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(originalImage, getExtension(new File(imageName)), baos);
        return baos.toByteArray();
    }

    /**
     * Rest find attribute.
     *
     * @param context the context
     * @param id the id
     * @param type the type
     * @param attribute the attribute
     * @param tenantId the tenant id
     * @param outputMediaType the output media type
     * @return the string
     * @throws RestCallFailedException the rest call failed exception
     * @throws URISyntaxException the URI syntax exception
     */
    public static String restFindAttribute(PersistenceContext context, Object id, String type, String attribute, Map<String, String> tenantId, MediaType outputMediaType) throws RestCallFailedException, URISyntaxException {
        StringBuilder uri = new StringBuilder();
        uri.append(RestUtils.getServerURI(context.getVersion()) + context.getName());
        if (tenantId != null) {
            for (String key : tenantId.keySet()) {
                uri.append(";" + key + "=" + tenantId.get(key));
            }
        }
        uri.append("/entity/" + type + "/" + id + "/" + attribute);
        WebResource webResource = client.resource(uri.toString());
        ClientResponse response = webResource.accept(outputMediaType).get(ClientResponse.class);
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status != Status.OK) {
            restCallFailed(context, status, result, outputMediaType);
        }

        return result;
    }

    /**
     * Rest find attribute query.
     *
     * @param context the context
     * @param id the id
     * @param type the type
     * @param attribute the attribute
     * @param parameters the parameters
     * @param hints the hints
     * @param outputMediaType the output media type
     * @return the string
     * @throws RestCallFailedException the rest call failed exception
     * @throws URISyntaxException the URI syntax exception
     */
    public static String restFindAttribute(PersistenceContext context, Object id, String type, String attribute, Map<String, Object> parameters, Map<String, String> hints, MediaType outputMediaType) throws RestCallFailedException, URISyntaxException {
        StringBuilder resourceURL = new StringBuilder();
        resourceURL.append(RestUtils.getServerURI(context.getVersion()) + context.getName() + "/entity/" + type + "/" + id + "/" + attribute);
        appendParametersAndHints(resourceURL, parameters, hints);
        WebResource webResource = client.resource(resourceURL.toString());

        ClientResponse response = webResource.accept(outputMediaType).get(ClientResponse.class);
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status != Status.OK) {
            restCallFailed(context, status, result, outputMediaType);
        }
        return result;
    }

    /**
     * Rest named paged multi result query.
     *
     * @param context the context
     * @param queryName the query name
     * @param parameters the parameters
     * @param hints the hints
     * @param outputMediaType the output media type
     * @return the string
     * @throws URISyntaxException the URI syntax exception
     */
    public static String restNamedPagedMultiResultQuery(PersistenceContext context, String queryName, Map<String, Object> parameters, Map<String, String> hints, MediaType outputMediaType) throws URISyntaxException {
        StringBuilder resourceURL = new StringBuilder();
        resourceURL.append(RestUtils.getServerURI(context.getVersion()) + context.getName() + "/query/" + queryName);
        appendParametersAndHints(resourceURL, parameters, hints);
        WebResource webResource = client.resource(resourceURL.toString());
        ClientResponse response = webResource.accept(outputMediaType).get(ClientResponse.class);
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status != Status.OK) {
            restCallFailed(context, status, result, outputMediaType);
        }
        return result;
    }

    /**
     * Rest update query.
     *
     * @param context the context
     * @param queryName the query name
     * @param returnType the return type
     * @param parameters the parameters
     * @param hints the hints
     * @param mediaType the media type
     * @return the string
     * @throws URISyntaxException the URI syntax exception
     */
    public static String restUpdateQuery(PersistenceContext context, String queryName, String returnType, Map<String, Object> parameters, Map<String, String> hints, MediaType mediaType) throws URISyntaxException {
        StringBuilder resourceURL = new StringBuilder();
        resourceURL.append(RestUtils.getServerURI(context.getVersion()) + context.getName() + "/query/" + queryName);
        appendParametersAndHints(resourceURL, parameters, hints);

        WebResource webResource = client.resource(resourceURL.toString());
        ClientResponse response = webResource.post(ClientResponse.class);
        Status status = response.getClientResponseStatus();
        String result = response.getEntity(String.class);
        if (status != Status.OK) {
            restCallFailed(context, status, result, mediaType);
        }
        return result;
    }

    private static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    @SuppressWarnings("unused")
    private static void writeToFile(String data) {
        BufferedWriter writer = null;
        try {
            String fileName = (System.currentTimeMillis() + ".txt");
            writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void appendParametersAndHints(StringBuilder resourceURL, Map<String, Object> parameters, Map<String, String> hints) {
        if (parameters != null && !parameters.isEmpty()) {
            for (String key : parameters.keySet()) {
                resourceURL.append(";" + key + "=" + parameters.get(key));
            }
        }
        if (hints != null && !hints.isEmpty()) {
            boolean firstElement = true;
            for (String key : hints.keySet()) {
                if (firstElement) {
                    resourceURL.append("?");
                    firstElement = false;
                } else {
                    resourceURL.append("&");
                }
                resourceURL.append(key + "=" + hints.get(key));
            }
        }
    }

    private static String convertStreamToString(InputStream is) {
        try {
            return new java.util.Scanner(is).useDelimiter("\\A").next();
        } catch (Exception e) {
            return null;
        }
    }

    private static void restCallFailed(PersistenceContext context, Status httpStatus, String response, MediaType mediaType) {
        if ((response != null) && (!response.isEmpty())) {
            if ((response.contains("problemType")) && (response.contains("title") && (response.contains("httpStatus")))) {
                try {
                    ErrorResponse errorDetail = (ErrorResponse) context.unmarshalEntity(ErrorResponse.class.getSimpleName(), mediaType, new ByteArrayInputStream(response.getBytes()));
                    throw new RestCallFailedException(httpStatus, errorDetail);
                } catch (JAXBException e) {
                    fail("Exception thrown unmarshalling: " + e);
                }
            }
        }
        throw new RestCallFailedException(httpStatus);
    }
}
