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
package uk.ac.ox.softeng.maurodatamapper.core.session

import uk.ac.ox.softeng.maurodatamapper.core.session.SessionController
import uk.ac.ox.softeng.maurodatamapper.core.session.SessionService
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.web.controllers.ControllerUnitTest

import static org.springframework.http.HttpStatus.OK

class SessionControllerSpec extends BaseUnitSpec implements ControllerUnitTest<SessionController> {

    void 'test isAuthenticatedSession when its not valid'() {
        given:
        controller.sessionService = Mock(SessionService) {
            1 * isAuthenticatedSession(_, _) >> false
        }

        when:
        request.method = 'GET'
        controller.isAuthenticatedSession()

        then:
        response.status == OK.value()
        response.json.authenticatedSession == false
    }

    void 'test isAuthenticatedSession'() {
        given:
        controller.sessionService = Mock(SessionService) {
            1 * isAuthenticatedSession(_, _) >> true
        }
        when:
        request.method = 'GET'
        controller.isAuthenticatedSession()

        then:
        response.status == OK.value()
        response.json.authenticatedSession == true
    }
}
