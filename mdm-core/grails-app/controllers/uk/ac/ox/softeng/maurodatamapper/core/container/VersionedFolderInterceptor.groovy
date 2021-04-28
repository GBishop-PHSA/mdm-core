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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.interceptor.SecurableResourceInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils


class VersionedFolderInterceptor extends SecurableResourceInterceptor {

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        VersionedFolder as Class<S>
    }

    @Override
    void checkIds() {
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'folderId')
        Utils.toUuid(params, 'versionedFolderId')
    }

    @Override
    UUID getId() {
        params.id ?: params.versionedFolderId ?: params.folderId
    }

    boolean before() {
        securableResourceChecks()

        boolean canRead = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(VersionedFolder, id)

        if (actionName == 'search') {
            return canRead ?: notFound(VersionedFolder, getId())
        }
        if (actionName == 'finalise') {
            return currentUserSecurityPolicyManager.userCanWriteSecuredResourceId(VersionedFolder, id, actionName) ?:
                   forbiddenOrNotFound(canRead, VersionedFolder, id)
        }
        //TODO confirm this is all that is necessary here
        if (actionName == 'newBranchModelVersion') {
            return currentUserSecurityPolicyManager.userCanWriteSecuredResourceId(VersionedFolder, id, actionName) ?:
                   forbiddenOrNotFound(canRead, VersionedFolder, id)
        }

        checkActionAuthorisationOnSecuredResource(VersionedFolder, getId(), true)
    }
}
