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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype

import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElementService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError
import spock.lang.Stepwise

@Slf4j
@Stepwise
class ReferenceDataTypeServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<ReferenceDataTypeService> {

    ReferenceDataModel referenceDataModel
    UUID id

    def setup() {
        log.debug('Setting up DataTypeServiceSpec Unit')
        mockArtefact(ReferenceDataElementService)
        mockArtefact(ReferenceDataTypeService)
        mockArtefact(ReferencePrimitiveTypeService)
        mockArtefact(ReferenceEnumerationTypeService)
        mockArtefact(ReferenceSummaryMetadataService)
        mockDomains(ReferenceDataModel, ReferenceDataType, ReferencePrimitiveType, ReferenceDataType, ReferenceEnumerationType, ReferenceEnumerationValue, ReferenceDataElement)

        referenceDataModel = new ReferenceDataModel(createdByUser: admin, label: 'Unit test model', folder: testFolder, authority: testAuthority)
        checkAndSave(referenceDataModel)

        ReferencePrimitiveType primitiveType = new ReferencePrimitiveType(createdByUser: editor, label: 'varchar')

        referenceDataModel.addToReferenceDataTypes(primitiveType)
        referenceDataModel.addToReferenceDataTypes(new ReferencePrimitiveType(createdByUser: admin, label: 'string'))
        referenceDataModel.addToReferenceDataTypes(new ReferencePrimitiveType(createdByUser: editor, label: 'integer'))

        ReferenceEnumerationType et1 = new ReferenceEnumerationType(createdByUser: editor, label: 'et1')
            .addToReferenceEnumerationValues(createdByUser: admin, key: 'key1', value: 'val1')
            .addToReferenceEnumerationValues(new ReferenceEnumerationValue(createdByUser: admin, key: 'key2', value: 'val2')
            )
        referenceDataModel.addToReferenceDataTypes(et1)
        referenceDataModel.addToReferenceDataTypes(new ReferenceEnumerationType(createdByUser: editor, label: 'moreet')
                                     .addToReferenceEnumerationValues(createdByUser: admin, key: 'key1', value: 'val1')
                                     .addToReferenceEnumerationValues(createdByUser: admin, key: 'key2', value: 'val2')
                                     .addToReferenceEnumerationValues(createdByUser: admin, key: 'key3', value: 'val3')
                                     .addToReferenceEnumerationValues(createdByUser: admin, key: 'key4', value: 'val4')
        )
        referenceDataModel.addToReferenceDataTypes(new ReferenceEnumerationType(createdByUser: admin, label: 'yesnounknown')
                                     .addToReferenceEnumerationValues(key: 'Y', value: 'Yes')
                                     .addToReferenceEnumerationValues(key: 'N', value: 'No')
                                     .addToReferenceEnumerationValues(key: 'U', value: 'Unknown'))

        

        checkAndSave(referenceDataModel)

        verifyBreadcrumbTrees()

        id = primitiveType.id
    }

    void "test get"() {

        expect:
        service.get(id) != null
    }

    void "test list"() {

        //referenceDataTypes are varchar, string, integer, et1, moreet, yesnounknown
        //Ordered by label: et1, integer, moreet, string, varchar, yesnounknown
        when:
        List<ReferenceDataType> dataTypeList = service.list(max: 2, offset: 2, sort: 'label')

        then:
        dataTypeList.size() == 2

        //At offset 2 we expect to see moreet, varchar
        and:
        dataTypeList[0].label == 'moreet'
        dataTypeList[0].domainType == 'ReferenceEnumerationType'

        and:
        dataTypeList[1].label == 'string'
        dataTypeList[1].domainType == 'ReferencePrimitiveType'
    }

    void "test count"() {
        expect:
        service.count() == 6
    }

    void "test delete ReferencePrimitiveType"() {
        given:
        ReferenceDataType pt = service.get(id)

        expect:
        service.count() == 6
        ReferenceDataType.findByLabel('varchar')

        when:
        service.delete(pt)

        then:
        ReferenceDataType.count() == 5

        and:
        !ReferenceDataType.findByLabel('varchar')      
    }
  

    void "test save"() {

        when:
        ReferenceDataType dataType = new ReferencePrimitiveType(createdByUser: reader2, label: 'saving test', referenceDataModel: referenceDataModel)
        service.save(dataType)

        then:
        dataType.id != null
    }

    void 'test copying primitive datatype'() {
        given:
        ReferenceDataType original = ReferencePrimitiveType.findByLabel('string')
        ReferenceDataModel copyModel = new ReferenceDataModel(createdByUser: admin, label: 'copy model', folder: testFolder, authority: testAuthority)

        expect:
        checkAndSave(copyModel)

        when:
        ReferenceDataType copy = service.copyReferenceDataType(copyModel, original, editor, userSecurityPolicyManager)

        then:
        checkAndSave(copyModel)

        when:
        original = ReferencePrimitiveType.findByLabel('string')
        copy = service.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.units == original.units

        and:
        copy.createdBy == editor.emailAddress
        copy.metadata?.size() == original.metadata?.size()
        copy.classifiers == original.classifiers

        and:
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }
    }

    void 'test copying enumeration datatype'() {
        given:
        ReferenceDataType original = ReferenceEnumerationType.findByLabel('yesnounknown')
        ReferenceDataModel copyModel = new ReferenceDataModel(createdByUser: admin, label: 'copy model', folder: testFolder, authority: testAuthority)

        expect:
        checkAndSave(copyModel)

        when:
        ReferenceDataType copy = service.copyReferenceDataType(copyModel, original, editor, userSecurityPolicyManager)

        then:
        checkAndSave(copyModel)

        when:
        original = ReferenceEnumerationType.findByLabel('yesnounknown')
        copy = service.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.referenceEnumerationValues.size() == original.referenceEnumerationValues.size()

        and:
        original.referenceEnumerationValues.every { o -> copy.referenceEnumerationValues.any { c -> c.key == o.key && c.value == o.value } }

        and:
        copy.createdBy == editor.emailAddress
        copy.metadata?.size() == original.metadata?.size()
        copy.classifiers == original.classifiers

        and:
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }
    }
}