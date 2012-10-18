package com.mgmtp.jfunk.web.util;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;

/**
 * @author rnaegele
 * @version $Id: $
 */
public abstract class BasePredicate<T> implements Predicate<T> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public final boolean apply(final T input) {
		log.info("Applying predicate: {}", this);
		boolean result = doApply(input);
		log.info("Predicate result: {}", result);
		return result;
	}

	protected abstract boolean doApply(final T input);

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
