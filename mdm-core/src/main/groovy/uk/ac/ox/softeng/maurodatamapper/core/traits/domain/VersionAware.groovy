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
package uk.ac.ox.softeng.maurodatamapper.core.traits.domain


import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.databinding.BindUsing

import java.time.OffsetDateTime

trait VersionAware {

    String branchName
    Boolean finalised
    OffsetDateTime dateFinalised

    @BindUsing({ obj, source -> Version.from(source['modelVersion'] as String) })
    Version modelVersion

    @BindUsing({ obj, source -> Version.from(source['documentationVersion'] as String) })
    Version documentationVersion

    void initialiseVersioning() {
        setDocumentationVersion Version.from('1')
        finalised = false
        branchName = VersionAwareConstraints.DEFAULT_BRANCH_NAME
    }
}