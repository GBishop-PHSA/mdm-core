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
package uk.ac.ox.softeng.maurodatamapper.profile

class UrlMappings {

    static mappings = {
        group '/api', {

            group '/profiles', {
                get '/providers'(controller: 'profile', action: 'profileProviders')

                group "/$profileNamespace/$profileName", {

                    // New URL replaces /api/profiles/namespace/name/customSearch
                    post '/search'(controller: 'profile', action: 'search')
                    // New URL replaces /api/dataModels/profile/namespace/name/version
                    get "/$catalogueItemDomainType"(controller: 'profile', action: 'listModelsInProfile')

                    // New URL replaces /api/dataModels/profile/values/namespace/name/version
                    get "/${catalogueItemDomainType}/values"(controller: 'profile', action: 'listValuesInProfile')

                    // Provide multiple ways to obtain profile of a catalogue item
                    get "/${catalogueItemDomainType}/${catalogueItemId}"(controller: 'profile', action: 'show')

                    post "/${catalogueItemDomainType}/${catalogueItemId}"(controller: 'profile', action: 'save')
                }
            }

            // Provide multiple ways to obtain profile of a catalogue item
            group "/${catalogueItemDomainType}/${catalogueItemId}", {
                get '/profiles/used'(controller: 'profile', action: 'profiles')
                get '/profiles/unused'(controller: 'profile', action: 'unusedProfiles')
                get '/profiles/otherMetadata'(controller: 'profile', action: 'otherMetadata')
                get "/profile/$profileNamespace/$profileName/$profileVersion?"(controller: 'profile', action: 'show')
                delete "/profile/$profileNamespace/$profileName/$profileVersion?"(controller: 'profile', action: 'deleteProfile')
                post "/profile/$profileNamespace/$profileName/$profileVersion?"(controller: 'profile', action: 'save')
            }
        }
    }
}