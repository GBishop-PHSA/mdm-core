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
package uk.ac.ox.softeng.maurodatamapper.core.feed

import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController

import grails.rest.RestfulController

/**
 * Produce an ATOM feed of all Models. Only respond in ATOM format. To render the response in ATOM,
 * beans need to be configured in uk.ac.ox.softeng.maurodatamapper.core.MdmCoreGrailsPlugin, like this:
 *
 * import uk.ac.ox.softeng.maurodatamapper.core.model.Model
 * import uk.ac.ox.softeng.maurodatamapper.core.rest.render.MdmAtomModelCollectionRenderer
 * beans = {
 *   halModelListRenderer(MdmAtomModelCollectionRenderer, Collection) {
 *       includes = []
 *   }
 *   halModelRenderer(MdmAtomModelCollectionRenderer, Model) {
 *       includes = []
 *   }
 * }
 * 
 * @since 04/01/2021
 */
class FeedController extends RestfulController<Model> implements ResourcelessMdmController {

    static responseFormats = ['atom']

    FeedService feedService

    FeedController() {
        super(Model)
    }

    def index() {
        params.format = 'atom'
        respond(feedService.findModels(currentUserSecurityPolicyManager))
    }

}
