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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class ReferenceTypeService extends ModelItemService<ReferenceType> {

    @Override
    ReferenceType get(Serializable id) {
        ReferenceType.get(id)
    }

    Long count() {
        ReferenceType.count()
    }

    @Override
    List<ReferenceType> list(Map args) {
        ReferenceType.list(args)
    }

    @Override
    List<ReferenceType> getAll(Collection<UUID> ids) {
        ReferenceType.getAll(ids).findAll()
    }

    @Override
    void deleteAll(Collection<ReferenceType> catalogueItems) {
        ReferenceType.deleteAll(catalogueItems)
    }

    @Override
    void delete(ReferenceType dataType) {
        delete(dataType, true)
    }

    void delete(ReferenceType dataType, boolean flush) {
        dataType.referenceClass.removeFromReferenceTypes(dataType)
        dataType.delete(flush: flush)
    }

    @Override
    boolean hasTreeTypeModelItems(ReferenceType catalogueItem, boolean forDiff) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(ReferenceType catalogueItem, boolean forDiff = false) {
        []
    }

    @Override
    ReferenceType findByIdJoinClassifiers(UUID id) {
        ReferenceType.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        ReferenceType.byClassifierId(ReferenceType, classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<ReferenceType> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        ReferenceType.byClassifierId(ReferenceType, classifier.id).list().findAll {
            userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)
        }
    }

    @Override
    Class<ReferenceType> getModelItemClass() {
        ReferenceType
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == ReferenceType.simpleName
    }

    @Override
    List<ReferenceType> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                   String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        List<ReferenceType> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = ReferenceType.luceneLabelSearch(ReferenceType, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }
        results
    }

    ReferenceType findOrCreateDataTypeForDataModel(DataModel dataModel, String label, String description, User createdBy,
                                                   DataClass referenceClass) {
        findOrCreateDataTypeForDataModel(dataModel, label, description, createdBy.emailAddress, referenceClass)
    }

    ReferenceType findOrCreateDataTypeForDataModel(DataModel dataModel, String label, String description, String createdByEmailAddress,
                                                   DataClass referenceClass) {
        String cleanLabel = label.trim()
        DataType dataType = dataModel.findDataTypeByLabel(cleanLabel)
        if (!dataType) {
            ReferenceType referenceType = new ReferenceType(label: cleanLabel, description: description, createdBy: createdByEmailAddress,
                                                            referenceClass: referenceClass)
            referenceClass.addToReferenceTypes(referenceType)
            dataModel.addToDataTypes(referenceType)
            return referenceType

        }
        if (!(dataType.instanceOf(ReferenceType)))
            return findOrCreateDataTypeForDataModel(dataModel, "Reference${cleanLabel}", description, createdByEmailAddress, referenceClass)

        if (description && dataType.description != description) {
            return findOrCreateDataTypeForDataModel(dataModel, "${cleanLabel}.1", description, createdByEmailAddress, referenceClass)
        }
        dataType as ReferenceType
    }

}
