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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.CreationMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.DeletionMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.FieldMergeDiff
import uk.ac.ox.softeng.maurodatamapper.util.Path

import grails.validation.Validateable

/**
 * @since 07/02/2018
 */
class FieldPatchData<T> implements Validateable {

    String fieldName
    Path path
    T sourceValue
    T targetValue
    T commonAncestorValue
    boolean isMergeConflict
    String type

    static constraints = {
        fieldName nullable: false, blank: false
        path nullable: false, blank: false
        sourceValue nullable: true
        targetValue nullable: true
        commonAncestorValue nullable: true
        type nullable: false, blank: false, inList: ['creation', 'deletion', 'modification']

    }

    boolean isMetadataChange() {
        false
    }

    boolean isCreation() {
        type == 'creation'
    }

    boolean isDeletion() {
        type == 'deletion'
    }

    boolean isModification() {
        type == 'modification'
    }

    String toString() {
        "Merge ${type} patch on ${path} :: Changing ${targetValue} to ${sourceValue}"
    }

    void setPath(String path) {
        this.path = Path.from(path)
    }

    void setPath(Path path) {
        this.path = path
    }

    Path getRootIndependentPath() {
        this.path.clone().tap {
            first().label = null
        }
    }

    static <P> FieldPatchData<P> from(FieldMergeDiff<P> fieldMergeDiff) {
        new FieldPatchData().tap {
            fieldName = fieldMergeDiff.fieldName
            sourceValue = fieldMergeDiff.source
            targetValue = fieldMergeDiff.target
            commonAncestorValue = fieldMergeDiff.commonAncestor
            path = fieldMergeDiff.fullyQualifiedPath
            isMergeConflict = fieldMergeDiff.isMergeConflict()
            type = 'modification'
        }
    }

    static <P extends Diffable> FieldPatchData<P> from(CreationMergeDiff<P> creationMergeDiff) {
        new FieldPatchData().tap {
            //            fieldName = creationMergeDiff.fieldName
            sourceValue = creationMergeDiff.source
            targetValue = creationMergeDiff.target
            commonAncestorValue = creationMergeDiff.commonAncestor
            path = creationMergeDiff.fullyQualifiedPath
            isMergeConflict = creationMergeDiff.isMergeConflict()
            type = 'creation'
        }
    }

    static <P extends Diffable> FieldPatchData<P> from(DeletionMergeDiff<P> deletionMergeDiff) {
        new FieldPatchData().tap {
            //            fieldName = deletionMergeDiff.fieldName
            sourceValue = deletionMergeDiff.source
            targetValue = deletionMergeDiff.target
            commonAncestorValue = deletionMergeDiff.commonAncestor
            path = deletionMergeDiff.fullyQualifiedPath
            isMergeConflict = deletionMergeDiff.isMergeConflict()
            type = 'deletion'
        }
    }
}
