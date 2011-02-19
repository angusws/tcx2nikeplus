
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
		if (Math.floor(Math.random() * (new Date().getDate()) * 2) == 0)
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
		width: 700,
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
	 * Converter Form
	 */
	var converterForm = new Ext.FormPanel({
		renderTo: 'divConverter',
		fileUpload: true,
		width: 448,
		frame: true,
		title: 'Garmin Forerunner TCX file to Nike+ Converter &amp; Uploader',
		autoHeight: true,
		bodyStyle: 'padding: 10px 10px 0 10px;',
		labelWidth: 100,
		style: 'text-align: left;',


		defaults: {
			anchor: '95%',
			allowBlank: true,
			msgTarget: 'side'
		},


        items: [

			// Garmin TCX File
			{
				xtype: 'fieldset',
				id: 'fileContainer',
				checkboxToggle: true,
				title: 'Garmin TCX file',
				autoHeight: true,
				collapsed: true,

				listeners: {
					expand: function() {
						Ext.getCmp('garminTcxFile').enable();
						Ext.getCmp('idContanier').collapse();
						Ext.getCmp('garminActivityId').disable();
					},

					collapse: function() {
						Ext.getCmp('idContanier').expand();
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
				id: 'idContanier',
				checkboxToggle: true,
				title: 'Garmin Activity ID',
				autoHeight: true,
				style: {marginBottom: '32px'},

				listeners: {
					expand: function() {
						Ext.getCmp('garminActivityId').enable();
						Ext.getCmp('fileContainer').collapse();
						Ext.getCmp('garminTcxFile').disable();
					},

					collapse: function() {
						Ext.getCmp('fileContainer').expand();
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

					{
						xtype: 'radiogroup',
						fieldLabel: 'Upload options',
						columns: [50, 100],
						items: [
							{boxLabel: 'GPS', name: 'rbGpsHeartRate', inputValue: 'gps', checked: true},
							{boxLabel: 'Heart Rate', name: 'rbGpsHeartRate', inputValue: 'heartrate'}
						]
					}
				]
			},





			
			// Simple Authentication
			{
				xtype: 'fieldset',
				id: 'idAuthSimple',
				checkboxToggle: true,
				title: 'Simple Authentication',
				autoHeight: true,
				defaults: {
					anchor: '100%'
				},

				listeners: {
					expand: function() {
						Ext.getCmp('nikeEmail').enable();
						Ext.getCmp('nikePassword').enable();
						Ext.getCmp('idAuthAdvanced').collapse();
						Ext.getCmp('nikePin').disable();
						Ext.getCmp('nikeEmpedId').disable();
						Ext.getCmp('nikePin').allowBlank = true;
						Ext.getCmp('nikePin').validate();
						Ext.getCmp('nikeEmpedId').allowBlank = true;
						Ext.getCmp('nikeEmpedId').validate();
					},

					collapse: function() {
						Ext.getCmp('idAuthAdvanced').expand();
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
				id: 'idAuthAdvanced',
				checkboxToggle: true,
				title: 'Advanced Authentication',
				autoHeight: true,
				collapsed: true,
				defaults: {
					anchor: '100%'
				},
				
				listeners: {
					expand: function() {
						Ext.getCmp('nikeEmail').disable();
						Ext.getCmp('nikePassword').disable();
						Ext.getCmp('idAuthSimple').collapse();
						Ext.getCmp('nikePin').enable();
						Ext.getCmp('nikeEmpedId').enable();
						Ext.getCmp('nikeEmail').allowBlank = true;
						Ext.getCmp('nikeEmail').validate();
						Ext.getCmp('nikePassword').allowBlank = true;
						Ext.getCmp('nikePassword').validate();
					},

					collapse: function() {
						Ext.getCmp('idAuthSimple').expand();
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
			}
		],

		buttons: [{
			text: 'Convert &amp; Upload',
			handler: function() {
				if (converterForm.getForm().isValid()) {

					// If we are dealing with a TCX file upload then ensure the file extension is tcx.
					if ((!Ext.getCmp('garminTcxFile').disabled) && (!validateFileExtension(Ext.getCmp('garminTcxFile').getValue()))) {
						Ext.MessageBox.alert('Garmin TCX File', 'Only tcx files are accepted.');
						return;
					}

					var nikePinValue =	converterForm.findById('nikePin').getValue();

					// Convert & Upload
					converterForm.getForm().submit({
						url: 'tcx2nikeplus/convert',
						params:{clientTimeZoneOffset : (0 - (new Date().getTimezoneOffset()))},
						timeout: 60,
						waitMsg: 'Converting &amp; Uploading your workout, please wait...',
						success: function(converterForm, o){
							msgSuccess('Success', o.result.data.errorMessage);
						},
						failure: function(converterForm, o) {
							msgFailure('Failure', o.result.data.errorMessage);
						}
					});
				}
			}
		},
		{
			text: 'Reset',
			handler: function() {
				converterForm.getForm().reset();
				converterForm.findById('fileContainer').expand();
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
	if ((type === 'garminActivityID') || (type === 'garminActivityID_GPS')) converterForm.findById('idContanier').expand();

	if ((Ext.getUrlParam('pin') != null) || (Ext.getUrlParam('empedID') != null)) converterForm.findById('idAuthAdvanced').expand();

	converterForm.findById('nikePin').setValue(Ext.getUrlParam('pin'));
	converterForm.findById('nikeEmpedId').setValue(Ext.getUrlParam('empedID'));

});
