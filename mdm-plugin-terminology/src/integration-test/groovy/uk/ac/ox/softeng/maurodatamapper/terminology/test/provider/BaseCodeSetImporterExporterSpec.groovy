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
package uk.ac.ox.softeng.maurodatamapper.terminology.test.provider

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
//import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.test.BaseCodeSetIntegrationSpec

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
 * @since 05/10/2020
 */
@Integration
@Rollback
@Slf4j
abstract class BaseCodeSetImporterExporterSpec extends BaseCodeSetIntegrationSpec {

    @Shared
    Path resourcesPath

    //@Shared
    //UUID complexTerminologyId

    @Shared
    UUID simpleTerminologyId

    @Shared
    UUID simpleCodeSetId    

    //Note: naming of these methods seems to be important. When using method names getImporterService and getExporterService
    //the following exception was raised:
    //"Failed to convert property value of type 'uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.JsonExporterService'
    // to required type 'uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.CodeSetJsonExporterService' for property 'jsonExporterService';"
    abstract ImporterProviderService getCodeSetImporterService()
    abstract ExporterProviderService getCodeSetExporterService()
    abstract void validateExportedModel(String testName, String exportedModel)

    abstract String getImportType()

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', importType)
        assert getCodeSetImporterService()
    }

    @Override
    void setupDomainData() {
        log.debug('Setting up CodeSetServiceSpec unit')

        //complexTerminologyId = complexTerminology.id
        simpleTerminologyId = simpleTerminology.id
        simpleCodeSetId = simpleCodeSet.id
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}.${importType}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    String exportModel(UUID codeSetId) {
        ByteArrayOutputStream byteArrayOutputStream = codeSetExporterService.exportDomain(admin, codeSetId)
        new String(byteArrayOutputStream.toByteArray(), Charset.defaultCharset())
    }

    CodeSet importAndConfirm(byte[] bytes) {
        CodeSet imported = codeSetimporterService.importCodeSet(admin, bytes)

        assert imported
        imported.folder = testFolder
        log.info('Checking imported model')
        importerService.checkImport(admin, imported, false, false)
        check(imported)
        log.info('Saving imported model')
        assert codeSetService.saveWithBatching(imported)
        sessionFactory.currentSession.flush()
        assert codeSetService.count() == 2

        CodeSet codeSet = codeSetService.get(imported.id)

        log.info('Confirming imported model')

        confirmCodeSet(codeSet)
        codeSet
    }


    void confirmCodeSet(codeSet) {
        assert codeSet
        assert codeSet.createdBy == admin.emailAddress
        assert codeSet.breadcrumbTree
        assert codeSet.breadcrumbTree.domainId == codeSet.id
        assert codeSet.breadcrumbTree.label == codeSet.label
    }

    void 'test that trying to export when specifying a null codeSetId fails with an exception'() {
        given:
        setupData()

        when:
        exportModel(null)

        then:
        ApiInternalException exception = thrown(ApiInternalException)
        exception.errorCode == 'CSEP01'
    }

    void 'test exporting and reimporting the simple bootstrapped codeset'() {
        given:
        setupData()

        expect:
        CodeSet.count() == 1

        when:
        String exported = exportModel(simpleCodeSetId)

        then:
        validateExportedModel('bootstrappedSimpleCodeSet', exported.replace(/Test Authority/, 'Mauro Data Mapper'))

        //note: importing does not actually save
        when:
        CodeSet imported = codeSetImporterService.importCodeSet(admin, exported.bytes)

        then:
        assert imported

        and:
        imported.classifiers
        imported.classifiers.size() == 1
        imported.classifiers[0].label == 'test classifier'
        //imported.termsPaths.size() == 2
        //log.debug(exported)
        //log.debug(imported.dateFinalised.toString())

        when:
        imported.folder = testFolder
        log.debug(imported.toString())
        ObjectDiff diff = codeSetService.diff(codeSetService.get(simpleCodeSetId), imported)

        then:
        diff.objectsAreIdentical()
    }

    /*void 'test exporting and reimporting the complex bootstrapped terminology'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        String exported = exportModel(complexTerminologyId)

        then:
        validateExportedModel('complex', exported.replace(/Test Authority/, 'Mauro Data Mapper'))

        //note: importing does not actually save
        when:
        Terminology imported = importerService.importTerminology(admin, exported.bytes)

        then:
        assert imported

        and:
        imported.classifiers
        imported.classifiers.size() == 2

        when:
        imported.folder = testFolder
        ObjectDiff diff = terminologyService.diff(terminologyService.get(complexTerminologyId), imported)

        then:
        diff.objectsAreIdentical()
    }

    void 'test empty data import'() {
        given:
        setupData()

        when:
        String data = ''
        importerService.importTerminology(admin, data.bytes)

        then:
        thrown(ApiBadRequestException)
    }

    void 'test simple data import'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        String data = new String(loadTestFile('terminologySimple'))
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
        !tm.metadata
        !tm.classifiers
        !tm.terms
        !tm.termRelationshipTypes

        when:
        String exported = exportModel(tm.id)

        then:
        validateExportedModel('terminologySimple', exported)

    }

    void 'test simple data with aliases'() {
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
