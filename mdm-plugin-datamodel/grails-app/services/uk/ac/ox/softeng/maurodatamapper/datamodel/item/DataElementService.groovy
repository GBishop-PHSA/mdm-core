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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelImport
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelImportService
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.similarity.SimilarityResult
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.similarity.DataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.apache.lucene.search.Query
import org.hibernate.search.engine.ProjectionConstants
import org.hibernate.search.jpa.FullTextEntityManager
import org.hibernate.search.jpa.FullTextQuery
import org.hibernate.search.jpa.Search
import org.hibernate.search.query.dsl.QueryBuilder

import javax.persistence.EntityManager

@Slf4j
@Transactional
class DataElementService extends ModelItemService<DataElement> {

    DataClassService dataClassService
    DataTypeService dataTypeService
    ModelImportService modelImportService   
    SummaryMetadataService summaryMetadataService

    @Override
    DataElement get(Serializable id) {
        DataElement.get(id)
    }

    Long count() {
        DataElement.count()
    }

    @Override
    List<DataElement> list(Map args) {
        DataElement.list(args)
    }

    @Override
    List<DataElement> getAll(Collection<UUID> ids) {
        DataElement.getAll(ids).findAll()
    }

    @Override
    boolean handlesPathPrefix(String pathPrefix) {
        pathPrefix == "de"
    }

    @Override
    void deleteAll(Collection<DataElement> dataElements) {
        dataElements.each { delete(it) }
    }

    void delete(UUID id) {
        delete(get(id), true)
    }

    void delete(DataElement dataElement, boolean flush = false) {
        if (!dataElement) return
        dataElement.breadcrumbTree.removeFromParent()
        dataElement.dataType = null
        dataElement.dataClass?.removeFromDataElements(dataElement)
        dataElement.delete(flush: flush)
    }

    void deleteAllByModelId(UUID dataModelId) {
        List<UUID> dataElementIds = DataElement.by().where {
            dataClass {
                eq('dataModel.id', dataModelId)
            }
        }.id().list() as List<UUID>

        if (dataElementIds) {
            log.trace('Removing facets for {} DataElements', dataElementIds.size())
            deleteAllFacetsByCatalogueItemIds(dataElementIds,
                                              'delete from datamodel.join_dataelement_to_facet where dataelement_id in :ids')

            log.trace('Removing {} DataElements', dataElementIds.size())
            sessionFactory.currentSession
                .createSQLQuery('DELETE FROM datamodel.data_element WHERE id IN :ids')
                .setParameter('ids', dataElementIds)
                .executeUpdate()
        }
        log.trace('DataElements removed')
    }

    void removeSummaryMetadataFromCatalogueItem(UUID catalogueItemId, SummaryMetadata summaryMetadata) {
        removeFacetFromDomain(catalogueItemId, summaryMetadata.id, 'summaryMetadata')
    }

    @Override
    void deleteAllFacetDataByCatalogueItemIds(List<UUID> catalogueItemIds) {
        super.deleteAllFacetDataByCatalogueItemIds(catalogueItemIds)
        summaryMetadataService.deleteAllByCatalogueItemIds(catalogueItemIds)
    }

    @Override
    DataElement save(Map args, DataElement dataElement) {
        if (!dataElement.dataType.ident()) {
            dataTypeService.save(dataElement.dataType)
        }
        super.save(args, dataElement)
    }

    @Override
    DataElement updateFacetsAfterInsertingCatalogueItem(DataElement catalogueItem) {
        super.updateFacetsAfterInsertingCatalogueItem(catalogueItem)
        if (catalogueItem.summaryMetadata) {
            catalogueItem.summaryMetadata.each {
                if (!it.isDirty('catalogueItemId')) it.trackChanges()
                it.catalogueItemId = catalogueItem.getId()
            }
            SummaryMetadata.saveAll(catalogueItem.summaryMetadata)
        }
        catalogueItem
    }

    @Override
    boolean hasTreeTypeModelItems(DataElement catalogueItem, boolean forDiff, boolean includeImported = false) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(DataElement catalogueItem, boolean forDiff = false, boolean includeImported = false) {
        []
    }

    @Override
    DataElement findByIdJoinClassifiers(UUID id) {
        DataElement.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        DataElement.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<DataElement> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        DataElement.byClassifierId(classifier.id).list().findAll { userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id) }
    }

    @Override
    Class<DataElement> getModelItemClass() {
        DataElement
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == DataElement.simpleName
    }


    @Override
    List<DataElement> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                 String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        List<DataElement> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = DataElement.luceneLabelSearch(DataElement, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    def saveAll(Collection<DataElement> dataElements) {

        List<Classifier> classifiers = dataElements.collectMany { it.classifiers ?: [] } as List<Classifier>
        if (classifiers) {
            log.trace('Saving {} classifiers')
            classifierService.saveAll(classifiers)
        }

        Collection<DataElement> alreadySaved = dataElements.findAll { it.ident() && it.isDirty() }
        Collection<DataElement> notSaved = dataElements.findAll { !it.ident() }

        if (alreadySaved) {
            log.trace('Straight saving {} already saved DataElements', alreadySaved.size())
            DataElement.saveAll(alreadySaved)
        }

        if (notSaved) {
            log.trace('Batch saving {} new DataElements in batches of {}', notSaved.size(), DataElement.BATCH_SIZE)
            List batch = []
            int count = 0

            notSaved.each { de ->

                batch += de
                count++
                if (count % DataElement.BATCH_SIZE == 0) {
                    batchSave(batch)
                    batch.clear()
                }
            }
            batchSave(batch)
            batch.clear()
        }
    }

    void batchSave(List<DataElement> dataElements) {
        long start = System.currentTimeMillis()
        log.trace('Performing batch save of {} DataElements', dataElements.size())

        DataElement.saveAll(dataElements)
        dataElements.each { updateFacetsAfterInsertingCatalogueItem(it) }

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        log.trace('Batch save took {}', Utils.getTimeString(System.currentTimeMillis() - start))
    }

    void matchUpDataTypes(DataModel dataModel, Collection<DataElement> dataElements) {
        if (dataElements) {
            log.debug("Matching up {} DataElements to a possible {} DataTypes", dataElements.size(), dataModel.dataTypes.size())
            def grouped = dataElements.groupBy { it.dataType.label }.sort { a, b ->
                def res = a.value.size() <=> b.value.size()
                if (res == 0) res = a.key <=> b.key
                res
            }
            log.debug('Grouped {} DataElements by DataType label', grouped.size())
            grouped.each { label, elements ->
                log.trace('Matching {} elements to DataType label {}', elements.size(), label)
                DataType dataType = dataModel.findDataTypeByLabel(label)

                if (!dataType) {
                    log.debug('No DataType for {} in DataModel, using first DataElement DataType as base', label)
                    DataElement dataElement = elements.first()
                    dataType = dataElement.dataType
                    dataType.createdBy = dataElement.createdBy
                    dataModel.addToDataTypes(dataType)
                }
                elements.each { dataType.addToDataElements(it) }
            }
        }
    }

    DataElement findByDataClassIdAndId(Serializable dataClassId, Serializable id) {
        DataElement.byDataClassIdAndId(dataClassId, id).find()
    }

    DataElement findByDataTypeIdAndId(Serializable dataTypeId, Serializable id) {
        DataElement.byDataTypeIdAndId(dataTypeId, id).find()
    }

    List<DataElement> findAllByDataClassId(Serializable dataClassId, Map pagination = [:], boolean includeImported = false, boolean includeExtends = false) {
        findAllByDataClassId(dataClassId, pagination, pagination, includeImported, includeExtends)
    }

    List<DataElement> findAllByDataClassId(Serializable dataClassId, Map filter, Map pagination, boolean includeImported = false, boolean includeExtends = false) {
        DataElement.withFilter(DataElement.byDataClassId(dataClassId, includeImported, includeExtends), filter).list(pagination)
    }

    List<DataElement> findAllByDataClassIdJoinDataType(Serializable dataClassId) {
        DataElement.byDataClassId(dataClassId).join('dataType').sort('label').list()
    }

    List<DataElement> findAllByDataTypeId(Serializable dataTypeId, Map pagination = [:]) {
        DataElement.withFilter(DataElement.byDataTypeId(dataTypeId), pagination).list(pagination)
    }

    List<DataElement> findAllByDataType(DataType dataType) {
        DataElement.byDataType(dataType).list()
    }

    List<DataElement> findAllByDataClass(DataClass dataClass) {
        DataElement.byDataClass(dataClass).list()
    }

    List<DataElement> findAllByDataModelId(Serializable dataModelId, Map pagination = [:]) {
        DataElement.byDataModelId(dataModelId).list(pagination)
    }

    List<DataElement> findAllByDataModelIdAndLabelIlike(Serializable dataModelId, String labelSearch, Map pagination = [:]) {
        DataElement.byDataModelIdAndLabelIlike(dataModelId, labelSearch).list(pagination)
    }

    Number countByDataClassId(Serializable dataClassId) {
        DataElement.byDataClassId(dataClassId).count()
    }

    Number countByDataTypeId(Serializable dataTypeId) {
        DataElement.byDataTypeId(dataTypeId).count()
    }

    Number countByDataModelId(Serializable dataModelId) {
        DataElement.byDataModelId(dataModelId).count()
    }

    DataElement findByDataClassPathAndLabel(DataModel dataModel, List<String> dataClassPath, String label) {
        DataClass dataClass = dataClassService.findDataClassByPath(dataModel, dataClassPath)
        dataClass ? findByDataClassIdAndLabel(dataClass.id, label) : null
    }

    DataElement findByDataClassIdAndLabel(Serializable dataClassId, String label) {
        DataElement.byDataClassIdAndLabel(dataClassId, label).get()
    }

    DataElement findOrCreateDataElementForDataClass(DataClass parentClass, String label, String description, User createdBy,
                                                    DataType dataType,
                                                    Integer minMultiplicity = 0, Integer maxMultiplicity = 1) {
        String cleanLabel = label.trim()
        DataElement dataElement = parentClass.findDataElement(cleanLabel)

        if (!dataElement) {
            dataElement = new DataElement(label: cleanLabel, description: description, createdBy: createdBy.emailAddress,
                                          minMultiplicity: minMultiplicity,
                                          maxMultiplicity: maxMultiplicity)

            if (!dataType.label) dataType.setLabel("$cleanLabel-dataType")
            dataType.addToDataElements(dataElement)
            parentClass.addToDataElements(dataElement)
        }
        if (dataElement.dataType.label != dataType.label) {
            return findOrCreateDataElementForDataClass(parentClass, "${cleanLabel}.1", description, createdBy, dataType, minMultiplicity,
                                                       maxMultiplicity)
        }
        dataElement
    }

    DataElement copy(Model copiedDataModel, DataElement original, UserSecurityPolicyManager userSecurityPolicyManager) {
        copyDataElement(copiedDataModel as DataModel, original, userSecurityPolicyManager.user, userSecurityPolicyManager)
    }

    DataElement copyDataElement(DataModel copiedDataModel, DataElement original, User copier,
                                UserSecurityPolicyManager userSecurityPolicyManager) {
        DataElement copy = new DataElement(minMultiplicity: original.minMultiplicity,
                                           maxMultiplicity: original.maxMultiplicity)

        copy = copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        DataType dataType = copiedDataModel.findDataTypeByLabel(original.dataType.label)

        // If theres no DataType then copy the original's DataType into the DataModel
        if (!dataType) {
            dataType = dataTypeService.copyDataType(copiedDataModel, original.dataType, copier,
                                                    userSecurityPolicyManager)
        }

        dataType.addToDataElements(copy)

        copy
    }

    @Override
    DataElement copyCatalogueItemInformation(DataElement original,
                                             DataElement copy,
                                             User copier,
                                             UserSecurityPolicyManager userSecurityPolicyManager,
                                             boolean copySummaryMetadata = false) {
        copy = super.copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager)
        if (copySummaryMetadata) {
            summaryMetadataService.findAllByCatalogueItemId(original.id).each {
                copy.addToSummaryMetadata(label: it.label, summaryMetadataType: it.summaryMetadataType, createdBy: copier.emailAddress)
            }
        }
        copy
    }

    DataElementSimilarityResult findAllSimilarDataElementsInDataModel(DataModel dataModelToSearch, DataElement dataElementToCompare, maxResults = 5) {

        EntityManager entityManager = sessionFactory.createEntityManager()
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager)


        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
            .buildQueryBuilder()
            .forEntity(DataElement)
            .get()

        Query moreLikeThisQuery = queryBuilder
            .bool()
            .must(queryBuilder.moreLikeThis()
                      .excludeEntityUsedForComparison()
                      .comparingField('label').boostedTo(1f)
                      .andField('description').boostedTo(1f)
                      .andField('dataType.label').boostedTo(1f)
                      .andField('dataClass.label').boostedTo(1f)
                      .toEntity(dataElementToCompare)
                      .createQuery()
            )
            .createQuery()

        SimilarityResult similarityResult = new DataElementSimilarityResult(dataElementToCompare)

        FullTextQuery query = fullTextEntityManager
            .createFullTextQuery(moreLikeThisQuery, DataElement)
            .setMaxResults(maxResults)
            .setProjection(ProjectionConstants.THIS, ProjectionConstants.SCORE)

        query.enableFullTextFilter('idPathSecured').setParameter('allowedIds', [dataModelToSearch.id])

        query.getResultList().each {
            similarityResult.add(get(it[0].id), it[1])
        }

        similarityResult
    }


    DataElement findDataElementWithSameLabelTree(DataModel dataModel, DataElement original) {
        DataClass parent = dataClassService.findSameLabelTree(dataModel, original.dataClass)
        parent.findDataElement(original.label)
    }

    void addDataElementIsFromDataElements(DataElement dataElement, Collection<DataElement> fromDataElements, User user) {
        addDataElementsAreFromDataElements([dataElement], fromDataElements, user)
    }

    void addDataElementsAreFromDataElement(Collection<DataElement> dataElements, DataElement fromDataElement, User user) {
        addDataElementsAreFromDataElements(dataElements, [fromDataElement], user)
    }

    void addDataElementsAreFromDataElements(Collection<DataElement> dataElements, Collection<DataElement> fromDataElements, User user) {
        if (!dataElements || !fromDataElements) throw new ApiInternalException('DESXX', 'No DataElements or FromDataElements exist to create links')
        List<SemanticLink> alreadyExistingLinks = semanticLinkService.findAllBySourceCatalogueItemIdInListAndTargetCatalogueItemIdInListAndLinkType(
            dataElements*.id, fromDataElements*.id, SemanticLinkType.IS_FROM)
        dataElements.each { de ->
            fromDataElements.each { fde ->
                // If no link already exists then add a new one
                if (!alreadyExistingLinks.any { it.catalogueItemId == de.id && it.targetCatalogueItemId == fde.id }) {
                    setDataElementIsFromDataElement(de, fde, user)
                }
            }
        }
    }

    void removeDataElementIsFromDataElements(DataElement dataElement, Collection<DataElement> fromDataElements) {
        removeDataElementsAreFromDataElements([dataElement], fromDataElements)
    }

    void removeDataElementsAreFromDataElement(Collection<DataElement> dataElements, DataElement fromDataElement) {
        removeDataElementsAreFromDataElements(dataElements, [fromDataElement])
    }

    void removeDataElementsAreFromDataElements(Collection<DataElement> dataElements, Collection<DataElement> fromDataElements) {
        if (!dataElements || !fromDataElements) throw new ApiInternalException('DESXX', 'No DataElements or FromDataElements exist to remove links')
        List<SemanticLink> links = semanticLinkService.findAllBySourceCatalogueItemIdInListAndTargetCatalogueItemIdInListAndLinkType(
            dataElements*.id, fromDataElements*.id, SemanticLinkType.IS_FROM)
        semanticLinkService.deleteAll(links)
    }

    void setDataElementIsFromDataElement(DataElement source, DataElement target, User user) {
        source.addToSemanticLinks(linkType: SemanticLinkType.IS_FROM, createdBy: user.getEmailAddress(), targetCatalogueItem: target)
    }

    /**
     * Find a DataElement which is labeled with label and which belongs to dataClass.
     * @param dataClass The DataClass which is the parent of the DataElement being sought
     * @param label The label of the DataElement being sought
     */
    DataElement findDataElement(DataClass dataClass, String label) {
        dataClass.dataElements.find { it.label == label.trim() }
    }

    /**
     * Find a DataElement which is labeled with label and whose parent is parentCatalogueItem. The parentCatalogueItem
     * can be a DataClass.
     * @param parentCatalogueItem The DataClass which is the parent of the DataElement being sought
     * @param label The label of the DataElement being sought
     */
    @Override
    DataElement findByParentAndLabel(CatalogueItem parentCatalogueItem, String label) {
        findDataElement(parentCatalogueItem, label)
    }

    /**
     * When a DataElement is imported into a DataClass, we also want to import the DataElement's
     * DataType into the DataModel to which the importing DataClass belongs.
     *
     * @param currentUser The user doing the import
     * @param imported The resource that was imported
     *
     */
    @Override
    void additionalModelImports(User currentUser, ModelImport imported) {

        //The DataElement that was imported
        DataElement dataElement = imported.importedCatalogueItem
        
        //The DataType of that DataElement
        DataType dataType = dataElement.dataType

        //The CatalogueItem (which will be a DataClass) into which the DataElement was imported
        CatalogueItem catalogueItem = imported.catalogueItem

        //The DataModel to which that DataClass belongs
        DataModel dataModel = catalogueItem.getModel()

        if (!dataModel.findDataTypeByLabelAndType(dataType.label, dataType.domainType)) {
            //Create a new ModelImport for the DataType into DataModel
            ModelImport modelImportDataType = new ModelImport(catalogueItem          : dataModel,
                                                              importedCatalogueItem  : dataType,
                                                              createdByUser          : currentUser)
        
            //Save the additional model import, indicating that this is an additional rather than
            //principal import and so should fail silently if it already exists.
            modelImportService.saveResource(currentUser, modelImportDataType, true)                                                  
        }
    }
}