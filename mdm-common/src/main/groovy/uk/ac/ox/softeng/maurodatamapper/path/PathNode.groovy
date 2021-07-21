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
package uk.ac.ox.softeng.maurodatamapper.path

import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.version.Version

import groovy.util.logging.Slf4j

/**
 * @since 28/08/2020
 */
@Slf4j
class PathNode {

    static final String MODEL_PATH_IDENTIFIER_SEPARATOR = '$'
    static final String ESCAPED_MODEL_PATH_IDENTIFIER_SEPARATOR = "\\${MODEL_PATH_IDENTIFIER_SEPARATOR}"
    static final String ATTRIBUTE_PATH_IDENTIFIER_SEPARATOR = '@'

    String prefix
    String identifier
    String attribute
    String modelIdentifier

    PathNode(String prefix, String identifier, boolean isRoot, boolean isLast) {
        this.prefix = prefix
        parseIdentifier(identifier, isRoot, isLast)
    }

    PathNode(String prefix, String identifier, String modelIdentifier, String attribute) {
        this.prefix = prefix
        this.identifier = identifier
        this.attribute = attribute
        this.modelIdentifier = modelIdentifier
    }

    /*
    Parse a string into a type prefix and identifier.
    The string is imagined to be of the format tp:identifier
    identifier can contain a : character, so some real examples are
    dm:my-data-model (type prefix = dm, identifier = my-data-model)
    te:my-code:my-definition (type prefix = te, identifier = my-code:my-definition)
     */

    PathNode(String node, boolean isRoot, boolean isLast) {
        node.find(/^(\w+):(.+)$/) {full, foundPrefix, fullIdentifier ->
            prefix = foundPrefix
            parseIdentifier(fullIdentifier, isRoot, isLast)
        }
    }

    void parseIdentifier(String fullIdentifier, boolean isRoot, boolean isLast) {
        String parsed = fullIdentifier
        if (isLast) {
            parsed.find(/^(.+?)${ATTRIBUTE_PATH_IDENTIFIER_SEPARATOR}(.+?)$/) {full, subIdentifier, attr ->
                attribute = attr
                parsed = subIdentifier
            }
        }
        if (isRoot) {
            parsed.find(/^(.+?)${ESCAPED_MODEL_PATH_IDENTIFIER_SEPARATOR}(.+?)$/) {full, subIdentifier, foundIdentifier ->
                parsed = subIdentifier
                modelIdentifier = foundIdentifier
            }
        }
        identifier = parsed
    }

    boolean isPropertyNode() {
        attribute
    }

    String toString() {
        String base = "${prefix}:${getFullIdentifier()}"
        if (attribute) base += "${ATTRIBUTE_PATH_IDENTIFIER_SEPARATOR}${attribute}"
        base
    }

    String getFullIdentifier() {
        String base = identifier
        if (modelIdentifier) base += "${MODEL_PATH_IDENTIFIER_SEPARATOR}${modelIdentifier}"
        base
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        PathNode pathNode = (PathNode) o
        if (prefix != pathNode.prefix) return false
        if (identifier != pathNode.identifier) return false

        if (attribute != pathNode.attribute) return false

        if (modelIdentifier || pathNode.modelIdentifier) {
            if (Version.isVersionable(modelIdentifier) &&
                Version.isVersionable(pathNode.modelIdentifier) &&
                Version.from(modelIdentifier) == Version.from(pathNode.modelIdentifier)) {
                return true
            }
            return modelIdentifier == pathNode.modelIdentifier
        }

        return true
    }

    int hashCode() {
        int result
        result = prefix.hashCode()
        result = 31 * result + identifier.hashCode()
        String adjusted = Version.isVersionable(modelIdentifier) ? Version.from(modelIdentifier).toString() : modelIdentifier
        result = 31 * result + (adjusted != null ? adjusted.hashCode() : 0)
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0)
        return result
    }

    boolean matches(PathNode pathNode) {
        matchesPrefix(pathNode.prefix) && matchesIdentifier(pathNode)
    }

    boolean matches(CreatorAware creatorAware) {
        matches(Path.from(creatorAware).last())
    }

    boolean matchesPrefix(String otherPrefix) {
        if (prefix != otherPrefix) {
            log.warn("Resource prefix [{}] does not match the path node [{}]", otherPrefix, this)
            return false
        }
        true
    }

    boolean matchesIdentifier(PathNode otherPathNode) {

        if (identifier == otherPathNode.identifier && modelIdentifier == otherPathNode.modelIdentifier) return true

        // If model identifier present on either side then we need to do some verification
        if ((modelIdentifier || otherPathNode.modelIdentifier)) {
            return matchesModelPathIdentifierFormat(otherPathNode.identifier, otherPathNode.modelIdentifier)
        }

        // Some of the legacy paths included : so we handle submissions of this format
        String[] identifierSplit = identifier.split(/:/)
        if (identifierSplit[0] == otherPathNode.identifier) return true

        identifierSplit = otherPathNode.identifier.split(/:/)
        if (identifier == identifierSplit[0]) return true
        log.warn("Resource identifier [{}] does not match the path node [{}]", otherPathNode, this)
        false
    }

    boolean matchesModelPathIdentifierFormat(String otherIdentifier, String otherModelIdentifier) {

        // if the main identifiers dont match then theres no point continuing
        if (identifier != otherIdentifier) return false

        // If the either node has no model identifier then its defaulting and if the main identifiers match then we're happy
        if ((!otherModelIdentifier || !modelIdentifier) && identifier == otherIdentifier) return true

        // identifier part and identity part match
        // Covers exact string match on both version and branch names
        if (modelIdentifier == otherModelIdentifier) return true

        // If path identity is versionable then its possible the identifierIdentity is a short hand version of the same version e.g. 1 vs 1.0.0
        if (Version.isVersionable(modelIdentifier) && Version.isVersionable(otherModelIdentifier)) {
            return Version.from(modelIdentifier) == Version.from(otherModelIdentifier)
        }

        // no other possible match style
        false
    }

    PathNode clone() {
        new PathNode(this.prefix, this.identifier, this.modelIdentifier, this.attribute)
    }
}
