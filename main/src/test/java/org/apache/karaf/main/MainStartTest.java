/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.main;

import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

import java.io.File;

import junit.framework.Assert;

import org.apache.karaf.main.util.Utils;
import org.junit.Test;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

public class MainStartTest {

	@Test
    public void testAutoStart() throws Exception {
        File basedir = new File(getClass().getClassLoader().getResource("foo").getPath()).getParentFile();
        File home = new File(basedir, "test-karaf-home");
        File data = new File(home, "data");

        Utils.deleteDirectory(data);

		String[] args = new String[0];
		System.setProperty("karaf.home", home.toString());
		System.setProperty("karaf.data", data.toString());

		Main main = new Main(args);
		main.launch();
		Framework framework = main.getFramework();
		Bundle[] bundles = framework.getBundleContext().getBundles();
		Assert.assertEquals(3, bundles.length);
		
		// Give the framework some time to start the bundles
		Thread.sleep(1000);

		Bundle bundle1 = framework.getBundleContext().getBundle("mvn:org.apache.aries.blueprint/org.apache.aries.blueprint.api/1.0.0");
		Assert.assertEquals(Bundle.ACTIVE, bundle1.getState());

		Bundle bundle2 = framework.getBundleContext().getBundle("pax-url-mvn.jar");
		Assert.assertEquals(Bundle.ACTIVE, bundle2.getState());

        main.destroy();
	}

    @Test
    public void testStopWithTimeout() throws Exception {
        File basedir = new File(getClass().getClassLoader().getResource("foo").getPath()).getParentFile();
        File home = new File(basedir, "test-karaf-home");
        File data = new File(home, "data");

        Utils.deleteDirectory(data);

		String[] args = new String[0];
		System.setProperty("karaf.home", home.toString());
		System.setProperty("karaf.data", data.toString());
        System.setProperty("karaf.framework.factory", "org.apache.felix.framework.FrameworkFactory");

        Main main = new Main(args);
        main.launch();
        Framework framework = main.getFramework();
        String activatorName = TimeoutShutdownActivator.class.getName().replace('.', '/') + ".class";
        Bundle bundle = framework.getBundleContext().installBundle("foo",
                TinyBundles.bundle()
                    .set( Constants.BUNDLE_ACTIVATOR, TimeoutShutdownActivator.class.getName() )
                    .add( activatorName, getClass().getClassLoader().getResourceAsStream( activatorName ) )
                    .build( withBnd() )
        );
        bundle.start();

        Thread.sleep(1000);

        long t0 = System.currentTimeMillis();
        Stop.main(null);
        main.awaitShutdown();
        long t1 = System.currentTimeMillis();
//        System.err.println("Shutdown duration: " + (t1 - t0) + " ms");
        Assert.assertTrue((t1 - t0) > TimeoutShutdownActivator.TIMEOUT / 2);
    }
}
