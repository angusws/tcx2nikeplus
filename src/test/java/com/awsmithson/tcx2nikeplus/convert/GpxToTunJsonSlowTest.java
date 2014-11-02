package com.awsmithson.tcx2nikeplus.convert;

import com.awsmithson.tcx2nikeplus.garmin.GarminDataTypeSlowTest;
import com.awsmithson.tcx2nikeplus.http.runjson.RunJsonSlowTest;
import com.awsmithson.tcx2nikeplus.jaxb.JAXBObject;
import com.awsmithson.tcx2nikeplus.nike.RunJson;
import com.topografix.gpx._1._1.GpxType;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class GpxToTunJsonSlowTest {

	private static final @Nonnull String ACTIVITY_34448379_GPX = "/garmin/gpx/activity_34448379.gpx";
	private static final @Nonnull String RUN_JSON_34448379 = "/garmin/gpx/expected_runjson/34448379_run-minimal.json";

	private static final @Nonnull String RUN_JSON_148656142 = "/garmin/gpx/expected_runjson/148656142_run-minimal.json";

	private static final @Nonnull String ACTIVITY_170865319_GPX = "/garmin/gpx/activity_170865319.gpx";
	private static final @Nonnull String RUN_JSON_170865319 = "/garmin/gpx/expected_runjson/170865319_run-minimal.json";

	@Test
	public void testConvert34448379() throws IOException, JAXBException, ConverterException {
		Assert.assertNotNull("Test file missing", getClass().getResource(ACTIVITY_34448379_GPX));
		Assert.assertNotNull("Test file missing", getClass().getResource(RUN_JSON_34448379));

		try (InputStream inputStream = getClass().getResourceAsStream(ACTIVITY_34448379_GPX)) {
			GpxType gpxType = JAXBObject.GPX_TYPE.unmarshall(inputStream);

			Converter<GpxType, RunJson> gpxToRunJson = new GpxToRunJson();
			RunJson runJson = gpxToRunJson.convert(gpxType);

			File expected = new File(getClass().getResource(RUN_JSON_34448379).getFile());
			RunJsonSlowTest.assertRunJsonEquals(expected, runJson);
		}
	}

	@Test
	public void testConvert148656142() throws IOException, JAXBException, ConverterException {
		Assert.assertNotNull("Test file missing", getClass().getResource(GarminDataTypeSlowTest.ACTIVITY_148656142_GPX));
		Assert.assertNotNull("Test file missing", getClass().getResource(RUN_JSON_148656142));

		try (InputStream inputStream = getClass().getResourceAsStream(GarminDataTypeSlowTest.ACTIVITY_148656142_GPX)) {
			GpxType gpxType = JAXBObject.GPX_TYPE.unmarshall(inputStream);

			Converter<GpxType, RunJson> gpxToRunJson = new GpxToRunJson();
			RunJson runJson = gpxToRunJson.convert(gpxType);

			File expected = new File(getClass().getResource(RUN_JSON_148656142).getFile());
			RunJsonSlowTest.assertRunJsonEquals(expected, runJson);
		}
	}

	@Test
	public void testConvert170865319() throws IOException, JAXBException, ConverterException {
		Assert.assertNotNull("Test file missing", getClass().getResource(ACTIVITY_170865319_GPX));
		Assert.assertNotNull("Test file missing", getClass().getResource(RUN_JSON_170865319));

		try (InputStream inputStream = getClass().getResourceAsStream(ACTIVITY_170865319_GPX)) {
			GpxType gpxType = JAXBObject.GPX_TYPE.unmarshall(inputStream);

			Converter<GpxType, RunJson> gpxToRunJson = new GpxToRunJson();
			RunJson runJson = gpxToRunJson.convert(gpxType);

			File expected = new File(getClass().getResource(RUN_JSON_170865319).getFile());
			RunJsonSlowTest.assertRunJsonEquals(expected, runJson);
		}
	}
}
