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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype


import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.DataTypeSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.datatype.ReferenceType

@Slf4j
class ReferenceTypeSpec extends DataTypeSpec<ReferenceType> implements DomainUnitTest<ReferenceType> {

    DataClass reference

    def setup() {
        log.debug('Setting up ReferenceTypeSpec unit')
        mockDomain(DataClass)
        reference = new DataClass(createdByUser: admin, label: 'reference', dataModel: dataSet)
        checkAndSave(reference)
    }

    @Override
    void setValidDomainOtherValues() {
        domain.referenceClass = reference
        domain
    }

    @Override
    void verifyDomainOtherConstraints(ReferenceType subDomain) {
        assert subDomain.referenceClass.id == reference.id
    }

    @Override
    ReferenceType createValidDomain(String label) {
        ReferenceType referenceType = super.createValidDomain(label)
        referenceType.referenceClass = reference
        referenceType
    }
}
