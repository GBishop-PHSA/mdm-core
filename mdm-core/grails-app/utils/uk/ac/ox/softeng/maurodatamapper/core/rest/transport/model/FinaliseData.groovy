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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model

import grails.databinding.BindUsing
import grails.validation.Validateable
import uk.ac.ox.softeng.maurodatamapper.util.Version
import uk.ac.ox.softeng.maurodatamapper.util.VersionChangeType

/**
 * @since 02/02/2018
 */
class FinaliseData implements Validateable {

    List<String> supersededBy = []
    VersionChangeType versionChangeType

    @BindUsing({obj, source -> Version.from(source['version'] as String)})
    Version version


    static constraints = {
        supersededBy nullable: true
        versionChangeType nullable: true
        version nullable: true
        versionChangeType validator: { VersionChangeType value, FinaliseData obj ->
            if (!value && !obj.version) {
                return ['mustGiveVersionOrVersionChangeType']
            }
        }
        version validator: { Version value, FinaliseData obj ->
            if (!obj.versionChangeType && !value) {
                return ['mustGiveVersionOrVersionChangeType']
            }
        }
    }
}
