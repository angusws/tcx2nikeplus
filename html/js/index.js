
Ext.onReady(function() {

	/*
	 * Add google analytics tracking to AJAX requests.
	 */
	Ext.Ajax.on('beforerequest', function(connection, options) {
		pageTracker._trackPageview('/' + options.url);
	});
	

	/*
	 * Charity url string
	 */
	function getCharityAnchor(text) {
		return '<a href="http://www.awsmithson.com/charity" target="_blank">' + text + '</a>';
	}
	
	/*
	 * Progress url string
	 */
	function getProgressAnchor(text) {
		return '<a href="http://www.awsmithson.com" target="_blank">' + text + '</a>';
	}
	
	
	/*
	 * Messages
	 */
	var msgSuccess = function(title, msg) {

		msg = '<div style="text-align: center; font-weight: bold;">' + msg + '</div>';

		// Show CLIC Sargent donation option.
		if (Math.floor(Math.random() * 3) == 0)
			msg = msg.concat('<div style="margin-top: 8px;">' +
				getCharityAnchor('<img src="http://uk.virginmoneygiving.com/giving/Images/banners/106x139_donate.png" style="float: left; margin: 0px 16px 0px 0px;" alt="Make a donation using Virgin Money Giving">') +
				'<div style="display: table-cell; vertical-align: middle; height: 139px;">' + 
				'To raise money for CLIC Sargent (UK childrens cancer charity) I ran ' + getProgressAnchor('2012 miles in 2012') + '.<br /><br />' +
				'If you are a regular user of tcx2nikeplus please ' + getCharityAnchor('visit my donation page') + '.  It\'s a great cause and every little helps.' + 
				'</div>');


		Ext.Msg.show({
			title: title,
			msg: msg,
			minWidth: 420,
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
		title: 'Garmin Forerunner TCX to Nike+ Converter &amp; Uploader',
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

			// Garmin Activity (nike+ heart-rate)
			{
				xtype: 'fieldset',
				id: 'fsGarminId',
				checkboxToggle: true,
				title: 'Garmin Activity',
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

		buttons: [{
			id: 'btnSubmit',
			text: 'Convert &amp; Upload',
			handler: function() {
				submit();
			}
		},
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
		}],
		
		keys: [{ 
			key: Ext.EventObject.ENTER, fn: function() {
				submit();
			} 
		}]
	});
	
	
	function submit() {
		
		var form = converterForm.getForm();
		
		if (form.isValid()) {
			// If we are dealing with a TCX file upload then ensure the file extension is tcx.
			var garminTcxFile = Ext.getCmp('garminTcxFile');
			if ((!garminTcxFile.disabled) && (!validateFileExtension(garminTcxFile.getValue()))) {
				Ext.MessageBox.alert('Garmin TCX File', 'Only tcx files are accepted.');
				return;
			}

			// Convert & Upload
			// Construct wait message text.
			var msg = '<div style="text-align: center;"><b>Please wait while your run is uploaded to nike+</b></div><br />';
			
			if (Math.floor(Math.random() * 3) == 0) {
				msg = msg.concat('In 2012 I ' + getProgressAnchor('ran 2012 miles') + ' to raise money for CLIC Sargent (UK childrens cancer charity).  ' + 
					'If you are a regular user of tcx2nikeplus please consider donating: ' + getCharityAnchor('http://awsmithson.com/charity') + ' (opens in new window).<br /><br />' + 
					'Many thanks to those who\'ve donated so far,<br />' +
					'Angus</br >');
			}
			
			// Show wait message - this will get replaced in the success or failure callback of form.submit().
			Ext.MessageBox.show({
				msg: msg,
				width: 420,
				wait: true,
				waitConfig: { interval: 800 }
			});
			
			// Submit form.
			form.submit({
				url: 'tcx2nikeplus/convert',
				params:{clientTimeZoneOffset : (0 - (new Date().getTimezoneOffset()))},
				timeout: 60000,
				success: function(converterForm, o) {

					// Save/Clear state
					// This is hacky but it'll do the job for now.
					if (Ext.getCmp('chkSaveCookies').checked) {

						var expiryDate = new Date(new Date().getTime()+(1000*60*60*24*90));		// 90 days

						Ext.util.Cookies.set('chkSaveCookies', true, expiryDate);

						if (Ext.getCmp('fsGarminId').collapsed) {
							Ext.util.Cookies.set('fsGarminIdCollapsed', true, expiryDate);
						}
						else {
							Ext.util.Cookies.set('fsGarminIdCollapsed', false, expiryDate);
						}
						Ext.util.Cookies.set('nikeEmail', Ext.getCmp('nikeEmail').getValue(), expiryDate);
					}
					else {
						Ext.util.Cookies.set('chkSaveCookies', false);
						Ext.util.Cookies.clear('fsGarminIdCollapsed');
						Ext.util.Cookies.clear('fsTcxFile');
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


	/*
	 * Default values
	 */
	Ext.getUrlParam = function(param) {
		var params = Ext.urlDecode(location.search.substring(1));
		return param ? params[param] : params;
	};

	var type = Ext.getUrlParam('type');
	if ((type === 'garminActivityID') || (type === 'garminActivityID_GPS')) Ext.getCmp('fsGarminId').expand();


	/*
	 * Cookies (these override the default values passed as http params).
	 */
	var fsGarminIdCollapsedValue = (Ext.util.Cookies.get('fsGarminIdCollapsed') === 'true');
	var nikeEmailValue = Ext.util.Cookies.get('nikeEmail');
	if (Ext.util.Cookies.get('chkSaveCookies') === 'true') {
		Ext.getCmp('chkSaveCookies').setValue(true);
		if (fsGarminIdCollapsedValue) Ext.getCmp('fsGarminId').collapse();
		if (nikeEmailValue != null ) Ext.getCmp('nikeEmail').setValue(nikeEmailValue);
	}
});
