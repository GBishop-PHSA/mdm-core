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
package uk.ac.ox.softeng.maurodatamapper.security


import uk.ac.ox.softeng.maurodatamapper.core.interceptor.TieredAccessSecurableResourceInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedUserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class CatalogueUserInterceptor extends TieredAccessSecurableResourceInterceptor {

    @Override
    List<String> getPublicAccessMethods() {
        ['resetPassword']
    }

    @Override
    List<String> getAuthenticatedAccessMethods() {
        ['search']
    }

    @Override
    List<String> getReadAccessMethods() {
        ['userPreferences']
    }

    @Override
    List<String> getEditAccessMethods() {
        ['updateUserPreferences', 'changePassword',]
    }

    @Override
    List<String> getApplicationAdminAccessMethods() {
        ['userExists', 'pending', 'pendingCount', 'adminRegister', 'userExists',
         'adminPasswordReset', 'approveRegistration', 'rejectRegistration']
    }

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        CatalogueUser
    }

    @Override
    void checkIds() {
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'catalogueUserId')
        Utils.toUuid(params, 'userGroupId')
    }

    @Override
    UUID getId() {
        params.catalogueUserId ?: params.id
    }

    boolean before() {
        securableResourceChecks()
        if (isSave() || actionName in ['sendPasswordResetLink']) {
            if (!currentUserSecurityPolicyManager.isAuthenticated()) return true
            if (currentUserSecurityPolicyManager instanceof PublicAccessSecurityPolicyManager) return true
            return methodNotAllowed('Cannot be logged in and request reset password link or self register')
        }

        if (isIndex() && params.containsKey('userGroupId')) {
            return currentUserSecurityPolicyManager.userCanReadSecuredResourceId(UserGroup, params.userGroupId) ?:
                   notFound(UserGroup, params.userGroupId)
        }

        if (actionName in getApplicationAdminAccessMethods()) {
            if (currentUserSecurityPolicyManager instanceof GroupBasedUserSecurityPolicyManager) {
                return (currentUserSecurityPolicyManager as GroupBasedUserSecurityPolicyManager).hasUserAdminRights() ?:
                       forbiddenDueToNotApplicationAdministrator()
            }
            return currentUserSecurityPolicyManager.isApplicationAdministrator() ?: forbiddenDueToNotApplicationAdministrator()
        }

        checkTieredAccessActionAuthorisationOnSecuredResource(CatalogueUser, getId())
    }
}