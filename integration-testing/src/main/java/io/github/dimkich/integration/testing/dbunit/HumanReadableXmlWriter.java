package io.github.dimkich.integration.testing.dbunit;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.TypeCastException;
import org.dbunit.dataset.stream.DataSetProducerAdapter;
import org.dbunit.dataset.stream.IDataSetConsumer;
import org.dbunit.util.xml.XmlWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;

public class HumanReadableXmlWriter implements IDataSetConsumer {
    private static final String DATASET = "dataset";

    private final XmlWriter xmlWriter;
    private ITableMetaData activeMetaData;

    public HumanReadableXmlWriter(OutputStream out) throws IOException {
        this(out, null);
    }

    public HumanReadableXmlWriter(OutputStream outputStream, Charset charset) {
        xmlWriter = new XmlWriter(outputStream, charset);
        xmlWriter.enablePrettyPrint(true);
    }

    public HumanReadableXmlWriter(Writer writer) {
        xmlWriter = new XmlWriter(writer);
        xmlWriter.enablePrettyPrint(true);
    }

    public HumanReadableXmlWriter(Writer writer, Charset charset) {
        xmlWriter = new XmlWriter(writer, charset);
        xmlWriter.enablePrettyPrint(true);
        xmlWriter.setIndent("    ");
    }

    public void setPrettyPrint(boolean enabled) {
        xmlWriter.enablePrettyPrint(enabled);
    }

    public void write(IDataSet dataSet) throws DataSetException {
        DataSetProducerAdapter provider = new DataSetProducerAdapter(dataSet);
        provider.setConsumer(this);
        provider.produce();
    }

    @Override
    public void startDataSet() throws DataSetException {
        try {
            xmlWriter.writeDeclaration();
            xmlWriter.writeElement(DATASET);
        } catch (IOException e) {
            throw new DataSetException(e);
        }
    }

    @Override
    public void endDataSet() throws DataSetException {
        try {
            xmlWriter.endElement();
            xmlWriter.close();
        } catch (IOException e) {
            throw new DataSetException(e);
        }
    }

    @Override
    public void startTable(ITableMetaData metaData) {
        activeMetaData = metaData;
    }

    @Override
    public void endTable() {
        activeMetaData = null;
    }

    @Override
    public void row(Object[] values) throws DataSetException {
        try {
            String tableName = activeMetaData.getTableName();
            xmlWriter.writeElement(tableName);

            Column[] columns = activeMetaData.getColumns();
            for (int i = 0; i < columns.length; i++) {
                String columnName = columns[i].getColumnName();
                Object value = values[i];

                try {
                    xmlWriter.writeElement(columnName);
                    if (value == null) {
                        xmlWriter.writeAttribute("null", "true", true);
                    } else {
                        String stringValue = DataType.asString(value);
                        xmlWriter.writeText(stringValue, true);
                    }
                    xmlWriter.endElement();
                } catch (TypeCastException e) {
                    throw new DataSetException("table=" +
                            activeMetaData.getTableName() + ", row=" + i +
                            ", column=" + columnName +
                            ", value=" + value, e);
                }
            }

            xmlWriter.endElement();
        } catch (IOException e) {
            throw new DataSetException(e);
        }
    }
}
