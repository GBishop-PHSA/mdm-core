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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.container

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndPermissionChangingFunctionalSpec

import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.plugin.json.builder.JsonOutput
import grails.testing.mixin.integration.Integration
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.FORBIDDEN
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: classifier
 *  |  POST    | /api/classifiers        | Action: save
 *  |  GET     | /api/classifiers        | Action: index
 *  |  DELETE  | /api/classifiers/${id}  | Action: delete
 *  |  PUT     | /api/classifiers/${id}  | Action: update
 *  |  GET     | /api/classifiers/${id}  | Action: show
 *  |  DELETE  | /api/classifiers/${classifierId}/readByAuthenticated  | Action: readByAuthenticated
 *  |  PUT     | /api/classifiers/${classifierId}/readByAuthenticated  | Action: readByAuthenticated
 *  |  DELETE  | /api/classifiers/${classifierId}/readByEveryone       | Action: readByEveryone
 *  |  PUT     | /api/classifiers/${classifierId}/readByEveryone       | Action: readByEveryone#
 *
 *
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers | Action: save
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers  | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers/${id}*  | Action: delete
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers/${id}*  | Action: show
 *
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers  | Action: save
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/classifiers  | Action: index
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierController
 */
@Integration
@Slf4j
class ClassifierFunctionalSpec extends UserAccessAndPermissionChangingFunctionalSpec {

    @Transactional
    String getTestClassifierId() {
        Classifier.findByLabel('Functional Test Classifier').id.toString()
    }

    @Transactional
    String getSimpleTerminologyId() {
        Terminology.findByLabel(BootstrapModels.SIMPLE_TERMINOLOGY_NAME).id.toString()
    }

    String getResourcePath() {
        'classifiers'
    }

    Map getValidJson() {
        [
            label: 'Functional Test Classifier 2',
        ]
    }

    Map getInvalidJson() {
        [
            label: 'Functional Test Classifier'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'Just something for testing'
        ]
    }

    Pattern getExpectedCreatedEditRegex() {
        ~/\[Classifier:Functional Test Classifier 2] created/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[Classifier:Functional Test Classifier 2] changed properties \[description]/
    }

    @Override
    List<String> getEditorAvailableActions() {
        ['show', 'comment', 'editDescription', 'update', 'save', 'softDelete', 'delete']
    }

    @Override
    String getEditorGroupRoleName() {
        GroupRole.CONTAINER_ADMIN_ROLE_NAME
    }

    @Override
    Boolean getReaderCanCreate() {
        true
    }

    @Override
    Boolean getAuthenticatedUsersCanCreate() {
        true
    }

    @Override
    void verifyL01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        assert response.body().count == 0
    }

    @Override
    void verifyN01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        response.body().count == 0
        response.body().items.size() == 0
    }

    @Override
    int getExpectedCountOfGroupsWithAccess() {
        2
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 6,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Functional Test Classifier",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier simple",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier2",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "Environment A",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "Environment B",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    }
  ]
}'''
    }

    @Override
    String getAdminIndexJson() {
        getEditorIndexJson()
    }

    @Override
    String getShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "id": "${json-unit.matches:id}",
  "label": "Functional Test Classifier 2",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": ["comment","delete","editDescription","save","show","softDelete","update"]
}'''
    }

    void "Test the catalogueItems action for classifier"() {
        when: "The catalogueItems action on a known classifier ID is requested unlogged in"
        GET("${getTestClassifierId()}/catalogueItems")

        then: "The response is not found"
        response.status == HttpStatus.NOT_FOUND

        when: "The catalogueItems action is requested on a known classifier ID (with no catalogueItems) logged in as editor"
        loginEditor()
        GET("classifiers/${getTestClassifierId()}/catalogueItems", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, '''{
  "count": 0,
  "items": []
}'''

        when: "The catalogueItems action is requested on a known classifier ID (with no catalogueItems) logged in as admin"
        loginAdmin()
        GET("${getTestClassifierId()}/catalogueItems", STRING_ARG)

        then: "The response is OK"
        verifyJsonResponse OK, '''{
  "count": 0,
  "items": []
}'''

        when: "A classifier is added to a terminology"
        loginAdmin()
        POST("terminologies/${getSimpleTerminologyId()}/classifiers", [
            label: 'A test classifier for a terminology'
        ], MAP_ARG, true)

        then: "Resource is created"
        response.status == HttpStatus.CREATED
        String newId = response.body().id

        when: "The catalogueItems action is requested on the new classifier ID (which is associated with a terminology) logged in as admin"
        loginAdmin()
        GET("${newId}/catalogueItems", STRING_ARG)

        then: "The response is OK"
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "Terminology",
      "label": "Simple Test Terminology"
    }
  ]
}'''

        when: "The classifier is requested from the terminology"
        loginAdmin()
        GET("terminologies/${getSimpleTerminologyId()}/classifiers/${newId}", MAP_ARG, true)

        then: "Resource is shown"
        verifyResponse(OK, response)
        assert responseBody().id == newId

        when: "The classifier is deleted from the terminology"
        loginAdmin()
        DELETE("terminologies/${getSimpleTerminologyId()}/classifiers/${newId}", MAP_ARG, true)

        then: "Resource is deleted"
        response.status == HttpStatus.NO_CONTENT

        when: "The catalogueItems action is requested on the new classifier ID (which has been deleted from the terminology) logged in as admin"
        loginAdmin()
        GET("${newId}/catalogueItems", STRING_ARG)

        then: "The response is OK"
        verifyJsonResponse OK, '''{
  "count": 0,
  "items": []
}'''

        cleanup:
        removeValidIdObject(newId)
    }

    void "CA01 test the creation of a classifier as part of a terminology"() {
        given: 'putting a catalog item id'
        String terminologyId = getSimpleTerminologyId()

        when: 'not authenticated'
        POST("terminologies/$terminologyId/classifiers", [
            label: 'A test classifier for a terminology'
        ], MAP_ARG, true)

        then:
        verifyResponse(NOT_FOUND, response)

        when: 'authenticated'
        loginAuthenticated()
        POST("terminologies/$terminologyId/classifiers", [
            label: 'A test classifier for a terminology'
        ], MAP_ARG, true)

        then:
        verifyResponse(NOT_FOUND, response)

        when: 'reader'
        loginReader()
        POST("terminologies/$terminologyId/classifiers", [
            label: 'A test classifier for a terminology'
        ], MAP_ARG, true)

        then:
        verifyResponse(FORBIDDEN, response)
    }

    void "CA01A Test as an editor"() {
        given: 'putting a catalog item id'
        String terminologyId = getSimpleTerminologyId()

        when: 'Editor'
        loginEditor()
        POST("terminologies/$terminologyId/classifiers", [
            label: 'A test classifier for a terminology'
        ], MAP_ARG, true)

        then:
        verifyResponse(CREATED, response)
        String createdId = response.body().id

        cleanup:
        removeValidIdObject(createdId)

    }


    void "CA01B Test as an Admin"() {
        given: 'putting a catalog item id'
        String terminologyId = getSimpleTerminologyId()

        when: "Admin"
        loginAdmin()
        POST("terminologies/$terminologyId/classifiers", [
            label: 'A test classifier for a terminology'
        ], MAP_ARG, true)

        then:
        verifyResponse(CREATED, response)
        String createdId = response.body().id

        cleanup:
        removeValidIdObject(createdId)

    }


    void "CA02 Test the catalogueItems delete action for classifier"() {

        given: 'putting a catalog item id'
        String terminologyId = getSimpleTerminologyId()

        when: 'making the call not logged in'
        GET("terminologies/$terminologyId/classifiers", MAP_ARG, true)

        then:
        verifyResponse(FORBIDDEN, response)

        when: 'making the call as authenticated'
        loginAuthenticated()
        GET("terminologies/$terminologyId/classifiers", MAP_ARG, true)

        then:
        verifyResponse(FORBIDDEN, response)

        when: 'making the call as a reader'
        loginReader()
        GET("terminologies/$terminologyId/classifiers", MAP_ARG, true)

        then:
        verifyResponse(OK, response)

        when: 'making the call as an editor'
        loginEditor()
        GET("terminologies/$terminologyId/classifiers", MAP_ARG, true)

        then:
        verifyResponse(OK, response)

        when: 'making the call as as admin'
        loginAdmin()
        GET("terminologies/$terminologyId/classifiers", MAP_ARG, true)

        then: 'response should be OK and include the classifier inside the terminology'
        verifyResponse(OK, response)
        assert responseBody().count == 1
        assert responseBody().items.any { it.label == 'test classifier simple' }

    }
}

