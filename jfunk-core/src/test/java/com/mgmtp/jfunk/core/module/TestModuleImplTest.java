/*
 *  Copyright (C) 2013 mgm technology partners GmbH, Munich.
 *
 *  See the LICENSE file distributed with this work for additional
 *  information regarding copyright ownership and intellectual property rights.
 */
package com.mgmtp.jfunk.core.module;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import org.apache.commons.lang3.mutable.MutableInt;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.eventbus.EventBus;
import com.google.inject.Injector;
import com.mgmtp.jfunk.core.scripting.ExecutionMode;
import com.mgmtp.jfunk.core.scripting.StepExecutor;
import com.mgmtp.jfunk.core.step.base.BaseStep;

/**
 * @author rnaegele
 * @version $Id$
 */
public class TestModuleImplTest {

	private TestModuleImpl module;
	private MutableInt counter;

	@BeforeMethod
	public void setUp() {
		counter = new MutableInt();
		module = new CountingTestModul(counter);
		module.stepExecutor = new StepExecutor(mock(Injector.class), mock(EventBus.class));
	}

	@Test
	public void testExecuteAll() {
		module.stepExecutor = new StepExecutor(mock(Injector.class), mock(EventBus.class));
		module.executionMode = ExecutionMode.all;
		module.execute();
		assertEquals(counter.intValue(), 7);
	}

	@Test
	public void testExecuteStartToFirstBreak() {
		module.stepExecutor = new StepExecutor(mock(Injector.class), mock(EventBus.class));
		module.executionMode = ExecutionMode.start;
		module.breakIndex = 0;
		module.execute();
		assertEquals(counter.intValue(), 3);
	}

	@Test
	public void testExecuteStartToSecondBreak() {
		module.stepExecutor = new StepExecutor(mock(Injector.class), mock(EventBus.class));
		module.executionMode = ExecutionMode.start;
		module.breakIndex = 1;
		module.execute();
		assertEquals(counter.intValue(), 5);
	}

	@Test
	public void testExecuteFinishFromFirstBreak() {
		module.stepExecutor = new StepExecutor(mock(Injector.class), mock(EventBus.class));
		module.executionMode = ExecutionMode.finish;
		module.breakIndex = 0;
		module.execute();
		assertEquals(counter.intValue(), 4);
	}

	@Test
	public void testExecuteFinishFromSecondBreak() {
		module.stepExecutor = new StepExecutor(mock(Injector.class), mock(EventBus.class));
		module.executionMode = ExecutionMode.finish;
		module.breakIndex = 1;
		module.execute();
		assertEquals(counter.intValue(), 2);
	}

	static class CountingTestModul extends TestModuleImpl {

		private final MutableInt counter;

		public CountingTestModul(final MutableInt counter) {
			super("count", "count");
			this.counter = counter;
		}

		@Override
		protected void executeSteps() {
			executeSteps(
					new IncrementerStep(),
					new IncrementerStep());
			executeStep(new IncrementerStep(), true);
			executeStep(new IncrementerStep());
			executeStep(new IncrementerStep(), true);
			executeSteps(
					new IncrementerStep(),
					new IncrementerStep());
		}

		class IncrementerStep extends BaseStep {
			@Override
			public void execute() {
				counter.increment();
			}
		}
	}
}
