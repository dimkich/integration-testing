package io.github.dimkich.integration.testing.dbunit;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.dbunit.dataset.Column;
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
import java.util.List;

@RequiredArgsConstructor
public class HumanReadableXmlProducer extends DefaultHandler
        implements IDataSetProducer, ContentHandler, ErrorHandler {
    private static final IDataSetConsumer EMPTY_CONSUMER = new DefaultConsumer();
    private final InputSource inputSource;
    private IDataSetConsumer consumer = EMPTY_CONSUMER;

    private int tagNestingLevel;
    private String activeTableName;
    private boolean activeTableStarted;
    private final StringBuffer activeCharacters = new StringBuffer();
    private final List<String> activeColumnNames = new ArrayList<>();
    private final List<Object> activeRowValues = new ArrayList<>();
    private boolean isNullActiveValue;

    @Override
    public void setConsumer(IDataSetConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    @SneakyThrows
    public void produce() {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        XMLReader xmlReader = saxParserFactory.newSAXParser().getXMLReader();

        xmlReader.setContentHandler(this);
        xmlReader.setEntityResolver(this);
        xmlReader.setErrorHandler(this);
        xmlReader.parse(inputSource);
    }

    @SneakyThrows
    public void startElement(String uri, String localName, String qName, Attributes attributes) {

        switch (tagNestingLevel) {
            case 0 -> consumer.startDataSet();
            case 1 -> {
                if (activeTableName != null && !qName.equals(activeTableName)) {
                    consumer.endTable();
                    activeTableStarted = false;
                    activeColumnNames.clear();
                }
                activeTableName = qName;
            }
            case 2 -> {
                if (!activeTableStarted) {
                    activeColumnNames.add(qName);
                }
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
            case 0:
                if (activeTableName != null) {
                    consumer.endTable();
                }
                consumer.endDataSet();
                break;
            case 1:
                if (!activeTableStarted) {
                    consumer.startTable(createMetaData());
                    activeTableStarted = true;
                }
                consumer.row(activeRowValues.toArray());
                activeRowValues.clear();
                break;
            case 2:
                if (isNullActiveValue) {
                    activeRowValues.add(null);
                } else {
                    activeRowValues.add(activeCharacters.toString());
                }
                break;
        }
    }

    public void characters(char[] ch, int start, int length) {
        activeCharacters.append(ch, start, length);
    }

    public void error(SAXParseException e) throws SAXException {
        throw e;
    }

    private boolean isNull(Attributes attributes) {
        for (int i = 0; i < attributes.getLength(); i++) {
            if ("null".equalsIgnoreCase(attributes.getQName(i)) && "true".equalsIgnoreCase(attributes.getValue(i))) {
                return true;
            }
        }
        return false;
    }

    private ITableMetaData createMetaData() {
        Column[] columns = new Column[activeColumnNames.size()];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = new Column(activeColumnNames.get(i), DataType.UNKNOWN);
        }
        return new DefaultTableMetaData(activeTableName, columns);
    }
}
