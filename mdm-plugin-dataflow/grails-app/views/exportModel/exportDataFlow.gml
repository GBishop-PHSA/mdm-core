import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata

xmlDeclaration()

'exp:exportModel'(exportModel.getXmlNamespaces()) {
    layout '/dataFlow/export.gml', dataFlow: exportModel.modelExportMap.dataFlow
    layout '/exportMetadata/export.gml', exportMetadata: exportModel.exportMetadata
}