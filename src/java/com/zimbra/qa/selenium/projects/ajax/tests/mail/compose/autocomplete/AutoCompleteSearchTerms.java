/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.selenium.projects.ajax.tests.mail.compose.autocomplete;

import java.util.*;

import org.testng.annotations.*;

import com.zimbra.qa.selenium.framework.core.Bugs;
import com.zimbra.qa.selenium.framework.items.*;
import com.zimbra.qa.selenium.framework.ui.*;
import com.zimbra.qa.selenium.framework.util.*;
import com.zimbra.qa.selenium.projects.ajax.core.PrefGroupMailByMessageTest;
import com.zimbra.qa.selenium.projects.ajax.ui.*;
import com.zimbra.qa.selenium.projects.ajax.ui.mail.FormMailNew;
import com.zimbra.qa.selenium.projects.ajax.ui.mail.FormMailNew.Field;


public class AutoCompleteSearchTerms extends PrefGroupMailByMessageTest {

	
	
	public AutoCompleteSearchTerms() throws HarnessException {
		logger.info("New "+ AutoCompleteGAL.class.getCanonicalName());
		
		super.startingAccountPreferences.put("zimbraPrefComposeFormat", "text");
		super.startingAccountPreferences.put("zimbraPrefGalAutoCompleteEnabled", "TRUE");
	
	}
	
	@Bugs(	ids = "46718")
	@Test(	description = "Autocomplete using stop word - IT",
			groups = { "functional" })
	public void AutCompleteSearchTerms_01() throws HarnessException {
		
		// See bug 46718
		final String StopWordsFirstName = "It" + ZimbraSeleniumProperties.getUniqueString();
		final String StopWordsLastName = "Be" + ZimbraSeleniumProperties.getUniqueString();


		// Create a GAL Entry
		ZimbraAccount StopWordsAccount = new ZimbraAccount();
		Map<String,String> attrs = new HashMap<String, String>() {
			private static final long serialVersionUID = -939087202049217426L;
			{
				put("givenName", StopWordsFirstName);
				put("sn", StopWordsLastName);
				put("displayName", StopWordsFirstName + " " + StopWordsLastName);
			}};
		StopWordsAccount.setAccountPreferences(attrs);
		StopWordsAccount.provision();
		StopWordsAccount.authenticate();


		// Message properties
		String subject = "subject" + ZimbraSeleniumProperties.getUniqueString();
		String body = "body" + ZimbraSeleniumProperties.getUniqueString();
		
		
		
		// Open the new mail form
		FormMailNew mailform = (FormMailNew) app.zPageMail.zToolbarPressButton(Button.B_NEW);
		ZAssert.assertNotNull(mailform, "Verify the new form opened");
		
		// Fill out the form with the data
		mailform.zFillField(Field.Subject, subject);
		mailform.zFillField(Field.Body, body);

		// Auto complete a name
		List<AutocompleteEntry> entries = mailform.zAutocompleteFillField(Field.To, "It");
		AutocompleteEntry found = null;
		for (AutocompleteEntry entry : entries) {
			if ( entry.getAddress().contains(StopWordsAccount.EmailAddress) ) {
				found = entry;
				break;
			}
		}
		ZAssert.assertNotNull(found, "Verify the autocomplete entry exists in the returned list");
		mailform.zAutocompleteSelectItem(found);
		
		// Send the message
		mailform.zSubmit();

		
		// Log into the destination account and make sure the message is received
		MailItem received = MailItem.importFromSOAP(StopWordsAccount, "subject:("+ subject +")");
		ZAssert.assertNotNull(received, "Verify the message is received correctly");
		
	}


	@Bugs(	ids = "46950")
	@Test(	description = "Autocomplete using search term - And",
			groups = { "functional" })
	public void AutCompleteSearchTerms_02() throws HarnessException {
		
		// See bug 46950
		final String QueryWordsFirstName = "Andrew" + ZimbraSeleniumProperties.getUniqueString();
		final String QueryWordsLastName = "Subject" + ZimbraSeleniumProperties.getUniqueString();

		// Create a GAL Entry
		ZimbraAccount QueryWordsAccount = new ZimbraAccount();
		Map<String,String> attrs = new HashMap<String, String>() {
			private static final long serialVersionUID = -939087202048117526L;
			{
				put("givenName", QueryWordsFirstName);
				put("sn", QueryWordsLastName);
				put("displayName", QueryWordsFirstName + " " + QueryWordsLastName);
			}};
			QueryWordsAccount.setAccountPreferences(attrs);
		QueryWordsAccount.provision();
		QueryWordsAccount.authenticate();


		// Message properties
		String subject = "subject" + ZimbraSeleniumProperties.getUniqueString();
		String body = "body" + ZimbraSeleniumProperties.getUniqueString();
		
		
		
		// Open the new mail form
		FormMailNew mailform = (FormMailNew) app.zPageMail.zToolbarPressButton(Button.B_NEW);
		ZAssert.assertNotNull(mailform, "Verify the new form opened");
		
		// Fill out the form with the data
		mailform.zFillField(Field.Subject, subject);
		mailform.zFillField(Field.Body, body);

		// Auto complete a name
		List<AutocompleteEntry> entries = mailform.zAutocompleteFillField(Field.To, "And");
		AutocompleteEntry found = null;
		for (AutocompleteEntry entry : entries) {
			if ( entry.getAddress().contains(QueryWordsAccount.EmailAddress) ) {
				found = entry;
				break;
			}
		}
		ZAssert.assertNotNull(found, "Verify the autocomplete entry exists in the returned list");
		mailform.zAutocompleteSelectItem(found);
		
		// Send the message
		mailform.zSubmit();

		
		// Log into the destination account and make sure the message is received
		MailItem received = MailItem.importFromSOAP(QueryWordsAccount, "subject:("+ subject +")");
		ZAssert.assertNotNull(received, "Verify the message is received correctly");
		
	}


}
