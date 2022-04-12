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
package uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValue
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter.ReferenceDataModelFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord

@Slf4j
class ReferenceDataCsvImporterService
    extends DataBindReferenceDataModelImporterProviderService<ReferenceDataModelFileImporterProviderServiceParameters> {

    AuthorityService authorityService

    @Override
    String getDisplayName() {
        'CSV Reference Data Importer'
    }

    @Override
    String getVersion() {
        '4.0'
    }

    @Override
    Boolean allowsExtraMetadataKeys() {
        false
    }

    @Override
    ReferenceDataModel importReferenceDataModel(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('JIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('JIS02', 'Cannot import empty content')

        def referenceDataElements = [:]
        ReferenceDataModel referenceDataModel = new ReferenceDataModel(createdBy: currentUser.emailAddress)
        referenceDataModel.authority = authorityService.getDefaultAuthority()

        ReferenceDataType stringDataType = new ReferencePrimitiveType(createdBy: currentUser.emailAddress, label: 'string')
        referenceDataModel.addToReferenceDataTypes(stringDataType)

        CSVFormat csvFormat = CSVFormat.newFormat((char) ',')
            .withQuote((char) '"')
            .withHeader()

        long start = System.currentTimeMillis()
        CSVParser parser = csvFormat.parse(
            new InputStreamReader(new ByteArrayInputStream(content), 'UTF8'))
        log.debug('Input parsed in {}', Utils.timeTaken(start))

        List headers = parser.getHeaderNames()
        headers.eachWithIndex {it, index ->
            ReferenceDataElement referenceDataElement = new ReferenceDataElement(referenceDataType: stringDataType, columnNumber: index, label: it, createdBy: currentUser.emailAddress)
            referenceDataModel.addToReferenceDataElements(referenceDataElement)
            referenceDataElements[it] = referenceDataElement
        }

        start = System.currentTimeMillis()
        int rowNumber = 1
        try {
            for (CSVRecord record : parser) {

                headers.each {
                    ReferenceDataValue referenceDataValue = new ReferenceDataValue(referenceDataElement: referenceDataElements[it], value: record.get(
                        it), rowNumber: rowNumber, createdBy: currentUser.emailAddress)

                    referenceDataModel.addToReferenceDataValues(referenceDataValue)
                }

                rowNumber++
                if (rowNumber % 1000 == 0) {
                    log.debug('rowNumber {}', rowNumber)
                }
            }
        } catch (Exception e) {
            throw new ApiInternalException('RDCIS01', 'Error at line ' + parser.getCurrentLineNumber(), e)
        }

        parser.close()
        log.debug('{} rows read in {}', rowNumber - 1, Utils.timeTaken(start))
        referenceDataModel
    }
}
