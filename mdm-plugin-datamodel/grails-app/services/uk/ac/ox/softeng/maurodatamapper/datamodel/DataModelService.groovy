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
package uk.ac.ox.softeng.maurodatamapper.datamodel


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.similarity.DataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.context.MessageSource

import java.time.OffsetDateTime
import java.time.ZoneOffset

@Slf4j
@Transactional
class DataModelService extends ModelService<DataModel> {

    DataTypeService dataTypeService
    DataClassService dataClassService
    DataElementService dataElementService
    MessageSource messageSource
    VersionLinkService versionLinkService
    EditService editService
    ClassifierService classifierService

    SessionFactory sessionFactory

    @Override
    DataModel get(Serializable id) {
        DataModel.get(id)
    }

    @Override
    List<DataModel> getAll(Collection<UUID> ids) {
        DataModel.getAll(ids).findAll()
    }

    @Override
    List<DataModel> list(Map pagination = [:]) {
        DataModel.list(pagination)
    }

    Long count() {
        DataModel.count()
    }

    int countByLabel(String label) {
        DataModel.countByLabel(label)
    }

    DataModel validate(DataModel dataModel) {
        log.debug('Validating DataModel')
        dataModel.validate()
        dataModel
    }

    @Override
    void deleteAll(Collection<DataModel> catalogueItems) {
        deleteAll(catalogueItems.id, true)
    }

    void delete(UUID id) {
        delete(get(id))
    }

    @Override
    void delete(DataModel dm) {
        dm?.deleted = true
    }

    void delete(DataModel dm, boolean permanent, boolean flush = true) {
        if (!dm) return
        if (permanent) {
            dm.folder = null
            dm.delete(flush: flush)
        } else delete(dm)
    }

    List<DataModel> deleteAll(List<Serializable> idsToDelete, Boolean permanent) {
        List<DataModel> updated = []
        idsToDelete.each {
            DataModel dm = get(it)
            delete(dm, permanent)
            if (!permanent) updated << dm
        }
        updated
    }

    DataModel save(DataModel dataModel) {
        log.debug('Saving {}({}) without batching', dataModel.label, dataModel.ident())
        dataModel.save(failOnError: true, validate: false)
        updateFacetsAfterInsertingCatalogueItem(dataModel)
    }

    DataModel saveWithBatching(DataModel dataModel) {
        if (dataModel.ident()) return save(dataModel)
        log.debug('Saving {} using batching', dataModel.label)
        Collection<DataType> dataTypes = []
        Collection<ReferenceType> referenceTypes = []
        Collection<DataClass> dataClasses = []

        if (dataModel.classifiers) {
            log.trace('Saving {} classifiers')
            classifierService.saveAll(dataModel.classifiers)
        }

        if (dataModel.dataTypes) {
            dataTypes.addAll dataModel.dataTypes.findAll {
                !it.instanceOf(ReferenceType)
            }
            referenceTypes.addAll dataModel.dataTypes.findAll {it.instanceOf(ReferenceType)}
            dataModel.dataTypes.clear()
        }

        if (dataModel.dataClasses) {
            dataClasses.addAll dataModel.dataClasses
            dataModel.dataClasses.clear()
        }

        save(dataModel)
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()


        log.trace('Saving {} datatypes', dataTypes.size())
        dataTypeService.saveAll(dataTypes)

        log.trace('Saving {} dataClasses', dataClasses.size())
        Collection<DataElement> dataElements = dataClassService.hierarchySaveAllAndGetDataElements(dataClasses)

        log.trace('Saving {} reference datatypes', referenceTypes.size())
        dataTypeService.saveAll(referenceTypes as Collection<DataType>)

        log.trace('Saving {} dataelements ', dataElements.size())
        dataElementService.saveAll(dataElements)

        dataModel
    }

    @Override
    List<DataModel> findAllReadableByEveryone() {
        DataModel.findAllByReadableByEveryone(true)
    }

    @Override
    List<DataModel> findAllReadableByAuthenticatedUsers() {
        DataModel.findAllByReadableByAuthenticatedUsers(true)
    }

    List<DataModel> findAllDataAssets() {
        findAllByModelType(DataModelType.DATA_ASSET)
    }

    List<DataModel> findAllDataStandards() {
        findAllByModelType(DataModelType.DATA_STANDARD)
    }

    List<DataModel> findAllByModelType(DataModelType type) {
        DataModel.findAllByModelType(type.label)
    }

    List<DataModel> findAllByModelType(String type) {
        DataModel.findAllByModelType(type)
    }

    List<DataModel> findAllByLabel(String label) {
        DataModel.findAllByLabel(label)
    }

    List<DataModel> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination = [:]) {
        DataModel.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    List<DataModel> findAllByMetadataNamespace(String namespace) {
        DataModel.byMetadataNamespace(namespace).list()
    }

    List<DataModel> findAllByDataLoaderPlugin(DataLoaderProviderService dataLoaderProviderService, Map pagination = [:]) {
        findAllByMetadataNamespaceAndKey(dataLoaderProviderService.namespace, dataLoaderProviderService.name, pagination)
    }

    List<DataModel> findAllByFolderId(UUID folderId) {
        DataModel.byFolderId(folderId).list()
    }

    List<UUID> findAllIdsByFolderId(UUID folderId) {
        DataModel.byFolderId(folderId).id().list() as List<UUID>
    }

    List<DataModel> findAllDeleted(Map pagination = [:]) {
        DataModel.byDeleted().list(pagination)
    }

    DataModel findLatestByDataLoaderPlugin(DataLoaderProviderService dataLoaderProviderService) {
        // If we get all the models with the DL metadata then sort by documentation version we should end up with the latest doc version.
        // We should do this rather than using the value of the metadata as its possible someone creates a new documentation version of the model
        // and we should use that one. All DL loaded models have the doc version set to the DL version.
        DataModel latest = DataModel
            .byMetadataNamespaceAndKey(dataLoaderProviderService.namespace, dataLoaderProviderService.name)
            .get([order: 'desc', sort: 'documentationVersion'])
        log.debug('Found DataModel {}({}) which matches DataLoaderPlugin {}({})', latest.label, latest.documentationVersion,
                  dataLoaderProviderService.name, dataLoaderProviderService.version)
        latest
    }


    void deleteAllUnusedDataTypes(DataModel dataModel) {
        log.debug('Cleaning DataModel {} of DataTypes', dataModel.label)
        dataModel.dataTypes.findAll {!it.dataElements}.each {
            dataTypeService.delete(it)
        }
    }

    void deleteAllUnusedDataClasses(DataModel dataModel) {
        log.debug('Cleaning DataModel {} of DataClasses', dataModel.label)
        dataModel.dataClasses.findAll {dataClassService.isUnusedDataClass(it)}.each {
            dataClassService.delete(it)
        }
    }

    void checkImportedDataModelAssociations(User importingUser, DataModel dataModel, Map bindingMap = [:]) {
        dataModel.createdBy = importingUser.emailAddress
        checkFacetsAfterImportingCatalogueItem(dataModel)

        if (dataModel.dataTypes) {
            dataModel.dataTypes.each {dt ->
                dataTypeService.checkImportedDataTypeAssociations(importingUser, dataModel, dt)
            }
        }

        if (dataModel.dataClasses) {
            Collection<DataClass> dataClasses = dataModel.childDataClasses
            dataClasses.each {dc ->
                dataClassService.checkImportedDataClassAssociations(importingUser, dataModel, dc)
            }
        }

        if (dataModel.dataTypes) {
            Set<ReferenceType> referenceTypes = dataModel.dataTypes.findAll {it.instanceOf(ReferenceType)} as Set<ReferenceType>
            if (referenceTypes) {
                log.debug('Matching {} ReferenceType referenceClasses', referenceTypes.size())
                dataTypeService.matchReferenceClasses(dataModel, referenceTypes,
                                                      bindingMap.dataTypes.findAll {it.domainType == DataType.REFERENCE_DOMAIN_TYPE})
            }
        }
        log.debug('DataModel associations checked')
    }

    DataModel ensureAllEnumerationTypesHaveValues(DataModel dataModel) {
        dataModel.dataTypes.findAll {it.instanceOf(EnumerationType) && !(it as EnumerationType).getEnumerationValues()}.each {EnumerationType et ->
            et.addToEnumerationValues(key: '-', value: '-')
        }
        dataModel
    }

    List<DataElement> getAllDataElementsOfDataModel(DataModel dataModel) {
        List<DataElement> allElements = []
        dataModel.dataClasses.each {allElements += it.dataElements ?: []}
        allElements
    }

    List<DataElement> findAllDataElementsWithNames(DataModel dataModel, Set<String> dataElementNames, boolean caseInsensitive) {
        if (!dataElementNames) return []
        getAllDataElementsOfDataModel(dataModel).findAll {
            caseInsensitive ?
            it.label.toLowerCase() in dataElementNames.collect {it.toLowerCase()} :
            it.label in dataElementNames
        }
    }

    Set<EnumerationType> findAllEnumerationTypeByNames(DataModel dataModel, Set<String> enumerationTypeNames, boolean caseInsensitive) {
        if (!enumerationTypeNames) return []
        dataModel.dataTypes.findAll {it.instanceOf(EnumerationType)}.findAll {
            caseInsensitive ?
            it.label.toLowerCase() in enumerationTypeNames.collect {it.toLowerCase()} :
            it.label in enumerationTypeNames
        } as Set<EnumerationType>
    }

    DataModel finaliseDataModel(DataModel dataModel, User user, List<Serializable> supersedeModelIds = []) {
        dataModel.finalised = true
        dataModel.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
        dataModel.addToAnnotations(createdBy: user.emailAddress, label: 'Finalised Model',
                                   description: "DataModel finalised by ${user.firstName} ${user.lastName} on " +
                                                "${OffsetDateTimeConverter.toString(dataModel.dateFinalised)}")
        editService.createAndSaveEdit(dataModel.id, dataModel.domainType,
                                      "DataModel finalised by ${user.firstName} ${user.lastName} on " +
                                      "${OffsetDateTimeConverter.toString(dataModel.dateFinalised)}",
                                      user)
        dataModel
    }

    ObjectDiff<DataModel> diff(DataModel thisDataModel, DataModel otherDataModel) {
        thisDataModel.diff(otherDataModel)
    }

    boolean newVersionCreationIsAllowed(DataModel dataModel) {
        if (!dataModel.finalised) {
            dataModel.errors.reject('invalid.datamodel.new.version.not.finalised.message',
                                    [dataModel.label, dataModel.id] as Object[],
                                    'DataModel [{0}({1})] cannot have a new version as it is not finalised')
            return false
        }
        DataModel superseding = findDataModelDocumentationSuperseding(dataModel)
        if (superseding) {
            dataModel.errors.reject('invalid.datamodel.new.version.superseded.message',
                                    [dataModel.label, dataModel.id, superseding.label, superseding.id] as Object[],
                                    'DataModel [{0}({1})] cannot have a new version as it has been superseded by [{2}({3})]')
            return false
        }

        true
    }

    DataModel createNewDocumentationVersion(DataModel dataModel, User user, boolean copyPermissions, boolean moveDataFlows,
                                            boolean throwErrors = false) {

        if (!newVersionCreationIsAllowed(dataModel)) return dataModel

        DataModel newDocVersion = copyDataModel(dataModel, user, copyPermissions, dataModel.label,
                                                Version.nextMajorVersion(dataModel.documentationVersion), throwErrors)
        setDataModelIsNewDocumentationVersionOfDataModel(newDocVersion, dataModel, user)
        if (moveDataFlows) {
            throw new ApiNotYetImplementedException('DMSXX', 'DataModel moving of DataFlows')
            //            moveTargetDataFlows(dataModel, newDocVersion)
        }

        if (newDocVersion.validate()) newDocVersion.save(flush: true)
        newDocVersion
    }

    DataModel createNewModelVersion(String label, DataModel dataModel, User user, boolean copyPermissions, boolean copyDataFlows,
                                    boolean throwErrors = false) {

        if (!newVersionCreationIsAllowed(dataModel)) return dataModel

        DataModel newModelVersion = copyDataModel(dataModel, user, copyPermissions, label, throwErrors)
        setDataModelIsNewModelVersionOfDataModel(newModelVersion, dataModel, user)
        if (copyDataFlows) {
            throw new ApiNotYetImplementedException('DMSXX', 'DataModel copying of DataFlows')
            //copyTargetDataFlows(dataModel, newModelVersion, user)
        }

        if (newModelVersion.validate()) newModelVersion.save(flush: true)
        newModelVersion
    }

    DataModel copyDataModel(DataModel original, User copier, boolean copyPermissions, String label, boolean throwErrors) {
        copyDataModel(original, copier, copyPermissions, label, Version.from('1'), throwErrors)
    }

    DataModel copyDataModel(DataModel original, User copier, boolean copyPermissions, String label,
                            Version copyVersion, boolean throwErrors) {

        DataModel copy = new DataModel(author: original.author,
                                       organisation: original.organisation, modelType: original.modelType,
                                       finalised: false, deleted: false, documentationVersion: copyVersion,
                                       folder: original.folder
        )

        copy = copyCatalogueItemInformation(original, copy, copier)
        copy.label = label

        if (copyPermissions) {
            if (throwErrors) {
                throw new ApiNotYetImplementedException('DMSXX', 'DataModel permission copying')
            }
            log.warn('Permission copying is not yet implemented')

        }

        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        if (copy.validate()) {
            copy.save(validate: false)
            editService.createAndSaveEdit(copy.id, copy.domainType,
                                          "DataModel ${original.modelType}:${original.label} created as a copy of ${original.id}",
                                          copier
            )
        } else throw new ApiInvalidModelException('DMS01', 'Copied DataModel is invalid', copy.errors, messageSource)

        copy.trackChanges()

        if (original.dataTypes) {
            // Copy all the datatypes
            original.dataTypes.each {dt ->
                dataTypeService.copyDataType(copy, dt, copier)
            }
        }

        if (original.childDataClasses) {
            // Copy all the dataclasses (this will also match up the reference types)
            original.childDataClasses.each {dc ->
                dataClassService.copyDataClass(copy, dc, copier)
            }
        }

        if (original.semanticLinks) {
            original.semanticLinks.each {link ->
                copy.addToSemanticLinks(createdBy: copier.emailAddress,
                                        linkType: link.linkType,
                                        targetCatalogueItemId: link.targetCatalogueItemId,
                                        targetCatalogueItemDomainType: link.targetCatalogueItemDomainType)
            }
        }

        if (original.versionLinks) {
            original.versionLinks.each {link ->
                copy.addToVersionLinks(createdBy: copier.emailAddress,
                                       linkType: link.linkType,
                                       targetModelId: link.targetModelId,
                                       targetModelDomainType: link.targetModelDomainType)
            }
        }

        copy
    }

    void setDataModelIsNewModelVersionOfDataModel(DataModel newModel, DataModel oldModel, User catalogueUser) {
        newModel.addToVersionLinks(
            linkType: VersionLinkType.NEW_MODEL_VERSION_OF,
            createdBy: catalogueUser.emailAddress,
            targetModel: oldModel
        )
    }

    void setDataModelIsNewDocumentationVersionOfDataModel(DataModel newModel, DataModel oldModel, User catalogueUser) {
        newModel.addToVersionLinks(
            linkType: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF,
            createdBy: catalogueUser.emailAddress,
            targetModel: oldModel
        )
    }

    List<DataElementSimilarityResult> suggestLinksBetweenModels(DataModel dataModel, DataModel otherDataModel, int maxResults) {
        dataModel.getAllDataElements().collect {de ->
            dataElementService.findAllSimilarDataElementsInDataModel(otherDataModel, de, maxResults)
        }
    }


    Map<UUID, Long> obtainChildKnowledge(List<DataModel> parents) {
        if (!parents) return [:]
        DetachedCriteria<DataClass> criteria = new DetachedCriteria<DataClass>(DataClass)
            .isNull('parentDataClass')
            .inList('dataModel', parents)
            .projections {
                groupProperty('dataModel.id')
                count()
            }.order('dataModel')
        criteria.list().collectEntries {[it[0], it[1]]}
    }

    @Override
    boolean hasTreeTypeModelItems(DataModel dataModel) {
        dataModel.hasChildren()
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(DataModel catalogueItem) {
        dataClassService.findAllWhereRootDataClassOfDataModelId(catalogueItem.id)
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        !domainType || domainType == DataModel.simpleName
    }


    @Override
    List<DataModel> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                               String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        List<DataModel> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = DataModel.luceneLabelSearch(DataModel, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    @Override
    DataModel findByIdJoinClassifiers(UUID id) {
        DataModel.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        removeAllFromContainer(classifier)
    }

    @Override
    List<DataModel> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        DataModel.byClassifierId(classifier.id).list().findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.id)}
    }

    @Override
    Class<DataModel> getModelClass() {
        DataModel
    }

    @Override
    List<DataModel> findAllByContainerId(UUID containerId) {
        // We do not concern ourselves any other types of containers for DataModels at this time
        findAllByFolderId(containerId)
    }

    @Override
    void deleteAllInContainer(Container container) {
        if (container.instanceOf(Folder)) {
            deleteAll(DataModel.byFolderId(container.id).id().list() as List<UUID>, true)
        }
        if (container.instanceOf(Classifier)) {
            deleteAll(DataModel.byClassifierId(container.id).id().list() as List<UUID>, true)
        }
    }

    @Override
    void removeAllFromContainer(Container container) {
        if (container.instanceOf(Folder)) {
            DataModel.byFolderId(container.id).list().each {
                it.folder = null
            }
        }
        if (container.instanceOf(Classifier)) {
            DataModel.byClassifierId(container.id).list().each {
                it.removeFromClassifiers(container as Classifier)
            }
        }
    }

    @Override
    List<DataModel> findAllReadableModels(UserSecurityPolicyManager userSecurityPolicyManager, boolean includeDocumentSuperseded,
                                          boolean includeModelSuperseded, boolean includeDeleted) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!ids) return []
        List<UUID> constrainedIds
        // The list of ids are ALL the readable ids by the user, no matter the model status
        if (includeDocumentSuperseded && includeModelSuperseded) {
            constrainedIds = new ArrayList<>(ids)
        } else if (includeModelSuperseded) {
            constrainedIds = findAllExcludingDocumentSupersededIds(ids)
        } else if (includeDocumentSuperseded) {
            constrainedIds = findAllExcludingModelSupersededIds(ids)
        } else {
            constrainedIds = findAllExcludingDocumentAndModelSupersededIds(ids)
        }
        if (!constrainedIds) return []
        DataModel.withReadable(DataModel.by(), constrainedIds, includeDeleted).list()
    }

    @Override
    List<UUID> findAllModelIdsWithChildren(List<DataModel> models) {
        models.findAll {it.hasChildren()}.collect {it.id}
    }

    @Override
    void removeVersionLinkFromModel(UUID modelId, VersionLink versionLink) {
        get(modelId).removeFromVersionLinks(versionLink)
    }

    @Override
    List<UUID> findAllSupersededModelIds(List<DataModel> models) {
        findAllSupersededIds(models.id)
    }

    @Override
    List<DataModel> findAllDocumentationSupersededModels(Map pagination) {
        List<UUID> ids = findAllDocumentSupersededIds(DataModel.by().id().list() as List<UUID>)
        DataModel.byIdInList(ids).list(pagination)
    }

    @Override
    List<DataModel> findAllModelSupersededModels(Map pagination) {
        List<UUID> ids = findAllModelSupersededIds(DataModel.by().id().list() as List<UUID>)
        DataModel.byIdInList(ids).list(pagination)
    }

    @Override
    List<DataModel> findAllDeletedModels(Map pagination) {
        DataModel.byDeleted().list(pagination)
    }

    List<UUID> findAllExcludingDocumentSupersededIds(List<UUID> readableIds) {
        readableIds - findAllDocumentSupersededIds(readableIds)
    }

    List<UUID> findAllExcludingModelSupersededIds(List<UUID> readableIds) {
        readableIds - findAllModelSupersededIds(readableIds)
    }

    List<UUID> findAllExcludingDocumentAndModelSupersededIds(List<UUID> readableIds) {
        readableIds - findAllSupersededIds(readableIds)
    }

    List<UUID> findAllSupersededIds(List<UUID> readableIds) {
        versionLinkService.filterModelIdsWhereModelIdIsSuperseded(DataModel.simpleName, readableIds)
    }

    List<UUID> findAllDocumentSupersededIds(List<UUID> readableIds) {
        versionLinkService.filterModelIdsWhereModelIdIsDocumentSuperseded(DataModel.simpleName, readableIds)
    }

    List<UUID> findAllModelSupersededIds(List<UUID> readableIds) {
        versionLinkService.filterModelIdsWhereModelIdIsModelSuperseded(DataModel.simpleName, readableIds)
    }

    List<DataModel> findAllDocumentSuperseded(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        DataModel.byIdInList(findAllDocumentSupersededIds(userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel))).list(pagination)
    }

    List<DataModel> findAllModelSuperseded(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        DataModel.byIdInList(findAllModelSupersededIds(userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel))).list(pagination)
    }

    DataModel findDataModelSuperseding(DataModel dataModel) {
        VersionLink link = versionLinkService.findLatestLinkSupersedingModelId(DataModel.simpleName, dataModel.id)
        if (!link) return null
        link.catalogueItemId == dataModel.id ? get(link.targetModelId) : get(link.catalogueItemId)
    }

    DataModel findDataModelDocumentationSuperseding(DataModel dataModel) {
        VersionLink link = versionLinkService.findLatestLinkDocumentationSupersedingModelId(DataModel.simpleName, dataModel.id)
        if (!link) return null
        link.catalogueItemId == dataModel.id ? get(link.targetModelId) : get(link.catalogueItemId)
    }

    /**
     * Find all resources by the defined user security policy manager. If none provided then assume no security policy in place in which case
     * everything is public.
     * @param userSecurityPolicyManager
     * @param pagination
     * @return
     */
    List<DataModel> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        ids ? DataModel.findAllByIdInList(ids, pagination) : []
    }

    void checkDocumentationVersion(DataModel dataModel, boolean importAsNewDocumentationVersion, User catalogueUser) {
        if (importAsNewDocumentationVersion) {

            if (countByLabel(dataModel.label)) {
                List<DataModel> existingModels = findAllByLabel(dataModel.label)
                existingModels.each {existing ->
                    log.debug('Setting DataModel as new documentation version of [{}:{}]', existing.label, existing.documentationVersion)
                    if (!existing.finalised) finaliseDataModel(existing, catalogueUser)
                    setDataModelIsNewDocumentationVersionOfDataModel(dataModel, existing, catalogueUser)
                }
                Version latestVersion = existingModels.max {it.documentationVersion}.documentationVersion
                dataModel.documentationVersion = Version.nextMajorVersion(latestVersion)

            } else log.info('Marked as importAsNewDocumentationVersion but no existing DataModels with label [{}]', dataModel.label)
        }
    }

    void checkFinaliseDataModel(DataModel dataModel, boolean finalised) {
        if (finalised && !dataModel.finalised) {
            dataModel.finalised = finalised
            dataModel.dateFinalised = dataModel.finalised ? OffsetDateTime.now() : null
        }
    }


}