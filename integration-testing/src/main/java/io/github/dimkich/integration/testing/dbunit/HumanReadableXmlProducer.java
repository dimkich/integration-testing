package io.github.dimkich.integration.testing.dbunit;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultTableMetaData;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.stream.DefaultConsumer;
import org.dbunit.dataset.stream.IDataSetConsumer;
import org.dbunit.dataset.stream.IDataSetProducer;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class HumanReadableXmlProducer extends DefaultHandler
        implements IDataSetProducer, ContentHandler, ErrorHandler {
    private static final IDataSetConsumer EMPTY_CONSUMER = new DefaultConsumer();
    private final InputSource inputSource;
    private IDataSetConsumer consumer = EMPTY_CONSUMER;

    private final Map<String, TableData> tables = new LinkedHashMap<>();
    private final BitSet activeRowPresent = new BitSet();
    private final StringBuffer activeCharacters = new StringBuffer();

    private int tagNestingLevel;
    private TableData activeTable;
    private Object[] activeRowValues;
    private String activeColumnName;
    private boolean isNullActiveValue;

    @Override
    public void setConsumer(IDataSetConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    @SneakyThrows
    public void produce() {
        tables.clear();
        tagNestingLevel = 0;
        activeTable = null;
        activeRowValues = null;
        activeColumnName = null;
        activeRowPresent.clear();

        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        XMLReader xmlReader = saxParserFactory.newSAXParser().getXMLReader();

        xmlReader.setContentHandler(this);
        xmlReader.setEntityResolver(this);
        xmlReader.setErrorHandler(this);
        xmlReader.parse(inputSource);

        emit();
        tables.clear();
    }

    @SneakyThrows
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        switch (tagNestingLevel) {
            case 1 -> {
                activeTable = tables.computeIfAbsent(qName, TableData::new);
                activeRowValues = new Object[activeTable.columnIndex.size()];
                activeRowPresent.clear();
            }
            case 2 -> {
                activeColumnName = qName;
                isNullActiveValue = isNull(attributes);
                activeCharacters.setLength(0);
            }
        }
        tagNestingLevel++;
    }

    @SneakyThrows
    public void endElement(String uri, String localName, String qName) {
        tagNestingLevel--;
        switch (tagNestingLevel) {
            case 1 -> {
                activeTable.rows.add(activeRowValues);
                activeRowValues = null;
                activeTable = null;
            }
            case 2 -> addValue();
        }
    }

    public void characters(char[] ch, int start, int length) {
        activeCharacters.append(ch, start, length);
    }

    public void error(SAXParseException e) throws SAXException {
        throw e;
    }

    private void addValue() throws DataSetException {
        int index = activeTable.columnIndex.computeIfAbsent(activeColumnName, k -> activeTable.columnIndex.size());
        if (activeRowPresent.get(index)) {
            throw new DataSetException("Duplicate column '" + activeColumnName + "' in table '"
                    + activeTable.name + "', row " + activeTable.rows.size());
        }
        activeRowPresent.set(index);
        if (index >= activeRowValues.length) {
            activeRowValues = Arrays.copyOf(activeRowValues, activeTable.columnIndex.size());
        }
        activeRowValues[index] = isNullActiveValue ? null : activeCharacters.toString();
    }

    private boolean isNull(Attributes attributes) {
        for (int i = 0; i < attributes.getLength(); i++) {
            if ("null".equalsIgnoreCase(attributes.getQName(i)) && "true".equalsIgnoreCase(attributes.getValue(i))) {
                return true;
            }
        }
        return false;
    }

    private void emit() throws DataSetException {
        consumer.startDataSet();
        for (TableData table : tables.values()) {
            consumer.startTable(createMetaData(table));
            int columnCount = table.columnIndex.size();
            for (int i = 0; i < table.rows.size(); i++) {
                Object[] row = table.rows.get(i);
                if (row.length < columnCount) {
                    row = Arrays.copyOf(row, columnCount);
                    table.rows.set(i, row);
                }
                consumer.row(row);
            }
            consumer.endTable();
        }
        consumer.endDataSet();
    }

    private ITableMetaData createMetaData(TableData table) {
        Column[] columns = new Column[table.columnIndex.size()];
        int i = 0;
        for (String columnName : table.columnIndex.keySet()) {
            columns[i++] = new Column(columnName, DataType.UNKNOWN);
        }
        return new DefaultTableMetaData(table.name, columns);
    }

    @RequiredArgsConstructor
    private static final class TableData {
        private final String name;
        private final Map<String, Integer> columnIndex = new LinkedHashMap<>();
        private final List<Object[]> rows = new ArrayList<>();
    }
}
