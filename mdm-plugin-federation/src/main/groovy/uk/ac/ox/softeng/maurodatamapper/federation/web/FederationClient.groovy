/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.federation.web

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import io.micronaut.core.annotation.AnnotationMetadataResolver
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.DefaultHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.http.client.LoadBalancer
import io.micronaut.http.client.exceptions.HttpClientException
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.ssl.NettyClientSslBuilder
import io.micronaut.http.codec.MediaTypeCodecRegistry
import io.micronaut.http.exceptions.HttpException
import io.micronaut.http.uri.UriBuilder
import io.netty.channel.MultithreadEventLoopGroup
import io.netty.util.concurrent.DefaultThreadFactory
import io.reactivex.Flowable
import org.springframework.context.ApplicationContext
import org.xml.sax.SAXException

import java.time.Duration
import java.util.concurrent.ThreadFactory

/**
 * @since 14/04/2021
 */
@Slf4j
class FederationClient {

    static final String API_KEY_HEADER = 'apiKey'
    private HttpClient client
    private String hostUrl
    private String contextPath

    FederationClient(String hostUrl, ApplicationContext applicationContext) {
        this(hostUrl,
             applicationContext.getBean(HttpClientConfiguration),
             applicationContext.getBean(NettyClientSslBuilder),
             applicationContext.getBean(MediaTypeCodecRegistry)
        )
    }

    FederationClient(String hostUrl,
                     HttpClientConfiguration httpClientConfiguration,
                     NettyClientSslBuilder nettyClientSslBuilder,
                     MediaTypeCodecRegistry mediaTypeCodecRegistry) {
        this(hostUrl,
             httpClientConfiguration,
             new DefaultThreadFactory(MultithreadEventLoopGroup),
             nettyClientSslBuilder,
             mediaTypeCodecRegistry
        )
    }

    private FederationClient(String hostUrl,
                             HttpClientConfiguration httpClientConfiguration,
                             ThreadFactory threadFactory,
                             NettyClientSslBuilder nettyClientSslBuilder,
                             MediaTypeCodecRegistry mediaTypeCodecRegistry) {
        this.hostUrl = hostUrl
        // The http client resolves using URI.resolve which ignores anything in the url path,
        // therefore we need to make sure its part of the context path.
        URI hostUri = hostUrl.toURI()
        if (hostUri.path && !hostUri.path.endsWith('api')) {
            String path = hostUri.path.endsWith('/') ? hostUri.path : "${hostUri.path}/"
            this.contextPath = "${path}api"
        } else {
            this.contextPath = 'api'
        }
        httpClientConfiguration.setReadTimeout(Duration.ofMinutes(5))
        client = new DefaultHttpClient(LoadBalancer.fixed(hostUrl.toURL()),
                                       httpClientConfiguration,
                                       this.contextPath,
                                       threadFactory,
                                       nettyClientSslBuilder,
                                       mediaTypeCodecRegistry,
                                       AnnotationMetadataResolver.DEFAULT)
        log.debug('Client created to connect to {}', hostUrl)
    }

    GPathResult getSubscribedCatalogueModelsFromAtomFeed(UUID apiKey) {
        // Currently we use the ATOM feed which is XML and the micronaut client isnt designed to decode XML
        retrieveXmlDataFromClient(UriBuilder.of('feeds/all'), apiKey)
    }

    Map<String, Object> getSubscribedCatalogueModels(UUID apiKey) {
        retrieveMapFromClient(UriBuilder.of('published/models'), apiKey)
    }

    List<Map<String, Object>> getAvailableExporters(UUID apiKey, String urlResourceType) {
        retrieveListFromClient(UriBuilder.of(urlResourceType).path('providers/exporters'), apiKey)
    }

    Map<String, Object> getVersionLinksForModel(UUID apiKey, String urlModelResourceType, UUID modelId) {
        retrieveMapFromClient(UriBuilder.of(urlModelResourceType).path(modelId.toString()), apiKey)
    }

    String getStringResourceExport(UUID apiKey, String urlResourceType, UUID resourceId, Map exporterInfo) {
        retrieveStringFromClient(UriBuilder.of(urlResourceType)
                                     .path(resourceId.toString())
                                     .path('export')
                                     .path(exporterInfo.namespace)
                                     .path(exporterInfo.name)
                                     .path(exporterInfo.version),
                                 apiKey
        )
    }

    private GPathResult retrieveXmlDataFromClient(UriBuilder uriBuilder, UUID apiKey, Map params = [:]) {
        String body = retrieveStringFromClient(uriBuilder, apiKey, params)
        try {
            new XmlSlurper().parseText(body)
        } catch (IOException | SAXException exception) {
            throw new ApiInternalException('FED01', "Could not translate XML from endpoint [${getFullUrl(uriBuilder, params)}].\n" +
                                                    "Exception: ${exception.getMessage()}")
        }
    }

    private Map<String, Object> retrieveMapFromClient(UriBuilder uriBuilder, UUID apiKey, Map params = [:]) {
        try {
            Flowable<Map> response = client.retrieve(HttpRequest
                                                         .GET(uriBuilder.expand(params))
                                                         .header(API_KEY_HEADER, apiKey.toString()),
                                                     Argument.mapOf(String, Object)) as Flowable<Map>
            response.blockingFirst()
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    private String retrieveStringFromClient(UriBuilder uriBuilder, UUID apiKey, Map params = [:]) {
        try {
            Flowable<String> response = client.retrieve(HttpRequest
                                                            .GET(uriBuilder.expand(params))
                                                            .header(API_KEY_HEADER, apiKey.toString()),
                                                        Argument.STRING) as Flowable<String>
            response.blockingFirst()
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    private List<Map<String, Object>> retrieveListFromClient(UriBuilder uriBuilder, UUID apiKey, Map params = [:]) {
        try {
            Flowable<List> response = client.retrieve(HttpRequest
                                                          .GET(uriBuilder.expand(params))
                                                          .header(API_KEY_HEADER, apiKey.toString()),
                                                      Argument.listOf(Map)) as Flowable<List>
            response.blockingFirst()
        }
        catch (HttpException ex) {
            handleHttpException(ex, getFullUrl(uriBuilder, params))
        }
    }

    private static void handleHttpException(HttpException ex, String fullUrl) throws ApiException {
        if (ex instanceof HttpClientResponseException) {
            if (ex.status == HttpStatus.NOT_FOUND) {
                throw new ApiBadRequestException('FED02', "Requested endpoint could not be found ${fullUrl}")
            } else {
                throw new ApiBadRequestException('FED03', "Could not load resource from endpoint [${fullUrl}].\n" +
                                                          "Response body [${ex.response.body()}]",
                                                 ex)
            }
        } else if (ex instanceof HttpClientException) {
            throw new ApiBadRequestException('FED04', "Could not load resource from endpoint [${fullUrl}]", ex)
        }
        throw new ApiInternalException('FED05', "Could not load resource from endpoint [${fullUrl}]", ex)
    }

    private String getFullUrl(UriBuilder uriBuilder, Map params) {
        String path = uriBuilder.build().toString()
        UriBuilder.of(hostUrl).path(contextPath).path(path).expand(params).toString()
    }
}
