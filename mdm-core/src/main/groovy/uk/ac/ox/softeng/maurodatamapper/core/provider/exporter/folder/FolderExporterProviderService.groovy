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
package uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.folder

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
@CompileStatic
abstract class FolderExporterProviderService extends ExporterProviderService {

    @Autowired
    FolderService folderService

    abstract ByteArrayOutputStream exportFolder(User currentUser, Folder folder) throws ApiException

    abstract ByteArrayOutputStream exportFolders(User currentUser, List<Folder> dataModels) throws ApiException

    @Override
    ByteArrayOutputStream exportDomain(User currentUser, UUID domainId) throws ApiException {
        Folder folder = folderService.get(domainId)
        if (!folder) {
            log.error('Cannot find container id [{}] to export', domainId)
            throw new ApiInternalException('DMEP01', "Cannot find container id [${domainId}] to export")
        }
        exportFolder(currentUser, folder)
    }

    @Override
    ByteArrayOutputStream exportDomains(User currentUser, List<UUID> domainIds) throws ApiException {
        List<Folder> folders = []
        List<UUID> cannotExport = []
        domainIds.each {
            Folder folder = folderService.get(it)
            if (!folder) {
                cannotExport.add it
            } else folders.add folder
        }
        log.warn('Cannot find container ids [{}] to export', cannotExport)
        exportFolders(currentUser, folders)
    }

    @Override
    Boolean canExportMultipleDomains() {
        false
    }

    @Override
    String getProviderType() {
        "Folder${ProviderType.EXPORTER.name}"
    }

}
