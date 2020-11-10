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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValueService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class ReferenceEnumerationTypeService extends ModelItemService<ReferenceEnumerationType> {

    ReferenceEnumerationValueService referenceEnumerationValueService

    @Override
    ReferenceEnumerationType get(Serializable id) {
        ReferenceEnumerationType.get(id)
    }

    Long count() {
        ReferenceEnumerationType.count()
    }

    @Override
    List<ReferenceEnumerationType> list(Map args) {
        ReferenceEnumerationType.list(args)
    }

    @Override
    List<ReferenceEnumerationType> getAll(Collection<UUID> ids) {
        ReferenceEnumerationType.getAll(ids).findAll()
    }

    @Override
    void deleteAll(Collection<ReferenceEnumerationType> catalogueItems) {
        ReferenceEnumerationType.deleteAll(catalogueItems)
    }

    @Override
    void delete(ReferenceEnumerationType enumerationType) {
        delete(enumerationType, true)
    }

    void delete(ReferenceEnumerationType enumerationType, boolean flush) {
        enumerationType.delete(flush: flush)
    }

    @Override
    boolean hasTreeTypeModelItems(ReferenceEnumerationType catalogueItem) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(ReferenceEnumerationType catalogueItem) {
        []
    }

    @Override
    ReferenceEnumerationType findByIdJoinClassifiers(UUID id) {
        ReferenceEnumerationType.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        ReferenceEnumerationType.byClassifierId(ReferenceEnumerationType, classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<ReferenceEnumerationType> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        ReferenceEnumerationType.byClassifierId(ReferenceEnumerationType, classifier.id).list().findAll {
            userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)
        }
    }

    @Override
    Class<ReferenceEnumerationType> getModelItemClass() {
        ReferenceEnumerationType
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == ReferenceEnumerationType.simpleName
    }

    @Override
    List<ReferenceEnumerationType> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                              String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        List<ReferenceEnumerationType> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = ReferenceEnumerationType.luceneLabelSearch(ReferenceEnumerationType, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    @Override
    ReferenceEnumerationType updateFacetsAfterInsertingCatalogueItem(ReferenceEnumerationType enumerationType) {
        super.updateFacetsAfterInsertingCatalogueItem(enumerationType) as ReferenceEnumerationType
        enumerationType.referenceEnumerationValues.each {
            referenceEnumerationValueService.updateFacetsAfterInsertingCatalogueItem(it)
        }
        enumerationType
    }

    @Override
    ReferenceEnumerationType checkFacetsAfterImportingCatalogueItem(ReferenceEnumerationType enumerationType) {
        enumerationType = super.checkFacetsAfterImportingCatalogueItem(enumerationType) as ReferenceEnumerationType
        enumerationType.referenceEnumerationValues.each {
            referenceEnumerationValueService.checkFacetsAfterImportingCatalogueItem(it)
        }
        enumerationType
    }

    private ReferenceEnumerationType addEnumerationValueToEnumerationType(ReferenceEnumerationType enumerationType, String key, String value, User createdBy) {
        if (key)
            enumerationType.addToReferenceEnumerationValues(key: key, value: value ?: key, createdBy: createdBy.emailAddress)
        enumerationType
    }

    private ReferenceEnumerationType addEnumerationValueToEnumerationType(ReferenceEnumerationType enumerationType, String key, String value, String category,
                                                                          User createdBy) {
        if (key)
            enumerationType.addToReferenceEnumerationValues(key: key, value: value ?: key, category: category, createdBy: createdBy.emailAddress)
        enumerationType
    }

    private ReferenceEnumerationType createDataType(String label, String description, User createdBy) {
        new ReferenceEnumerationType(label: label, description: description, createdBy: createdBy.emailAddress)
    }

    private ReferenceEnumerationType findOrCreateDataTypeForDataModel(ReferenceDataModel referenceDataModel, String label, String description, User createdBy) {
        String cleanLabel = label.trim()
        ReferenceEnumerationType enumerationType = referenceDataModel.findDataTypeByLabelAndType(cleanLabel, ReferenceDataType.ENUMERATION_DOMAIN_TYPE) as ReferenceEnumerationType
        if (!enumerationType) {
            enumerationType = createDataType(cleanLabel, description, createdBy)
            referenceDataModel.addToReferenceDataTypes(enumerationType)
        }
        enumerationType
    }
}