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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationService
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFileService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.RuleService
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.hibernate.SessionFactory

@Slf4j
abstract class CatalogueItemService<K extends CatalogueItem> implements MdmDomainService<K>, MultiFacetAwareService<K> {

    SessionFactory sessionFactory
    ClassifierService classifierService
    MetadataService metadataService
    RuleService ruleService
    SemanticLinkService semanticLinkService
    AnnotationService annotationService
    ReferenceFileService referenceFileService

    Class<K> getMultiFacetAwareClass() {
        getDomainClass()
    }

    abstract void deleteAll(Collection<K> catalogueItems)

    /**
     * Use domain.getAll(ids) to retrieve objects from the database.
     *
     * Make sure you use findAll() on the output of this, its possible to get ids which dont exist in this domain and the Grails implementation
     * of getAll(ids) will return a list of null elements
     * @param ids
     * @return
     */
    abstract List<K> getAll(Collection<UUID> ids)

    abstract K findByIdJoinClassifiers(UUID id)

    abstract void removeAllFromClassifier(Classifier classifier)

    abstract List<K> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier)

    abstract List<K> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                String searchTerm, String domainType)

    abstract Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType)

    @Override
    K save(Map args, K catalogueItem) {
        // If inserting then we will need to update all the facets with the CIs "id" after insert
        // If updating then we dont need to do this as the ID has already been done
        boolean inserting = !(catalogueItem as GormEntity).ident() ?: args.insert
        Map saveArgs = new HashMap(args)
        if (args.flush) {
            saveArgs.remove('flush')
            (catalogueItem as GormEntity).save(saveArgs)
            if (inserting) updateFacetsAfterInsertingCatalogueItem(catalogueItem)
            // We do need to ensure the BT hasnt changed (e.g. a move of a MI inside an M)
            checkBreadcrumbTreeAfterSavingCatalogueItem(catalogueItem)
            sessionFactory.currentSession.flush()
        } else {
            (catalogueItem as GormEntity).save(args)
            if (inserting) updateFacetsAfterInsertingCatalogueItem(catalogueItem)
            // We do need to ensure the BT hasnt changed (e.g. a move of a MI inside an M)
            checkBreadcrumbTreeAfterSavingCatalogueItem(catalogueItem)
        }
        catalogueItem
    }

    boolean hasTreeTypeModelItems(K catalogueItem) {
        hasTreeTypeModelItems(catalogueItem, false)
    }

    boolean hasTreeTypeModelItems(K catalogueItem, boolean fullTreeRender) {
        false
    }

    List<ModelItem> findAllTreeTypeModelItemsIn(K catalogueItem) {
        findAllTreeTypeModelItemsIn(catalogueItem, false)
    }

    List<ModelItem> findAllTreeTypeModelItemsIn(K catalogueItem, boolean fullTreeRender) {
        []
    }

    void addClassifierToCatalogueItem(UUID catalogueItemId, Classifier classifier) {
        get(catalogueItemId).addToClassifiers(classifier)
    }

    void removeClassifierFromCatalogueItem(UUID catalogueItemId, Classifier classifier) {
        removeFacetFromDomain(catalogueItemId, classifier.id, 'classifiers')
    }

    K copyCatalogueItemInformation(K original, K copy, User copier, UserSecurityPolicyManager userSecurityPolicyManager,
                                   CopyInformation copyInformation = null) {
        copy.createdBy = copier.emailAddress
        copy.description = original.description

        // Allow copying with a new label
        if (copyInformation && copyInformation.validate()) {
            copy.label = copyInformation.copyLabel
        } else {
            copy.label = original.label
        }

        classifierService.findAllByCatalogueItemId(userSecurityPolicyManager, original.id).each {copy.addToClassifiers(it)}
        metadataService.findAllByMultiFacetAwareItemId(original.id).each {copy.addToMetadata(it.namespace, it.key, it.value, copier.emailAddress)}
        ruleService.findAllByMultiFacetAwareItemId(original.id).each {rule ->
            Rule copiedRule = new Rule(name: rule.name, description: rule.description, createdBy: copier.emailAddress)
            rule.ruleRepresentations.each {ruleRepresentation ->
                copiedRule.addToRuleRepresentations(language: ruleRepresentation.language,
                                                    representation: ruleRepresentation.representation,
                                                    createdBy: copier.emailAddress)
            }
            copy.addToRules(copiedRule)
        }

        semanticLinkService.findAllBySourceMultiFacetAwareItemId(original.id).each {link ->
            copy.addToSemanticLinks(createdBy: copier.emailAddress, linkType: link.linkType,
                                    targetMultiFacetAwareItemId: link.targetMultiFacetAwareItemId,
                                    targetMultiFacetAwareItemDomainType: link.targetMultiFacetAwareItemDomainType,
                                    unconfirmed: true)
        }

        copy
    }

    void setCatalogueItemRefinesCatalogueItem(CatalogueItem source, CatalogueItem target, User catalogueUser) {
        source.addToSemanticLinks(linkType: SemanticLinkType.REFINES, createdBy: catalogueUser.emailAddress, targetMultiFacetAwareItem: target)
    }

    void checkBreadcrumbTreeAfterSavingCatalogueItem(K catalogueItem) {
        catalogueItem.breadcrumbTree?.trackChanges()
        catalogueItem.breadcrumbTree?.beforeValidate()
        catalogueItem.breadcrumbTree?.save(validate: false)
    }

    K updateFacetsAfterInsertingCatalogueItem(K catalogueItem) {
        updateFacetsAfterInsertingMultiFacetAware(catalogueItem)
    }

    K checkFacetsAfterImportingCatalogueItem(K catalogueItem) {
        checkFacetsAfterImportingMultiFacetAware(catalogueItem)
        catalogueItem
    }

    /*
     * Find a CatalogueItem which is labeled with label and whose parent is parentCatalogueItem.
     * @param parentCatalogueItem The CatalogueItem which is the parent of the CatalogueItem being sought
     * @param label The label of the CatalogueItem being sought
     */

    K findByParentAndLabel(CatalogueItem parentCatalogueItem, String label) {
        findByParentIdAndLabel(parentCatalogueItem.id, label)
    }

    K findByParentIdAndLabel(UUID parentId, String label) {
        null
    }

    @Override
    K findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier) {
        findByParentIdAndLabel(parentId, pathIdentifier)
    }
}