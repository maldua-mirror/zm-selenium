
package com.zimbra.qa.selenium.projects.ajax.tests.zimlets.smime.mail;


import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import com.zimbra.common.soap.Element;
import com.zimbra.qa.selenium.framework.items.MailItem;
import com.zimbra.qa.selenium.framework.ui.Action;
import com.zimbra.qa.selenium.framework.ui.Button;
import com.zimbra.qa.selenium.framework.util.ConfigProperties;
import com.zimbra.qa.selenium.framework.util.HarnessException;
import com.zimbra.qa.selenium.framework.util.SleepUtil;
import com.zimbra.qa.selenium.framework.util.ZAssert;
import com.zimbra.qa.selenium.framework.util.ZimbraAccount;
import com.zimbra.qa.selenium.framework.util.ZimbraAdminAccount;
import com.zimbra.qa.selenium.projects.ajax.core.AjaxCommonTest;
import com.zimbra.qa.selenium.projects.ajax.ui.mail.DisplayMail;
import com.zimbra.qa.selenium.projects.ajax.ui.mail.FormMailNew;
import com.zimbra.qa.selenium.projects.ajax.ui.mail.SeparateWindowDisplayMail;
import com.zimbra.qa.selenium.projects.ajax.ui.mail.DisplayMail.Field;
import com.zimbra.qa.selenium.projects.ajax.ui.zimlet.DialogViewCertificate;

public class ReplyUnsignedMail extends AjaxCommonTest {

	public ReplyUnsignedMail() {
		
		logger.info("New "+ ReplyUnsignedMail.class.getCanonicalName());		
	}

	@Test ( description = "Verify that an unsigned email can be replied as signed", priority=4, 
			groups = {"smoke", "L1", "network"})
	
	public void ReplyUnsignedMailAsSigned_01() throws HarnessException  {
		ZimbraAccount user3 = new ZimbraAccount("user3"+ "@" + ConfigProperties.getStringProperty("testdomain", "testdomain.com"), null);
		user3.provision();
		user3.authenticate();

		// Modify the test account and change zimbraFeatureSMIMEEnabled to TRUE
		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
			+		"<id>"+ user3.ZimbraId +"</id>"
			+		"<a n='zimbraFeatureSMIMEEnabled'>TRUE</a>"
			+	"</ModifyAccountRequest>");

		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
			+		"<id>"+ ZimbraAccount.Account1().ZimbraId +"</id>"
			+		"<a n='zimbraFeatureSMIMEEnabled'>TRUE</a>"
			+	"</ModifyAccountRequest>");
		
		// Create file item
		String filePath = ConfigProperties.getBaseDirectory()
				+ "/data/private/certs/user3_digitalid.p12";

		// Upload file to server through RestUtil
		String attachmentId = user3.uploadFile(filePath);

		user3.soapSend(
				"<SaveSmimeCertificateRequest xmlns='urn:zimbraAccount'>" +
				"<upload id='" + attachmentId + "'></upload>" +
                "<password>test123</password>" +
                "</SaveSmimeCertificateRequest>");
		
        app.zPageMain.zLogout();
		app.zPageLogin.zLogin(user3);
		
		String subject = "subject"+ ConfigProperties.getUniqueString();
		ZimbraAccount.Account1().soapSend(
				"<SendMsgRequest xmlns='urn:zimbraMail'>" +
					"<m>" +
						"<e t='t' a='"+ user3.EmailAddress +"'/>" +
						"<su>"+ subject +"</su>" +
						"<mp ct='text/plain'>" +
							"<content>content"+ ConfigProperties.getUniqueString() +"</content>" +
						"</mp>" +
					"</m>" +
				"</SendMsgRequest>");
	
		// Refresh current view
		ZAssert.assertTrue(app.zPageMail.zVerifyMailExists(subject), "Verify message displayed in current view");
	
		// Select the item
		app.zPageMail.zListItem(Action.A_LEFTCLICK, subject);
		

		// Reply the item
		FormMailNew mailform = (FormMailNew) app.zPageMail.zToolbarPressButton(Button.B_REPLY);

		// Fill out the form with the data
		String bodyContent = "Signed Content "+ ConfigProperties.getUniqueString();
		mailform.zFillField(com.zimbra.qa.selenium.projects.ajax.ui.mail.FormMailNew.Field.Body, bodyContent);
		
		//Choose sign only from the secure email drop-down
		mailform.zToolbarPressPulldown(Button.B_SECURE_EMAIL, Button.O_SIGN);
		SleepUtil.sleepSmall();
		
		// Send the message
		mailform.zSubmit();
		SleepUtil.sleepMedium();
		
		//Search for the signed mail in recipients inbox
		MailItem received = MailItem.importFromSOAP(ZimbraAccount.Account1(), "in:inbox subject:("+ subject +")");
		ZAssert.assertEquals(received.dFromRecipient.dEmailAddress, user3.EmailAddress, "Verify the from field is correct");
		ZAssert.assertEquals(received.dToRecipients.get(0).dEmailAddress, ZimbraAccount.Account1().EmailAddress, "Verify the to field is correct");
		ZAssert.assertStringContains(received.dBodyText, bodyContent, "Verify the body field is correct");
		ZAssert.assertEquals(received.dIsSigned, "true", "Verify that message is signed correctly");

		//Login as the recipient
        app.zPageMain.zLogout();
		app.zPageLogin.zLogin(ZimbraAccount.Account1());

		// Select the message so that it shows in the reading pane
		DisplayMail actual = (DisplayMail) app.zPageMail.zListItem(Action.A_LEFTCLICK, subject);
		
		// Verify body content
		ZAssert.assertStringContains(actual.zGetMailProperty(Field.Body), bodyContent, "Verify plain text content");
		
		//Verify the Mail security String
		ZAssert.assertTrue(actual.zMailSecurityPresent(user3.EmailAddress), "Signed by String present");

		//Data required for matching certificate details
		String issuedToOrganization = "Synacor";
		String issuedByOrganization = "Zimbra";
		String issuedByEmail = "admin@testdomain.com";
		String algorithm = "SHA256WITHRSA";
		
		//Click on View certificate.
		actual.zPressButton(Button.B_VIEW_CERTIFICATE);
		DialogViewCertificate dialog = (DialogViewCertificate) new DialogViewCertificate(app, app.zPageMail);
		
		//Verify certificate details
		ZAssert.assertEquals(user3.EmailAddress, dialog.zGetDisplayedText(user3.EmailAddress),"Issued to email address matched");
		ZAssert.assertEquals(issuedToOrganization, dialog.zGetDisplayedTextIssuedToOrganization(),"Issued to Organisation matched");
		ZAssert.assertEquals(issuedByOrganization, dialog.zGetDisplayedTextIssuedByOrganization(),"Issued by Organisation matched");
		ZAssert.assertEquals(issuedByEmail, dialog.zGetDisplayedTextIssuedByEmail(),"Issued by email address matched");
		ZAssert.assertEquals(algorithm, dialog.zGetDisplayedTextAlgorithm(),"Algorithm matched");
		
	}

	@Test ( description = "Verify that an unsigned email can be replied as signed and encrypted", priority=4, 
			groups = {"smoke", "L1", "network"})
	
	public void ReplyUnsignedMailAsSignedAndEncrypted_02() throws HarnessException  {
		ZimbraAccount user3 = new ZimbraAccount("user3"+ "@" + ConfigProperties.getStringProperty("testdomain", "testdomain.com"), null);
		user3.provision();
		user3.authenticate();

		ZimbraAccount user5 = new ZimbraAccount("user5"+ "@" + ConfigProperties.getStringProperty("testdomain", "testdomain.com"), null);
		user5.provision();
		user5.authenticate();
		
		// Modify the test account and change zimbraFeatureSMIMEEnabled to TRUE
		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
			+		"<id>"+ user3.ZimbraId +"</id>"
			+		"<a n='zimbraFeatureSMIMEEnabled'>TRUE</a>"
			+	"</ModifyAccountRequest>");

		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
			+		"<id>"+ user5.ZimbraId +"</id>"
			+		"<a n='zimbraFeatureSMIMEEnabled'>TRUE</a>"
			+	"</ModifyAccountRequest>");
		
		// Create file item
		String filePath = ConfigProperties.getBaseDirectory()
				+ "/data/private/certs/user3_digitalid.p12";

		// Upload file to server through RestUtil
		String attachmentId = user3.uploadFile(filePath);

		user3.soapSend(
				"<SaveSmimeCertificateRequest xmlns='urn:zimbraAccount'>" +
				"<upload id='" + attachmentId + "'></upload>" +
                "<password>test123</password>" +
                "</SaveSmimeCertificateRequest>");

		// Create file item
		filePath = ConfigProperties.getBaseDirectory()
				+ "/data/private/certs/user5_digitalid.p12";

		// Upload file to server through RestUtil
		 attachmentId = user5.uploadFile(filePath);

		user5.soapSend(
				"<SaveSmimeCertificateRequest xmlns='urn:zimbraAccount'>" +
				"<upload id='" + attachmentId + "'></upload>" +
                "<password>zimbra</password>" +
                "</SaveSmimeCertificateRequest>");
	
		// Create file item
		String certPath = ConfigProperties.getBaseDirectory()
				+ "/data/private/certs/user5.cer";

		// Upload file to server through RestUtil
		String certId = user3.uploadFile(certPath);

		
		user3.soapSend(
				"<CreateContactRequest xmlns='urn:zimbraMail'>" +
				"<cn>"+
				"<a n='firstName'>user5</a>" +
				"<a n='lastName'>user</a>" +
				"<a n='email'>" + user5.EmailAddress + "</a>" +
				"<a n='userCertificate' aid='" + certId + "'></a>" +
				"</cn>" +
				"</CreateContactRequest>");
		
        app.zPageMain.zLogout();
		app.zPageLogin.zLogin(user3);
		
		String subject = "subject"+ ConfigProperties.getUniqueString();
		user5.soapSend(
				"<SendMsgRequest xmlns='urn:zimbraMail'>" +
					"<m>" +
						"<e t='t' a='"+ user3.EmailAddress +"'/>" +
						"<su>"+ subject +"</su>" +
						"<mp ct='text/plain'>" +
							"<content>content"+ ConfigProperties.getUniqueString() +"</content>" +
						"</mp>" +
					"</m>" +
				"</SendMsgRequest>");
	
		// Refresh current view
		ZAssert.assertTrue(app.zPageMail.zVerifyMailExists(subject), "Verify message displayed in current view");
	
		// Select the item
		app.zPageMail.zListItem(Action.A_LEFTCLICK, subject);
		

		// Reply the item
		FormMailNew mailform = (FormMailNew) app.zPageMail.zToolbarPressButton(Button.B_REPLY);

		// Fill out the form with the data
		String bodyContent = "Signed & Encrypted Content "+ ConfigProperties.getUniqueString();
		mailform.zFillField(com.zimbra.qa.selenium.projects.ajax.ui.mail.FormMailNew.Field.Body, bodyContent);
		
		//Choose sign only from the secure email drop-down
		mailform.zToolbarPressPulldown(Button.B_SECURE_EMAIL, Button.O_SIGN_AND_ENCRYPT);
		SleepUtil.sleepSmall();
		
		// Send the message
		mailform.zSubmit();
		SleepUtil.sleepMedium();
		
		//Search for the signed mail in recipients inbox
		MailItem received = MailItem.importFromSOAP(user5, "in:inbox subject:("+ subject +")");
		ZAssert.assertEquals(received.dFromRecipient.dEmailAddress, user3.EmailAddress, "Verify the from field is correct");
		ZAssert.assertEquals(received.dToRecipients.get(0).dEmailAddress, user5.EmailAddress, "Verify the to field is correct");
		ZAssert.assertStringContains(received.dBodyText, bodyContent, "Verify the body field is correct");
		ZAssert.assertEquals(received.dIsSigned, "true", "Verify that message is signed correctly");
		ZAssert.assertEquals(received.dIsEncrypted, "true", "Verify that message is encrypted correctly");

		//Login as the recipient
        app.zPageMain.zLogout();
		app.zPageLogin.zLogin(user5);

		// Select the message so that it shows in the reading pane
		DisplayMail actual = (DisplayMail) app.zPageMail.zListItem(Action.A_LEFTCLICK, subject);
		
		//Verify the Mail security String
		ZAssert.assertTrue(actual.zMailSecurityPresent(user3.EmailAddress), "Signed and Encrypted by String present");

		//Data required for matching certificate details
		String issuedToOrganization = "Synacor";
		String issuedByOrganization = "Zimbra";
		String issuedByEmail = "admin@testdomain.com";
		String algorithm = "SHA256WITHRSA";
		
		//Click on View certificate.
		actual.zPressButton(Button.B_VIEW_CERTIFICATE);
		DialogViewCertificate dialog = (DialogViewCertificate) new DialogViewCertificate(app, app.zPageMail);

		//Verify certificate details
		ZAssert.assertEquals(user3.EmailAddress, dialog.zGetDisplayedText(user3.EmailAddress),"Issued to email address matched");
		ZAssert.assertEquals(issuedToOrganization, dialog.zGetDisplayedTextIssuedToOrganization(),"Issued to Organisation matched");
		ZAssert.assertEquals(issuedByOrganization, dialog.zGetDisplayedTextIssuedByOrganization(),"Issued by Organisation matched");
		ZAssert.assertEquals(issuedByEmail, dialog.zGetDisplayedTextIssuedByEmail(),"Issued by email address matched");
		ZAssert.assertEquals(algorithm, dialog.zGetDisplayedTextAlgorithm(),"Algorithm matched");
		
	}

	@Test ( description = "Verify that an unsigned email can be replied from a new window as signed", priority=4, 
			groups = {"smoke", "L1", "network"})
	
	public void ReplyUnsignedMailAsSigned_03() throws HarnessException  {
		ZimbraAccount user3 = new ZimbraAccount("user3"+ "@" + ConfigProperties.getStringProperty("testdomain", "testdomain.com"), null);
		user3.provision();
		user3.authenticate();

		// Modify the test account and change zimbraFeatureSMIMEEnabled to TRUE
		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
			+		"<id>"+ user3.ZimbraId +"</id>"
			+		"<a n='zimbraFeatureSMIMEEnabled'>TRUE</a>"
			+	"</ModifyAccountRequest>");

		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
			+		"<id>"+ ZimbraAccount.Account1().ZimbraId +"</id>"
			+		"<a n='zimbraFeatureSMIMEEnabled'>TRUE</a>"
			+	"</ModifyAccountRequest>");
		
		// Create file item
		String filePath = ConfigProperties.getBaseDirectory()
				+ "/data/private/certs/user3_digitalid.p12";

		// Upload file to server through RestUtil
		String attachmentId = user3.uploadFile(filePath);

		user3.soapSend(
				"<SaveSmimeCertificateRequest xmlns='urn:zimbraAccount'>" +
				"<upload id='" + attachmentId + "'></upload>" +
                "<password>test123</password>" +
                "</SaveSmimeCertificateRequest>");
		
        app.zPageMain.zLogout();
		app.zPageLogin.zLogin(user3);
		
		String subject = "subject"+ ConfigProperties.getUniqueString();
		String bodyContent = "Signed Content "+ ConfigProperties.getUniqueString();
		
		ZimbraAccount.Account1().soapSend(
				"<SendMsgRequest xmlns='urn:zimbraMail'>" +
					"<m>" +
						"<e t='t' a='"+ user3.EmailAddress +"'/>" +
						"<su>"+ subject +"</su>" +
						"<mp ct='text/plain'>" +
							"<content>"+ bodyContent +"</content>" +
						"</mp>" +
					"</m>" +
				"</SendMsgRequest>");
	
		// Refresh current view
		ZAssert.assertTrue(app.zPageMail.zVerifyMailExists(subject), "Verify message displayed in current view");
	
		// Select the item
		app.zPageMail.zListItem(Action.A_LEFTCLICK, subject);
		
		SeparateWindowDisplayMail window = null;
		String windowTitle = "Zimbra: " + subject;

		try {

			// Choose Actions -> Launch in Window
			window = (SeparateWindowDisplayMail)app.zPageMail.zToolbarPressPulldown(Button.B_ACTIONS, Button.B_LAUNCH_IN_SEPARATE_WINDOW);

			window.zSetWindowTitle(windowTitle);
			window.zWaitForActive();

			ZAssert.assertTrue(window.zIsActive(), "Verify the window is active");

			window.zToolbarPressButton(Button.B_REPLY);
			
			windowTitle = "Zimbra: Reply";
			window.zSetWindowTitle(windowTitle);
			window.zWaitForActive();
			ZAssert.assertTrue(window.zIsActive(), "Verify the window is active");
			window.zToolbarPressPulldown(Button.B_SECURE_EMAIL, Button.O_SIGN);
			SleepUtil.sleepSmall();
			window.zToolbarPressButton(Button.B_SEND);		
			SleepUtil.sleepMedium();
			
			windowTitle = "Zimbra: " + subject;
			window.zSetWindowTitle(windowTitle);
			window.zWaitForActive();
			window.zToolbarPressButton(Button.B_CLOSE);

		} finally {
			app.zPageMain.zCloseWindow(window, windowTitle, app);
		}	

		
		//Search for the signed mail in recipients inbox
		MailItem received = MailItem.importFromSOAP(ZimbraAccount.Account1(), "in:inbox subject:("+ subject +")");
		ZAssert.assertEquals(received.dFromRecipient.dEmailAddress, user3.EmailAddress, "Verify the from field is correct");
		ZAssert.assertEquals(received.dToRecipients.get(0).dEmailAddress, ZimbraAccount.Account1().EmailAddress, "Verify the to field is correct");
		ZAssert.assertStringContains(received.dBodyText, bodyContent, "Verify the body field is correct");
		ZAssert.assertEquals(received.dIsSigned, "true", "Verify that message is signed correctly");
		
	}

	@Test ( description = "Verify that an unsigned email can be replied from a new window as signed and encrypted", priority=4, 
			groups = {"smoke", "L1", "network"})
	
	public void ReplyUnsignedMailAsSignedAndEncrypted_04() throws HarnessException  {
		ZimbraAccount user3 = new ZimbraAccount("user3"+ "@" + ConfigProperties.getStringProperty("testdomain", "testdomain.com"), null);
		user3.provision();
		user3.authenticate();

		ZimbraAccount user5 = new ZimbraAccount("user5"+ "@" + ConfigProperties.getStringProperty("testdomain", "testdomain.com"), null);
		user5.provision();
		user5.authenticate();
		
		// Modify the test account and change zimbraFeatureSMIMEEnabled to TRUE
		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
			+		"<id>"+ user3.ZimbraId +"</id>"
			+		"<a n='zimbraFeatureSMIMEEnabled'>TRUE</a>"
			+	"</ModifyAccountRequest>");

		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
			+		"<id>"+ user5.ZimbraId +"</id>"
			+		"<a n='zimbraFeatureSMIMEEnabled'>TRUE</a>"
			+	"</ModifyAccountRequest>");
		
		// Create file item
		String filePath = ConfigProperties.getBaseDirectory()
				+ "/data/private/certs/user3_digitalid.p12";

		// Upload file to server through RestUtil
		String attachmentId = user3.uploadFile(filePath);

		user3.soapSend(
				"<SaveSmimeCertificateRequest xmlns='urn:zimbraAccount'>" +
				"<upload id='" + attachmentId + "'></upload>" +
                "<password>test123</password>" +
                "</SaveSmimeCertificateRequest>");

		// Create file item
		filePath = ConfigProperties.getBaseDirectory()
				+ "/data/private/certs/user5_digitalid.p12";

		// Upload file to server through RestUtil
		 attachmentId = user5.uploadFile(filePath);

		user5.soapSend(
				"<SaveSmimeCertificateRequest xmlns='urn:zimbraAccount'>" +
				"<upload id='" + attachmentId + "'></upload>" +
                "<password>zimbra</password>" +
                "</SaveSmimeCertificateRequest>");
	
		// Create file item
		String certPath = ConfigProperties.getBaseDirectory()
				+ "/data/private/certs/user5.cer";

		// Upload file to server through RestUtil
		String certId = user3.uploadFile(certPath);

		
		user3.soapSend(
				"<CreateContactRequest xmlns='urn:zimbraMail'>" +
				"<cn>"+
				"<a n='firstName'>user5</a>" +
				"<a n='lastName'>user</a>" +
				"<a n='email'>" + user5.EmailAddress + "</a>" +
				"<a n='userCertificate' aid='" + certId + "'></a>" +
				"</cn>" +
				"</CreateContactRequest>");
		
        app.zPageMain.zLogout();
		app.zPageLogin.zLogin(user3);
		
		String subject = "subject"+ ConfigProperties.getUniqueString();
		String bodyContent = "Signed and Encrypted Content "+ ConfigProperties.getUniqueString();
		
		user5.soapSend(
				"<SendMsgRequest xmlns='urn:zimbraMail'>" +
					"<m>" +
						"<e t='t' a='"+ user3.EmailAddress +"'/>" +
						"<su>"+ subject +"</su>" +
						"<mp ct='text/plain'>" +
							"<content>"+ bodyContent +"</content>" +
						"</mp>" +
					"</m>" +
				"</SendMsgRequest>");
	
		// Refresh current view
		ZAssert.assertTrue(app.zPageMail.zVerifyMailExists(subject), "Verify message displayed in current view");
	
		// Select the item
		app.zPageMail.zListItem(Action.A_LEFTCLICK, subject);
		
		SeparateWindowDisplayMail window = null;
		String windowTitle = "Zimbra: " + subject;

		try {

			// Choose Actions -> Launch in Window
			window = (SeparateWindowDisplayMail)app.zPageMail.zToolbarPressPulldown(Button.B_ACTIONS, Button.B_LAUNCH_IN_SEPARATE_WINDOW);

			window.zSetWindowTitle(windowTitle);
			window.zWaitForActive();

			ZAssert.assertTrue(window.zIsActive(), "Verify the window is active");

			window.zToolbarPressButton(Button.B_REPLY);
			
			windowTitle = "Zimbra: Reply";
			window.zSetWindowTitle(windowTitle);
			window.zWaitForActive();
			ZAssert.assertTrue(window.zIsActive(), "Verify the window is active");
			window.zToolbarPressPulldown(Button.B_SECURE_EMAIL, Button.O_SIGN_AND_ENCRYPT);
			SleepUtil.sleepSmall();
			window.zToolbarPressButton(Button.B_SEND);		
			SleepUtil.sleepMedium();
			
			windowTitle = "Zimbra: " + subject;
			window.zSetWindowTitle(windowTitle);
			window.zWaitForActive();
			window.zToolbarPressButton(Button.B_CLOSE);

		} finally {
			app.zPageMain.zCloseWindow(window, windowTitle, app);
		}	
		
		//Search for the signed mail in recipients inbox
		MailItem received = MailItem.importFromSOAP(user5, "in:inbox subject:("+ subject +")");
		ZAssert.assertEquals(received.dFromRecipient.dEmailAddress, user3.EmailAddress, "Verify the from field is correct");
		ZAssert.assertEquals(received.dToRecipients.get(0).dEmailAddress, user5.EmailAddress, "Verify the to field is correct");
		ZAssert.assertStringContains(received.dBodyText, bodyContent, "Verify the body field is correct");
		ZAssert.assertEquals(received.dIsSigned, "true", "Verify that message is signed correctly");
		ZAssert.assertEquals(received.dIsEncrypted, "true", "Verify that message is encrypted correctly");
		
	}

	@Test ( description = "Verify that an unsigned email with attachment can be replied as signed", priority=4, 
			groups = {"smoke", "L1", "network"})
	
	public void ReplyUnsignedMailWithAttachmentAsSigned_04() throws HarnessException  {
		ZimbraAccount user3 = new ZimbraAccount("user3"+ "@" + ConfigProperties.getStringProperty("testdomain", "testdomain.com"), null);
		user3.provision();
		user3.authenticate();

		// Modify the test account and change zimbraFeatureSMIMEEnabled to TRUE
		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
			+		"<id>"+ user3.ZimbraId +"</id>"
			+		"<a n='zimbraFeatureSMIMEEnabled'>TRUE</a>"
			+	"</ModifyAccountRequest>");

		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
			+		"<id>"+ ZimbraAccount.Account1().ZimbraId +"</id>"
			+		"<a n='zimbraFeatureSMIMEEnabled'>TRUE</a>"
			+	"</ModifyAccountRequest>");
		
		// Create file item
		String filePath = ConfigProperties.getBaseDirectory()
				+ "/data/private/certs/user3_digitalid.p12";

		// Upload file to server through RestUtil
		String attachmentId = user3.uploadFile(filePath);

		user3.soapSend(
				"<SaveSmimeCertificateRequest xmlns='urn:zimbraAccount'>" +
				"<upload id='" + attachmentId + "'></upload>" +
                "<password>test123</password>" +
                "</SaveSmimeCertificateRequest>");
		
        app.zPageMain.zLogout();
		app.zPageLogin.zLogin(user3);
		
		String subject = "subject"+ ConfigProperties.getUniqueString();
		String filename = "testpdffile.pdf";
		String filePath1 = ConfigProperties.getBaseDirectory() + "/data/public/other/"+ filename;
		String dAttachmentId  = ZimbraAccount.Account1().uploadFile(filePath1);
		
		ZimbraAccount.Account1().soapSend(
				"<SendMsgRequest xmlns='urn:zimbraMail'>" +
					"<m>" +
						"<e t='t' a='"+ user3.EmailAddress +"'/>" +
						"<su>"+ subject +"</su>" +
						"<mp ct='text/plain'>" +
							"<content>content"+ ConfigProperties.getUniqueString() +"</content>" +
						"</mp>" +
						"<attach aid='" + dAttachmentId + "'/>"+	
					"</m>" +
				"</SendMsgRequest>");
		
		// Refresh current view
		ZAssert.assertTrue(app.zPageMail.zVerifyMailExists(subject), "Verify message displayed in current view");
	
		// Select the item
		app.zPageMail.zListItem(Action.A_LEFTCLICK, subject);
		

		// Reply the item
		FormMailNew mailform = (FormMailNew) app.zPageMail.zToolbarPressButton(Button.B_REPLY);

		// Fill out the form with the data
		String bodyContent = "Signed Content "+ ConfigProperties.getUniqueString();
		mailform.zFillField(com.zimbra.qa.selenium.projects.ajax.ui.mail.FormMailNew.Field.Body, bodyContent);
		
		//Choose sign only from the secure email drop-down
		mailform.zToolbarPressPulldown(Button.B_SECURE_EMAIL, Button.O_SIGN);
		SleepUtil.sleepSmall();
		
		// Send the message
		mailform.zSubmit();
		SleepUtil.sleepMedium();
		
		//Search for the signed mail in recipients inbox
		MailItem received = MailItem.importFromSOAP(ZimbraAccount.Account1(), "in:inbox subject:("+ subject +")");
		ZAssert.assertEquals(received.dFromRecipient.dEmailAddress, user3.EmailAddress, "Verify the from field is correct");
		ZAssert.assertEquals(received.dToRecipients.get(0).dEmailAddress, ZimbraAccount.Account1().EmailAddress, "Verify the to field is correct");
		ZAssert.assertStringContains(received.dBodyText, bodyContent, "Verify the body field is correct");
		ZAssert.assertEquals(received.dIsSigned, "true", "Verify that message is signed correctly");
		
		ZimbraAccount.Account1().soapSend(
				"<GetMsgRequest xmlns='urn:zimbraMail'>"
				+		"<m id='"+ received.getId() +"'/>"
				+	"</GetMsgRequest>");

		Element[] nodes = ZimbraAccount.Account1().soapSelectNodes("//mail:mp[@cd='attachment']");
		ZAssert.assertEquals(nodes.length, 1, "Verify the attachment does not exist in the replied mail");
		
	}

	@Test ( description = "Verify that an unsigned email with attachment can be replied as signed", priority=4, 
			groups = {"smoke", "L1", "network"})
	
	public void ReplySignedMailWithAttachmentAsSigned_05() throws HarnessException  {
		ZimbraAccount user5 = new ZimbraAccount("user5"+ "@" + ConfigProperties.getStringProperty("testdomain", "testdomain.com"), null);
		user5.provision();
		user5.authenticate();

		ZimbraAccount user6 = new ZimbraAccount("user6"+ "@" + ConfigProperties.getStringProperty("testdomain", "testdomain.com"), null);
		user6.provision();
		user6.authenticate();
		
		// Modify the test account and change zimbraFeatureSMIMEEnabled to TRUE
		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
			+		"<id>"+ user5.ZimbraId +"</id>"
			+		"<a n='zimbraFeatureSMIMEEnabled'>TRUE</a>"
			+	"</ModifyAccountRequest>");

		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
			+		"<id>"+ user6.ZimbraId +"</id>"
			+		"<a n='zimbraFeatureSMIMEEnabled'>TRUE</a>"
			+	"</ModifyAccountRequest>");
		
		// Create file item
		String filePath = ConfigProperties.getBaseDirectory()
				+ "/data/private/certs/user5_digitalid.p12";

		// Upload file to server through RestUtil
		String attachmentId = user5.uploadFile(filePath);

		user5.soapSend(
				"<SaveSmimeCertificateRequest xmlns='urn:zimbraAccount'>" +
				"<upload id='" + attachmentId + "'></upload>" +
                "<password>zimbra</password>" +
                "</SaveSmimeCertificateRequest>");

		// Create file item
		filePath = ConfigProperties.getBaseDirectory()
				+ "/data/private/certs/user6_digitalid.p12";

		// Upload file to server through RestUtil
		 attachmentId = user6.uploadFile(filePath);

		user6.soapSend(
				"<SaveSmimeCertificateRequest xmlns='urn:zimbraAccount'>" +
				"<upload id='" + attachmentId + "'></upload>" +
                "<password>zimbra</password>" +
                "</SaveSmimeCertificateRequest>");
			
        app.zPageMain.zLogout();
		app.zPageLogin.zLogin(user5);
		
		String subject = "subject"+ ConfigProperties.getUniqueString();
		String filename = "testpdffile.pdf";
		String filePath1 = ConfigProperties.getBaseDirectory() + "/data/public/other/"+ filename;
		String dAttachmentId  = user6.uploadFile(filePath1);
		
		user6.soapSend(
				"<SendSecureMsgRequest sign='true' xmlns='urn:zimbraMail'>" +
					"<m>" +
						"<e t='t' a='"+ user5.EmailAddress +"'/>" +
						"<su>"+ subject +"</su>" +
						"<mp ct='text/plain'>" +
							"<content>content"+ ConfigProperties.getUniqueString() +"</content>" +
						"</mp>" +
						"<attach aid='" + dAttachmentId + "'/>"+	
					"</m>" +
				"</SendSecureMsgRequest>");
		
		// Refresh current view
		ZAssert.assertTrue(app.zPageMail.zVerifyMailExists(subject), "Verify message displayed in current view");
	
		// Select the item
		app.zPageMail.zListItem(Action.A_LEFTCLICK, subject);
		

		// Reply the item
		FormMailNew mailform = (FormMailNew) app.zPageMail.zToolbarPressButton(Button.B_REPLY);

		// Fill out the form with the data
		String bodyContent = "Signed Content "+ ConfigProperties.getUniqueString();
		mailform.zFillField(com.zimbra.qa.selenium.projects.ajax.ui.mail.FormMailNew.Field.Body, bodyContent);
		
		//Choose sign only from the secure email drop-down
		mailform.zToolbarPressPulldown(Button.B_SECURE_EMAIL, Button.O_SIGN);
		SleepUtil.sleepSmall();
		
		// Send the message
		mailform.zSubmit();
		SleepUtil.sleepMedium();
		
		//Search for the signed mail in recipients inbox
		MailItem received = MailItem.importFromSOAP(user6, "in:inbox subject:("+ subject +")");
		ZAssert.assertEquals(received.dFromRecipient.dEmailAddress, user5.EmailAddress, "Verify the from field is correct");
		ZAssert.assertEquals(received.dToRecipients.get(0).dEmailAddress, user6.EmailAddress, "Verify the to field is correct");
		ZAssert.assertStringContains(received.dBodyText, bodyContent, "Verify the body field is correct");
		ZAssert.assertEquals(received.dIsSigned, "true", "Verify that message is signed correctly");
		
		user6.soapSend(
				"<GetMsgRequest xmlns='urn:zimbraMail'>"
				+		"<m id='"+ received.getId() +"'/>"
				+	"</GetMsgRequest>");

		Element[] nodes = user6.soapSelectNodes("//mail:mp[@cd='attachment']");
		ZAssert.assertEquals(nodes.length, 1, "Verify the attachment does not exist in the replied mail");
		
	}

	
	@AfterMethod(groups={"always"})
    public void afterMethod() throws HarnessException {
	
	ZimbraAdminAccount.GlobalAdmin().soapSend(
			"<GetAccountRequest xmlns='urn:zimbraAdmin'> "+
			"<account by='name'>user3@testdomain.com</account>" + 
			"</GetAccountRequest>");
    String user3Id = ZimbraAdminAccount.GlobalAdmin().soapSelectValue("//admin:GetAccountResponse/admin:account", "id");
	
	ZimbraAdminAccount.GlobalAdmin().soapSend(
			"<DeleteAccountRequest xmlns='urn:zimbraAdmin'>"
		+		"<id>"+ user3Id + "</id>"
		+	"</DeleteAccountRequest>");
	
	ZimbraAdminAccount.GlobalAdmin().soapSend(
			"<GetAccountRequest xmlns='urn:zimbraAdmin'> "+
			"<account by='name'>user5@testdomain.com</account>" + 
			"</GetAccountRequest>");
    String user5Id = ZimbraAdminAccount.GlobalAdmin().soapSelectValue("//admin:GetAccountResponse/admin:account", "id");
	
	ZimbraAdminAccount.GlobalAdmin().soapSend(
			"<DeleteAccountRequest xmlns='urn:zimbraAdmin'>"
		+		"<id>"+ user5Id + "</id>"
		+	"</DeleteAccountRequest>");
	
	ZimbraAdminAccount.GlobalAdmin().soapSend(
			"<GetAccountRequest xmlns='urn:zimbraAdmin'> "+
			"<account by='name'>user6@testdomain.com</account>" + 
			"</GetAccountRequest>");
    String user6Id = ZimbraAdminAccount.GlobalAdmin().soapSelectValue("//admin:GetAccountResponse/admin:account", "id");
	
	ZimbraAdminAccount.GlobalAdmin().soapSend(
			"<DeleteAccountRequest xmlns='urn:zimbraAdmin'>"
		+		"<id>"+ user6Id + "</id>"
		+	"</DeleteAccountRequest>");

	}
	
}