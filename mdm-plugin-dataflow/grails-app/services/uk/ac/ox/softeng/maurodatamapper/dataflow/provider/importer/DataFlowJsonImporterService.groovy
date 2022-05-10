/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer.JsonImportMapping
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer.parameter.DataFlowFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.util.logging.Slf4j

@Slf4j
class DataFlowJsonImporterService extends DataBindDataFlowImporterProviderService<DataFlowFileImporterProviderServiceParameters>
    implements JsonImportMapping {

    @Override
    String getDisplayName() {
        'JSON DataFlow Importer'
    }

    @Override
    String getVersion() {
        '4.0'
    }

    @Override
    Boolean handlesContentType(String contentType) {
        contentType.toLowerCase() == 'application/mauro.dataflow+json'
    }

    @Override
    DataFlow importDataFlow(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('JIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('JIS02', 'Cannot import empty content')

        def result = slurpAndClean(content)
        Map dataFlow = result.dataFlow
        if (!dataFlow) throw new ApiBadRequestException('JIS03', 'Cannot import JSON as dataFlow is not present')

        log.debug('Importing DataFlow map')
        bindMapToDataFlow currentUser, new HashMap(dataFlow)
    }
}
