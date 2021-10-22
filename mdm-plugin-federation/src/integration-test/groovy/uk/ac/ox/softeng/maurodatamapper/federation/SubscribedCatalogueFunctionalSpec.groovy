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
package uk.ac.ox.softeng.maurodatamapper.federation


import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

/**
 * @see SubscribedCatalogueController* Controller: subscribedCatalogue
 *  | POST   | /api/subscribedCatalogues                                         | Action: save           |
 *  | GET    | /api/subscribedCatalogues                                         | Action: index          |
 *  | DELETE | /api/subscribedCatalogues/${id}                                   | Action: delete         |
 *  | PUT    | /api/subscribedCatalogues/${id}                                   | Action: update         |
 *  | GET    | /api/subscribedCatalogues/${id}                                   | Action: show           |
 *  | GET    | /api/subscribedCatalogues/${subscribedCatalogueId}/testConnection | Action: testConnection |
 *
 */
@Integration
@Slf4j
class SubscribedCatalogueFunctionalSpec extends ResourceFunctionalSpec<SubscribedCatalogue> {
    
    @Override
    String getResourcePath() {
        'subscribedCatalogues'
    }

    //note: using a groovy string like "http://localhost:$serverPort/" causes the url to be stripped when saving
    Map getValidJson() {
        [
            url          : "http://localhost:$serverPort".toString(),
            apiKey       : '67421316-66a5-4830-9156-b1ba77bba5d1',
            label        : 'Functional Test Label',
            description  : 'Functional Test Description',
            refreshPeriod: 7
        ]
    }

    Map getInvalidJson() {
        [
            url   : 'wibble',
            apiKey: '67421316-66a5-4830-9156-b1ba77bba5d1'
        ]
    }

    //note: any-string on the Url is a workaround after the previous note  
    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "url": "${json-unit.any-string}",
  "label": 'Functional Test Label',
  "description": 'Functional Test Description',
  "refreshPeriod": 7,
  "apiKey": "67421316-66a5-4830-9156-b1ba77bba5d1"
}'''
    }

    void 'T01 : Test the testConnection action'() {
        when:
        POST('', getValidJson())

        then:
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id

        when:
        GET("${subscribedCatalogueId}/testConnection", STRING_ARG)

        then:
        verifyJsonResponse OK, null

        cleanup:
        DELETE(subscribedCatalogueId)
    }
}
