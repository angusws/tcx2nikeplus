
Ext.onReady(function() {

	// Add google analytics tracking to AJAX requests.
	Ext.Ajax.on('beforerequest', function(connection, options) {
		pageTracker._trackPageview('/' + options.url);
	});

	// Charity url string
	function getCharityAnchor(text) {
		return '<a href="http://www.awsmithson.com/charity" target="_blank">' + text + '</a>';
	}
	
	// Progress url string
	function getProgressAnchor(text) {
		return '<a href="http://www.awsmithson.com" target="_blank">' + text + '</a>';
	}

	// Messages
	var msgSuccess = function(title, msg, nikeActivityId) {

		msg = '<div style="text-align: center; font-weight: bold;">' + msg + '</div>';

		// Show Nike+ URL
		if (nikeActivityId) {
			var nikeActivityUrl = "https://secure-nikeplus.nike.com/plus/activity/running/detail/" + nikeActivityId;
			msg = msg.concat('<div style="text-align: center;">' +
				'View your workout at:<br />' +
				'<a href="' + nikeActivityUrl + '" target="_blank">' + nikeActivityUrl + '</a>' +
				'</div>');
		}

		// Show paypal donation option.
		if (Math.floor(Math.random() * 4) == 0) {
			msg = msg.concat("<br /><br />If you are regular user please consider donating to help cover the costs of domain/hosting &amp; future development.<br /><br />" +
				"<hr />" +
				"<b>Paypal</b>" +
				"<div style=\"text-align: center;\">" +
				"  <form action=\"https://www.paypal.com/cgi-bin/webscr\" method=\"post\">" +
				"    <input type=\"hidden\" name=\"cmd\" value=\"_s-xclick\">" +
				"    <input type=\"hidden\" name=\"encrypted\" value=\"-----BEGIN PKCS7-----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-----END PKCS7-----\">" +
				"    <input type=\"image\" src=\"https://www.paypal.com/en_US/GB/i/btn/btn_donateCC_LG.gif\" border=\"0\" name=\"submit\" alt=\"PayPal - The safer, easier way to pay online.\">" +
				"    <img alt=\"\" border=\"0\" src=\"https://www.paypal.com/en_GB/i/scr/pixel.gif\" width=\"1\" height=\"1\">" +
				"  </form>" +
				"</div>" +
				"<hr />" +
				"<b>Bitcoin</b>" +
				"<br /><a href=\"bitcoin:16aB2V5HkYrUxWRreWvrVdRH13m4bQMqZ?label=tcx2nikeplus%20donation\">1NVMCeoBfTAJQ1qwX2Dx1C8zkcRCQWwHBq</a>" +
				"<br />" +
				"<br /><a href=\"bitcoin:16aB2V5HkYrUxWRreWvrVdRH13m4bQMqZ?label=tcx2nikeplus%20donation\">" +
				"	<img alt=\"You can click or scan this QR code for the donation details.  Thanks.\" src=\"img/16aB2V5HkYrUxWRreWvrVdRH13m4bQMqZ.png\"/>" +
				"</a>" +
				"<hr />");
		}

		Ext.Msg.show({
			title: title,
			msg: msg,
			minWidth: 700,
			modal: true,
			buttons: Ext.Msg.OK
		});
	};

	var msgFailure = function(title, msg) {
		Ext.Msg.show({
			title: title,
			msg: msg,
			minWidth: 500,
			modal: true,
			icon: Ext.Msg.ERROR,
			buttons: Ext.Msg.OK
		});
	};

	// Tabs
	new Ext.TabPanel({
		renderTo: 'tabs',
		width: 580,
		activeTab: 0,
		plain: true,
		style: 'text-align: left;',

		defaults: {
			autoHeight: true
		},

		items: [
			{
				title: 'Instructions',
				contentEl:'tabInstructions'
			},
			{
				title: 'About',
				contentEl: 'tabAbout'
			},
			{
				title: 'News',
				contentEl: 'tabNews'
			},
			{
				title: 'Contact',
				contentEl: 'tabContact'
			},
			{
				title: 'Donate',
				contentEl: 'tabDonate'
			},
			{
				title: 'FAQ',
				contentEl: 'tabFAQ'
			}
		],

		listeners: {
			'tabchange': function(tabPanel, tab){
				pageTracker._trackPageview('/tcx2nikeplus/' + tab.contentEl);
			}
		}
	});

	// GPX File validation
	function validateFileExtensionGpx(fileName) {
		var exp = /^.*.(gpx)$/i;
		return exp.test(fileName);
	}

	// TCX File validation
	function validateFileExtensionTcx(fileName) {
		var exp = /^.*.(tcx)$/i;
		return exp.test(fileName);
	}

	// Override Ext.util.Cookies.clear so it nullifies the cookie.
	Ext.util.Cookies.clear = function(name) {
		if (!this._clearExpireDate)
			this._clearExpireDate = new Date(0);

		this.set(name, '', this._clearExpireDate);
	};

	// Converter Form
	var converterForm = new Ext.FormPanel({
		renderTo: 'divConverter',
		fileUpload: true,
		width: 480,
		frame: true,
		title: '[GPX / TCX / Garmin] &rarr; Nike+ Converter &amp; Uploader',
		autoHeight: true,
		bodyStyle: 'padding: 10px 10px 0 10px;',
		labelWidth: 116,
		style: 'text-align: left;',

		defaults: {
			anchor: '100%',
			allowBlank: true,
			msgTarget: 'side'
		},

		items: [
			// GPX File
			{
				xtype: 'fieldset',
				id: 'fsGpxFile',
				checkboxToggle: true,
				title: 'GPX file',
				autoHeight: true,
				collapsed: true,

				listeners: {
					expand: function(p) {
						p.items.each(
							function(i) {
								i.enable();
							}
						, this);

						Ext.getCmp('fsTcxFile').collapse();
						Ext.getCmp('fsGarminId').collapse();
					},

					collapse: function(p) {
						p.items.each(
							function(i) {
								i.disable();
								i.allowBlank = true;
								i.validate();
							}
						, this);
					}
				},

				items: [{
					xtype: 'fileuploadfield',
					id: 'gpxFile',
					anchor: '100%',
					hideLabel: true,
					emptyText: 'Select a gpx file',
					allowBlank: false,
					disabled: true
				}]
			},

			// TCX File
			{
				xtype: 'fieldset',
				id: 'fsTcxFile',
				checkboxToggle: true,
				title: 'TCX file',
				autoHeight: true,
				collapsed: true,

				listeners: {
					expand: function(p) {
						p.items.each(
							function(i) {
								i.enable();
							}
						, this);
						
						Ext.getCmp('fsGpxFile').collapse();
						Ext.getCmp('fsGarminId').collapse();
					},

					collapse: function(p) {
						p.items.each(
							function(i) {
								i.disable();
								i.allowBlank = true;
								i.validate();
							}
						, this);
					}
				},

				items: [{
					xtype: 'fileuploadfield',
					id: 'garminTcxFile',
					anchor: '100%',
					hideLabel: true,
					emptyText: 'Select a tcx file',
					allowBlank: false,
					disabled: true
				}]
			},

			// Garmin Activity ID
			{
				xtype: 'fieldset',
				id: 'fsGarminId',
				checkboxToggle: true,
				title: 'Garmin Activity ID',
				autoHeight: true,
				style: {marginBottom: '32px'},

				listeners: {
					expand: function(p) {
						p.items.each(
							function(i) {
								i.enable();
							}
						, this);
						
						Ext.getCmp('fsGpxFile').collapse();
						Ext.getCmp('fsTcxFile').collapse();
					},

					collapse: function(p) {
						p.items.each(
							function(i) {
								i.disable();
								i.allowBlank = true;
								i.validate();
							}
						, this);
					}
				},

				items: [
					{
						xtype: 'textfield',
						id: 'garminActivityId',
						hideLabel: true,
						allowBlank: false,
						anchor: '100%'
					}
				]
			},
			
			// Simple Authentication
			{
				xtype: 'fieldset',
				id: 'fsAuth',
				title: 'Nike+ Authentication',
				autoHeight: true,
				defaults: {
					anchor: '100%'
				},
				
				items: [
					// Nike+ Email
					{
						xtype: 'textfield',
						fieldLabel: 'Nike+ Email',
						id: 'nikeEmail',
						allowBlank: false
					},

					// Nike+ Password
					{
						xtype: 'textfield',
						inputType: 'password',
						fieldLabel: 'Nike+ Password',
						id: 'nikePassword',
						allowBlank: false
					}
				]
			},
			
			// Remember settings?
			{
				xtype: 'checkbox',
				id: 'chkSaveCookies',
				fieldLabel: 'Remember settings'
			}
		],

		buttons: [
			// Submit button
			{
				id: 'btnSubmit',
				text: 'Convert &amp; Upload',
				handler: function() {
					submit();
				}
			},

			// Reset button
			{
				text: 'Reset',
				handler: function() {
					var btnSubmit = Ext.getCmp('btnSubmit');
					btnSubmit.setText('Convert &amp; Upload');
					btnSubmit.enable();
					converterForm.getForm().reset();

					// Ensure that the fxTcxFile and fsAuthAdvanced elements are disabled.
					Ext.getCmp('fsTcxFile').items.each(
						function(i) {
							i.disable();
							i.allowBlank = true;
							i.validate();
						}
					, this);
				}
			}
		],

		// If the user hits the enter/return key, submit.
		keys: [{ 
			key: Ext.EventObject.ENTER, fn: function() {
				submit();
			} 
		}]
	});
	
	
	function submit() {
		
		var form = converterForm.getForm();
		
		if (form.isValid()) {
			// If we are dealing with a GPX file upload then ensure the file extension is gpx.
			var gpxFile = Ext.getCmp('gpxFile');
			if ((!gpxFile.disabled) && (!validateFileExtensionGpx(gpxFile.getValue()))) {
				Ext.MessageBox.alert('GPX File', 'Only gpx files are accepted.');
				return;
			}

			// If we are dealing with a TCX file upload then ensure the file extension is tcx.
			var garminTcxFile = Ext.getCmp('garminTcxFile');
			if ((!garminTcxFile.disabled) && (!validateFileExtensionTcx(garminTcxFile.getValue()))) {
				Ext.MessageBox.alert('Garmin TCX File', 'Only tcx files are accepted.');
				return;
			}

			// Convert & Upload
			// Construct wait message text.
			var msg = '<div style="text-align: center;"><b>Please wait while your workout is uploaded to nike+</b></div><br />';

			// Used to show donation options here (whilst the conversion/upload is taking place), but feel it would be confusing and
			// users wouldn't be comfortable clicking a link in case it disturbed the upload process (which it wouldn't).
			
			// Show wait message - this will get replaced in the success or failure callback of form.submit().
			Ext.MessageBox.show({
				msg: msg,
				width: 420,
				wait: true,
				waitConfig: { interval: 800 }
			});
			
			// Submit form.
			form.submit({
				method:'POST',
				url: 'tcx2nikeplus/convert',
				params:{clientTimeZoneOffset : (0 - (new Date().getTimezoneOffset()))},
				timeout: 60000,
				success: function(converterForm, o) {

					// Save/Clear state
					// This is hacky but it'll do the job for now.
					if (Ext.getCmp('chkSaveCookies').checked) {

						var expiryDate = new Date(new Date().getTime()+(1000*60*60*24*90));		// 90 days

						Ext.util.Cookies.set('chkSaveCookies', true, expiryDate);

						Ext.util.Cookies.set('fsGpxFileCollapsed', Ext.getCmp('fsGpxFile').collapsed, expiryDate);
						Ext.util.Cookies.set('fsTcxFileCollapsed', Ext.getCmp('fsTcxFile').collapsed, expiryDate);
						Ext.util.Cookies.set('fsGarminIdCollapsed', Ext.getCmp('fsGarminId').collapsed, expiryDate);

						Ext.util.Cookies.set('nikeEmail', Ext.getCmp('nikeEmail').getValue(), expiryDate);
					}
					else {
						Ext.util.Cookies.set('chkSaveCookies', false);
						Ext.util.Cookies.clear('fsGpxFileCollapsed');
						Ext.util.Cookies.clear('fsTcxFileCollapsed');
						Ext.util.Cookies.clear('fsGarminIdCollapsed');
						Ext.util.Cookies.clear('nikeEmail');
					}

					msgSuccess('Success', o.result.data.errorMessage, o.result.data.nikeActivityId);
				},
				failure: function(converterForm, o) {
					msgFailure('Failure', o.result.data.errorMessage);
				}
			});
		}
	}

	// Tooltips
	new Ext.ToolTip({
		target: 'gpxFile',
		title: 'GPX File',
		html: 'Click Browse to select a gpx file to convert.'
	});

	new Ext.ToolTip({
		target: 'garminTcxFile',
		title: 'TCX File',
		html: 'Click Browse to select a tcx file to convert.'
	});

	new Ext.ToolTip({
		target: 'garminActivityId',
		title: 'Garmin Connect Activity',
		html: 'For example: http://connect.garmin.com/activity/21742933'
	});

	new Ext.ToolTip({
		target: 'nikeEmail',
		title: 'Nike+ Email',
		html: 'The email address you use to log into http://www.nikeplus.com'
	});

	new Ext.ToolTip({
		target: 'nikePassword',
		title: 'Nike+ Password',
		html: 'The password you use to log into http://www.nikeplus.com'
	});


	// Default values
	Ext.getUrlParam = function(param) {
		var params = Ext.urlDecode(location.search.substring(1));
		return param ? params[param] : params;
	};

	var type = Ext.getUrlParam('type');
	if ((type === 'garminActivityID') || (type === 'garminActivityID_GPS')) Ext.getCmp('fsGarminId').expand();


	// Cookies (these override the default values passed as http params).
	if (Ext.util.Cookies.get('chkSaveCookies') === 'true') {
		Ext.getCmp('chkSaveCookies').setValue(true);

		if (Ext.util.Cookies.get('fsGpxFileCollapsed') === 'false') {
			Ext.getCmp('fsGpxFile').expand();
		} else if (Ext.util.Cookies.get('fsTcxFileCollapsed') === 'false') {
			Ext.getCmp('fsTcxFile').expand();
		} if (Ext.util.Cookies.get('fsGarminIdCollapsed') === 'false') {
			Ext.getCmp('fsGarminId').expand();
		}

		var nikeEmailValue = Ext.util.Cookies.get('nikeEmail');
		if (nikeEmailValue != null ) {
			Ext.getCmp('nikeEmail').setValue(nikeEmailValue);
		}
	}
});
