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

import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService

import grails.validation.ValidationException

import javax.transaction.Transactional

@Transactional
class UserGroupService {

    GroupRoleService groupRoleService

    UserGroup get(Serializable id) {
        UserGroup.get(id)
    }

    List<UserGroup> list(Map pagination) {
        pagination ? UserGroup.withFilter(pagination).list(pagination) : UserGroup.list()
    }

    Long count() {
        UserGroup.count()
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(UserGroup group) {
        if (!group) return
        List<CatalogueUser> members = []
        members += group.groupMembers
        members.each {
            it.removeFromGroups(group)
            it.save()
        }
        group.delete(flush: true)
    }

    UserGroup findByName(String name) {
        UserGroup.findByName(name)
    }

    UserGroup createNewGroup(CatalogueUser createdBy, String name, String description = null, List<CatalogueUser> members = []) {
        UserGroup group = new UserGroup(createdBy: createdBy.emailAddress, name: name, description: description)
        members.each {group.addToGroupMembers(it)}
        group.addToGroupMembers(createdBy)
    }

    UserGroup generateAndSaveNewGroup(CatalogueUser createdBy, String initialName, String description = null, List<CatalogueUser> members = []) {
        UserGroup userGroup = createNewGroup(createdBy, initialName, description, members)
        if (!userGroup.validate()) {
            if (userGroup.errors.getFieldError('name')) {
                return generateAndSaveNewGroup(createdBy, "${initialName}*", description, members)
            }
            throw new ValidationException('Could not create new UserGroup', userGroup.errors)
        }
        userGroup.save(flush: true, validate: false)
    }

    List<UserGroup> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(UserGroup)
        ids ? UserGroup.withFilter(pagination, UserGroup.byIdInList(ids)).list(pagination) : []
    }

    List<UserGroup> findAllByApplicationGroupRoleId(UUID groupRoleId, Map pagination = [:]) {
        findAllByApplicationGroupRole(groupRoleService.get(groupRoleId), pagination)
    }

    List<UserGroup> findAllByApplicationGroupRole(GroupRole groupRole, Map pagination = [:]) {
        if (!groupRole || !groupRole.isApplicationLevelRole()) return []
        UserGroup.byApplicationGroupRoleId(groupRole.id).list(pagination)
    }

    List<UserGroup> findAllBySecurableResourceAndGroupRoleId(String securableResourceDomainType, UUID securableResourceId, UUID groupRoleId,
                                                             Map pagination = [:]) {
        UserGroup.bySecurableResourceAndGroupRoleId(securableResourceDomainType, securableResourceId, groupRoleId).list(pagination)
    }
}