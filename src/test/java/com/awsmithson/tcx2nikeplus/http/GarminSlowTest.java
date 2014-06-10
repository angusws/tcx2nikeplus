package com.awsmithson.tcx2nikeplus.http;


import com.garmin.xmlschemas.trainingcenterdatabase.v2.TrainingCenterDatabaseT;

import com.topografix.gpx._1._1.GpxType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

public class GarminSlowTest {

	@Test
	public void testDownloadGarminTcx() throws Throwable {
		try (CloseableHttpClient closeableHttpClient = Garmin.getGarminHttpSession()) {
			TrainingCenterDatabaseT trainingCenterDatabase = Garmin.downloadTcx(closeableHttpClient, 148656142);
			Assert.assertNotNull("trainingCenterDatabase was null", trainingCenterDatabase);
			Assert.assertEquals("activities size incorrect", 1, trainingCenterDatabase.getActivities().getActivity().size());
			Assert.assertEquals("activity-1 ID incorrect", "2012-02-11T09:31:05.000Z", trainingCenterDatabase.getActivities().getActivity().get(0).getId().toString());
		}
	}

	@Test
	public void testDownloadGarminGpx() throws Throwable {
		try (CloseableHttpClient closeableHttpClient = Garmin.getGarminHttpSession()) {
			GpxType gpxType = Garmin.downloadGpx(closeableHttpClient, 148656142);
			Assert.assertNotNull("gpxType was null", gpxType);
			Assert.assertEquals("trk size incorrect", 1, gpxType.getTrk().size());
			Assert.assertEquals("trk name incorrect", "Edinburgh parkrun 117", gpxType.getTrk().get(0).getName());
			Assert.assertEquals("trkpt size incorrect", 221, gpxType.getTrk().get(0).getTrkseg().get(0).getTrkpt().size());
		}
	}
}
