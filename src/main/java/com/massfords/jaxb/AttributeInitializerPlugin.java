package com.massfords.jaxb;

import com.sun.codemodel.JFieldVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CAttributePropertyInfo;
import com.sun.tools.xjc.model.CCustomizations;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import org.jvnet.jaxb2_commons.util.CustomizationUtils;
import org.jvnet.jaxb2_commons.util.FieldAccessorUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fast and dirty JAXB plugin to add an attribute initializer to attributes that
 * have a default value but only get the default behavior provided when you call
 * the getter. This may be fine where you have JAXB or Schema aware parties on
 * both ends of the wire but in cases where you're going from JAXB to JSON it's
 * nice to provide all of the default values since the recipient may be worlds
 * away from having access to a Schema and would instead prefer to be told what
 * the default values for fields are.
 *
 * I had assumed that this was a solved problem but here's what I found:
 * - XJC behavior for default attributes is to provide the default value through
 * the getter. This is fine but if you're marshalling via the XmlFieldAccessor
 * then this doesn't help.
 * - There's a JAXB Default Value plugin but this focuses on elements since
 * attributes are supposedly covered.
 *
 * @author markford
 */
public class AttributeInitializerPlugin extends Plugin {
    /**
     * Namespace value that we look for in the schema
     */
    private static final String NAMESPACE_URI = "urn:com.massfords.jaxb";

    /**
     * The one element that we know how to process
     */
    private static final QName ATTR_INIT_QNAME = new QName(NAMESPACE_URI, "attrinit");

    /**
     * This is the flag to trigger the behavior
     */
    @Override
    public String getOptionName() {
        return "Xattrinit";
    }

    @Override
    public String getUsage() {
        return null;
    }

    @Override
    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) throws SAXException {

        for (ClassOutline classOutline : outline.getClasses()) {
            processClassOutline(outline, classOutline, errorHandler);
        }
        return true;
    }

    private void processClassOutline(Outline outline, ClassOutline classOutline,
                                       ErrorHandler errorHandler) throws SAXException {

        for (FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
            processFieldOutline(outline, fieldOutline, errorHandler);
        }
    }

    private void processFieldOutline(Outline outline, FieldOutline fieldOutline,
                                     ErrorHandler errorHandler) throws SAXException {
        // Get all customizations attached to this field
        final CCustomizations customizations =
                CustomizationUtils.getCustomizations(fieldOutline);

        // Look to see if it has any of the ones we're looking to handle
        List<CPluginCustomization> ourCustomizations = customizations.stream()
                .filter(cPluginCustomization ->
                        ATTR_INIT_QNAME.getNamespaceURI().equals(cPluginCustomization.element.getNamespaceURI()) &&
                                ATTR_INIT_QNAME.getLocalPart().equals(cPluginCustomization.element.getLocalName()))
                .collect(Collectors.toList());

        if (!ourCustomizations.isEmpty()) {

            // mark them as acknowledged
            ourCustomizations.forEach(CPluginCustomization::markAsAcknowledged);

            // warn the user if they've provided more than one customization
            // element. We're only going to process the first one in the list
            if (ourCustomizations.size()>1) {
                ourCustomizations.subList(1, ourCustomizations.size()).forEach(
                        c->warn("ignoring extra attrinit elements", errorHandler, c.locator)
                );
            }

            // used to report the location of an error
            Locator locator = ourCustomizations.get(0).locator;

            // get a reference to the field in question. This should never be
            // null but I'll check anyway and print a warning if it is
            final JFieldVar field = FieldAccessorUtils.field(fieldOutline);
            if (field != null) {
                CPropertyInfo propInfo = fieldOutline.getPropertyInfo();

                if (propInfo instanceof CAttributePropertyInfo) {
                    CAttributePropertyInfo attribPropInfo = (CAttributePropertyInfo) propInfo;
                    if (attribPropInfo.defaultValue != null) {
                        field.init(attribPropInfo.defaultValue.compute(outline));
                    } else {
                        fatal("extension element should only appear on attribute declarations with default value",
                                errorHandler, locator);
                    }
                } else {
                    fatal("extension element should only appear on attribute declarations",
                            errorHandler, locator);
                }
            } else {
                fatal("was expecting to find a field but didn't", errorHandler, locator);
            }
        } // no need for an else here. It's not a schema particle we're interested in
    }

    /**
     * Odd that the ErrorHandler has a checked exception when trying to report an error.
     * @param message
     * @param errorHandler
     * @param locator
     */
    private void warn(String message, ErrorHandler errorHandler, Locator locator) {
        try {
            errorHandler.warning(new SAXParseException(message, locator, null));
        } catch (SAXException e) {
            // ignore any exceptions while we're trying to warn the user
        }
    }

    /**
     * Reports a fatal exception to the error handler. These are conditions that
     * the user should be aware of because they've configured our plugin but
     * have configured it incorrectly.
     * @param message
     * @param errorHandler
     * @param locator
     * @throws SAXException
     */
    private void fatal(String message, ErrorHandler errorHandler, Locator locator) throws SAXException {
        errorHandler.fatalError(new SAXParseException(message, locator, null));
    }

    /**
     * Returns all of the namespaces we're looking to handle
     * @return
     */
    @Override
    public List<String> getCustomizationURIs() {
        return Collections.singletonList(NAMESPACE_URI);
    }

    /**
     * Returns true if it's an element we know how to handle
     * @param nsUri
     * @param localName
     * @return
     */
    @Override
    public boolean isCustomizationTagName(String nsUri, String localName) {
        return NAMESPACE_URI.equals(nsUri) && ATTR_INIT_QNAME.getLocalPart().equals(localName);
    }

}
