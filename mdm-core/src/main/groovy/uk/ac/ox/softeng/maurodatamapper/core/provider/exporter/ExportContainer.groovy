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
package uk.ac.ox.softeng.maurodatamapper.core.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import groovy.xml.Namespace

class ExportContainer {

    Namespace xmlNamespace
    Map containerExportMap
    ExportMetadata exportMetadata
    String containerExportTemplatePath
    String exportContainerType
    Namespace containerXmlNamespace

    ExportContainer(Container container, String containerType, String version, ExportMetadata exportMetadata) {
        this(container, containerType, version, '', exportMetadata)
    }

    ExportContainer(Container container, String containerType, String version, String templatePathFileExtension, ExportMetadata exportMetadata) {
        this(container, containerType, version, version, templatePathFileExtension, exportMetadata)
    }

    ExportContainer(Container container, String containerType, String version, String containerVersion, String templatePathFileExtension,
                    ExportMetadata exportMetadata) {
        this.exportMetadata = exportMetadata
        this.exportContainerType = containerType
        this.xmlNamespace = new Namespace("http://maurodatamapper.com/export/${version}", 'xmlns:exp')
        this.containerXmlNamespace = new Namespace("http://maurodatamapper.com/$exportContainerType/$containerVersion", 'xmlns:mdm')
        this.containerExportTemplatePath = "/$exportContainerType/export${templatePathFileExtension ? ".$templatePathFileExtension" : ''}"
        this.containerExportMap = [export: container]
        this.containerExportMap[exportContainerType] = container
    }

    Map getXmlNamespaces() {
        Map ns = [:]
        ns[xmlNamespace.prefix] = xmlNamespace.uri
        ns[containerXmlNamespace.prefix] = containerXmlNamespace.uri
        ns
    }
}
