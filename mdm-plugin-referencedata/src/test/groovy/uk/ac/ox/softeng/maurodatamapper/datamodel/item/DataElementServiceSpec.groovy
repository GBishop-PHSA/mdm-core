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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j
import spock.lang.Stepwise

@Slf4j
@Stepwise
class DataElementServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<DataElementService> {

    UUID simpleId
    UUID contentId
    UUID childId
    UUID dataModelId
    UUID elementId
    UUID element2Id
    UUID id

    DataModel dataModel

    def setup() {
        log.debug('Setting up DataElementServiceSpec Unit')
        mockArtefact(DataTypeService)
        mockArtefact(SummaryMetadataService)
        mockDomains(DataModel, DataClass, DataType, PrimitiveType, ReferenceType, EnumerationType, EnumerationValue, DataElement)

        dataModel = new DataModel(createdByUser: admin, label: 'Unit test model', folder: testFolder, authority: testAuthority)
        checkAndSave(dataModel)

        dataModel.addToDataTypes(new PrimitiveType(createdByUser: admin, label: 'string'))
        dataModel.addToDataTypes(new PrimitiveType(createdByUser: editor, label: 'integer'))

        DataClass simple = new DataClass(createdByUser: admin, label: 'dc1')
        DataElement element = new DataElement(createdByUser: admin, label: 'ele1', dataType: dataModel.findDataTypeByLabel('string'))
        simple.addToDataElements(element)
        dataModel.addToDataClasses(simple)

        DataClass content = new DataClass(createdByUser: editor, label: 'content', description: 'A dataclass with elements')
        content.addToDataElements(createdByUser: editor, label: 'ele1', dataType: dataModel.findDataTypeByLabel('string'))
        content.addToDataElements(createdByUser: reader1, label: 'element2', description: 'another',
                                  dataType: dataModel.findDataTypeByLabel('integer'))
        content.addToDataElements(createdByUser: reader1, label: 'element3', dataType: dataModel.findDataTypeByLabel('integer'),
                                  maxMultiplicity: 1, minMultiplicity: 0)
        DataClass child = new DataClass(createdByUser: editor, label: 'child')

        DataElement el2 = new DataElement(createdByUser: editor, label: 'another', minMultiplicity: 1, maxMultiplicity: 1,
                                          dataType: dataModel.findDataTypeByLabel('integer'))

        child.addToDataElements(el2)
        content.addToDataClasses(child)
        dataModel.addToDataClasses(content)

        checkAndSave(dataModel)

        verifyBreadcrumbTrees()

        elementId = element.id
        childId = child.id
        contentId = content.id
        simpleId = simple.id
        dataModelId = dataModel.id
        element2Id = el2.id
        id = element.id
    }

    void "test get"() {


        expect:
        service.get(id) != null
    }

    void "test list"() {

        when:
        List<DataElement> dataElementList = service.list(max: 2, offset: 2)

        then:
        dataElementList.size() == 2

        and:
        dataElementList[0].label == 'element2'
        dataElementList[0].dataClassId == DataClass.findByLabel('content').id
        dataElementList[0].minMultiplicity == null
        dataElementList[0].maxMultiplicity == null
        dataElementList[0].dataTypeId == DataType.findByLabel('integer').id

        and:
        dataElementList[1].label == 'element3'
        dataElementList[1].dataClassId == DataClass.findByLabel('content').id
        dataElementList[1].minMultiplicity == 0
        dataElementList[1].maxMultiplicity == 1
        dataElementList[1].dataTypeId == DataType.findByLabel('integer').id
    }

    void "test count"() {

        expect:
        service.count() == 5
    }

    void "test delete"() {

        expect:
        service.count() == 5

        when:
        service.delete(id)

        then:
        service.count() == 4
    }

    void "test save"() {

        when:
        DataElement dataElement = new DataElement(createdByUser: reader2, label: 'saving test', dataClass: DataClass.get(contentId),
                                                  dataType: dataModel.findDataTypeByLabel('string'))
        service.save(dataElement)

        then:
        dataElement.id != null

        when:
        DataElement saved = service.get(dataElement.id)

        then:
        saved.breadcrumbTree
        saved.breadcrumbTree.domainId == saved.id
        saved.breadcrumbTree.parent.domainId == contentId
    }


    void 'test copying DataElement'() {
        given:
        DataElement original = service.get(id)
        DataClass copyClass = new DataClass(label: 'copy', createdByUser: editor)
        dataModel.addToDataClasses(copyClass)

        expect:
        checkAndSave(dataModel)

        when:
        DataElement copy = service.copyDataElement(dataModel, original, editor, userSecurityPolicyManager)
        copyClass.addToDataElements(copy)

        then:
        checkAndSave(dataModel)

        when:
        original = service.get(id)
        copy = service.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.minMultiplicity == original.minMultiplicity
        copy.maxMultiplicity == original.maxMultiplicity

        and:
        copy.createdBy == editor.emailAddress
        copy.metadata?.size() == original.metadata?.size()
        copy.classifiers == original.classifiers

        and:
        copy.dataType.label == original.dataType.label

        and:
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }
    }

    void 'test copying DataElement with metadata and classifiers'() {
        given:
        DataElement original = service.get(id)
        DataClass copyClass = new DataClass(label: 'copy', createdByUser: editor)
        dataModel.addToDataClasses(copyClass)

        expect:
        checkAndSave(dataModel)

        when:
        DataElement copy = service.copyDataElement(dataModel, original, editor, userSecurityPolicyManager)
        copyClass.addToDataElements(copy)

        then:
        checkAndSave(copy)

        when:
        original = service.get(id)
        copy = service.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.minMultiplicity == original.minMultiplicity
        copy.maxMultiplicity == original.maxMultiplicity

        and:
        copy.metadata.every { md ->
            original.metadata.any { md.namespace == it.namespace && md.key == it.key && md.value == it.value }
        }

        and:
        !copy.classifiers
        !original.classifiers

        and:
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }

    }

    void 'test copying DataElement with datatype not present'() {
        given:
        DataElement original = service.get(id)
        DataModel copyModel = new DataModel(createdByUser: admin, label: 'copy model', folder: testFolder, authority: testAuthority)
        DataClass copyClass = new DataClass(label: 'copy', createdByUser: editor)
        copyModel.addToDataClasses(copyClass)

        expect:
        checkAndSave(copyModel)

        when:
        DataElement copy = service.copyDataElement(copyModel, original, editor, userSecurityPolicyManager)
        copyClass.addToDataElements(copy)

        then:
        checkAndSave(copyModel)

        when:
        original = service.get(id)
        copy = service.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.minMultiplicity == original.minMultiplicity
        copy.maxMultiplicity == original.maxMultiplicity

        and:
        copy.createdBy == editor.emailAddress
        copy.metadata?.size() == original.metadata?.size()
        copy.classifiers == original.classifiers

        and:
        copy.dataType.label == original.dataType.label

        and:
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }
    }
}
