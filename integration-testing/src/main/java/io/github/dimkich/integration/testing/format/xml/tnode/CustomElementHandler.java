package io.github.dimkich.integration.testing.format.xml.tnode;

import lombok.Getter;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;

import java.util.Objects;

@Getter
public class CustomElementHandler implements ElementHandler {
    private Element startElement;
    private Element endElement;

    @Override
    public void onStart(ElementPath elementPath) {
        startElement = elementPath.getCurrent();
    }

    @Override
    public void onEnd(ElementPath elementPath) {
        endElement = elementPath.getCurrent();
    }

    public boolean isStartAndEndElementsEquals() {
        return Objects.equals(startElement, endElement);
    }
}
