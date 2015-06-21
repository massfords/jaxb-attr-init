package com.massfords.jaxb;

import org.jvnet.jaxb2.maven2.AbstractXJC2Mojo;
import org.jvnet.jaxb2.maven2.test.RunXJC2Mojo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author markford
 */
public class AttributeInitializerPluginTest extends RunXJC2Mojo {

    @Override
    public File getSchemaDirectory() {
        return new File(getBaseDir(), "src/test/resources");
    }

    @Override
    protected void configureMojo(AbstractXJC2Mojo mojo) {
        super.configureMojo(mojo);
        mojo.setForceRegenerate(true);
        mojo.setBindingExcludes(new String[]{"*.xjb"});
    }

    @Override
    public List<String> getArgs() {
        final List<String> args = new ArrayList<>(super.getArgs());
        args.add("-Xattrinit");
        args.add("-Xannotate");
        args.add("-extension");
        return args;
    }

    @Override
    public void testExecute() throws Exception {
        super.testExecute();
    }
}
