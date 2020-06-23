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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel.facet.summarymetadata

import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.time.OffsetDateTime
import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED

/**
 * <pre>
 * Controller: summaryMetadataReport
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports  | Action: save
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports  | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}| Action:delete
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}| Action:update
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${summaryMetadataId}/summaryMetadataReports/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReportController
 */
@Slf4j
@Integration
class SummaryMetadataReportFunctionalSpec extends UserAccessFunctionalSpec {

    static final OffsetDateTime dateTime = OffsetDateTime.now()
    static final OffsetDateTimeConverter offsetDateTimeConverter = new OffsetDateTimeConverter()

    @Override
    String getResourcePath() {
        "dataModels/${getComplexDataModelId()}/summaryMetadata/${getSummaryMetadataId()}/summaryMetadataReports"
    }

    @Override
    String getEditsFullPath(String id) {
        "dataModels/${getComplexDataModelId()}"
    }

    @Transactional
    String getSummaryMetadataId() {
        SummaryMetadata.findByLabel('Functional Test Summary Metadata').id.toString()
    }

    @OnceBefore
    @Transactional
    def setupSummaryMetadata() {
        log.debug('Check and setup summary metadata')
        DataModel dataModel = DataModel.findByLabel(BootstrapModels.COMPLEX_DATAMODEL_NAME)
        new SummaryMetadata(summaryMetadataType: SummaryMetadataType.NUMBER,
                            label: 'Functional Test Summary Metadata',
                            catalogueItem: dataModel,
                            createdBy: userEmailAddresses.functionalTest).save(flush: true)
    }

    @Transactional
    @Override
    def cleanupSpec() {
        log.info('Removing functional test summary metadata reports')
        SummaryMetadata.findByLabel('Functional Test Summary Metadata')?.delete(flush: true)
    }

    @Transactional
    String getComplexDataModelId() {
        DataModel.findByLabel('Complex Test DataModel').id.toString()
    }

    @Override
    Boolean readerPermissionIsInherited() {
        true
    }

    @Override
    void verifyL03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexDataModelId()
    }

    @Override
    void verifyL03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexDataModelId()
    }

    @Override
    void verifyL03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexDataModelId()
    }

    @Override
    void verifyN03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexDataModelId()
    }

    @Override
    void verifyN03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexDataModelId()
    }

    @Override
    void verifyN03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getComplexDataModelId()
    }

    @Override
    void verifyR04UnknownIdResponse(HttpResponse<Map> response, String id) {
        verifyForbidden response
    }

    void verifySameValidDataCreationResponse() {
        verifyResponse CREATED, response
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[Summary Metadata Report:.+?] added to component \[.+?]/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[Summary Metadata Report:.+?] changed properties \[reportDate]/
    }

    @Override
    Map getValidJson() {
        [
            reportValue: 'Some interesting report',
            reportDate : offsetDateTimeConverter.toString(dateTime)
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            reportDate: null
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            reportDate: offsetDateTimeConverter.toString(dateTime.plusDays(1))
        ]
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "reportDate": "${json-unit.matches:offsetDateTime}",
  "reportValue": "Some interesting report"
}'''
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 0,
  "items": []
}'''
    }
}