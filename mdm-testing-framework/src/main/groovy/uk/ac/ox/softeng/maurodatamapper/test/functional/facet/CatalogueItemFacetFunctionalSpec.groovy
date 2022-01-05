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
package uk.ac.ox.softeng.maurodatamapper.test.functional.facet


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

@Slf4j
abstract class CatalogueItemFacetFunctionalSpec<D extends GormEntity> extends ResourceFunctionalSpec<D> {

    @Shared
    Folder folder

    abstract UUID getCatalogueItemId()

    abstract String getCatalogueItemDomainResourcePath()

    abstract String getFacetResourcePath()

    @Override
    String getResourcePath() {
        "${getCatalogueItemDomainResourcePath()}/${getCatalogueItemId()}/${getFacetResourcePath()}"
    }

    @Transactional
    def setupSpec() {
        log.debug('Check and setup folder')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec CatalogueItemFacetFunctionalSpec')
        cleanUpResources(Folder)
    }

    String getCopyResourcePath(String copyId) {
        "${catalogueItemDomainResourcePath}/${copyId}/${facetResourcePath}"
    }
}