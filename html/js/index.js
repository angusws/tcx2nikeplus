
Ext.onReady(function() {



	/*
	 * Messages
	 */
	var msgSuccess = function(title, msg) {
		Ext.Msg.show({
			title: title,
			msg: msg,
			minWidth: 200,
			modal: true,
			icon: Ext.Msg.INFO,
			buttons: Ext.Msg.OK
		});
	};


	var msgFailure = function(title, msg) {
		Ext.Msg.show({
			title: title,
			msg: msg,
			minWidth: 200,
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
			}
		]
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
	var fp = new Ext.FormPanel({
		renderTo: 'divConverter',
		fileUpload: true,
		width: 450,
		frame: true,
		title: 'Garmin Forerunner TCX file to Nike+ Converter &amp; Uploader',
		autoHeight: true,
		bodyStyle: 'padding: 10px 10px 0 10px;',
		labelWidth: 80,
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
				id: 'fileFieldSet',
				checkboxToggle: true,
				title: 'Garmin TCX file',
				autoHeight: true,
				defaults: { width: 250 },

				listeners: {
					expand: function() {
						Ext.getCmp('garminTcxFile').enable();
						Ext.getCmp('idFieldSet').collapse();
						Ext.getCmp('garminActivityId').disable();
					}
				},

				items: [{
					xtype: 'fileuploadfield',
					id: 'garminTcxFile',
					emptyText: 'Select a tcx file',
					//buttonText: 'Browse',
					allowBlank: false
				}]
			},

			// Garmin Activity ID
			{
				xtype: 'fieldset',
				id: 'idFieldSet',
				checkboxToggle: true,
				title: 'Garmin Connect Activity ID',
				autoHeight: true,
				defaults: { width: 100 },
				collapsed: true,

				listeners: {
					expand: function() {
						Ext.getCmp('garminActivityId').enable();
						Ext.getCmp('fileFieldSet').collapse();
						Ext.getCmp('garminTcxFile').disable();

						// Add the ToolTip the 1st time we enable the garminActivityId NumberField because you can't declare ToolTips on disabled Components.
						if (activityIdToolTip == null)
							new Ext.ToolTip({
								target: 'garminActivityId',
								title: 'Garmin Connect Activity ID',
								html: 'Example: For the workout http://connect.garmin.com/activity/21742933 the activity ID is 21742933.'
							});
					}
				},

				items: [{
					xtype: 'textfield',
					id: 'garminActivityId',
					allowBlank: false,
					disabled: true
				}]
			},

			// Nike+ pin
			{
				xtype: 'textfield',
				id: 'nikePin',
				fieldLabel: 'Nike+ PIN',
				width: 100
			},

			// Emped ID
			{
				xtype: 'textfield',
				id: 'nikeEmpedId',
				fieldLabel: 'Emped ID'
			}
		],

		buttons: [{
			text: 'Convert &amp; Upload',
			handler: function() {
				if (fp.getForm().isValid()) {

					// If we are dealing with a TCX file upload then ensure the file extension is tcx.
					if ((!Ext.getCmp('garminTcxFile').disabled) && (!validateFileExtension(Ext.getCmp('garminTcxFile').getValue()))) {
						Ext.MessageBox.alert('Garmin TCX File', 'Only tcx files are accepted.');
						return;
					}

					var nikePinValue =	fp.findById('nikePin').getValue();

					// Simple Convert
					if ((nikePinValue.length == 0)) {
						fp.getForm().submit({
							url: 'convert'
						});
					}

					// Convert & Upload
					else {
						fp.getForm().submit({
							url: 'convert',
							timeout: 60,
							waitMsg: 'Converting &amp; Uploading your workout, please wait...',
							success: function(fp, o){
								msgSuccess('Success', o.result.data.errorMessage);
							},
							failure: function(pf, o) {
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
				fp.getForm().reset();
				fp.findById('fileFieldSet').expand();
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

	var activityIdToolTip = null;	// Only add this if/when we enable the garminActivityId NumberField.

	new Ext.ToolTip({
		target: 'nikePin',
		title: 'Nike+ PIN',
		html: 'https://secure-nikerunning.nike.com/nikeplus/v2/services/app/generate_pin.jsp?login=LOGIN&password=PASSWORD<br />for your pin.<br />See instructions for more details.'
	});

	new Ext.ToolTip({
		target: 'nikeEmpedId',
		title: 'Emped ID',
		html: 'Not required, although you may want to include it.<br />See instructions for more details.'
	});





	/*
	 * Default values
	 */
	Ext.getUrlParam = function(param) {
		var params = Ext.urlDecode(location.search.substring(1));
		return param ? params[param] : params;
	};

	fp.findById('nikePin').setValue(Ext.getUrlParam('pin'));
	fp.findById('nikeEmpedId').setValue(Ext.getUrlParam('empedID'));

	if (Ext.getUrlParam('type') === 'garminActivityID')
		fp.findById('idFieldSet').expand();



});
