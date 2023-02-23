/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.container.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
abstract class ContainerExporterProviderService<C extends Container> extends ExporterProviderService {

    abstract ContainerService<C> getContainerService()

    abstract String getDomainType()

    List<C> retrieveExportableContainers(List<UUID> containerIds) throws ApiBadRequestException {
        List<C> containers = containerService.getAll(containerIds)
        List<UUID> cannotExport = containerIds.collect().tap {removeAll {containers*.id}}
        if (!containers) throw new ApiBadRequestException('CIPS01', "Cannot find ${domainType} IDs [${cannotExport}] to export")
        if (cannotExport) log.warn("Cannot find ${domainType} IDs [${cannotExport}] to export")
        containers
    }

    @Override
    String getFileName(MdmDomain domain) {
        "${(domain as Container).label}.${getFileExtension()}"
    }
}
