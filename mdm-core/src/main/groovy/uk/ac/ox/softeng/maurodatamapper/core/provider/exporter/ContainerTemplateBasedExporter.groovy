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
package uk.ac.ox.softeng.maurodatamapper.core.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.text.Template
import groovy.util.logging.Slf4j

@Slf4j
trait ContainerTemplateBasedExporter extends TemplateBasedExporter {

    @Override
    String getExportViewPath() {
        '/exportContainer/export'
    }

    ByteArrayOutputStream exportContainer(ExportContainer exportContainer, String format) {
        log.debug('Exporting container using template')
        Template template = templateEngine.resolveTemplate(exportViewPath)

        if (!template) {
            log.error('Could not find template for format {} at path {}', format, exportViewPath)
            throw new ApiInternalException('TBE02', "Could not find template for format ${format} at path ${exportViewPath}")
        }
        long start = System.currentTimeMillis()
        def writable = template.make(exportContainer: exportContainer)
        log.debug('Making template took {}', Utils.timeTaken(start))
        start = System.currentTimeMillis()
        def sw = new StringWriter()
        writable.writeTo(sw)
        log.debug('Writing template took {}', Utils.timeTaken(start))
        start = System.currentTimeMillis()
        ByteArrayOutputStream os = new ByteArrayOutputStream()
        os.write(sw.toString().bytes)
        log.debug('Outputting template took {}', Utils.timeTaken(start))
        os
    }

}