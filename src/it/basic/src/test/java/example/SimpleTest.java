package example;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author markford
 */
public class SimpleTest {

    @Test
    public void marshall() throws Exception {
        JAXBContext context = JAXBContext.newInstance(POP3Host.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter sw = new StringWriter();
        m.marshal(new POP3Host(), sw);
        String xml = sw.toString();

        assertXml("/host.xml", xml);
    }

    @Test
    public void unmarshall() throws Exception {
        JAXBContext context = JAXBContext.newInstance(POP3Host.class);
        Unmarshaller u = context.createUnmarshaller();
        POP3Host back = (POP3Host) u.unmarshal(reader("/host.xml"));
        assertNotNull(back);
    }

    private void assertXml(String path, String xml) throws SAXException, IOException {
        Diff diff = XMLUnit.compareXML(reader(path), xml);
        assertTrue("expected the docs to be similar", diff.similar());
    }

    private Reader reader(String path) {
        return new InputStreamReader(getClass().getResourceAsStream(path));
    }

}
