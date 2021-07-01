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
package uk.ac.ox.softeng.maurodatamapper.federation.publish

import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Transactional
@Slf4j
class PublishService {

    AuthorityService authorityService

    @Autowired(required = false)
    List<ModelService> modelServices

    List<Model> findAllReadableModelsToPublish(UserSecurityPolicyManager userSecurityPolicyManager) {
        List<Model> models = []
        modelServices.each {
            List<Model> readableModels = it.findAllReadableModels(userSecurityPolicyManager, false, true, false)
            // Only publish finalised models which belong to this instance of MDM
            List<Model> publishableModels = readableModels.findAll { Model model ->
                model.finalised && model.authority.id == authorityService.getDefaultAuthority().id
            } as List<Model>
            models.addAll(publishableModels)
        }
        models
    }

    List<PublishedModel> findAllPublishedReadableModels(UserSecurityPolicyManager userSecurityPolicyManager) {
        findAllReadableModelsToPublish(userSecurityPolicyManager).collect { new PublishedModel(it) }
    }
}