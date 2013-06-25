/*
 *  Copyright (C) 2013 mgm technology partners GmbH, Munich.
 *
 *  See the LICENSE file distributed with this work for additional
 *  information regarding copyright ownership and intellectual property rights.
 */
package com.mgmtp.jfunk.core.module;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayListWithExpectedSize;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.mgmtp.jfunk.common.JFunkConstants;
import com.mgmtp.jfunk.common.util.Configuration;
import com.mgmtp.jfunk.core.config.ArchiveDir;
import com.mgmtp.jfunk.core.config.ModuleArchiveDir;
import com.mgmtp.jfunk.core.module.ContainerModule.Builder.ModuleWithCallbacks;
import com.mgmtp.jfunk.core.scripting.ScriptContext;

/**
 * Allows grouping of independent {@link TestModule}s into one container. The main purpose of this
 * is that, in addition to the test archives of the individual {@link TestModule}s, an overall test
 * archive is written containing the individual archives and a log file of the complete execution of
 * all contained {@link TestModule}s. A {@link ContainerModule} is created using
 * {@link ContainerModule.Builder}:
 * 
 * <pre>
 * 
 * &#064;Test
 * public void testContainerModule() {
 * 	jFunkRunner.set(&quot;archive.dir&quot;, &quot;testruns/container&quot;);
 * 
 * 	TestModule testModule = ContainerModule.forName(&quot;container&quot;)
 * 			.addTestModule(new FooModule(), new Runnable() {
 * 				&#064;Override
 * 				public void run() {
 * 					jFunkRunner.prepareNextDataSet(&quot;foo&quot;);
 * 				}
 * 			}, null)
 * 			.addTestModule(new BarModule(), new Runnable() {
 * 				&#064;Override
 * 				public void run() {
 * 					jFunkRunner.prepareNextDataSet(&quot;bar&quot;);
 * 				}
 * 			}, null)
 * 			.build();
 * 
 * 	jFunkRunner.run(testModule);
 * }
 * </pre>
 * 
 * @author rnaegele
 * @since 3.1
 */
public class ContainerModule implements TestModule {
	private final String name;
	private final List<ModuleWithCallbacks> modulesWithCallbacks;
	private boolean error;

	@Inject
	private ScriptContext scriptContext;

	@Inject
	@ModuleArchiveDir
	// need provider because it is not yet set at injection time
	private Provider<File> moduleArchiveDirProvider;

	@Inject
	@ArchiveDir
	// need provider because it is not yet set at injection time
	private Provider<File> archiveDirProvider;

	@Inject
	private Configuration config;

	private ContainerModule(final String name, final List<ModuleWithCallbacks> modulesWithCallbacks) {
		this.name = checkNotNull(name, "'name' must not be null");
		this.modulesWithCallbacks = checkNotNull(modulesWithCallbacks, "'modulesWithCallbacks' must not be null");
		checkState(modulesWithCallbacks.size() > 0, "At least one test module must be specified");
	}

	/**
	 * @return the module name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Executes the added test modules and their callbacks in the order of their addition using
	 * {@link ScriptContext#run(TestModule)}.
	 */
	@Override
	public void execute() {
		// save archive dir of the outer module so it can be reset later
		String previousArchiveDir = archiveDirProvider.get().getPath();

		try {
			// make sure archive dirs are within that of the container module
			config.put(JFunkConstants.ARCHIVE_DIR, moduleArchiveDirProvider.get().getPath());

			for (ModuleWithCallbacks moduleWithCallbacks : modulesWithCallbacks) {
				TestModule testModule = moduleWithCallbacks.testModule;

				Runnable beforeModuleCallback = moduleWithCallbacks.beforeModuleCallback;
				if (beforeModuleCallback != null) {
					beforeModuleCallback.run();
				}

				scriptContext.run(testModule);

				Runnable afterModuleCallback = moduleWithCallbacks.afterModuleCallback;
				if (afterModuleCallback != null) {
					afterModuleCallback.run();
				}
			}
		} finally {
			// reset for outer module, so property is correctly archived
			config.put(JFunkConstants.ARCHIVE_DIR, previousArchiveDir);

			// it does not make sense to archive datasets in the container module, 
			// these would be those of the test module executed last
			config.put(JFunkConstants.ARCHIVE_DATASETS, "false");
		}
	}

	@Override
	public String getDataSetKey() {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setDataSetKey(final String dataSetKey) {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setError(final boolean error) {
		this.error = error;
	}

	@Override
	public boolean isError() {
		return error;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(ToStringStyle.SHORT_PREFIX_STYLE);
	}

	/**
	 * Creates a {@link Builder} for creating a {@link ContainerModule} with the specified name.
	 * 
	 * @param name
	 *            the module name
	 * @return the {@link Builder}
	 */
	public static Builder forName(final String name) {
		return new Builder(name);
	}

	/**
	 * Builder for {@link ContainerModule}.
	 * 
	 * @author rnaegele
	 */
	public static class Builder {

		private final String name;
		private final List<ModuleWithCallbacks> modulesWithCallbacks = newArrayListWithExpectedSize(2);

		/**
		 * @param name
		 *            the module name
		 */
		public Builder(final String name) {
			this.name = name;
		}

		/**
		 * Adds a test module to the {@link ContainerModule}.
		 * 
		 * @param testModule
		 *            the test module to be added to the {@link ContainerModule}
		 * @return this {@link Builder} instance
		 */
		public Builder addTestModule(final TestModule testModule) {
			modulesWithCallbacks.add(new ModuleWithCallbacks(testModule, null, null));
			return this;
		}

		/**
		 * Adds a test module and the specified callbacks to the {@link ContainerModule}. Callbacks
		 * are executed before and/or after the specified module in the scope of the
		 * {@link ContainerModule}.
		 * 
		 * @param testModule
		 *            the test module to be added to the {@link ContainerModule}
		 * @param beforeModuleCallback
		 *            a {@link Runnable} to be executed just before the specified module is
		 *            executed, may be {@code null}
		 * @param afterModuleCallback
		 *            a {@link Runnable} to be executed right after the specified module is
		 *            executed, may be {@code null}
		 * @return this {@link Builder} instance
		 */
		public Builder addTestModule(final TestModule testModule, final Runnable beforeModuleCallback,
				final Runnable afterModuleCallback) {
			modulesWithCallbacks.add(new ModuleWithCallbacks(testModule, beforeModuleCallback, afterModuleCallback));
			return this;
		}

		/**
		 * Create the {@link ContainerModule} from the {@link Builder}.
		 * 
		 * @return the {@link ContainerModule}
		 */
		public ContainerModule build() {
			return new ContainerModule(name, modulesWithCallbacks);
		}

		static class ModuleWithCallbacks {
			public ModuleWithCallbacks(final TestModule testModule, final Runnable beforeModuleCallback,
					final Runnable afterModuleCallback) {
				this.testModule = testModule;
				this.beforeModuleCallback = beforeModuleCallback;
				this.afterModuleCallback = afterModuleCallback;
			}

			TestModule testModule;
			Runnable beforeModuleCallback;
			Runnable afterModuleCallback;
		}
	}
}