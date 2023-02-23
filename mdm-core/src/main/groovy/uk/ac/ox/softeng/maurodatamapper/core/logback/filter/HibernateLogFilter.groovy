/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.logback.filter

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.EvaluatorFilter
import ch.qos.logback.core.spi.FilterReply

import java.util.regex.Pattern

/**
 * @since 01/02/2022
 */
class HibernateLogFilter extends EvaluatorFilter<ILoggingEvent> {

    static final List<Pattern> matchingPatterns = [
        ~/.*Specified config option \[importFrom].*/,
        ~/HHH90000022.*/,
        ~/HHH000179.*/,
    ]

    HibernateLogFilter() {
        onMatch = FilterReply.DENY
        evaluator = new PatternMatchingEvaluator(matchingPatterns)
    }
}
