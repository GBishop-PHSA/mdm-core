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
package uk.ac.ox.softeng.maurodatamapper.core.admin

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController

import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j

@Slf4j
class ApiPropertyController extends EditLoggingController<ApiProperty> {

    ApiPropertyService apiPropertyService

    static responseFormats = ['json', 'xml']

    ApiPropertyController() {
        super(ApiProperty)
    }

    @Override
    def index(Integer max) {
        def res = listAllResources(params)
        // The new grails-views code sets the modelAndView object rather than writing the response
        // Therefore if thats written then we dont want to try and re-write it
        if (response.isCommitted() || modelAndView) return
        respond res, [model: [userSecurityPolicyManager: currentUserSecurityPolicyManager], view: 'index']
    }

    @Override
    protected void serviceDeleteResource(ApiProperty resource) {
        apiPropertyService.delete(resource)
    }

    @Override
    protected List<ApiProperty> listAllReadableResources(Map params) {
        if (!apiPropertyService.count()) {
            throw new ApiInternalException('AS01', "Api Properties have not been loaded. " +
                                                   "Please contact the System Administrator")
        }
        if ((params as GrailsParameterMap).boolean('openAccess')) return apiPropertyService.findAllByPubliclyVisible(params)
        currentUserSecurityPolicyManager.isApplicationAdministrator() ? apiPropertyService.list(params) : []

    }
}