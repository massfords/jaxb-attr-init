# Attribute Initializer Plugin

Fast and dirty JAXB plugin to add an attribute initializer to attributes that
have a default value but only get the default behavior provided when you call
the getter. This may be fine where you have JAXB or Schema aware parties on
both ends of the wire but in cases where you're going from JAXB to JSON it's
nice to provide all of the default values since the recipient may be worlds
away from having access to a Schema and would instead prefer to be told what
the default values for fields are.

I had assumed that this was a solved problem but here's what I found:

- XJC behavior for default attributes is to provide the default value through
  the getter. This is fine but if you're marshalling via the XmlFieldAccessor
  then this doesn't help.
- There's a JAXB Default Value plugin but this focuses on elements since
  attributes are supposedly covered.

# Example Use Case

Consider the following schema and snippet:

```xml

<xs:complexType name="POP3Host">
    <xs:sequence>
        <xs:element name="foo" type="xs:string"/>
    </xs:sequence>
    <xs:attribute name="Port" type="xs:int" use="optional" default="110"/>
</xs:complexType>
```

```java
class Example {
    // default generated getter
    public int getPort() {
        if (port == null) {
            return 110;
        } else {
            return port;
        }
    }

    static void example() {
        POP3Host host = new POP3Host();
        int port = host.getPort();
        marshalToJSON(host);
    }
}
```

In the above snippet, the caller explicitly invokes the getter on the POP3Host
and with the default code generated from the XJC plugin, the port field will be
seen to be null and then initialized to 110 before getting returned.

However, what if the caller never invokes the getPort() method?

```java
class Example {
    static void example() {
        POP3Host host = new POP3Host();
        marshalToJSON(host);
    }
}
```

The recipient of this payload will get a structure that doesn't have a value for
the port field. If they are using JAXB with the same schema then this isn't a
problem. However, if they're not aware of the schema, then they won't know what
the default value is.

One common scenario for the above would be a presentation layer in JSON. Perhaps
there's some approach here where a JSON Schema could be derived from the XSD but
having the generated code initialize with the default value is much easier.

# Sample Schema

See simple.xsd in src/test/resources for an example of the annotation in play
along with a test case that runs the plugin. So far there are no assertions in the
test case but the generated code looks good. Again, assertions will come later if
there's a second commit on this project ;)

```xml

<xs:complexType name="POP3Host">
    <xs:sequence>
        <xs:element name="foo" type="xs:string"/>
    </xs:sequence>
    <xs:attribute name="Port" type="xs:int" use="optional" default="110">
        <xs:annotation>
            <xs:appinfo>
                <mf:attrinit/>
            </xs:appinfo>
        </xs:annotation>
    </xs:attribute>
</xs:complexType>
```

The application fo the mf:attrinit above triggers the plugin to generate an initializer
for the field.