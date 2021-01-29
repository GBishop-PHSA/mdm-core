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
package uk.ac.ox.softeng.maurodatamapper.dataflow.component


import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.orm.hibernate.proxy.HibernateProxyHandler

@Slf4j
@Transactional
class DataElementComponentService extends ModelItemService<DataElementComponent> {

    private static HibernateProxyHandler proxyHandler = new HibernateProxyHandler();

    @Override
    DataElementComponent get(Serializable id) {
        DataElementComponent.get(id)
    }

    @Override
    List<DataElementComponent> getAll(Collection<UUID> ids) {
        DataElementComponent.getAll(ids).findAll()
    }


    List<DataElementComponent> list(Map args) {
        DataElementComponent.list(args)
    }

    Long count() {
        DataElementComponent.count()
    }

    void delete(Serializable id) {
        DataElementComponent dataElementComponent = get(id)
        if (dataElementComponent) delete(dataElementComponent)
    }

    void delete(DataElementComponent dataElementComponent, boolean flush = false) {
        if (!dataElementComponent) return

        DataClassComponent dataClassComponent = proxyHandler.unwrapIfProxy(dataElementComponent.dataClassComponent) as DataClassComponent
        dataElementComponent.dataClassComponent = dataClassComponent

        dataElementComponent.sourceDataElements.each { source ->
            dataElementComponent.targetDataElements.each { target ->
                semanticLinkService.deleteBySourceCatalogueItemAndTargetCatalogueItemAndLinkType(source, target, SemanticLinkType.IS_FROM)
            }
        }

        dataElementComponent.breadcrumbTree.removeFromParent()

        dataClassComponent.removeFromDataElementComponents(dataElementComponent)
        dataClassComponent.trackChanges() // Discard any latent changes to the DataClassComponent as we dont want them
        dataElementComponent.delete(flush: flush)
    }

    @Override
    void deleteAll(Collection<DataElementComponent> dataElementComponents) {
        dataElementComponents.each {
            delete(it)
        }
    }

    void deleteAllByModelId(UUID modelId) {

        List<UUID> dataElementComponentIds = DataElementComponent.by().where {
            dataClassComponent {
                dataFlow {
                    or {
                        eq 'source.id', modelId
                        eq 'target.id', modelId
                    }
                }
            }
        }.id().list() as List<UUID>


        if (dataElementComponentIds) {

            log.trace('Removing links to DataElements in {} DataElementComponents', dataElementComponentIds.size())
            sessionFactory.currentSession
                .createSQLQuery('delete from dataflow.join_data_element_component_to_source_data_element where data_element_component_id in :ids')
                .setParameter('ids', dataElementComponentIds)
                .executeUpdate()

            sessionFactory.currentSession
                .createSQLQuery('delete from dataflow.join_data_element_component_to_target_data_element where data_element_component_id in :ids')
                .setParameter('ids', dataElementComponentIds)
                .executeUpdate()

            log.trace('Removing facets for {} DataElementComponents', dataElementComponentIds.size())
            deleteAllFacetsByCatalogueItemIds(dataElementComponentIds,
                                              'delete from dataflow.join_dataelementcomponent_to_facet where dataelementcomponent_id in :ids')

            log.trace('Removing {} DataElementComponents', dataElementComponentIds.size())
            sessionFactory.currentSession
                .createSQLQuery('delete from dataflow.data_element_component where id in :ids')
                .setParameter('ids', dataElementComponentIds)
                .executeUpdate()

            log.trace('DataElementComponents removed')
        }
    }

    @Override
    Class<DataElementComponent> getModelItemClass() {
        DataElementComponent
    }

    @Override
    boolean hasTreeTypeModelItems(DataElementComponent dataElementComponent, boolean forDiff) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(DataElementComponent dataElementComponent, boolean forDiff = false) {
        []
    }

    @Override
    DataElementComponent findByIdJoinClassifiers(UUID id) {
        DataElementComponent.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        DataElementComponent.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<DataElementComponent> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        DataElementComponent.byClassifierId(classifier.id).list().
            findAll { userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id) }
    }

    @Override
    List<DataElementComponent> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                          String searchTerm, String domainType) {
        []
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        false
    }

    @Deprecated(forRemoval = true)
    DataElementComponent findByDataFlowIdAndId(UUID dataFlowId, Serializable id) {
        DataElementComponent.byDataFlowIdAndId(dataFlowId, Utils.toUuid(id)).find()
    }

    DataElementComponent findByDataClassComponentIdAndId(UUID dataClassComponentId, Serializable id) {
        DataElementComponent.byDataClassComponentIdAndId(dataClassComponentId, Utils.toUuid(id)).find()
    }

    @Deprecated(forRemoval = true)
    List<DataElementComponent> findAllByDataFlowId(UUID dataFlowId, Map pagination = [:]) {
        DataElementComponent.byDataFlowId(dataFlowId).list(pagination)
    }

    List<DataElementComponent> findAllByDataClassComponentId(UUID dataClassComponentId, Map pagination = [:]) {
        DataElementComponent.byDataClassComponentId(dataClassComponentId).list(pagination)
    }

    @Deprecated(forRemoval = true)
    List<DataElementComponent> findAllByDataFlowIdAndDataClassId(UUID dataFlowId, UUID dataClassId, Map pagination = [:]) {
        DataElementComponent.byDataFlowIdAndDataClassId(dataFlowId, dataClassId).list(pagination)
    }

    /*
TODO data flow copying
       void moveDataFlowComponentToNewTarget(DataFlowComponent dataFlowComponent, DataModel targetDataModel) {
           Collection<DataElement> elements = []
           elements += dataFlowComponent.targetElements
           elements.each {e ->
               dataFlowComponent.removeFromTargetElements(e)
               dataFlowComponent.addToTargetElements(dataElementService.findDataElementWithSameLabelTree(targetDataModel, e))
           }
       }

       DataFlowComponent copyDataFlowComponentToNewTarget(DataFlowComponent original, DataModel targetDataModel, CatalogueUser copier) {

           DataFlowComponent copy = new DataFlowComponent()
           catalogueItemService.copyCatalogueItemInformation(original, copy, copier)

           original.sourceElements.each {se ->
               copy.addToSourceElements(se)
           }

           original.targetElements.each {te ->
               copy.addToTargetElements(dataElementService.findDataElementWithSameLabelTree(targetDataModel, te))
           }

           copy
       }

    */
}