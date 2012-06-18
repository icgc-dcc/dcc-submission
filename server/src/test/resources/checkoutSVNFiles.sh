svn checkout https://code.oicr.on.ca/svn/biomart/martloader/ converter --depth empty
cd converter
svn export --force https://code.oicr.on.ca/svn/biomart/martloader/icgc.0.7.xml
svn export --force https://code.oicr.on.ca/svn/biomart/martloader/executor/.dcc-loader/codec codec
svn export --force https://code.oicr.on.ca/svn/biomart/martloader/executor/.dcc-loader/data_model/source source
