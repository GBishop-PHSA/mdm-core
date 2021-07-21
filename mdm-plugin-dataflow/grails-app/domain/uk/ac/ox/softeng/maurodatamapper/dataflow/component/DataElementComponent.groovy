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
package uk.ac.ox.softeng.maurodatamapper.dataflow.component

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelItemConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import org.grails.datastore.gorm.GormEntity

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Resource(readOnly = false, formats = ['json', 'xml'])
class DataElementComponent implements ModelItem<DataElementComponent, DataModel> {

    UUID id
    DataClassComponent dataClassComponent
    String definition

    static belongsTo = [
        DataClassComponent
    ]

    static hasMany = [
        classifiers       : Classifier,
        metadata          : Metadata,
        annotations       : Annotation,
        semanticLinks     : SemanticLink,
        referenceFiles    : ReferenceFile,
        sourceDataElements: DataElement,
        targetDataElements: DataElement,
        rules             : Rule
    ]

    static transients = ['aliases', 'model']

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        sourceDataElements minSize: 1
        targetDataElements minSize: 1
        definition nullable: true, blank: false
    }

    static mapping = {
        definition type: 'text'
        sourceDataElements joinTable: [name: 'join_data_element_component_to_source_data_element', key: 'data_element_component_id'],
                           index: 'jdectsde_data_element_component_idx'
        targetDataElements joinTable: [name: 'join_data_element_component_to_target_data_element', key: 'data_element_component_id'],
                           index: 'jdecttde_data_element_component_idx'
    }

    static mappedBy = [
        sourceDataElements: 'none',
        targetDataElements: 'none'
    ]

    DataElementComponent() {
    }

    @Override
    String getDomainType() {
        DataElementComponent.simpleName
    }

    @Override
    String getPathPrefix() {
        'dec'
    }

    @Override
    GormEntity getPathParent() {
        dataClassComponent
    }

    @Override
    def beforeValidate() {
        beforeValidateModelItem()
    }

    @Override
    def beforeInsert() {
        buildPath()
    }

    @Override
    def beforeUpdate() {
        buildPath()
    }

    @Override
    String getEditLabel() {
        "DataElementComponent:${label}"
    }

    @Override
    DataModel getModel() {
        dataClassComponent.model
    }

    @Override
    Boolean hasChildren() {
        false
    }

    ObjectDiff<DataElementComponent> diff(DataElementComponent otherDataFlow) {
        catalogueItemDiffBuilder(DataElementComponent, this, otherDataFlow)
    }

    static DetachedCriteria<DataElementComponent> by() {
        new DetachedCriteria<DataElementComponent>(DataElementComponent)
    }

    @Deprecated(forRemoval = true)
    static DetachedCriteria<DataElementComponent> byDataFlowId(UUID dataFlowId) {
        by().eq('dataClassComponent.dataFlow.id', dataFlowId)
    }

    @Deprecated(forRemoval = true)
    static DetachedCriteria<DataElementComponent> byDataFlowIdAndId(UUID dataFlowId, UUID id) {
        byDataFlowId(dataFlowId).idEq(id)
    }

    static DetachedCriteria<DataElementComponent> byDataClassComponentId(UUID dataClassComponentId) {
        by().eq('dataClassComponent.id', dataClassComponentId)
    }

    static DetachedCriteria<DataElementComponent> byDataClassComponentIdAndId(UUID dataClassComponentId, UUID id) {
        byDataClassComponentId(dataClassComponentId).idEq(id)
    }

    static DetachedCriteria<DataElementComponent> byDataElementId(UUID dataElementId) {
        by().or {
            sourceDataElements {
                eq('id', dataElementId)
            }
            targetDataElements {
                eq('id', dataElementId)
            }
        }
    }

    static DetachedCriteria<DataElementComponent> bySourceDataElementId(UUID dataElementId) {
        by().eq('sourceDataElements.id', dataElementId)
    }

    static DetachedCriteria<DataElementComponent> byTargetDataElementId(UUID dataElementId) {
        by().eq('targetDataElements.id', dataElementId)
    }

    @Deprecated(forRemoval = true)
    static DetachedCriteria<DataElementComponent> byDataFlowIdAndDataClassId(UUID dataFlowId, UUID dataClassId) {
        byDataFlowId(dataFlowId).or {
            sourceDataElements {
                eq('dataClass.id', dataClassId)
            }
            targetDataElements {
                eq('dataClass.id', dataClassId)
            }
        }
    }

    static DetachedCriteria<DataElementComponent> byClassifierId(UUID classifierId) {
        where {
            classifiers {
                eq 'id', classifierId
            }
        }
    }
    static DetachedCriteria<DataElementComponent> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<DataElementComponent> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }
}