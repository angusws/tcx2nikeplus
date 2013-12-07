package com.awsmithson.tcx2nikeplus.cli;

import com.awsmithson.tcx2nikeplus.http.NikePlus;

import java.io.File;


public class Upload
{
	public static void main(String[] args) {
		String nikePlusEmail = args[0];
		String nikePlusPassword = args[1];
		File runXml = new File(args[2]);
		File gpxXml = (args.length > 3) ? new File(args[3]) : null;

		NikePlus np = new NikePlus();
		try {
			np.fullSync(nikePlusEmail, nikePlusPassword.toCharArray(), runXml, gpxXml);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
