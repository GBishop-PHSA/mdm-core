import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataClassComponent

DataClassComponent dcc = dataClassComponent as DataClassComponent

'mdm:dataClassComponent' {
    layout '/catalogueItem/_export.gml', catalogueItem: dcc

    'mdm:definition' dcc.definition
    
    if (dcc.sourceDataClasses) {
        'mdm:sourceDataClasses' {
            dcc.sourceDataClasses.sort{it.label}.each {dc ->
                layout '/dataClassComponent/_exportDataClass.gml', dataClass: dc
            }
        }
    }

    if (dcc.targetDataClasses) {
        'mdm:targetDataClasses' {
            dcc.targetDataClasses.sort{it.label}.each {dc ->
                layout '/dataClassComponent/_exportDataClass.gml', dataClass: dc
            }
        }
    }    

    if (dcc.dataElementComponents) {
        'mdm:dataElementComponents' {
            dcc.dataElementComponents.sort{it.label}.each {de ->
                layout '/dataElementComponent/_export.gml', dataElementComponent: de
            }
        }
    }
}