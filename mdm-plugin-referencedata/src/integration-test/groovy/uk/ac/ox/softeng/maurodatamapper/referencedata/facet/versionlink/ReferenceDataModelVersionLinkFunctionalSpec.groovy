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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet.versionlink


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.ModelVersionLinkFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import spock.lang.Shared

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkController
 */
@Integration
@Slf4j
class ReferenceDataModelVersionLinkFunctionalSpec extends ModelVersionLinkFunctionalSpec {

    @Shared
    ReferenceDataModel referenceDataModel
    @Shared
    ReferenceDataModel otherReferenceDataModel
    @Shared
    ReferenceDataElement referenceDataElement
    @Shared
    ReferenceDataType referenceDataType

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        referenceDataModel = new ReferenceDataModel(label: 'Functional Test ReferenceDataModel', createdBy: 'functionalTest@test.com',
                                  folder: folder, authority: testAuthority).save(flush: true)
        otherReferenceDataModel = new ReferenceDataModel(label: 'Functional Test ReferenceDataModel 2', createdBy: 'functionalTest@test.com',
                                       folder: folder, authority: testAuthority).save(flush: true)
        referenceDataType = new ReferencePrimitiveType(label: 'string', createdBy: 'functionalTest@test.com',
                                     referenceDataModel: referenceDataModel).save(flush: true)
        referenceDataElement = new ReferenceDataElement(label: 'Functional Test ReferenceDataElement', createdBy: 'functionalTest@test.com',
                                      referenceDataModel: referenceDataModel, referenceDataType: referenceDataType).save(flush: true)
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec PluginCatalogueItemFunctionalSpec')
        cleanUpResources(ReferenceDataModel, Folder, ReferenceDataElement, ReferenceDataType)
    }

    @Override
    UUID getCatalogueItemId() {
        referenceDataModel.id
    }

    @Override
    String getCatalogueItemDomainResourcePath() {
        'referenceDataModels'
    }

    @Override
    String getTargetModelId() {
        otherReferenceDataModel.id.toString()
    }

    @Override
    String getTargetModelDomainType() {
        'ReferenceDataModel'
    }

    @Override
    String getModelDomainType() {
        'ReferenceDataModel'
    }

    @Override
    String getSourceModelJsonString() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "ReferenceDataModel",
    "label": "Functional Test ReferenceDataModel"
  }'''
    }

    @Override
    String getTargetModelJsonString() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "ReferenceDataModel",
    "label": "Functional Test ReferenceDataModel 2"
  }'''
    }
}