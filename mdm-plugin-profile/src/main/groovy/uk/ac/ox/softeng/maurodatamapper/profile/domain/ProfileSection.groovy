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
package uk.ac.ox.softeng.maurodatamapper.profile.domain


import grails.validation.Validateable

class ProfileSection implements Cloneable, Validateable {

    String sectionName
    String sectionDescription
    List<ProfileField> fields = []

    static constraints = {
        sectionName blank: false
        sectionDescription nullable: true, blank: false
        fields minSize: 1
    }

    @Override
    boolean validate() {
        validate null, null, null
        fields.eachWithIndex {field, i ->
            field.validate()
            if (field.hasErrors()) {
                field.errors.fieldErrors.each {err ->
                    this.errors.rejectValue("fields[$i].${err.field}", err.code, err.arguments, err.defaultMessage)
                }
            }
        }
        !hasErrors()
    }
}
