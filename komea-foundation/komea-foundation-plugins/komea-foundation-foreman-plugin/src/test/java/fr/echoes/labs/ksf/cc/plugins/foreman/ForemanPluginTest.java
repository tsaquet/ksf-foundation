package fr.echoes.labs.ksf.cc.plugins.foreman;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import fr.echoes.labs.ksf.cc.plugins.foreman.ForemanPlugin;
import fr.echoes.labs.ksf.extensions.api.IExtension;

public class ForemanPluginTest {

	@Test
	public void testGetExtensions() throws Exception {
		try (final ForemanPlugin foremanPlugin = new ForemanPlugin()) {
			foremanPlugin.init();
			final List<IExtension> extensions = foremanPlugin.getExtensions();
			System.out.println(extensions);
			assertEquals(2, extensions.size());
		}
	}

}
