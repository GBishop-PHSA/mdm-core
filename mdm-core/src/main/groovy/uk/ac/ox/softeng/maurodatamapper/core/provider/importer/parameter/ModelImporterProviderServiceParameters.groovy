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
package uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportGroupConfig
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig

class ModelImporterProviderServiceParameters implements ImporterProviderServiceParameters {

    @ImportParameterConfig(
        displayName = 'Finalised',
        description = '''Whether the new model is to be marked as finalised.
Note that if the model is already finalised this will not be overridden.''',
        order = 0,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    Boolean finalised

    @ImportParameterConfig(
        displayName = 'Import as New Documentation Version',
        description = '''Should the Model/s be imported as new Documentation Version/s.
If selected then any models with the same name will be superseded and the imported models will be given the latest documentation version of the 
existing Models.
If not selected then the 'Model Name' field should be used to ensure the imported DataModel is uniquely named, otherwise you could get an error
.''',
        order = 0,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    Boolean importAsNewDocumentationVersion

    @ImportParameterConfig(
        displayName = 'Folder',
        description = 'The folder into which the Model/s should be imported.',
        order = 0,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    UUID folderId

    @ImportParameterConfig(
        optional = true,
        displayName = 'Model name',
        description = '''Label of Model, this will override any existing name provided in the imported data.
Note that if importing multiple models this will be ignored.''',
        order = 0,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    String dataModelName
}