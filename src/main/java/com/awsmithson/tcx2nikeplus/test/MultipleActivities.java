package com.awsmithson.tcx2nikeplus.test;

import com.awsmithson.tcx2nikeplus.convert.ConvertTcx;
import com.awsmithson.tcx2nikeplus.util.Log;
import com.awsmithson.tcx2nikeplus.util.Util;

import org.w3c.dom.Document;

import java.io.File;

@Deprecated
public class MultipleActivities {

	private static final Log log = Log.getInstance();
	
	
	public MultipleActivities() {
	}
	
	
	private void process(File inFile) throws Throwable {
		Document[] docs = Util.parseMultipleWorkouts(inFile);
		
		ConvertTcx convertTcx = new ConvertTcx();
		
		int failCount = 0;
		
		for (Document doc : docs) {
			try {
				convertTcx.generateNikePlusXml(doc, null);
			}
			catch (Throwable t) {
				log.out("workout failed.");
				failCount++;
			}
		}
		
		if (failCount > 0) log.out("%d of your workouts were corrupt and could not be converted.", failCount);
	}



	public static void main(String[] args) throws Throwable {
		MultipleActivities mw = new MultipleActivities();
		mw.process(new File(args[0]));
	}
}
