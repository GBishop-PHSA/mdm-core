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
package uk.ac.ox.softeng.maurodatamapper.core.model


import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.PathAware
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource

/**
 * This is the base trait for any container of models.
 * Currently {@link uk.ac.ox.softeng.maurodatamapper.core.container.Folder} are physical containers and
 * {@link uk.ac.ox.softeng.maurodatamapper.core.container.Classifier} are virtual containers.
 *
 * @since 05/11/2019
 */
trait Container implements PathAware, InformationAware, SecurableResource, EditHistoryAware {

    abstract boolean hasChildren()

    abstract Boolean getDeleted()
}
