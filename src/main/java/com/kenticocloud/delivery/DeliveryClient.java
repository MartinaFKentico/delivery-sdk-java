/*
 * MIT License
 *
 * Copyright (c) 2017
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.kenticocloud.delivery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Executes requests against the Kentico Cloud Delivery API.
 */
public class DeliveryClient {

    static final String ITEMS = "items";
    static final String TYPES = "types";
    static final String ELEMENTS = "elements";

    private ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient httpClient;
    private DeliveryOptions deliveryOptions;

    /**
     * Initializes a new instance of the {@link DeliveryClient} class for retrieving content of the specified project.
     * @throws IllegalArgumentException Thrown if the arguments in the {@link DeliveryOptions} are invalid.
     * @param deliveryOptions The settings of the Kentico Cloud project.
     */
    public DeliveryClient(DeliveryOptions deliveryOptions) {
        if (deliveryOptions == null) {
            throw new IllegalArgumentException("The Delivery options object is not specified.");
        }
        if (deliveryOptions.getProjectId() == null || deliveryOptions.getProjectId().isEmpty()) {
            throw new IllegalArgumentException("Kentico Cloud project identifier is not specified.");
        }
        try {
            UUID.fromString(deliveryOptions.getProjectId());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format(
                            "Provided string is not a valid project identifier (%s).  Have you accidentally passed the Preview API key instead of the project identifier?",
                            deliveryOptions.getProjectId()),
                    e);
        }
        if (deliveryOptions.isUsePreviewApi()) {
            if (deliveryOptions.getPreviewApiKey() == null || deliveryOptions.getPreviewApiKey().isEmpty()) {
                throw new IllegalArgumentException("The Preview API key is not specified.");
            }
        }
        this.deliveryOptions = deliveryOptions;
        httpClient = HttpClients.createDefault();
    }

    public ContentItemsListingResponse getItems() throws IOException {
        return getItems(new ArrayList<>());
    }

    public ContentItemsListingResponse getItems(List<NameValuePair> params) throws IOException {
        HttpUriRequest request = buildGetRequest(ITEMS, params);

        HttpResponse response = httpClient.execute(request);

        handleErrorIfNecessary(response);

        return objectMapper.readValue(response.getEntity().getContent(), ContentItemsListingResponse.class);
    }

    public ContentItemResponse getItem(String contentItemCodename) throws IOException {
        return getItem(contentItemCodename, new ArrayList<>());
    }

    public ContentItemResponse getItem(String contentItemCodename, List<NameValuePair> params) throws IOException {
        HttpUriRequest request = buildGetRequest(String.format("%s/%s", ITEMS, contentItemCodename), params);

        HttpResponse response = httpClient.execute(request);

        handleErrorIfNecessary(response);

        return objectMapper.readValue(response.getEntity().getContent(), ContentItemResponse.class);
    }

    public ContentTypesListingResponse getTypes() throws IOException {
        return getTypes(new ArrayList<>());
    }

    public ContentTypesListingResponse getTypes(List<NameValuePair> params) throws IOException {
        HttpUriRequest request = buildGetRequest(TYPES, params);

        HttpResponse response = httpClient.execute(request);

        handleErrorIfNecessary(response);

        return objectMapper.readValue(response.getEntity().getContent(), ContentTypesListingResponse.class);
    }

    public ContentType getType(String contentTypeCodeName) throws IOException {
        return getType(contentTypeCodeName, new ArrayList<>());
    }

    public ContentType getType(String contentTypeCodeName, List<NameValuePair> params) throws IOException {
        HttpUriRequest request = buildGetRequest(String.format("%s/%s", TYPES, contentTypeCodeName), params);

        HttpResponse response = httpClient.execute(request);

        handleErrorIfNecessary(response);

        return objectMapper.readValue(response.getEntity().getContent(), ContentType.class);
    }

    public Element getContentTypeElement(String contentTypeCodeName, String elementCodeName) throws IOException {
        return getContentTypeElement(contentTypeCodeName, elementCodeName, new ArrayList<>());
    }

    public Element getContentTypeElement(String contentTypeCodeName, String elementCodeName, List<NameValuePair> params) throws IOException {
        HttpUriRequest request = buildGetRequest(String.format("%s/%s/%s/%s", TYPES, contentTypeCodeName, ELEMENTS, elementCodeName), params);

        HttpResponse response = httpClient.execute(request);

        handleErrorIfNecessary(response);

        return objectMapper.readValue(response.getEntity().getContent(), Element.class);
    }

    protected HttpUriRequest buildGetRequest(String apiCall, List<NameValuePair> nameValuePairs) {
        RequestBuilder requestBuilder = RequestBuilder.get(String.format("%s/%s", getBaseUrl(), apiCall));
        if (deliveryOptions.isUsePreviewApi()) {
            requestBuilder.setHeader(
                    HttpHeaders.AUTHORIZATION, String.format("Bearer %s", deliveryOptions.getPreviewApiKey())
            );
        }
        requestBuilder.setHeader(HttpHeaders.ACCEPT, "application/json");
        for (NameValuePair nameValuePair : nameValuePairs) {
            requestBuilder.addParameter(nameValuePair);
        }
        return requestBuilder.build();
    }

    private String getBaseUrl() {
        if (deliveryOptions.isUsePreviewApi()) {
            return String.format(deliveryOptions.getPreviewEndpoint(), deliveryOptions.getProjectId());
        } else {
            return String.format(deliveryOptions.getProductionEndpoint(), deliveryOptions.getProjectId());
        }
    }

    private void handleErrorIfNecessary(HttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() >= 400) {
            KenticoError kenticoError = objectMapper.readValue(response.getEntity().getContent(), KenticoError.class);
            throw new KenticoErrorException(kenticoError);
        } else if (response.getStatusLine().getStatusCode() >= 500) {
            throw new IOException("Unknown error with Kentico API.  Kentico is likely suffering site issues.");
        }
    }
}