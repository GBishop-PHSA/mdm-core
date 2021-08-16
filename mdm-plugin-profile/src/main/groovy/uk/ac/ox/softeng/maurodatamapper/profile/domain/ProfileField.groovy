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

class ProfileField implements Validateable {

    String fieldName
    String metadataPropertyName
    String description
    Integer maxMultiplicity
    Integer minMultiplicity
    List<String> allowedValues
    String regularExpression

    ProfileFieldDataType dataType

    String currentValue

    static constraints = {
        fieldName blank: false
        metadataPropertyName nullable: true, blank: false
        description nullable: true, blank: false
        regularExpression nullable: true, blank: false
        currentValue nullable: true, validator: {val, obj ->
            if (!val && (obj.minMultiplicity > 0)) return ['null.message', obj.fieldName]
            if (val) {
                if (obj.allowedValues && !(val in obj.allowedValues)) return ['not.inlist.message', obj.allowedValues, obj.fieldName]
                if (obj.regularExpression && !val.matches(obj.regularExpression)) return ['doesnt.match.message', obj.regularExpression, obj.fieldName]
                String typeError = obj.dataType.validateString(val)
                if (typeError) return ['typeMismatch', typeError, obj.fieldName]
            }
        }
    }

    void setDataType(ProfileFieldDataType type) {
        dataType = type
    }

    void setDataType(String type) {
        dataType = ProfileFieldDataType.findForLabel(type)
    }

    String getMetadataKeyForSaving(String sectionName) {
        metadataPropertyName ? metadataPropertyName : "${sectionName}/${fieldName}"
    }
}
