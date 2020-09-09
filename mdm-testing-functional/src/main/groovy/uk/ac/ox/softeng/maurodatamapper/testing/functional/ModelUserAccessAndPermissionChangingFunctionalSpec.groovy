/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional


import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndPermissionChangingFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: model
 *
 *  |  GET     | /api/dataModels        | Action: index
 *  |  DELETE  | /api/dataModels/${id}  | Action: delete
 *  |  PUT     | /api/dataModels/${id}  | Action: update
 *  |  GET     | /api/dataModels/${id}  | Action: show
 *  |  POST    | /api/folders/${folderId}/dataModels                 | Action: save
 *
 *  |  DELETE  | /api/dataModels/${dataModelId}/readByAuthenticated    | Action: readByAuthenticated
 *  |  PUT     | /api/dataModels/${dataModelId}/readByAuthenticated    | Action: readByAuthenticated
 *  |  DELETE  | /api/dataModels/${dataModelId}/readByEveryone         | Action: readByEveryone
 *  |  PUT     | /api/dataModels/${dataModelId}/readByEveryone         | Action: readByEveryone
 *
 *  |  GET     | /api/dataModels/types  | Action: types
 *  |  GET     | /api/dataModels/${dataModelId}/hierarchy  | Action: hierarchy
 *  |  PUT     | /api/dataModels/${dataModelId}/finalise   | Action: finalise
 *
 *  |  PUT     | /api/dataModels/${dataModelId}/newForkModel          | Action: newForkModel
 *  |  PUT     | /api/dataModels/${dataModelId}/newDocumentationVersion  | Action: newDocumentationVersion
 *
 *  |  PUT     | /api/folders/${folderId}/dataModels/${dataModelId}      | Action: changeFolder
 *  |  PUT     | /api/dataModels/${dataModelId}/folder/${folderId}       | Action: changeFolder
 *
 *  |  GET     | /api/dataModels/providers/defaultDataTypeProviders       | Action: defaultDataTypeProviders
 *  |  GET     | /api/dataModels/providers/importers                      | Action: importerProviders
 *  |  GET     | /api/dataModels/providers/exporters                      | Action: exporterProviders
 *  |  GET     | /api/dataModels/${dataModelId}/diff/${otherDataModelId}  | Action: diff
 *
 *  |  POST    | /api/dataModels/import/${importerNamespace}/${importerName}/${importerVersion}                 | Action: importDataModels
 *  |  POST    | /api/dataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}                 | Action: exportDataModels
 *  |  GET     | /api/dataModels/${dataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportDataModel
 *
 *  |   GET    | /api/dataModels/${dataModelId}/search  | Action: search
 *  |   POST   | /api/dataModels/${dataModelId}/search  | Action: search
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.controller.ModelController
 */
@Integration
@Slf4j
abstract class ModelUserAccessAndPermissionChangingFunctionalSpec extends UserAccessAndPermissionChangingFunctionalSpec {

    String getValidFinalisedId() {
        String id = getValidId()
        loginEditor()
        PUT("$id/finalise", [:])
        verifyResponse OK, response
        logout()
        id
    }

    abstract String getModelType()

    @Override
    void removeValidIdObjectUsingTransaction(String id) {
        log.info('Removing valid id {} using permanent API call', id)
        loginAdmin()
        DELETE("${id}?permanent=true")
        response.status() in [NO_CONTENT, NOT_FOUND]
    }


    void 'L16 : Test finalising Model (as not logged in)'() {
        given:
        String id = getValidId()

        when: 'not logged in'
        PUT("$id/finalise", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N16 : Test finalising Model (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when: 'authenticated user'
        PUT("$id/finalise", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R16 : Test finalising Model (as reader)'() {
        given:
        String id = getValidId()

        when: 'logged in as reader'
        loginReader()
        PUT("$id/finalise", ["version": "3.9.0"])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E16 : Test finalising Model (as editor)'() {
        given:
        String id = getValidId()

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/finalise", [:])

        then:
        verifyResponse OK, response
        responseBody().finalised == true
        responseBody().dateFinalised
        responseBody().availableActions == [
            "show",
            "comment",
            "softDelete",
            "delete"
        ]
        responseBody().modelVersion == '1.0.0'

        when: 'log out and log back in again in as editor available actions are correct'
        logout()
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == [
            "show",
            "comment",
            "softDelete",
            "delete"
        ]

        when: 'log out and log back in again in as admin available actions are correct'
        logout()
        loginAdmin()
        GET(id)

        then:
        verifyResponse OK, response
        responseBody().availableActions == [
            "show",
            "comment",
            "softDelete",
            "delete"
        ]

        cleanup:
        removeValidIdObject(id)
    }

    void 'L17 : test creating a new fork model of a Model<T> (as not logged in)'() {
        given:
        String id = getValidFinalisedId()

        when: 'not logged in'
        PUT("$id/newForkModel", [label: "Functional Test ${modelType} v2" as String])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N17 : test creating a new fork model of a Model<T> (as authenticated/no access)'() {
        given:
        String id = getValidFinalisedId()

        when:
        loginAuthenticated()
        PUT("$id/newForkModel", [label: "Functional Test ${modelType} v2" as String])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R17 : test creating a new fork model of a Model<T> (as reader)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as reader'
        loginReader()
        PUT("$id/newForkModel", [label: "Functional Test ${modelType} v2" as String])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == "Functional Test ${modelType} v2"

        when:
        String forkId = responseBody().id
        GET("$forkId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_FORK_OF.label
        responseBody().items.first().sourceModel.id == forkId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        cleanup:
        removeValidIdObjectUsingTransaction(forkId)
        removeValidIdObjectUsingTransaction(id)
        cleanUpRoles(forkId)
        cleanUpRoles(id)
    }

    void 'E17 : test creating a new fork model of a Model<T> (as editor)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as writer'
        loginEditor()
        PUT("$id/newForkModel", [label: "Functional Test ${modelType} v2" as String])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == "Functional Test ${modelType} v2"

        when:
        String forkId = responseBody().id
        GET("$forkId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_FORK_OF.label
        responseBody().items.first().sourceModel.id == forkId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        cleanup:
        removeValidIdObjectUsingTransaction(forkId)
        removeValidIdObjectUsingTransaction(id)
        cleanUpRoles(forkId)
        cleanUpRoles(id)
    }

    void 'L18 : test creating a new documentation version of a Model<T> (as not logged in)'() {
        given:
        String id = getValidFinalisedId()

        when: 'not logged in'
        PUT("$id/newDocumentationVersion", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N18 : test creating a new documentation version of a Model<T> (as authenticated/no access)'() {
        given:
        String id = getValidFinalisedId()

        when:
        loginAuthenticated()
        PUT("$id/newDocumentationVersion", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R18 : test creating a new documentation version of a Model<T> (as reader)'() {
        given:
        String id = getValidFinalisedId()

        when:
        loginReader()
        PUT("$id/newDocumentationVersion", [:])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E18 : test creating a new documentation version of a Model<T> (as editor)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newDocumentationVersion", [:])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '2.0.0'

        when:
        String docId = responseBody().id
        GET("$docId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF.label
        responseBody().items.first().sourceModel.id == docId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(docId)
        cleanUpRoles(id)
        cleanUpRoles(docId)
    }

    void 'L19 : test creating a new branch model version of a Model<T> (as not logged in)'() {
        given:
        String id = getValidFinalisedId()

        when: 'not logged in'
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N19 : test creating a new branch model version of a Model<T> (as authenticated/no access)'() {
        given:
        String id = getValidFinalisedId()

        when:
        loginAuthenticated()
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R19 : test creating a new branch model version of a Model<T> (as reader)'() {
        given:
        String id = getValidFinalisedId()

        when:
        loginReader()
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E19a : test creating a new model version of a Model<T> (no branch name) (as editor)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '1.0.0'
        responseBody().branchName == 'main'
        !responseBody().modelVersion

        when:
        String branchId = responseBody().id
        GET("$branchId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label
        responseBody().items.first().sourceModel.id == branchId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(branchId)
        cleanUpRoles(id)
        cleanUpRoles(branchId)
    }

    void 'E19b : test creating a new branch model version of a Model<T> (as editor)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: 'newBranchModelVersion'])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '1.0.0'
        responseBody().branchName == 'newBranchModelVersion'
        !responseBody().modelVersion

        when:
        String branchId = responseBody().id
        GET("$branchId/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'VersionLink'
        responseBody().items.first().linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label
        responseBody().items.first().sourceModel.id == branchId
        responseBody().items.first().targetModel.id == id
        responseBody().items.first().sourceModel.domainType == responseBody().items.first().targetModel.domainType

        when:
        GET('')

        then:
        verifyResponse OK, response
        responseBody().count >= 3

        when:
        String mainBranchId = responseBody().items.find {
            it.label == validJson.label &&
            !(it.id in [branchId, id])
        }?.id

        then:
        mainBranchId

        when:
        GET(mainBranchId)

        then:
        verifyResponse OK, response
        responseBody().branchName == 'main'
        !responseBody().modelVersion

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(branchId)
        removeValidIdObjectUsingTransaction(mainBranchId)
        cleanUpRoles(id)
        cleanUpRoles(branchId)
        cleanUpRoles(mainBranchId)
    }

    void 'E19c : test creating a new model version of a Model<T> and finalising (as editor)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response

        when:
        String branchId = responseBody().id
        PUT("$branchId/finalise", [:])

        then:
        verifyResponse OK, response
        responseBody().finalised == true
        responseBody().dateFinalised
        responseBody().availableActions == [
            "show",
            "comment",
            "softDelete",
            "delete"
        ]
        responseBody().modelVersion == '2.0.0'

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(branchId)
        cleanUpRoles(id)
        cleanUpRoles(branchId)
    }

    void 'E19d : test creating a new branch model version of a Model<T> and trying to finalise(as editor)'() {
        given:
        String id = getValidFinalisedId()

        when: 'logged in as editor'
        loginEditor()
        PUT("$id/newBranchModelVersion", [branchName: 'newBranchModelVersion'])

        then:
        verifyResponse CREATED, response
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().documentationVersion == '1.0.0'
        responseBody().branchName == 'newBranchModelVersion'
        !responseBody().modelVersion

        when:
        String branchId = responseBody().id
        PUT("$branchId/finalise", [:])

        then:
        verifyForbidden response

        when:
        GET('')

        then:
        verifyResponse OK, response
        responseBody().count >= 3

        when:
        String mainBranchId = responseBody().items.find {
            it.label == validJson.label &&
            !(it.id in [branchId, id])
        }?.id

        then:
        mainBranchId

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObjectUsingTransaction(branchId)
        removeValidIdObjectUsingTransaction(mainBranchId)
        cleanUpRoles(id)
        cleanUpRoles(branchId)
        cleanUpRoles(mainBranchId)
    }
}