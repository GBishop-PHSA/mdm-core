/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.federation

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.importer.ImporterService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.AnonymisableService
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.AnonymousUser

import grails.gorm.transactions.Transactional
import grails.rest.Link
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.time.OffsetDateTime

@Slf4j
@Transactional
class SubscribedModelService implements SecurableResourceService<SubscribedModel>, AnonymisableService {

    @Autowired(required = false)
    List<ModelService> modelServices

    SubscribedCatalogueService subscribedCatalogueService
    FolderService folderService
    ImporterService importerService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    @Override
    SubscribedModel get(Serializable id) {
        SubscribedModel.get(id)
    }

    @Override
    List<SubscribedModel> getAll(Collection<UUID> containerIds) {
        SubscribedModel.getAll(containerIds)
    }

    SubscribedModel findBySubscribedCatalogueIdAndSubscribedModelId(UUID subscribedCatalogueId, String subscribedModelId) {
        SubscribedModel.bySubscribedCatalogueIdAndSubscribedModelId(subscribedCatalogueId, subscribedModelId).get()
    }

    SubscribedModel findBySubscribedCatalogueIdAndId(UUID subscribedCatalogueId, UUID id) {
        SubscribedModel.bySubscribedCatalogueIdAndId(subscribedCatalogueId, id).get()
    }

    SubscribedModel findBySubscribedModelId(String subscribedModelId) { // TODO remove as SM ID may no longer be unique
        SubscribedModel.bySubscribedModelId(subscribedModelId).get()
    }

    List<SubscribedModel> list(Map pagination = [:]) {
        SubscribedModel.list(pagination)
    }

    List<SubscribedModel> findAllBySubscribedCatalogueId(UUID subscribedCatalogueId, Map pagination = [:]) {
        SubscribedModel.bySubscribedCatalogueId(subscribedCatalogueId).list(pagination)
    }

    @Override
    List<SubscribedModel> findAllReadableByEveryone() {
        SubscribedModel.findAllByReadableByEveryone(true)
    }

    @Override
    List<Authority> findAllReadableByAuthenticatedUsers() {
        SubscribedModel.findAllByReadableByAuthenticatedUsers(true)
    }

    Long count() {
        SubscribedModel.count()
    }

    @Override
    void delete(SubscribedModel subscribedModel) {
        subscribedModel.delete(flush: true)
        if (securityPolicyManagerService) {
            securityPolicyManagerService.removeSecurityForSecurableResource(subscribedModel, null)
        }
    }

    @Override
    boolean handles(Class clazz) {
        clazz == SubscribedModel
    }

    @Override
    boolean handles(String domainType) {
        domainType == SubscribedModel.simpleName
    }

    SubscribedModel save(SubscribedModel subscribedModel) {
        subscribedModel.save(failOnError: true, validate: false)
    }

    def federateSubscribedModel(SubscribedModel subscribedModel, UserSecurityPolicyManager userSecurityPolicyManager) {
        Folder folder = folderService.get(subscribedModel.folderId)

        log.debug('Exporting SubscribedModel')
        try {
            List<Link> exportLinks = getExportLinksForSubscribedModel(subscribedModel)

            ImporterProviderService importerProviderService

            Link exportLink = exportLinks.find {link -> importerProviderService = importerService.findImporterProviderServiceByContentType(link.contentType)}
            if (!importerProviderService) {
                log.debug('No ImporterProviderService found for any published content type')
                subscribedModel.errors.reject('invalid.subscribedmodel.import.format.unsupported',
                                              'Could not export SubscribedModel from SubscribedCatalogue')
                return subscribedModel.errors
            }

            ModelImporterProviderServiceParameters parameters = importerService.createNewImporterProviderServiceParameters(importerProviderService)

            if (parameters.hasProperty('importFile')?.type != FileParameter) {
                throw new ApiInternalException('MSXX', "Importer ${importerProviderService.class.simpleName} " +
                                                       'for model cannot import file content')
            }

            byte[] resourceBytes = exportSubscribedModelFromSubscribedCatalogueLink(subscribedModel, exportLink)



        } catch (ApiException exception) {
            log.warn("Failed to federate subscribedModel due to [${exception.message}]")
            subscribedModel.errors.reject('invalid.subscribedmodel.federate.exception',
                                          [exception.message].toArray(),
                                          'Could not federate SubscribedModel into local Catalogue due to [{0}]')
            return subscribedModel.errors
        }

        subscribedModel
    }

    def federateSubscribedModel(SubscribedModel subscribedModel, UserSecurityPolicyManager userSecurityPolicyManager) {

        Folder folder = folderService.get(subscribedModel.folderId)

        //Export the requested model from the SubscribedCatalogue
        log.debug('Exporting SubscribedModel')
        try {
            String exportedJson = exportSubscribedModelFromSubscribedCatalogue(subscribedModel)

            if (!exportedJson) {
                log.debug('No Json exported')
                subscribedModel.errors.reject('invalid.subscribedmodel.export',
                                              'Could not export SubscribedModel from SubscribedCatalogue')
                return subscribedModel.errors
            }

            //Get a ModelService to handle the domain type we are dealing with
            ModelService modelService = modelServices.find {it.handles(subscribedModel.subscribedModelType)}
            ModelImporterProviderService modelImporterProviderService = modelService.getJsonModelImporterProviderService()

            //Import the model
            ModelImporterProviderServiceParameters parameters =
                modelImporterProviderService.createNewImporterProviderServiceParameters() as ModelImporterProviderServiceParameters

            if (parameters.hasProperty('importFile')?.type != FileParameter) {
                throw new ApiInternalException('MSXX', "Assigned JSON importer ${modelImporterProviderService.class.simpleName} " +
                                                       'for model cannot import file content')
            }

            parameters.importFile = new FileParameter(fileContents: exportedJson.getBytes())
            parameters.folderId = folder.id
            parameters.finalised = true
            parameters.useDefaultAuthority = false

            Model model = modelImporterProviderService.importDomain(userSecurityPolicyManager.user, parameters)

            if (!model) {
                subscribedModel.errors.reject('invalid.subscribedmodel.import',
                                              'Could not import SubscribedModel into local Catalogue')
                return subscribedModel.errors
            }

            log.debug('Importing model {}, version {} from authority {}', model.label, model.modelVersion, model.authority)
            if (modelService.countByAuthorityAndLabelAndVersion(model.authority, model.label, model.modelVersion)) {
                subscribedModel.errors.reject('invalid.subscribedmodel.import.already.exists',
                                              [model.authority, model.label, model.modelVersion].toArray(),
                                              'Model from authority [{0}] with label [{1}] and version [{2}] already exists in catalogue')
                return subscribedModel.errors
            }


            model.folder = folder

            modelService.validate(model)

            if (model.hasErrors()) {
                return model.errors
            }

            log.debug('No errors in imported model')

            Model savedModel = modelService.saveModelWithContent(model)
            log.debug('Saved model')

            if (securityPolicyManagerService) {
                log.debug('add security to saved model')
                userSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(savedModel,
                                                                                                         userSecurityPolicyManager.user,
                                                                                                         savedModel.label)
            }


            //Record the ID of the imported model against the subscription makes it easier to track version links later.
            subscribedModel.lastRead = OffsetDateTime.now()
            subscribedModel.localModelId = savedModel.id

            //Handle version linking
            Map versionLinks = getVersionLinks(modelService.getUrlResourceName(), subscribedModel)
            if (versionLinks) {
                log.debug('add version links')
                addVersionLinksToImportedModel(userSecurityPolicyManager.user, versionLinks, modelService, subscribedModel)
            }

        } catch (ApiException exception) {
            log.warn("Failed to federate subscribedModel due to [${exception.message}]")
            subscribedModel.errors.reject('invalid.subscribedmodel.federate.exception',
                                          [exception.message].toArray(),
                                          'Could not federate SubscribedModel into local Catalogue due to [{0}]')
            return subscribedModel.errors
        }

        subscribedModel
    }

    /**
     * Get version links from the subscribed catalogue for the specified subscribed model
     * 1. Create a URL for {modelDomainType}/{modelId}/versionLinks
     * 2. Slurp the Json returned by this endpoint and return the object
     *
     * @param urlModelResourceType
     * @param subscribedModel
     * @return A map of slurped version links
     */
    Map getVersionLinks(String urlModelResourceType, SubscribedModel subscribedModel) {
        subscribedCatalogueService.
            getVersionLinksForModel(subscribedModel.subscribedCatalogue, urlModelResourceType, subscribedModel.subscribedModelId)
    }

    Map getNewerPublishedVersions(SubscribedModel subscribedModel) {
        subscribedCatalogueService.getNewerPublishedVersionsForPublishedModel(subscribedModel.subscribedCatalogue, subscribedModel.subscribedModelId)
    }

    /**
     * Export a model as Json from a remote SubscribedCatalogue
     * 1. Find an exporter on the remote SubscribedCatalogue
     * 2. If an exporter is found, use it to export the model
     *
     * @param subscribedModel
     * @return String of the exported json
     */
    String exportSubscribedModelFromSubscribedCatalogue(SubscribedModel subscribedModel) {

        //Get a ModelService to handle the domain type we are dealing with
        ModelService modelService = modelServices.find {it.handles(subscribedModel.subscribedModelType)}

        Map exporter = getJsonExporter(subscribedModel.subscribedCatalogue, modelService)

        subscribedCatalogueService.getStringResourceExport(subscribedModel.subscribedCatalogue, modelService.getUrlResourceName(),
                                                           subscribedModel.subscribedModelId, exporter)

        // byte[] bytesOut = subscribedCatalogueService.getBytesResourceExport(subscribedModel.subscribedCatalogue, )
        // new String(subscribedCatalogueService.getBytesResourceExport(subscribedModel.subscribedCatalogue, getExportLinksForSubscribedModel(subscribedModel).first().href))
    }

    byte[] exportSubscribedModelFromSubscribedCatalogueLink(SubscribedModel subscribedModel, Link link) {
        subscribedCatalogueService.getBytesResourceExport(subscribedModel.subscribedCatalogue, link.href)
    }

    /**
     * Create version links of type NEW_MODEL_VERSION_OF
     * @return
     */
    void addVersionLinksToImportedModel(User currentUser, Map versionLinks, ModelService modelService, SubscribedModel subscribedModel) {
        log.debug('addVersionLinksToImportedModel')
        List matches = []
        if (versionLinks && versionLinks.items) {
            matches = versionLinks.items.findAll {
                it.sourceModel.id == subscribedModel.subscribedModelId && it.linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label
            }
        }

        if (matches) {
            matches.each {vl ->
                log.debug('matched')
                //Get Subscribed models for the new (source) and old (target) versions of the model
                SubscribedModel sourceSubscribedModel = subscribedModel
                SubscribedModel targetSubscribedModel = findBySubscribedModelId(vl.targetModel.id)

                if (sourceSubscribedModel && targetSubscribedModel) {
                    Model localSourceModel
                    Model localTargetModel

                    //Get the local copies of the new (source) and old (target) of the models
                    localSourceModel = modelService.get(sourceSubscribedModel.localModelId)
                    localTargetModel = modelService.get(targetSubscribedModel.localModelId)

                    //Create a local version link between the local copies of the new (source) and old (target) models
                    if (localSourceModel && localTargetModel) {
                        //Do we alreday have a version link between these two model versions?
                        boolean exists = localSourceModel.versionLinks && localSourceModel.versionLinks.find {
                            it.model.id == localSourceModel.id && it.targetModel.id == localTargetModel.id && it.linkType ==
                            VersionLinkType.NEW_MODEL_VERSION_OF
                        }

                        if (!exists) {
                            log.debug('setModelIsNewBranch')
                            modelService.setModelIsNewBranchModelVersionOfModel(localSourceModel, localTargetModel, currentUser)
                        }
                    }
                }
            }
        }
        log.debug('exit addVersionLinksToImportedModel')
    }

    /**
     * Find an exporter for the domain type that we want to export from the subscribed catalogue
     * 1. Create a URL for {modelDomainType}/providers/exporters
     * 2. Slurp the Json returned by this endpoint
     * 3. Return an exporter from this slurped json which has a file extension of json
     *
     * @param subscribedCatalogue
     * @param urlModelType
     * @return An exporter
     */
    private Map getJsonExporter(SubscribedCatalogue subscribedCatalogue, ModelService modelService) {

        //Make an object by slurping json
        List<Map<String, Object>> exporters = subscribedCatalogueService.getAvailableExportersForResourceType(subscribedCatalogue, modelService.getUrlResourceName())

        //Find a json exporter
        Map exporterMap = exporters.find {it.fileType == 'text/json' && it.name == modelService.getJsonModelExporterProviderService().name}

        //Can't use DataBindingUtils because of a clash with grails 'version' property
        if (!exporterMap) {
            throw new ApiBadRequestException('SMSXX', 'Cannot export model from subscribed catalogue as no JSON exporter available')
        }
        exporterMap
    }

    private List<Link> getExportLinksForSubscribedModel(SubscribedModel subscribedModel) {
        subscribedCatalogueService.listPublishedModels(subscribedModel.subscribedCatalogue)
            .findAll {pm -> pm.modelId == subscribedModel.subscribedModelId}
            .sort {pm -> pm.lastUpdated}
            .last().links
    }

    void anonymise(String createdBy) {
        SubscribedModel.findAllByCreatedBy(createdBy).each { subscribedModel ->
            subscribedModel.createdBy = AnonymousUser.ANONYMOUS_EMAIL_ADDRESS
            subscribedModel.save(validate: false)
        }
    }
}
