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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.CatalogueItemAwareServiceSpec

import grails.testing.services.ServiceUnitTest

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

class AnnotationServiceSpec extends CatalogueItemAwareServiceSpec<Annotation, AnnotationService> implements ServiceUnitTest<AnnotationService> {

    UUID id
    Annotation parent
    Annotation nested

    def setup() {
        mockDomains(Folder, BasicModel, Edit, Annotation, Authority)
        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                    authority: testAuthority)

        basicModel.addToAnnotations(createdBy: admin.emailAddress, label: 'annotation 1')
        parent = new Annotation(createdBy: editor.emailAddress, label: 'parent annotation', description: 'the parent')
        parent.addToChildAnnotations(createdBy: reader1.emailAddress, description: 'reader annotation')

        nested = new Annotation(createdBy: reader1.emailAddress, description: 'nestedparent')
        nested.addToChildAnnotations(new Annotation(createdBy: editor.emailAddress, description: 'editor annotation'))
        parent.addToChildAnnotations(nested)
        basicModel.addToAnnotations(parent)

        checkAndSave basicModel

        id = nested.id

        ModelService basicModelService = Stub() {
            get(_) >> basicModel
            getModelClass() >> BasicModel
            handles('BasicModel') >> true
            removeAnnotationFromCatalogueItem(basicModel.id, _) >> {UUID bmid, Annotation annotation ->
                basicModel.removeFromAnnotations(annotation)
            }
        }
        service.catalogueItemServices = [basicModelService]

    }

    void "test get"() {
        expect:
        service.get(id) != null
    }

    void "test list"() {
        when:
        List<Annotation> annotationList = service.list(max: 2, offset: 2)

        then:
        annotationList.size() == 2

        and:
        annotationList[0].description == 'editor annotation'
        annotationList[0].depth == 2
        annotationList[0].path == "/${parent.id}/${nested.id}"
        !annotationList[0].childAnnotations.size()


        and:
        annotationList[1].description == 'nestedparent'
        annotationList[1].depth == 1
        annotationList[1].path == "/${parent.id}"

        and:
        annotationList[1].childAnnotations.size() == 1
        annotationList[1].childAnnotations[0].depth == 2
        annotationList[1].childAnnotations[0].path == "/${parent.id}/${id}"
    }

    void "test count"() {
        expect:
        service.count() == 5
    }

    void "test delete"() {
        expect:
        service.count() == 5

        when: 'deleting nested should delete all children'
        service.delete(id)

        then:
        service.count() == 3
    }

    void 'test findAllWhereRootAnnotationOfCatalogueItemId'() {
        when:
        List<Annotation> annotations = service.findAllWhereRootAnnotationOfCatalogueItemId(UUID.randomUUID())

        then:
        !annotations

        when:
        annotations = service.findAllWhereRootAnnotationOfCatalogueItemId(basicModel.id)

        then:
        annotations.size() == 2
    }

    void 'test findAllByParentAnnotationId'() {
        when:
        List<Annotation> annotations = service.findAllByParentAnnotationId(UUID.randomUUID())

        then:
        !annotations

        when:
        annotations = service.findAllByParentAnnotationId(Annotation.findByLabel('annotation 1').id)

        then:
        !annotations

        when:
        annotations = service.findAllByParentAnnotationId(parent.id)

        then:
        annotations.size() == 2

        when:
        annotations = service.findAllByParentAnnotationId(nested.id)

        then:
        annotations.size() == 1
    }

    void 'test findByCatalogueItemIdAndId for root annotation'() {
        when:
        Annotation annotation = service.findByCatalogueItemIdAndId(UUID.randomUUID(), id)

        then:
        !annotation

        when:
        annotation = service.findByCatalogueItemIdAndId(basicModel.id, parent.id)

        then:
        annotation
        annotation.id == parent.id
        annotation.label == parent.label
    }

    void 'test findByCatalogueItemIdAndId for nested annotation'() {

        when:
        Annotation annotation = service.findByCatalogueItemIdAndId(basicModel.id, id)

        then:
        annotation
        annotation.id == id
        annotation.label == nested.label
    }

    @Override
    Annotation getAwareItem() {
        parent
    }

    @Override
    Annotation getUpdatedAwareItem() {
        parent.description = 'altered'
        parent
    }

    @Override
    int getExpectedCountOfAwareItemsInBasicModel() {
        5
    }

    @Override
    String getChangedPropertyName() {
        'description'
    }
}
