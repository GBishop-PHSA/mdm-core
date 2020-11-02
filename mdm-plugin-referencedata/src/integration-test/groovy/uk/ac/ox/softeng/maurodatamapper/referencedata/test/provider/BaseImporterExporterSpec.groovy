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
package uk.ac.ox.softeng.maurodatamapper.referencedata.test.provider

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.test.BaseReferenceDataModelIntegrationSpec

import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 17/09/2020
 */
@Integration
@Rollback
@Slf4j
abstract class BaseImporterExporterSpec extends BaseReferenceDataModelIntegrationSpec {

    @Shared
    Path resourcesPath

    @Shared
    UUID exampleReferenceDataModelId

    @Shared
    UUID secondExampleReferenceDataModelId

    abstract ImporterProviderService getImporterService()
    abstract ExporterProviderService getExporterService()
    abstract void validateExportedModel(String testName, String exportedModel)

    abstract String getImportType()

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', importType)
        assert getImporterService()
    }

    @Override
    void setupDomainData() {
        log.debug('Setting up ReferenceDataModelServiceSpec unit')

        exampleReferenceDataModelId = exampleReferenceDataModel.id
        secondExampleReferenceDataModelId = secondExampleReferenceDataModel.id
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}.${importType}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    String exportModel(UUID referenceDataModelId) {
        ByteArrayOutputStream byteArrayOutputStream = exporterService.exportDomain(admin, referenceDataModelId)
        new String(byteArrayOutputStream.toByteArray(), Charset.defaultCharset())
    }

    ReferenceDataModel importAndConfirm(byte[] bytes) {
        ReferenceDataModel imported = importerService.importReferenceDataModel(admin, bytes)

        assert imported
        imported.folder = testFolder
        log.info('Checking imported model')
        importerService.checkImport(admin, imported, false, false)
        check(imported)
        log.info('Saving imported model')
        assert referenceDataModelService.saveWithBatching(imported)
        sessionFactory.currentSession.flush()
        assert referenceDataModelService.count() == 3

        ReferenceDataModel referenceDataModel = referenceDataModelService.get(imported.id)

        log.info('Confirming imported model')

        confirmReferenceDataModel(referenceDataModel)
        referenceDataModel
    }


    void confirmReferenceDataModel(referenceDataModel) {
        assert referenceDataModel
        assert referenceDataModel.createdBy == admin.emailAddress
        assert referenceDataModel.breadcrumbTree
        assert referenceDataModel.breadcrumbTree.domainId == referenceDataModel.id
        assert referenceDataModel.breadcrumbTree.label == referenceDataModel.label
    }

    void 'RDM01: test that trying to export when specifying a null referenceDataModelId fails with an exception'() {
        given:
        setupData()

        when:
        exportModel(null)

        then:
        ApiInternalException exception = thrown(ApiInternalException)
        exception.errorCode == 'RDMEP01'
    }

    void 'RDM02: test exporting and reimporting the bootstrapped example reference data model'() {
        given:
        setupData()

        expect:
        ReferenceDataModel.count() == 2

        when:
        String exported = exportModel(exampleReferenceDataModelId)

        then:
        validateExportedModel('bootstrapExample', exported.replace(/Test Authority/, 'Mauro Data Mapper'))

        //note: importing does not actually save
        when:
        ReferenceDataModel imported = importerService.importReferenceDataModel(admin, exported.bytes)

        then:
        assert imported

        and:
        imported.classifiers
        imported.classifiers.size() == 1
        imported.classifiers[0].label == 'test classifier simple'

        when:
        imported.folder = testFolder
        ObjectDiff diff = referenceDataModelService.diff(referenceDataModelService.get(exampleReferenceDataModelId), imported)

        then:
        diff.objectsAreIdentical()
    }


    void 'RDM03: test empty data import'() {
        given:
        setupData()

        when:
        String data = ''
        importerService.importReferenceDataModel(admin, data.bytes)

        then:
        thrown(ApiBadRequestException)
    }

    void 'RDM04: test simple data import and export'() {
        given:
        setupData()

        expect:
        ReferenceDataModel.count() == 2

        when:
        String data = new String(loadTestFile('importSimple'))
        log.debug("importing ${data}")
        and:
        ReferenceDataModel rdm = importAndConfirm(data.bytes)

        then:
        rdm.label == 'importSimple Reference Data Model'
        rdm.author == 'Test Author'
        rdm.organisation == 'Test Organisation'
        rdm.documentationVersion.toString() == '1.0.0'
        rdm.finalised == false
        rdm.authority.label == 'Mauro Data Mapper'
        rdm.authority.url == 'http://localhost'
        !rdm.aliases
        !rdm.annotations
        !rdm.metadata
        !rdm.classifiers
        !rdm.referenceDataTypes
        !rdm.referenceDataElements
        !rdm.referenceDataValues


        when:
        String exported = exportModel(rdm.id)

        then:
        validateExportedModel('importSimple', exported)
    }

    void 'RDM05: test simple data with aliases import and export'() {
        given:
        setupData()

        expect:
        ReferenceDataModel.count() == 2

        when:
        String data = new String(loadTestFile('importSimpleWithAliases'))
        log.debug("importing ${data}")
        and:
        ReferenceDataModel rdm = importAndConfirm(data.bytes)

        then:
        rdm.label == 'importSimple Reference Data Model'
        rdm.author == 'Test Author'
        rdm.organisation == 'Test Organisation'
        rdm.documentationVersion.toString() == '1.0.0'
        rdm.finalised == false
        rdm.authority.label == 'Mauro Data Mapper'
        rdm.authority.url == 'http://localhost'
        rdm.aliases.size() == 2
        'Alias 1' in rdm.aliases
        'Alias 2' in rdm.aliases
        !rdm.annotations
        !rdm.metadata
        !rdm.classifiers
        !rdm.referenceDataTypes
        !rdm.referenceDataElements
        !rdm.referenceDataValues


        when:
        String exported = exportModel(rdm.id)

        then:
        validateExportedModel('importSimpleWithAliases', exported)
    }    

    /*void 'test simple data with aliases'() {
        given:
        setupData()
        def testName = 'terminologyWithAliases'

        expect:
        Terminology.count() == 2

        when:
        String data = new String(loadTestFile(testName))
        log.debug("importing ${data}")
        and:
        Terminology tm = importAndConfirm(data.bytes)

        then:
        tm.label == 'Simple Test Terminology Import'
        tm.author == 'Test Author'
        tm.organisation == 'Test Organisation'
        tm.documentationVersion.toString() == '1.0.0'
        tm.finalised == false
        tm.authority.label == 'Mauro Data Mapper'
        tm.authority.url == 'http://localhost'
        tm.aliases.size() == 2
        'Alias 1' in tm.aliases
        'Alias 2' in tm.aliases
        !tm.annotations
        !tm.metadata
        !tm.classifiers
        !tm.terms
        !tm.termRelationshipTypes

        when:
        String exported = exportModel(tm.id)

        then:
        validateExportedModel(testName, exported)
    }

    void 'test simple data with annotations'() {
        given:
        setupData()
        def testName = 'terminologyWithAnnotations'

        expect:
        Terminology.count() == 2

        when:
        String data = new String(loadTestFile(testName))
        log.debug("importing ${data}")
        and:
        Terminology tm = importAndConfirm(data.bytes)

        then:
        tm.label == 'Simple Test Terminology Import'
        tm.author == 'Test Author'
        tm.organisation == 'Test Organisation'
        tm.documentationVersion.toString() == '1.0.0'
        tm.finalised == false
        tm.authority.label == 'Mauro Data Mapper'
        tm.authority.url == 'http://localhost'
        tm.annotations.size() == 1
        !tm.metadata
        !tm.classifiers
        !tm.terms
        !tm.termRelationshipTypes

        when:
        Annotation ann = tm.annotations[0]

        then:
        ann.description == 'test annotation 1 description'
        ann.label == 'test annotation 1 label'        

        when:
        String exported = exportModel(tm.id)

        then:
        validateExportedModel(testName, exported)
    }

    void 'test simple data with metadata'() {
        given:
        setupData()
        def testName = 'terminologyWithMetadata'

        expect:
        Terminology.count() == 2

        when:
        String data = new String(loadTestFile(testName))
        log.debug("importing ${data}")
        and:
        Terminology tm = importAndConfirm(data.bytes)

        then:
        tm.label == 'Simple Test Terminology Import'
        tm.author == 'Test Author'
        tm.organisation == 'Test Organisation'
        tm.documentationVersion.toString() == '1.0.0'
        tm.finalised == false
        tm.authority.label == 'Mauro Data Mapper'
        tm.authority.url == 'http://localhost'
        !tm.annotations
        tm.metadata.size() == 3
        !tm.classifiers
        !tm.terms
        !tm.termRelationshipTypes      

        when:
        String exported = exportModel(tm.id)

        then:
        validateExportedModel(testName, exported)
    }

    void 'test complex'() {
        given:
        setupData()
        def testName = 'terminologyComplex'

        expect:
        Terminology.count() == 2

        when:
        String data = new String(loadTestFile(testName))
        log.debug("importing ${data}")
        and:
        Terminology tm = importAndConfirm(data.bytes)

        then:
        tm.label == 'Complex Test Terminology Import'
        tm.author == 'Test Author'
        tm.organisation == 'Test Organisation'
        tm.documentationVersion.toString() == '1.0.0'
        tm.finalised == false
        tm.authority.label == 'Mauro Data Mapper'
        tm.authority.url == 'http://localhost'     
        tm.annotations.size() == 2
        tm.metadata.size() == 3
        tm.classifiers.size() == 2
        tm.termRelationshipTypes.size() == 4
        tm.terms.size() == 101

        and:
        def i = 0
        for (i = 0; i <= 100; i++) {
            tm.terms.any {it.code == "CTT${i}" && it.definition == "Complex Test Term ${i}"}
        }

    }     */
}
