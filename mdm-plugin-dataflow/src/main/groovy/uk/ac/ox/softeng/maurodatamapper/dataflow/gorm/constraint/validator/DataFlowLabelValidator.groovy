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
package uk.ac.ox.softeng.maurodatamapper.dataflow.gorm.constraint.validator

import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.UniqueStringValidator

/**
 * @since 19/04/2018
 */
class DataFlowLabelValidator extends UniqueStringValidator<DataFlow> {

    DataFlowLabelValidator(DataFlow object) {
        super(object)
    }

    @Override
    boolean objectParentIsNotSaved() {
        false //Always false as DataFlows are not in a DataModel collection
    }

    @Override
    boolean valueIsNotUnique(String value) {
        if (object.id) {
            return DataFlow.countByTargetAndLabelAndIdNotEqual(object.target, value, object.id) >= 1
        }
        return DataFlow.countByTargetAndLabel(object.target, value) >= 1
    }
}
