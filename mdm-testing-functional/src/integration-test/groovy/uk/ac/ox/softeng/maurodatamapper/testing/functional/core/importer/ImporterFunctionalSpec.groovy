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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.importer


import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.JsonImporterService
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: importer
 *  |  GET     | /api/importer/parameters/${ns}?/${name}?/${version}?  | Action: parameters
 * </pre>
 */
@Integration
@Slf4j
class ImporterFunctionalSpec extends FunctionalSpec {

    JsonImporterService jsonImporterService

    @Override
    String getResourcePath() {
        'importer'
    }

    void 'test importer parameters'() {
        given:
        String endpoint = "parameters/${jsonImporterService.class.packageName}/${jsonImporterService.class.simpleName}/${jsonImporterService.version}"
        when: 'Unlogged in call to check'
        GET(endpoint)

        then:
        verifyForbidden response

        when: 'logged in as user'
        loginAuthenticated()
        GET(endpoint, STRING_ARG)

        then:
        verifyJsonResponse OK, getExpectedJson()
    }

    String getExpectedJson() {
        '''{
            "importer": {
                "name": "JsonImporterService",
                "version": "2.0",
                "displayName": "JSON DataModel Importer",
                "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer",
                "allowsExtraMetadataKeys": true,
                "knownMetadataKeys": [],
                "providerType": "DataModelImporter",
                "paramClassType": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter
                .DataModelFileImporterProviderServiceParameters",
                "canImportMultipleDomains": false
            },
            "parameterGroups": [
                {
                    "name": "DataModel",
                    "parameters": [
                    {
                        "name": "folderId",
                        "type": "Folder",
                        "optional": false,
                        "displayName": "Folder",
                        "description": "The folder into which the DataModel/s should be imported."
                    },
                    {
                        "name": "dataModelName",
                        "type": "String",
                        "optional": true,
                        "displayName": "DataModel name",
                        "description": "Label of DataModel, this will override any existing name provided in the imported data.\\n''' +
        '''Note that if importing multiple models this will be ignored."
        },
        {
          "name": "finalised",
          "type": "Boolean",
          "optional": false,
          "displayName": "Finalised",
          "description": "Whether the new model is to be marked as finalised.\\n''' +
        '''Note that if the model is already finalised this will not be overridden."
        },
        {
          "name": "importAsNewDocumentationVersion",
          "type": "Boolean",
          "optional": false,
          "displayName": "Import as New Documentation Version",
          "description": "Should the DataModel/s be imported as new Documentation Version/s.\\n''' +
        '''If selected then any models with the same name will be superseded and the imported models will be given the''' +
        ''' latest documentation version of the existing DataModels.\\n''' +
        '''If not selected then the \'DataModel Name\' field should be used to ensure the imported DataModel is uniquely''' +
        ''' named, otherwise you could get an error."
        }
      ]
    },
    {
      "name": "Source",
      "parameters": [
        {
          "name": "importFile",
          "type": "File",
          "optional": false,
          "displayName": "File",
          "description": "The file containing the data to be imported"
        }
      ]
    }
  ]
}'''
    }
}