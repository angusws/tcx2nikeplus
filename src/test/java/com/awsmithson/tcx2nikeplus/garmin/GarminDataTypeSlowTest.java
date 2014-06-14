package com.awsmithson.tcx2nikeplus.garmin;


import com.awsmithson.tcx2nikeplus.convert.ConvertTcxV2;
import com.garmin.xmlschemas.trainingcenterdatabase.v2.TrainingCenterDatabaseT;
import com.topografix.gpx._1._1.GpxType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.TimeZone;

public class GarminDataTypeSlowTest {

	private static final @Nonnull String ACTIVITY_148656142_TCX = "/garmin/tcx/activity_148656142.tcx";
	private static final @Nonnull String ACTIVITY_148656142_GPX = "/garmin/gpx/activity_148656142.gpx";

	@Test
	public void testDownloadGarminTcx() throws IOException, JAXBException, URISyntaxException {
		try (CloseableHttpClient closeableHttpClient = GarminDataType.getGarminHttpSession()) {
			TrainingCenterDatabaseT trainingCenterDatabase = GarminDataType.TCX.downloadAndUnmarshall(closeableHttpClient, 148656142);
			Assert.assertNotNull("trainingCenterDatabase was null", trainingCenterDatabase);
		}
	}

	@Test
	public void testDownloadGarminGpx() throws IOException, JAXBException, URISyntaxException {
		try (CloseableHttpClient closeableHttpClient = GarminDataType.getGarminHttpSession()) {
			GpxType gpxType = GarminDataType.GPX.downloadAndUnmarshall(closeableHttpClient, 148656142);
			Assert.assertNotNull("gpxType was null", gpxType);
		}
	}

	@Test
	public void testUnmarshallTcx() throws IOException, JAXBException {
		Assert.assertNotNull("Test file missing", getClass().getResource(ACTIVITY_148656142_TCX));
		try (InputStream inputStream = getClass().getResourceAsStream(ACTIVITY_148656142_TCX)) {
			TrainingCenterDatabaseT trainingCenterDatabase = GarminDataType.TCX.unmarshall(inputStream);
			Assert.assertEquals("activities size incorrect", 1, trainingCenterDatabase.getActivities().getActivity().size());
			Calendar expected = Calendar.getInstance();
			expected.set(2012, 1, 11, 9, 31, 5);
			expected.set(Calendar.MILLISECOND, 0);
			Assert.assertEquals("activity start time incorrect", expected.getTime(), ConvertTcxV2.getActivityStartTime(trainingCenterDatabase.getActivities().getActivity().get(0)));
		}
	}

	@Test
	public void testUnmarshallGpx() throws IOException, JAXBException {
		Assert.assertNotNull("Test file missing", getClass().getResource(ACTIVITY_148656142_GPX));
		try (InputStream inputStream = getClass().getResourceAsStream(ACTIVITY_148656142_GPX)) {
			GpxType gpxType = GarminDataType.GPX.unmarshall(inputStream);
			Calendar expected = Calendar.getInstance();
			expected.set(2012, 1, 11, 9, 31, 5);
			expected.set(Calendar.MILLISECOND, 0);

			Assert.assertEquals("trk size incorrect", 1, gpxType.getTrk().size());
			Assert.assertEquals("trk name incorrect", "Edinburgh parkrun 117", gpxType.getTrk().get(0).getName());
			Assert.assertEquals("trkpt size incorrect", 221, gpxType.getTrk().get(0).getTrkseg().get(0).getTrkpt().size());
			Assert.assertEquals("activity start time incorrect", expected.getTime(), gpxType.getMetadata().getTime().toGregorianCalendar().getTime());
		}
	}
}
