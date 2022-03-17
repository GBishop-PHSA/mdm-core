/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.gorm

import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.PagedResultList
import groovy.util.logging.Slf4j

/**
 * @since 25/02/2022
 */
@Slf4j
@SuppressWarnings('GroovyAssignabilityCheck')
class HQLPagedResultList<E> extends PagedResultList<E> {

    private Class<E> gormEntityClass
    private Map pagination
    private String listQuery
    private String countQuery
    private Map<String, Object> queryParams

    HQLPagedResultList(Class<E> gormEntityClass) {
        super(null)
        this.gormEntityClass = gormEntityClass
    }

    HQLPagedResultList list(String hqlQuery) {
        this.listQuery = hqlQuery
        this
    }

    HQLPagedResultList count(String hqlQuery) {
        this.countQuery = hqlQuery
        this
    }


    HQLPagedResultList queryParams(Map<String, Object> queryParams) {
        this.queryParams = queryParams
        this
    }


    HQLPagedResultList paginate(Map pagination) {
        this.pagination = pagination
        log.trace('Paginating query with paging {}\n{}', pagination, listQuery)
        def queryResult = (gormEntityClass).findAll(listQuery, queryParams, pagination)
        resultList = queryResult.collect {obj ->
            obj.class.isArray() ? obj.find {Utils.parentClassIsAssignableFromChild(gormEntityClass, it.class)} : obj
        }
        this
    }

    HQLPagedResultList postProcess(Closure closure){
        resultList.each closure
        this
    }

    @Override
    protected void initialize() {
        log.trace('Getting count for query \n{}', countQuery)
        totalCount = gormEntityClass.executeQuery(countQuery, queryParams).first()
    }
}