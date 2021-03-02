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
package uk.ac.ox.softeng.maurodatamapper.referencedata

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelConstraints
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.search.StandardSearch
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.ParentOwnedLabelCollectionValidator
import uk.ac.ox.softeng.maurodatamapper.hibernate.VersionUserType
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataAware
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValue
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import grails.validation.ValidationErrors
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormValidationApi
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Slf4j
@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferenceDataModel implements Model<ReferenceDataModel>, ReferenceSummaryMetadataAware {

    UUID id

    Boolean hasChild

    static hasMany = [
            referenceDataTypes      : ReferenceDataType,
            referenceDataElements   : ReferenceDataElement,
            referenceDataValues     : ReferenceDataValue,
            classifiers             : Classifier,
            metadata                : Metadata,
            semanticLinks           : SemanticLink,
            annotations             : Annotation,
            versionLinks            : VersionLink,
            referenceFiles          : ReferenceFile,
            referenceSummaryMetadata: ReferenceSummaryMetadata,
            rules                   : Rule
    ]

    static belongsTo = [Folder]

    static transients = ['hasChild', 'aliases']

    static constraints = {
        CallableConstraints.call(ModelConstraints, delegate)
        referenceDataTypes validator: { val, obj -> new ParentOwnedLabelCollectionValidator(obj, 'referenceDataTypes').isValid(val) }
    }

    static mapping = {
        documentationVersion type: VersionUserType
        modelVersion type: VersionUserType
        folder cascade: 'none'
        referenceDataTypes cascade: 'all-delete-orphan', cascadeValidate: 'dirty'
        referenceDataElements cascade: 'all-delete-orphan', cascadeValidate: 'dirty'
        referenceDataValues cascade: 'all-delete-orphan', cascadeValidate: 'dirty'
    }

    static mappedBy = [
        metadata   : 'none',
        referenceDataTypes  : 'referenceDataModel',
        referenceDataElements  : 'referenceDataModel',
        referenceDataValues  : 'referenceDataModel'
    ]

    static search = {
        CallableSearch.call(StandardSearch, delegate)
    }

    ReferenceDataModel() {
        modelType = ReferenceDataModel.simpleName
        documentationVersion = Version.from('1')
        finalised = false
        deleted = false
        readableByAuthenticatedUsers = false
        readableByEveryone = false
        breadcrumbTree = new BreadcrumbTree(this)
        branchName = VersionAwareConstraints.DEFAULT_BRANCH_NAME
    }

    @Override
    String getDomainType() {
        ReferenceDataModel.simpleName
    }

    ObjectDiff<ReferenceDataModel> diff(ReferenceDataModel otherDataModel) {
        modelDiffBuilder(ReferenceDataModel, this, otherDataModel)
            .appendList(ReferenceDataType, 'referenceDataTypes', this.referenceDataTypes, otherDataModel.referenceDataTypes)
            .appendList(ReferenceDataType, 'referenceDataElements', this.referenceDataElements, otherDataModel.referenceDataElements)
    }

    def beforeValidate() {
        beforeValidateCatalogueItem()
        this.referenceDataTypes?.each { it.beforeValidate() }
        this.referenceDataElements?.each { it.beforeValidate() }
    }

    ReferenceDataType findReferenceDataTypeByLabel(String label) {
        this.referenceDataTypes?.find { it.label == label }
    }

    ReferenceDataType findReferenceDataTypeByLabelAndType(String label, String type) {
        this.referenceDataTypes?.find { it.domainType == type && it.label == label }
    }

    int countReferenceDataTypesByLabel(String label) {
        this.referenceDataTypes?.count { it.label == label } ?: 0
    }

    List<ReferenceDataType> getSortedReferenceDataTypes() {
        this.referenceDataTypes?.sort() ?: []
    }

    int countReferenceDataElementsByLabel(String label) {
        this.referenceDataElements?.count { it.label == label } ?: 0
    }    

    @Override
    String getEditLabel() {
        "${modelType}:${label}"
    }

    static DetachedCriteria<ReferenceDataModel> by() {
        new DetachedCriteria<ReferenceDataModel>(ReferenceDataModel)
    }

    static DetachedCriteria<ReferenceDataModel> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<ReferenceDataModel> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }

    static DetachedCriteria<ReferenceDataModel> withFilter(DetachedCriteria<ReferenceDataModel> criteria, Map filters) {
        withCatalogueItemFilter(criteria, filters)
    }
}