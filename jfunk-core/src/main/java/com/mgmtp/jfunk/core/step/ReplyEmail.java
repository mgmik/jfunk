/*
 * Copyright (c) 2013 mgm technology partners GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mgmtp.jfunk.core.step;

import java.io.File;
import java.util.Date;

import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;

import com.mgmtp.jfunk.core.config.ArchiveDir;
import com.mgmtp.jfunk.core.exception.MailException;
import com.mgmtp.jfunk.core.exception.StepException;
import com.mgmtp.jfunk.core.mail.BaseMailObject;
import com.mgmtp.jfunk.core.mail.EmailParser;
import com.mgmtp.jfunk.core.step.base.BaseStep;

/**
 * Waits for an e-mail matching subject and body patterns and replies to it.
 * 
 */
public class ReplyEmail extends BaseStep {

	private final String replyBody;
	private final String subjectPattern;
	private final String bodyPattern;

	@Inject
	EmailParser emailParser;

	@Inject
	@ArchiveDir
	File archiveDir;

	/**
	 * Creates a new instance.
	 * 
	 * @param subjectPattern
	 *            a regular expression matching the e-mail's subject
	 * @param bodyPattern
	 *            a regular expression matching the e-mail's body
	 * @param replyBody
	 *            the body content for the reply e-mail
	 */
	public ReplyEmail(final String subjectPattern, final String bodyPattern, final String replyBody) {
		this.subjectPattern = subjectPattern;
		this.bodyPattern = bodyPattern;
		this.replyBody = replyBody;
	}

	/**
	 * Tries to find an e-mail with matching subject and body and replies to it. The subject of the
	 * reply e-mail is prefixed with "RE: ".
	 */
	@Override
	public void execute() {
		log.info("Waiting for e-mail to reply to...");

		BaseMailObject mail = new BaseMailObject(subjectPattern, bodyPattern, emailParser, archiveDir) {
			@Override
			protected boolean processMsg(final Message msg, final String subjectString, final String bodyString)
					throws MailException {
				try {
					log.info("Replying to e-mail...");

					Message replyMsg = msg.reply(false);
					replyMsg.addFrom(msg.getAllRecipients());
					replyMsg.setSubject("RE: " + msg.getSubject());
					replyMsg.setSentDate(new Date());
					replyMsg.setText(replyBody);
					emailParser.send(msg);

					log.info("Reply e-mail successfully sent.");
					return true;
				} catch (MessagingException e) {
					throw new MailException("Error replying to e-mail", e);
				}
			}
		};

		try {
			if (!mail.read()) {
				throw new StepException("No e-mail received");
			}
		} catch (MailException e) {
			throw new StepException("Error receiving e-mails", e);
		} catch (MessagingException e) {
			throw new StepException("Error receiving e-mails", e);
		}
	}
}