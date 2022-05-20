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
package uk.ac.ox.softeng.maurodatamapper.core.async


import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j

@Slf4j
class DomainExportInterceptor implements MdmInterceptor {

    DomainExportService domainExportService

    boolean before() {
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'domainExportId')

        // Anyone authenticated can index as the controller will listAllReadable
        if (isIndex()) {
            return currentUserSecurityPolicyManager.isAuthenticated() ?: forbiddenDueToPermissions()
        }

        checkActionAuthorisationOnUnsecuredResource(params.domainExportId ?: params.id)
    }

    boolean checkActionAuthorisationOnUnsecuredResource(UUID id) {

        DomainExport domainExport = domainExportService.get(id)
        if (!domainExport) return notFound(DomainExport, id)

        List<MdmDomain> exportedDomains = domainExportService.getExportedDomains(domainExport)

        for (MdmDomain exportedDomain : exportedDomains) {
            if (!checkActionAuthorisationOnUnsecuredResource(id, exportedDomain)) return false
        }
        true
    }

    boolean checkActionAuthorisationOnUnsecuredResource(UUID id, MdmDomain exportedDomain) {
        if (!exportedDomain) return notFound(DomainExport, id)

        // If the exported domain is a SR then we can use that against the UserSecurityPolicyManager
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, exportedDomain.class)) {
            return checkActionAuthorisationOnUnsecuredResource(id, exportedDomain.class as Class<SecurableResource>, exportedDomain.id)
        }

        // Currently you can't export a ModelItem but that may change so lets code it in now,
        // check the MI and the Model access
        if (Utils.parentClassIsAssignableFromChild(ModelItem, exportedDomain.class)) {
            ModelItem modelItem = exportedDomain as ModelItem
            return checkActionAuthorisationOnUnsecuredResource(id, modelItem.model.class, modelItem.model.id)
        }

        // Otherwise its an unknown ExportedDomain type so we just say they can't read it
        log.warn('Could not determine if exported domain {}:{} can be acted on', exportedDomain.domainType, exportedDomain.id)
        notFound(DomainExport, id)
    }

    boolean checkActionAuthorisationOnUnsecuredResource(UUID id,
                                                        Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        boolean canRead = currentUserSecurityPolicyManager.userCanReadResourceId(DomainExport, id, owningSecureResourceClass, owningSecureResourceId)

        if (actionName == 'download') {
            return canRead ?: notFound(DomainExport, id)
        }
        // Otherwise just fall through to the default handling
        checkActionAuthorisationOnUnsecuredResource(DomainExport, id, owningSecureResourceClass, owningSecureResourceId)
    }
}
