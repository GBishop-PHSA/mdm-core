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
package uk.ac.ox.softeng.maurodatamapper.security.policy


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.VirtualSecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsApplication
import grails.core.GrailsClass
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j

import java.util.function.Predicate

import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.APPLICATION_ADMIN_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.AUTHOR_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.CONTAINER_ADMIN_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.CONTAINER_GROUP_ADMIN_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.EDITOR_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.GROUP_ADMIN_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.READER_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.REVIEWER_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.USER_ADMIN_ROLE_NAME

/**
 * This class should be built using the GroupBasedSecurityPolicyManagerService which will have transactionality available.
 * All operations on this class and inside this class should assume no session and no transaction are available.
 * Therefore everything needed to determine access rights needs to be defined when the class is created.
 *
 * Application permitted roles should be the complete list of inherited and assigned application roles.
 * Virtual Securable Resource Group Roles should be the complete list of inherited and assigned roles for each securable resource,
 * in the event a user is application or site admin level then all secured resources should be assigned a virtual level, this will allow
 * zero calls to the database when ascertaining access. It also means we can make 1 check for rights to access.
 */
@Slf4j
class GroupBasedUserSecurityPolicyManager implements UserSecurityPolicyManager {

    CatalogueUser user
    Set<UserGroup> userGroups
    List<SecurableResourceGroupRole> securableResourceGroupRoles
    Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles
    Set<GroupRole> applicationPermittedRoles

    GrailsApplication grailsApplication

    GroupBasedUserSecurityPolicyManager() {
        userGroups = [] as Set
        setNoAccess()
    }

    GroupBasedUserSecurityPolicyManager inApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
        this
    }

    GroupBasedUserSecurityPolicyManager forUser(CatalogueUser catalogueUser) {
        user = catalogueUser
        this
    }

    GroupBasedUserSecurityPolicyManager inGroups(Set<UserGroup> userGroups) {
        this.userGroups = userGroups
        this
    }

    GroupBasedUserSecurityPolicyManager withSecurableRoles(List<SecurableResourceGroupRole> securableResourceGroupRoles) {
        this.securableResourceGroupRoles = securableResourceGroupRoles
        this
    }

    GroupBasedUserSecurityPolicyManager includeSecurableRoles(List<SecurableResourceGroupRole> securableResourceGroupRoles) {
        this.securableResourceGroupRoles.addAll(securableResourceGroupRoles)
        this
    }

    GroupBasedUserSecurityPolicyManager withVirtualRoles(Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles) {
        this.virtualSecurableResourceGroupRoles = virtualSecurableResourceGroupRoles
        this
    }

    GroupBasedUserSecurityPolicyManager withApplicationRoles(Set<GroupRole> applicationPermittedRoles) {
        this.applicationPermittedRoles = applicationPermittedRoles
        this
    }

    GroupBasedUserSecurityPolicyManager includeApplicationRoles(Set<GroupRole> applicationPermittedRoles) {
        this.applicationPermittedRoles.addAll(applicationPermittedRoles)
        this
    }

    GroupBasedUserSecurityPolicyManager includeVirtualRoles(Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles) {
        this.virtualSecurableResourceGroupRoles.addAll(virtualSecurableResourceGroupRoles)
        this
    }

    GroupBasedUserSecurityPolicyManager hasNoAccess() {
        setNoAccess()
        this
    }

    GroupBasedUserSecurityPolicyManager removeVirtualRoleIf(@ClosureParams(value =
        SimpleType, options = 'uk.ac.ox.softeng.maurodatamapper.security.role.VirtualSecurableResourceGroupRole') Closure predicate) {
        virtualSecurableResourceGroupRoles.removeIf([test: predicate] as Predicate)
        this
    }

    GroupBasedUserSecurityPolicyManager removeAssignedRoleIf(@ClosureParams(value =
        SimpleType, options = 'uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole') Closure predicate) {
        securableResourceGroupRoles.removeIf([test: predicate] as Predicate)
        this
    }

    // If the usergroups are set then the entire security policy is unsure so will need to be rebuilt
    void setUserGroups(Set<UserGroup> userGroups) {
        setNoAccess()
        this.userGroups = userGroups.toSet()
    }

    void setNoAccess() {
        applicationPermittedRoles = [] as Set
        securableResourceGroupRoles = []
        virtualSecurableResourceGroupRoles = [] as Set
    }

    @Override
    List<UUID> listReadableSecuredResourceIds(Class<? extends SecurableResource> securableResourceClass) {
        virtualSecurableResourceGroupRoles
            .findAll {it.domainType == securableResourceClass.simpleName}
            .collect {it.domainId}
            .toSet()
            .toList()
    }

    @Override
    boolean userCanReadResourceId(Class resourceClass, UUID id,
                                  Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return userCanReadSecuredResourceId(resourceClass, id)
        }
        return userCanReadSecuredResourceId(owningSecureResourceClass, owningSecureResourceId)
    }

    @Override
    boolean userCanCreateResourceId(Class resourceClass, UUID id,
                                    Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return userCanCreateSecuredResourceId(resourceClass, id)
        }
        if (Utils.parentClassIsAssignableFromChild(Annotation, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(Model, owningSecureResourceClass)) {
            return getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, REVIEWER_ROLE_NAME)
        }
        return getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, EDITOR_ROLE_NAME)
    }

    @Override
    boolean userCanEditResourceId(Class resourceClass, UUID id,
                                  Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return userCanEditSecuredResourceId(resourceClass, id)
        }
        return getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, EDITOR_ROLE_NAME)
    }

    @Override
    boolean userCanDeleteResourceId(Class resourceClass, UUID id,
                                    Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return userCanDeleteSecuredResourceId(resourceClass, id, false)
        }
        return getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, EDITOR_ROLE_NAME)
    }

    @Override
    boolean userCanReadSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        if (Utils.parentClassIsAssignableFromChild(UserGroup, securableResourceClass) && !id) {
            return hasApplicationLevelRole(CONTAINER_GROUP_ADMIN_ROLE_NAME)
        }
        if (Utils.parentClassIsAssignableFromChild(CatalogueUser, securableResourceClass) && !id) {
            return hasApplicationLevelRole(USER_ADMIN_ROLE_NAME)
        }
        hasAnyAccessToSecuredResource(securableResourceClass, id)
    }

    @Override
    boolean userCanCreateSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        userCanWriteSecuredResourceId(securableResourceClass, id, 'save')
    }

    @Override
    boolean userCanEditSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        userCanWriteSecuredResourceId(securableResourceClass, id, 'update')
    }

    @Override
    boolean userCanDeleteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, boolean permanent) {
        userCanWriteSecuredResourceId(securableResourceClass, id, permanent ? 'delete' : 'softDelete')
    }

    @Override
    List<String> userAvailableActions(Class resourceClass, UUID id) {
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return securedResourceUserAvailableActions(resourceClass, id)
        }
        throw new ApiNotYetImplementedException('USPM01', 'Obtain user actions for non-secured resource')
    }

    @Override
    List<String> userAvailableActions(String domainType, UUID id) {
        GrailsClass grailsClass = Utils.lookupGrailsDomain(grailsApplication, domainType)
        return userAvailableActions(grailsClass.clazz, id)
    }

    @Override
    List<String> userAvailableActions(Serializable resourceClass, UUID id) {
        if (resourceClass instanceof Class) {
            return userAvailableActions(resourceClass as Class, id)
        }
        if (resourceClass instanceof String) {
            return userAvailableActions(resourceClass as String, id)
        }
        throw new ApiInternalException('USPM02', "Unrecognised resourceClass type ${resourceClass.class}")
    }

    @Override
    boolean isApplicationAdministrator() {
        applicationPermittedRoles.any {it.name == APPLICATION_ADMIN_ROLE_NAME}
    }

    @Override
    boolean isAuthenticated() {
        user && user.emailAddress != UnloggedUser.instance.emailAddress
    }

    @Override
    boolean isPending() {
        user && user.pending
    }

    GroupRole getHighestApplicationLevelAccess() {
        applicationPermittedRoles.sort().first()
    }

    GroupRole findHighestAccessToSecurableResource(String securableResourceDomainType, UUID securableResourceId) {
        Set<VirtualSecurableResourceGroupRole> found = virtualSecurableResourceGroupRoles.findAll {
            it.domainType == securableResourceDomainType && it.domainId == securableResourceId
        }
        if (!found) return []
        found.sort().first().groupRole
    }

    GroupRole findHighestAssignedAccessToSecurableResource(String securableResourceDomainType, UUID securableResourceId) {
        List<SecurableResourceGroupRole> found = securableResourceGroupRoles.findAll {
            it.securableResourceDomainType == securableResourceDomainType && it.securableResourceId == securableResourceId
        }
        if (!found) return null
        found.collect {it.groupRole}.sort().first()
    }

    boolean hasUserAdminRights() {
        hasApplicationLevelRole(USER_ADMIN_ROLE_NAME)
    }

    boolean hasGroupAdminRights() {
        hasApplicationLevelRole(GROUP_ADMIN_ROLE_NAME)
    }

    private List<String> securedResourceUserAvailableActions(Class<? extends SecurableResource> securableResourceClass, UUID id) {

        if (Utils.parentClassIsAssignableFromChild(CatalogueUser, securableResourceClass)) {
            return getStandardActionsWithControlRole(securableResourceClass, id, USER_ADMIN_ROLE_NAME) ?: ['show']
        }

        if (Utils.parentClassIsAssignableFromChild(UserGroup, securableResourceClass)) {
            return getStandardActionsWithControlRole(securableResourceClass, id, GROUP_ADMIN_ROLE_NAME)
        }

        if (Utils.parentClassIsAssignableFromChild(Container, securableResourceClass)) {
            return getStandardActionsWithControlRole(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)
        }

        if (Utils.parentClassIsAssignableFromChild(Model, securableResourceClass)) {
            VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)
            if (role) {
                return role.isFinalisedModel() ? ['delete', 'softDelete', 'show', 'comment'] :
                       ['delete', 'softDelete', 'update', 'save', 'show', 'comment', 'editDescription']
            }
            role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
            if (role) {
                return role.isFinalisedModel() ? ['softDelete', 'show', 'comment'] :
                       ['softDelete', 'update', 'save', 'show', 'comment', 'editDescription']
            }
            role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, AUTHOR_ROLE_NAME)
            if (role) {
                return role.isFinalisedModel() ? ['show', 'comment'] : ['show', 'comment', 'editDescription']
            }
            if (getSpecificLevelAccessToSecuredResource(securableResourceClass, id, REVIEWER_ROLE_NAME)) {
                return ['show', 'comment']
            }
            if (getSpecificLevelAccessToSecuredResource(securableResourceClass, id, READER_ROLE_NAME)) {
                return ['show']
            }
        }
        log.warn('Attempt to gain available actions for unknown secured class {} id {} to {}', securableResourceClass.simpleName, id)
        []
    }

    @Override
    boolean userCanWriteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, String action) {

        if (Utils.parentClassIsAssignableFromChild(Container, securableResourceClass)) {
            // If no id then its a top level container and therefore anyone who's logged in can create
            if (!id) return isAuthenticated()
            if (action in ['save']) {
                // Editors can save new folders and models
                return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
            }
            return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)
        }

        if (Utils.parentClassIsAssignableFromChild(CatalogueUser, securableResourceClass)) {
            // User cannot delete themselves
            if (action in ['delete', 'softDelete'] && id == user.id) {
                return false
            }
            return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, USER_ADMIN_ROLE_NAME)
        }

        if (Utils.parentClassIsAssignableFromChild(UserGroup, securableResourceClass)) {
            switch (action) {
                case 'save':
                    return hasApplicationLevelRole(CONTAINER_GROUP_ADMIN_ROLE_NAME)
                default:
                    return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, GROUP_ADMIN_ROLE_NAME)
            }
        }

        if (Utils.parentClassIsAssignableFromChild(Model, securableResourceClass)) {
            switch (action) {
                case 'newDocumentationVersion':
                    VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
                    return role ? role.isFinalisedModel() : false
                case 'delete':
                    return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)
                case 'softDelete':
                    return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
                case 'update':
                    VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, AUTHOR_ROLE_NAME)
                    return role ? !role.isFinalisedModel() : false
                case 'save':
                    VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
                    return role ? !role.isFinalisedModel() : false
                default:
                    log.warn('Attempt to access secured class {} id {} to {}', securableResourceClass.simpleName, id, action)
                    return false
            }
        }

        log.warn('Attempt to access secured class {} id {} to {}', securableResourceClass.simpleName, id, action)
        false

    }

    private boolean hasAnyAccessToSecuredResource(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        if (id) {
            return virtualSecurableResourceGroupRoles.any {it.domainType == securableResourceClass.simpleName && it.domainId == id}
        }

        // No id means indexing endpoint
        // Users and Groups can be indexed with show actions by the bottom layer of application roles
        if (Utils.parentClassIsAssignableFromChild(UserGroup, securableResourceClass)) {
            return hasApplicationLevelRole(CONTAINER_GROUP_ADMIN_ROLE_NAME)
        }

        if (Utils.parentClassIsAssignableFromChild(CatalogueUser, securableResourceClass)) {
            return hasApplicationLevelRole(CONTAINER_GROUP_ADMIN_ROLE_NAME)
        }

        return virtualSecurableResourceGroupRoles.any {it.domainType == securableResourceClass.simpleName}
    }

    private VirtualSecurableResourceGroupRole getSpecificLevelAccessToSecuredResource(Class<? extends SecurableResource> securableResourceClass,
                                                                                      UUID id,
                                                                                      String roleName) {
        virtualSecurableResourceGroupRoles.find {
            it.domainType == securableResourceClass.simpleName &&
            it.domainId == id &&
            it.groupRole.name == roleName
        }
    }

    private boolean hasApplicationLevelRole(String rolename) {
        applicationPermittedRoles.any {it.name == rolename}
    }

    private List<String> getStandardActionsWithControlRole(Class<? extends SecurableResource> securableResourceClass, UUID id, String roleName) {
        if (getSpecificLevelAccessToSecuredResource(securableResourceClass, id, roleName)) {
            return id ? ['update', 'delete', 'show'] : ['save', 'update', 'delete', 'show']
        } else if (hasAnyAccessToSecuredResource(securableResourceClass, id)) {
            return ['show']
        } else return []
    }
}
