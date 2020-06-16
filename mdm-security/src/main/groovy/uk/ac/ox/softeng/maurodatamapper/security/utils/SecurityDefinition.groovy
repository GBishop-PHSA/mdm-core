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
package uk.ac.ox.softeng.maurodatamapper.security.utils


import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole

trait SecurityDefinition {

    CatalogueUser admin
    CatalogueUser editor
    CatalogueUser pending

    CatalogueUser containerAdmin
    CatalogueUser author
    CatalogueUser reviewer
    CatalogueUser reader
    CatalogueUser authenticated

    UserGroup admins
    UserGroup editors
    UserGroup readers

    Map<String, String> userEmailAddresses = [
        admin          : 'admin@maurodatamapper.com',

        unitTest       : 'unit-test@test.com',
        integrationTest: 'integration-test@test.com',
        functionalTest : 'functional-test@test.com',
        development    : 'development@test.com',

        pending        : 'pending@test.com',

        userAdmin      : 'user_admin@test.com',
        groupAdmin     : 'group_admin@test.com',

        containerAdmin : 'container_admin@test.com',
        editor         : 'editor@test.com',
        author         : 'author@test.com',
        reviewer       : 'reviewer@test.com',
        reader         : 'reader@test.com',
        authenticated  : 'authenticated@test.com',
        authenticated2 : 'authenticated2@test.com'
    ]

    void createModernSecurityUsers(String creatorKey) {
        admin = new CatalogueUser(emailAddress: userEmailAddresses.admin,
                                  firstName: 'Admin',
                                  lastName: 'User',
                                  organisation: 'Oxford BRC Informatics',
                                  jobTitle: 'God',
                                  createdBy: userEmailAddresses[creatorKey])
        admin.encryptAndSetPassword('password')
        containerAdmin = new CatalogueUser(emailAddress: userEmailAddresses.containerAdmin,
                                           firstName: 'containerAdmin', lastName: 'User',
                                           createdBy: userEmailAddresses[creatorKey])
        containerAdmin.encryptAndSetPassword('password')
        editor = new CatalogueUser(emailAddress: userEmailAddresses.editor,
                                   firstName: 'editor', lastName: 'User',
                                   createdBy: userEmailAddresses[creatorKey])
        editor.encryptAndSetPassword('password')
        pending = new CatalogueUser(emailAddress: userEmailAddresses.pending,
                                    firstName: 'pending', lastName: 'User',
                                    createdBy: userEmailAddresses[creatorKey],
                                    pending: true,
                                    organisation: 'Oxford', jobTitle: 'tester')
        pending.encryptAndSetPassword('test password')

        author = new CatalogueUser(emailAddress: userEmailAddresses.author,
                                   firstName: 'author', lastName: 'User',
                                   createdBy: userEmailAddresses[creatorKey])
        reviewer = new CatalogueUser(emailAddress: userEmailAddresses.reviewer,
                                     firstName: 'reviewer', lastName: 'User',
                                     createdBy: userEmailAddresses[creatorKey])
        reader = new CatalogueUser(emailAddress: userEmailAddresses.reader,
                                   firstName: 'reader', lastName: 'User',
                                   createdBy: userEmailAddresses[creatorKey])
        authenticated = new CatalogueUser(emailAddress: userEmailAddresses.authenticated,
                                          firstName: 'authenticated', lastName: 'User',
                                          createdBy: userEmailAddresses[creatorKey])
    }

    void createBasicGroups(String creatorKey) {
        admins = new UserGroup(createdBy: userEmailAddresses[creatorKey],
                               name: 'administrators', applicationGroupRole: GroupRole.findByName(GroupRole.APPLICATION_ADMIN_ROLE_NAME))
            .addToGroupMembers(admin)
        editors = new UserGroup(createdBy: userEmailAddresses[creatorKey],
                                name: 'editors')
            .addToGroupMembers(containerAdmin)
            .addToGroupMembers(editor)
        readers = new UserGroup(createdBy: userEmailAddresses[creatorKey],
                                name: 'readers')
            .addToGroupMembers(author)
            .addToGroupMembers(reviewer)
            .addToGroupMembers(reader)
    }


    @Deprecated
    User getReader1() {
        throw new IllegalAccessException('Reader1 user is not available')
    }

    @Deprecated
    User getReader2() {
        throw new IllegalAccessException('Reader2 user is not available')
    }


    @Deprecated
    User getReader3() {
        throw new IllegalAccessException('Reader3 user is not available')
    }

    @Deprecated
    User getReader4() {
        throw new IllegalAccessException('Reader4 user is not available')
    }
}