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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.JsonExporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.JsonImporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.test.provider.BaseImporterExporterSpec
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import com.google.common.base.CaseFormat
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.junit.Assert

import java.nio.file.Files
import java.nio.file.Path

/**
 * @since 17/09/2020
 */
@Integration
@Rollback
@Slf4j
class JsonImporterExporterServiceSpec extends BaseImporterExporterSpec implements JsonComparer {

    JsonImporterService jsonImporterService
    JsonExporterService jsonExporterService

    @Override
    String getImportType() {
        'json'
    }

    @Override
    JsonImporterService getImporterService() {
        jsonImporterService
    }

    @Override
    JsonExporterService getExporterService() {
        jsonExporterService
    }

    @Override
    void validateExportedModel(String testName, String exportedModel) {
        assert exportedModel, 'There must be an exported model string'

        Path expectedPath = resourcesPath.resolve("${CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, testName)}.${importType}")
        if (!Files.exists(expectedPath)) {
            Files.write(expectedPath, exportedModel.bytes)
            Assert.fail("Expected export file ${expectedPath} does not exist")
        }

        String expectedJson = replaceContentWithMatchers(Files.readString(expectedPath)).replace(/Test Authority/, 'Mauro Data Mapper')
        verifyJson(expectedJson, exportedModel)
    }


    void 'test that trying to import multiple terminologies fails'() {
        given:
        setupData()

        expect:
        !importerService.canImportMultipleDomains()

        when:
        importerService.importTerminologies(admin, loadTestFile('simpleTerminology'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message.contains('cannot import multiple Terminologies')
    }

}