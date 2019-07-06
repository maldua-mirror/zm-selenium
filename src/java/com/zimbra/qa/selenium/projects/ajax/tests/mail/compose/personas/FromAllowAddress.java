/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.selenium.projects.ajax.tests.mail.compose.personas;

import org.testng.annotations.*;
import com.zimbra.common.soap.Element;
import com.zimbra.qa.selenium.framework.ui.Button;
import com.zimbra.qa.selenium.framework.util.*;
import com.zimbra.qa.selenium.projects.ajax.core.SetGroupMailByMessagePreference;
import com.zimbra.qa.selenium.projects.ajax.pages.mail.FormMailNew;
import com.zimbra.qa.selenium.projects.ajax.pages.mail.FormMailNew.Field;

public class FromAllowAddress extends SetGroupMailByMessagePreference {

	private String AllowEmailAddress = null;
	private String AllowFromDisplay = null;

	public FromAllowAddress() {
		logger.info("New "+ FromAllowAddress.class.getCanonicalName());
		super.startingAccountPreferences.put("zimbraPrefComposeFormat", "text");
	}

	@BeforeMethod( groups = { "always" } )
	public void addAliasToActiveAccount() throws HarnessException {

		AllowFromDisplay = "allowed" + ConfigProperties.getUniqueString();
		AllowEmailAddress = AllowFromDisplay +
					"@" +
					ConfigProperties.getStringProperty("testdomain");

		String identity = "identity" + ConfigProperties.getUniqueString();

		ZimbraAdminAccount.GlobalAdmin().soapSend(
					"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
				+		"<id>"+ app.zGetActiveAccount().ZimbraId +"</id>"
				+		"<a n='zimbraAllowFromAddress'>"+ AllowEmailAddress +"</a>"
				+	"</ModifyAccountRequest>");

		app.zGetActiveAccount().soapSend(
				" <CreateIdentityRequest xmlns='urn:zimbraAccount'>"
			+		"<identity name='"+ identity +"'>"
			+			"<a name='zimbraPrefIdentityName'>"+ identity +"</a>"
			+			"<a name='zimbraPrefFromDisplay'>"+ AllowFromDisplay +"</a>"
			+			"<a name='zimbraPrefFromAddress'>"+ AllowEmailAddress +"</a>"
			+			"<a name='zimbraPrefReplyToEnabled'>FALSE</a>"
			+			"<a name='zimbraPrefReplyToDisplay'/>"
			+			"<a name='zimbraPrefDefaultSignatureId'/>"
			+			"<a name='zimbraPrefForwardReplySignatureId'/>"
			+			"<a name='zimbraPrefWhenSentToEnabled'>FALSE</a>"
			+			"<a name='zimbraPrefWhenInFoldersEnabled'>FALSE</a>"
			+		"</identity>"
			+	"</CreateIdentityRequest>");

		// Refresh UI
		app.zPageMain.zRefreshMainUI();
	}


	@Test (description = "Send a mail using zimbraAllowFromAddress",
			groups = { "sanity" })

	public void FromAllowAddress_01() throws HarnessException {

		// Create the message data to be sent
		String subject = "subject" + ConfigProperties.getUniqueString();

		// Open the new mail form
		FormMailNew mailform = (FormMailNew) app.zPageMail.zToolbarPressButton(Button.B_NEW);
		ZAssert.assertNotNull(mailform, "Verify the new form opened");

		// Fill out the form with the data
		mailform.zFillField(Field.From, AllowEmailAddress);
		mailform.zFillField(Field.To, ZimbraAccount.AccountA().EmailAddress);
		mailform.zFillField(Field.Subject, subject);
		mailform.zFillField(Field.Body, "content" + ConfigProperties.getUniqueString());
		mailform.zSubmit();

		// Verify the message shows as from the alias
		ZimbraAccount.AccountA().soapSend(
					"<SearchRequest types='message' xmlns='urn:zimbraMail'>"
			+			"<query>subject:("+ subject +")</query>"
			+		"</SearchRequest>");
		String id = ZimbraAccount.AccountA().soapSelectValue("//mail:m", "id");

		ZimbraAccount.AccountA().soapSend(
					"<GetMsgRequest xmlns='urn:zimbraMail'>"
			+			"<m id='"+ id +"' html='1'/>"
			+		"</GetMsgRequest>");

		// Verify From: alias
		String address = ZimbraAccount.AccountA().soapSelectValue("//mail:e[@t='f']", "a");
		ZAssert.assertEquals(address, AllowEmailAddress, "Verify the from is the alias email address");

		// Verify no headers contain active account
		Element[] nodes = ZimbraAccount.AccountA().soapSelectNodes("//mail:e");
		for (Element e : nodes) {
			String attr = e.getAttribute("a", null);
			if ( attr != null ) {
				ZAssert.assertStringDoesNotContain(
						attr,
						app.zGetActiveAccount().EmailAddress,
						"Verify no headers contain the active account email address");
			}
		}
	}
}