package io.github.dimkich.integration.testing.dbunit;

import org.dbunit.dataset.CachedDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.xml.sax.InputSource;

import java.io.*;
import java.nio.charset.Charset;

public class HumanReadableXmlDataSet extends CachedDataSet {

    public HumanReadableXmlDataSet(Reader reader) throws DataSetException {
        super(new HumanReadableXmlProducer(new InputSource(reader)));
    }

    public HumanReadableXmlDataSet(InputStream in) throws DataSetException {
        super(new HumanReadableXmlProducer(new InputSource(in)));
    }

    public static void write(IDataSet dataSet, OutputStream out) throws IOException, DataSetException {
        HumanReadableXmlDataSet.write(dataSet, out, null);
    }

    public static void write(IDataSet dataSet, OutputStream out, Charset charset) throws IOException, DataSetException {
        HumanReadableXmlWriter datasetWriter = new HumanReadableXmlWriter(out, charset);
        datasetWriter.write(dataSet);
    }

    public static void write(IDataSet dataSet, Writer writer) throws DataSetException {
        write(dataSet, writer, null);
    }

    public static void write(IDataSet dataSet, Writer writer, Charset charset) throws DataSetException {
        HumanReadableXmlWriter datasetWriter = new HumanReadableXmlWriter(writer, charset);
        datasetWriter.write(dataSet);
    }
}
