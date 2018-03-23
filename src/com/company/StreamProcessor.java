package com.company;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;

public class StreamProcessor implements AutoCloseable{
    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    private final XMLStreamReader reader;

    StreamProcessor(InputStream is) throws XMLStreamException {
        reader = FACTORY.createXMLStreamReader(is);
    }

    boolean startElement(String element, String stop_tag) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLEvent.START_ELEMENT && element.equals(reader.getLocalName()))
                return true;
            if(event == XMLEvent.START_ELEMENT && stop_tag.equals(reader.getLocalName()))
                return false;
        }
        return false;
    }

    String getAttribute(String name) throws XMLStreamException {
        return reader.getAttributeValue(null, name);
    }

    @Override
    public void close() {
        if (reader != null)
            try {
                reader.close();
            }catch (XMLStreamException e) {}
    }
}
