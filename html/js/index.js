
Ext.onReady(function() {

	/*
	 * Add google analytics tracking to AJAX requests.
	 */
	Ext.Ajax.on('beforerequest', function(connection, options) {
		pageTracker._trackPageview('/' + options.url);
	});


	/*
	 * Messages
	 */
	var msgSuccess = function(title, msg) {

		msg = "<div style=\"text-align: center; font-weight: bold;\">" + msg + "</div>";
		
		//msg = msg.concat("<br /><br />The converter code has recently been re-written; if you have old workouts which previously didn't convert with an \"InvalidRunError: null\" error - try them again, they should work now.");
		
		// Include the donate button randomly dependent on the date of the month.
		//if (Math.floor(Math.random() * (new Date().getDate()) * 2) == 0)			// 1/(day-of-month*2) likelhood?
		//if (Math.floor(Math.random() * (new Date().getDate())) == 0)
		if (Math.floor(Math.random() * 10) == 0)
			msg = msg.concat("<br /><br />If you are a regular user please consider donating." +
				"<br /><br /><form action=\"https://www.paypal.com/cgi-bin/webscr\" method=\"post\">" +
				"  <input type=\"hidden\" name=\"cmd\" value=\"_s-xclick\">" +
				"  <input type=\"hidden\" name=\"encrypted\" value=\"-----BEGIN PKCS7-----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-----END PKCS7-----\">" +
				"  <input type=\"image\" src=\"https://www.paypal.com/en_US/GB/i/btn/btn_donateCC_LG.gif\" border=\"0\" name=\"submit\" alt=\"PayPal - The safer, easier way to pay online.\">" +
				"  <img alt=\"\" border=\"0\" src=\"https://www.paypal.com/en_GB/i/scr/pixel.gif\" width=\"1\" height=\"1\">" +
				"</form>");

		Ext.Msg.show({
			title: title,
			msg: msg,
			minWidth: 300,
			modal: true,
			icon: Ext.Msg.INFO,
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


	/*
	 * Tabs
	 */
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
			}
		],

		listeners: {
            'tabchange': function(tabPanel, tab){
                pageTracker._trackPageview('/tcx2nikeplus/' + tab.contentEl);
            }
        }
    });





	/*
	 * TCX File validation
	 */
	function validateFileExtension(fileName) {
		var exp = /^.*.(tcx|TCX)$/;
		return exp.test(fileName);
	}


	/*
	 * Override Ext.util.Cookies.clear so it nullifies the cookie.
	 */
	Ext.util.Cookies.clear = function(name) {
		if (!this._clearExpireDate)
			this._clearExpireDate = new Date(0);

		this.set(name, '', this._clearExpireDate);
	};


	/*
	 * Converter Form
	 */
	var converterForm = new Ext.FormPanel({
		renderTo: 'divConverter',
		fileUpload: true,
		width: 480,
		frame: true,
		title: 'Garmin Forerunner TCX file to Nike+ Converter &amp; Uploader',
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

			// Garmin TCX File
			{
				xtype: 'fieldset',
				id: 'fsTcxFile',
				checkboxToggle: true,
				title: 'Garmin TCX file',
				autoHeight: true,
				collapsed: true,

				listeners: {
					expand: function(p) {
						p.items.each(
							function(i) {
								i.enable();
							}
						, this);
						
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
						
						Ext.getCmp('fsGarminId').expand();
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

			// Garmin Activity ID (nike+ heart-rate)
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

						Ext.getCmp('fsTcxFile').expand();
					}
				},

				items: [
					{
						xtype: 'textfield',
						id: 'garminActivityId',
						hideLabel: true,
						allowBlank: false,
						width: 100
					},

					// Include GPS?
					{
						xtype: 'checkbox',
						id: 'chkGps',
						fieldLabel: 'Include GPS',
						checked: true
					}
				]
			},





			
			// Simple Authentication
			{
				xtype: 'fieldset',
				id: 'fsAuthSimple',
				checkboxToggle: true,
				title: 'Simple Authentication',
				autoHeight: true,
				defaults: {
					anchor: '100%'
				},

				listeners: {
					expand: function(p) {
						p.items.each(
							function(i) {
								i.enable();
								i.allowBlank = false;
							}
						, this);

						Ext.getCmp('fsAuthAdvanced').collapse();
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


			// Advanced Authentication
			{
				xtype: 'fieldset',
				id: 'fsAuthAdvanced',
				checkboxToggle: true,
				title: 'Advanced Authentication',
				autoHeight: true,
				collapsed: true,
				defaults: {
					anchor: '100%'
				},
				
				listeners: {
					expand: function(p) {
						p.items.each(
							function(i) {
								i.enable();
							}
						, this);

						Ext.getCmp('nikePin').allowBlank = false;
						Ext.getCmp('fsAuthSimple').collapse();
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
					// Nike+ pin
					{
						xtype: 'textfield',
						id: 'nikePin',
						fieldLabel: 'Nike+ PIN'
					},

					// Emped ID
					{
						xtype: 'textfield',
						id: 'nikeEmpedId',
						fieldLabel: 'Emped ID'
					}
				]
			},


			// Remember settings?
			{
				xtype: 'checkbox',
				id: 'chkSaveCookies',
				fieldLabel: "Remember settings"
			}
		],

		buttons: [{
			id: 'btnSubmit',
			text: 'Convert &amp; Upload',
			handler: function() {
				if (converterForm.getForm().isValid()) {
					// If we are dealing with a TCX file upload then ensure the file extension is tcx.
					if ((!Ext.getCmp('garminTcxFile').disabled) && (!validateFileExtension(Ext.getCmp('garminTcxFile').getValue()))) {
						Ext.MessageBox.alert('Garmin TCX File', 'Only tcx files are accepted.');
						return;
					}


					// Simple Convert
					if ((Ext.getCmp('fsAuthSimple').collapsed) && (Ext.getCmp('fsAuthAdvanced').collapsed)) {
						// FIX-ME: There will be a nicer way to disable/enable the submit button on a simple xml download.
						this.setText('Please wait...');
						this.disable();
						converterForm.getForm().submit({
							url: 'tcx2nikeplus/convert'
						});
					}

					// Convert & Upload
					else {
						converterForm.getForm().submit({
							url: 'tcx2nikeplus/convert',
							params:{clientTimeZoneOffset : (0 - (new Date().getTimezoneOffset()))},
							timeout: 60000,
							waitMsg: 'Converting &amp; Uploading your workout, please wait...',
							success: function(converterForm, o) {

								// Save/Clear state
								// This is hacky but it'll do the job for now.
								if (Ext.getCmp('chkSaveCookies').checked) {

									var expiryDate = new Date(new Date().getTime()+(1000*60*60*24*90));		// 90 days

									Ext.util.Cookies.set('chkSaveCookies', true, expiryDate);

									if (Ext.getCmp('fsGarminId').collapsed) {
										Ext.util.Cookies.set('fsGarminIdCollapsed', true, expiryDate);
										Ext.util.Cookies.clear('chkGps');
									}
									else {
										Ext.util.Cookies.set('fsGarminIdCollapsed', false, expiryDate);
										Ext.util.Cookies.set('chkGps', Ext.getCmp('chkGps').getValue(), expiryDate);
									}

									if (Ext.getCmp('fsAuthAdvanced').collapsed) {
										Ext.util.Cookies.set('nikeEmail', Ext.getCmp('nikeEmail').getValue(), expiryDate);
										Ext.util.Cookies.clear('nikePin');
										Ext.util.Cookies.clear('nikeEmpedId');
									}

									if (Ext.getCmp('fsAuthSimple').collapsed) {
										Ext.util.Cookies.set('nikePin', Ext.getCmp('nikePin').getValue(), expiryDate);
										Ext.util.Cookies.set('nikeEmpedId', Ext.getCmp('nikeEmpedId').getValue(), expiryDate);
										Ext.util.Cookies.clear('nikeEmail');
									}
								}
								else {
									Ext.util.Cookies.set('chkSaveCookies', false);
									Ext.util.Cookies.clear('fsGarminIdCollapsed');
									Ext.util.Cookies.clear('fsTcxFile');
									Ext.util.Cookies.clear('chkGps');
									Ext.util.Cookies.clear('nikePin');
									Ext.util.Cookies.clear('nikeEmpedId');
									Ext.util.Cookies.clear('nikeEmail');
								}

								msgSuccess('Success', o.result.data.errorMessage);
							},
							failure: function(converterForm, o) {
								msgFailure('Failure', o.result.data.errorMessage);
							}
						});
					}
					
				}
			}
		},
		{
			text: 'Reset',
			handler: function() {
				Ext.getCmp('btnSubmit').setText('Convert &amp; Upload');
				Ext.getCmp('btnSubmit').enable();
				converterForm.getForm().reset();

				// Ensure that the fxTcxFile and fsAuthAdvanced elements are disabled.
				Ext.getCmp('fsTcxFile').items.each(
					function(i) {
						i.disable();
						i.allowBlank = true;
						i.validate();
					}
				, this);
				Ext.getCmp('fsAuthAdvanced').items.each(
					function(i) {
						i.disable();
						i.allowBlank = true;
						i.validate();
					}
				, this);
			}
		}]
	});



	
	/*
	 * Tooltips
	 */
	new Ext.ToolTip({
		target: 'garminTcxFile',
		title: 'Garmin TCX File',
		html: 'Click Browse to select a garmin tcx file to convert.'
	});

	new Ext.ToolTip({
		target: 'garminActivityId',
		title: 'Garmin Connect Activity ID',
		html: 'For the workout http://connect.garmin.com/activity/21742933 the activity ID is 21742933.'
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

	new Ext.ToolTip({
		target: 'nikePin',
		title: 'Nike+ PIN',
		html: 'Get your nike+ pin at<br />https://secure-nikerunning.nike.com/nikeplus/v2/services/app/generate_pin.jsp?login=LOGIN&password=PASSWORD'
	});

	new Ext.ToolTip({
		target: 'nikeEmpedId',
		title: 'Emped ID',
		html: 'Not required, although you may want to include it.'
	});





	/*
	 * Default values
	 */
	Ext.getUrlParam = function(param) {
		var params = Ext.urlDecode(location.search.substring(1));
		return param ? params[param] : params;
	};

	var type = Ext.getUrlParam('type');
	if ((type === 'garminActivityID') || (type === 'garminActivityID_GPS')) Ext.getCmp('fsGarminId').expand();

	Ext.getCmp('nikePin').setValue(Ext.getUrlParam('pin'));
	Ext.getCmp('nikeEmpedId').setValue(Ext.getUrlParam('empedID'));
	


	/*
	 * Cookies (these override the default values passed as http params).
	 */
	var fsGarminIdCollapsedValue = (Ext.util.Cookies.get('fsGarminIdCollapsed') === 'true');
	var chkGpsValue = Ext.util.Cookies.get('chkGps');
	var nikePinValue = Ext.util.Cookies.get('nikePin');
	var nikeEmpedIdValue = Ext.util.Cookies.get('nikeEmpedId');
	var nikeEmailValue = Ext.util.Cookies.get('nikeEmail');
	if (Ext.util.Cookies.get('chkSaveCookies') === 'true') {
		Ext.getCmp('chkSaveCookies').setValue(true);
		if (fsGarminIdCollapsedValue) Ext.getCmp('fsGarminId').collapse();
		if (chkGpsValue != null) Ext.getCmp('chkGps').setValue(chkGpsValue);
		if (nikePinValue != null ) Ext.getCmp('nikePin').setValue(nikePinValue);
		if (nikeEmpedIdValue != null ) Ext.getCmp('nikeEmpedId').setValue(nikeEmpedIdValue);
		if (nikeEmailValue != null ) Ext.getCmp('nikeEmail').setValue(nikeEmailValue);
	}


	// Select the default simple/advanced authentication option.
	if ((Ext.getCmp('nikePin').getValue().length > 0) || (Ext.getCmp('nikeEmpedId').getValue().length > 0))
		Ext.getCmp('fsAuthAdvanced').expand();
});
