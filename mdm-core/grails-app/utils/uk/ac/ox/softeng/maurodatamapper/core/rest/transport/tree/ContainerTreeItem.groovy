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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import groovy.transform.CompileStatic

/**
 * @since 07/01/2020
 */
@CompileStatic
class ContainerTreeItem extends TreeItem {

    Boolean deleted
    String containerType
    UUID containerId
    Boolean finalised
    Version documentationVersion
    Version modelVersion
    String modelVersionTag
    String branchName
    boolean versionAware
    Integer depth

    ContainerTreeItem(Container container, List<String> availableTreeActions) {
        super(container, container.label, null, availableTreeActions)
        containerId = container.getParent()?.id
        deleted = container.deleted
        containerType = container.domainType
        renderChildren = true
        versionAware = false
        depth = path.size() - 1 // Root elements have a depth of 0
        if (Utils.parentClassIsAssignableFromChild(VersionedFolder, container.class)) {
            finalised = ((VersionedFolder) container).finalised
            documentationVersion = ((VersionedFolder) container).documentationVersion
            modelVersion = ((VersionedFolder) container).modelVersion
            modelVersionTag = ((VersionedFolder) container).modelVersionTag
            branchName = ((VersionedFolder) container).branchName
            versionAware = true
        }
    }

    boolean isEmptyContainerTree() {
        if (childSet.isEmpty()) return true
        if (any {!(it instanceof ContainerTreeItem)}) return false
        findAll {it instanceof ContainerTreeItem}.every {((ContainerTreeItem) it).isEmptyContainerTree()}
    }

    void recursivelyRemoveEmptyChildContainers() {
        childSet.removeIf {it instanceof ContainerTreeItem && ((ContainerTreeItem) it).isEmptyContainerTree()}
        findAllChildContainerTrees().each {it.recursivelyRemoveEmptyChildContainers()}
    }

    Set<ContainerTreeItem> findAllChildContainerTrees() {
        findAll {it instanceof ContainerTreeItem} as Set<ContainerTreeItem>
    }
}
