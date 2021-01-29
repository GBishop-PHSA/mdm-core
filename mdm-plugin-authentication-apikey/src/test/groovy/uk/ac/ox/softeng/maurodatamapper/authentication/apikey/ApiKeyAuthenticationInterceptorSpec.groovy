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
package uk.ac.ox.softeng.maurodatamapper.authentication.apikey

import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class ApiKeyAuthenticationInterceptorSpec extends BaseUnitSpec implements InterceptorUnitTest<ApiKeyAuthenticationInterceptor> {

    void "Test interceptor matching"() {
        when: "A request sent to the interceptor"
        withRequest(uri: '/api/dataModels')

        then: "The interceptor does match"
        interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/oxfordbrc/api/dataModels')

        then: "The interceptor does match"
        interceptor.doesMatch()
    }

    void "Test interceptor non-matching"() {

        when: "A request sent to the interceptor"
        withRequest(controller: 'authenticating', action: 'login')

        then: "The interceptor does match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(controller: 'authenticating', action: 'logout')

        then: "The interceptor does match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/api/authentication/logout')

        then: "The interceptor does match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/api/authentication/logout')

        then: "The interceptor does match"
        !interceptor.doesMatch()



        when: "A request sent to the interceptor"
        withRequest(uri: '/oxfordbrc/')

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/')

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/oxfordbrc')

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/views/home.html')

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/scripts/services/jointDiagramService3.js')

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/bower_components/angular-tree-control/css/tree-control.css')

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/oxfordbrc/styles/vendor.5a20e16f.css')

        then: "The interceptor does not match"
        !interceptor.doesMatch()

        when: "A request sent to the interceptor"
        withRequest(uri: '/oxfordbrc/scripts/scripts.91b6fb8f.js')

        then: "The interceptor does not match"
        !interceptor.doesMatch()
    }
}
