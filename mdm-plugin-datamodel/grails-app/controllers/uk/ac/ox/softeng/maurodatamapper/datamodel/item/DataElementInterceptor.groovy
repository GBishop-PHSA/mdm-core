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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.controller.DataModelSecuredInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class DataElementInterceptor extends DataModelSecuredInterceptor {

    @Override
    Class getModelItemClass() {
        DataElement
    }

    @Override
    void checkIds() {
        super.checkIds()
        Utils.toUuid(params, 'dataClassId')
        Utils.toUuid(params, 'otherDataClassId')
        Utils.toUuid(params, 'otherDataElementId')
    }

    boolean before() {
        performChecks()

        if (actionName == 'suggestLinks') {
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, params.otherDataModelId)) {
                return notFound(DataModel, params.dataModelId)
            }
            if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, params.otherDataModelId)) {
                return notFound(DataModel, params.otherDataModelId)
            }
            return true
        }

        if (actionName == 'copyDataElement') {
            return canEditDataModelAndReadOtherDataModel()
        }

        checkStandardActions()
    }
}