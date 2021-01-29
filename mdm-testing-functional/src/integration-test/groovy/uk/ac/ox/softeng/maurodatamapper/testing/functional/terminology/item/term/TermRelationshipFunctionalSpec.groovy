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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.terminology.item.term

import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

import java.util.regex.Pattern

/**
 * <pre>
 * Controller: termRelationship
 *  |   POST   | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships  | Action: save
 *  |   GET    | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships  | Action: index
 *  |  DELETE  | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships/${id}  | Action: delete
 *  |   PUT    | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships/${id}  | Action: update
 *  |   GET    | /api/terminologies/${terminologyId}/terms/${termId}/termRelationships/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationshipController
 */
@Integration
@Slf4j
class TermRelationshipFunctionalSpec extends UserAccessFunctionalSpec {

    @Override
    String getResourcePath() {
        "terminologies/${getComplexTerminologyId()}/terms/${getTopTermId()}/termRelationships"
    }

    @Override
    String getEditsPath() {
        'termRelationships'
    }

    @Transactional
    String getComplexTerminologyId() {
        Terminology.findByLabel(BootstrapModels.COMPLEX_TERMINOLOGY_NAME).id.toString()
    }

    @Transactional
    String getSimpleTerminologyId() {
        Terminology.findByLabel(BootstrapModels.SIMPLE_TERMINOLOGY_NAME).id.toString()
    }

    @Transactional
    String getTopTermId() {
        Term.byTerminologyIdAndCode(Utils.toUuid(getComplexTerminologyId()), 'CTT00').get().id.toString()
    }

    @Transactional
    String getOtherTermId() {
        Term.byTerminologyIdAndCode(Utils.toUuid(getComplexTerminologyId()), 'CTT100').get().id.toString()
    }

    @Transactional
    String getRelationshipTypeId() {
        TermRelationshipType.byTerminologyIdAndLabel(Utils.toUuid(getComplexTerminologyId()), 'broaderThan').get().id.toString()
    }

    @Transactional
    String getOtherRelationshipTypeId() {
        TermRelationshipType.byTerminologyIdAndLabel(Utils.toUuid(getComplexTerminologyId()), 'narrowerThan').get().id.toString()
    }

    @Override
    Map getValidJson() {
        [
            relationshipType: getRelationshipTypeId(),
            sourceTerm      : getTopTermId(),
            targetTerm      : getOtherTermId()
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            relationshipType: getRelationshipTypeId(),
            sourceTerm      : getTopTermId(),
            targetTerm      : getTopTermId()
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            relationshipType: [id: getOtherRelationshipTypeId()],
        ]
    }

    Boolean readerPermissionIsInherited() {
        true
    }

    @Override
    void verifyL03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexTerminologyId()
    }

    @Override
    void verifyL03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexTerminologyId()
    }

    @Override
    void verifyL03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexTerminologyId()
    }

    @Override
    void verifyN03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexTerminologyId()
    }

    @Override
    void verifyN03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexTerminologyId()
    }

    @Override
    void verifyN03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexTerminologyId()
    }

    @Override
    void verifyR04UnknownIdResponse(HttpResponse<Map> response, String id) {
        verifyForbidden response
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[\w+:.+?] changed properties \[relationshipType, label]/
    }

    @Override
    void verifySameValidDataCreationResponse() {
        verifyResponse HttpStatus.CREATED, response
    }

    void verifyE03ValidResponseBody(HttpResponse<Map> response) {
        assert response.body().id
        assert response.body().relationshipType.id == getRelationshipTypeId()
        assert response.body().sourceTerm.id == getTopTermId()
        assert response.body().targetTerm.id == getOtherTermId()
        assert response.body().label == 'broaderThan'
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "TermRelationship",
  "label": "broaderThan",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Complex Test Terminology",
      "domainType": "Terminology",
      "finalised": false
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "CTT00: Complex Test Term 00",
      "domainType": "Term"
    }
  ],
  "availableActions": [
    "delete",
    "update",
    "save",
    "show",
    "comment",
    "editDescription"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "relationshipType": {
    "id": "${json-unit.matches:id}",
    "domainType": "TermRelationshipType",
    "label": "broaderThan",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Complex Test Terminology",
        "domainType": "Terminology",
        "finalised": false
      }
    ],
    "displayLabel": "Broader Than"
  },
  "sourceTerm": {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT00: Complex Test Term 00",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Complex Test Terminology",
        "domainType": "Terminology",
        "finalised": false
      }
    ],
    "description": "This is a very important description",
    "code": "CTT00",
    "definition": "Complex Test Term 00",
    "url": "https://google.co.uk"
  },
  "targetTerm": {
    "id": "${json-unit.matches:id}",
    "domainType": "Term",
    "label": "CTT100: Complex Test Term 100",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Complex Test Terminology",
        "domainType": "Terminology",
        "finalised": false
      }
    ],
    "code": "CTT100",
    "definition": "Complex Test Term 100"
  }
}'''
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 19,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "is-a-part-of",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT1: Complex Test Term 1",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
        "id": "${json-unit.matches:id}",
        "domainType": "TermRelationshipType",
        "label": "is-a-part-of",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "displayLabel": "Is A Part Of"
      },
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT1: Complex Test Term 1",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT1",
        "definition": "Complex Test Term 1"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT00: Complex Test Term 00",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "description": "This is a very important description",
        "code": "CTT00",
        "definition": "Complex Test Term 00",
        "url": "https://google.co.uk"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "is-a-part-of",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT2: Complex Test Term 2",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
        "id": "${json-unit.matches:id}",
        "domainType": "TermRelationshipType",
        "label": "is-a-part-of",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "displayLabel": "Is A Part Of"
      },
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT2: Complex Test Term 2",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT2",
        "definition": "Complex Test Term 2"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT00: Complex Test Term 00",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "description": "This is a very important description",
        "code": "CTT00",
        "definition": "Complex Test Term 00",
        "url": "https://google.co.uk"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "is-a-part-of",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT3: Complex Test Term 3",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
        "id": "${json-unit.matches:id}",
        "domainType": "TermRelationshipType",
        "label": "is-a-part-of",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "displayLabel": "Is A Part Of"
      },
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT3: Complex Test Term 3",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT3",
        "definition": "Complex Test Term 3"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT00: Complex Test Term 00",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "description": "This is a very important description",
        "code": "CTT00",
        "definition": "Complex Test Term 00",
        "url": "https://google.co.uk"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "is-a-part-of",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT4: Complex Test Term 4",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
        "id": "${json-unit.matches:id}",
        "domainType": "TermRelationshipType",
        "label": "is-a-part-of",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "displayLabel": "Is A Part Of"
      },
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT4: Complex Test Term 4",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT4",
        "definition": "Complex Test Term 4"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT00: Complex Test Term 00",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "description": "This is a very important description",
        "code": "CTT00",
        "definition": "Complex Test Term 00",
        "url": "https://google.co.uk"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "is-a-part-of",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT5: Complex Test Term 5",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
        "id": "${json-unit.matches:id}",
        "domainType": "TermRelationshipType",
        "label": "is-a-part-of",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "displayLabel": "Is A Part Of"
      },
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT5: Complex Test Term 5",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT5",
        "definition": "Complex Test Term 5"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT00: Complex Test Term 00",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "description": "This is a very important description",
        "code": "CTT00",
        "definition": "Complex Test Term 00",
        "url": "https://google.co.uk"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "is-a-part-of",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT6: Complex Test Term 6",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
        "id": "${json-unit.matches:id}",
        "domainType": "TermRelationshipType",
        "label": "is-a-part-of",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "displayLabel": "Is A Part Of"
      },
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT6: Complex Test Term 6",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT6",
        "definition": "Complex Test Term 6"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT00: Complex Test Term 00",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "description": "This is a very important description",
        "code": "CTT00",
        "definition": "Complex Test Term 00",
        "url": "https://google.co.uk"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "is-a-part-of",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT7: Complex Test Term 7",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
        "id": "${json-unit.matches:id}",
        "domainType": "TermRelationshipType",
        "label": "is-a-part-of",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "displayLabel": "Is A Part Of"
      },
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT7: Complex Test Term 7",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT7",
        "definition": "Complex Test Term 7"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT00: Complex Test Term 00",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "description": "This is a very important description",
        "code": "CTT00",
        "definition": "Complex Test Term 00",
        "url": "https://google.co.uk"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "is-a-part-of",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT8: Complex Test Term 8",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
        "id": "${json-unit.matches:id}",
        "domainType": "TermRelationshipType",
        "label": "is-a-part-of",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "displayLabel": "Is A Part Of"
      },
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT8: Complex Test Term 8",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT8",
        "definition": "Complex Test Term 8"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT00: Complex Test Term 00",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "description": "This is a very important description",
        "code": "CTT00",
        "definition": "Complex Test Term 00",
        "url": "https://google.co.uk"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "is-a-part-of",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT9: Complex Test Term 9",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
        "id": "${json-unit.matches:id}",
        "domainType": "TermRelationshipType",
        "label": "is-a-part-of",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "displayLabel": "Is A Part Of"
      },
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT9: Complex Test Term 9",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT9",
        "definition": "Complex Test Term 9"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT00: Complex Test Term 00",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "description": "This is a very important description",
        "code": "CTT00",
        "definition": "Complex Test Term 00",
        "url": "https://google.co.uk"
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "TermRelationship",
      "label": "is-a-part-of",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test Terminology",
          "domainType": "Terminology",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "CTT10: Complex Test Term 10",
          "domainType": "Term"
        }
      ],
      "relationshipType": {
        "id": "${json-unit.matches:id}",
        "domainType": "TermRelationshipType",
        "label": "is-a-part-of",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "displayLabel": "Is A Part Of"
      },
      "sourceTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT10: Complex Test Term 10",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "code": "CTT10",
        "definition": "Complex Test Term 10"
      },
      "targetTerm": {
        "id": "${json-unit.matches:id}",
        "domainType": "Term",
        "label": "CTT00: Complex Test Term 00",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "domainType": "Terminology",
            "finalised": false
          }
        ],
        "description": "This is a very important description",
        "code": "CTT00",
        "definition": "Complex Test Term 00",
        "url": "https://google.co.uk"
      }
    }
  ]
}'''
    }
}