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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.controller.CatalogueItemController
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService

import grails.gorm.transactions.Transactional

class DataElementController extends CatalogueItemController<DataElement> {
    static responseFormats = ['json', 'xml']

    DataClassService dataClassService
    DataElementService dataElementService
    DataModelService dataModelService

    DataElementController() {
        super(DataElement)
    }

    @Transactional
    def copyDataElement() {
        if (handleReadOnly()) {
            return
        }

        DataModel dataModel = dataModelService.get(params.dataModelId)
        DataClass dataClass = dataClassService.get(params.dataClassId)
        DataElement original = dataElementService.findByDataClassIdAndId(params.otherDataClassId, params.dataElementId)

        if (!original) return notFound(params.dataElementId)
        DataElement copy
        try {
            copy = dataElementService.copyDataElement(dataModel, original, currentUser)
            dataClass.addToDataElements(copy)
            dataClassService.matchUpAndAddMissingReferenceTypeClasses(dataModel, dataModelService.get(params.otherDataModelId), currentUser)
        } catch (ApiInvalidModelException ex) {
            transactionStatus.setRollbackOnly()
            respond ex.errors, view: 'create' // STATUS CODE 422
            return
        }

        dataModelService.validate(dataModel)
        if (dataModel.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond dataModel.errors, view: 'create' // STATUS CODE 422
            return
        }

        saveResource copy

        saveResponse copy
    }

    def suggestLinks() {
        DataModel dataModel = dataModelService.get(params.dataModelId)
        DataModel otherDataModel = dataModelService.get(params.otherDataModelId)

        if (!dataModel) return notFound(params.dataModelId)
        if (!otherDataModel) return notFound(params.otherDataModelId)

        DataElement dataElement = queryForResource(params.dataElementId)
        if (!dataElement) return notFound(params.dataElementId)

        int maxResults = params.max ?: 5

        respond dataElementService.findAllSimilarDataElementsInDataModel(otherDataModel, dataElement, maxResults)
    }


    @Override
    protected DataElement queryForResource(Serializable resourceId) {
        if (params.dataTypeId) {
            return dataElementService.findByDataTypeIdAndId(params.dataTypeId, resourceId)
        }

        return dataElementService.findByDataClassIdAndId(params.dataClassId, resourceId)
    }

    @Override
    protected List<DataElement> listAllReadableResources(Map params) {
        if (params.dataTypeId) {
            return dataElementService.findAllByDataTypeId(params.dataTypeId, params)
        }
        if (params.all) removePaginationParameters()

        return dataElementService.findAllByDataClassId(params.dataClassId, params)
    }

    @Override
    void serviceDeleteResource(DataElement resource) {
        dataElementService.delete(resource, true)
    }

    @Override
    protected void serviceInsertResource(DataElement resource) {
        dataElementService.save(resource)
    }

    @Override
    protected DataElement createResource() {
        DataElement resource = super.createResource() as DataElement
        dataClassService.get(params.dataClassId)?.addToDataElements(resource)
        resource
    }

    @Override
    protected DataElement updateResource(DataElement resource) {
        if (!resource.dataType.ident()) resource.dataType.save()
        super.updateResource(resource) as DataElement
    }
}