package com.awsmithson.tcx2nikeplus.http.runjson;


import com.awsmithson.tcx2nikeplus.nike.RunJson;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RunJsonSlowTest {

	private static final @Nonnull String RUN_2014_06_14_MINIMAL = "/nikeplus/run/2014-06-14_run-minimal.json";

	private static Function<String, BigDecimal> STRING_TO_BIG_DECIMAL_FUNCTION = new Function<String, BigDecimal>() {
		@Override
		public @Nullable BigDecimal apply(@Nullable String input) {
			return new BigDecimal(input);
		}
	};


	/*
	 * Not a great test; only checks that GSON serialization/de-serilization works.
	 */
	@Test
	public void testGsonSerialization() throws FileNotFoundException {
		// First, parse our "expected" JSON from our input file, using the GSON framework.
		Assert.assertNotNull("Test file missing", getClass().getResource(RUN_2014_06_14_MINIMAL));

		// Now, generate serialize a RunJson object to JSON, using the GSON framework.
		List<RunJson.Detail> detail = ImmutableList.of(
				new RunJson.Detail("distance", "time", "sec", 0L, "10", "dataStream", Lists.transform(Arrays.asList("0.037", "0.084", "0.131", "0.179", "0.226", "0.273", "0.341", "0.368", "0.415", "0.462", "0.510", "0.557", "0.604", "0.652", "0.699", "0.746", "0.793", "0.841", "0.888", "0.935", "0.983", "1.030", "1.077", "1.124", "1.172", "1.219", "1.266", "1.314", "1.361", "1.408", "1.455", "1.503", "1.550", "1.607", "1.655", "1.702", "1.749", "1.796", "1.844", "1.891", "1.938", "1.986", "2.033", "2.080", "2.128", "2.175", "2.222", "2.269", "2.317"), STRING_TO_BIG_DECIMAL_FUNCTION)),
				new RunJson.Detail("heartrate", "time", "sec", 0L, "10", "dataStream", Lists.transform(Arrays.asList("120", "130", "140", "150", "160", "161", "162", "163", "164", "165", "166", "166", "167", "168", "169", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "165", "160"), STRING_TO_BIG_DECIMAL_FUNCTION)));

		RunJson.Summary summary = new RunJson.Summary(
				ImmutableList.of(
						new RunJson.Summary.Snapshot("mileSplit",
								ImmutableList.of(
										new RunJson.Summary.Snapshot.DataSeries(new RunJson.Summary.Snapshot.DataSeries.Metrics(1, 312802), "dataPoint")
								)
						),
						new RunJson.Summary.Snapshot("kmSplit",
								ImmutableList.of(
										new RunJson.Summary.Snapshot.DataSeries(new RunJson.Summary.Snapshot.DataSeries.Metrics(1, 196084), "dataPoint"),
										new RunJson.Summary.Snapshot.DataSeries(new RunJson.Summary.Snapshot.DataSeries.Metrics(2, 363439), "dataPoint")
								)
						)
				),
				ImmutableList.of(new RunJson.Summary.DeviceConfig(new RunJson.Summary.DeviceConfig.Component("iphone", "device")))
		);
		RunJson runJson = new RunJson(new BigDecimal("2.317167236328125"), new BigDecimal("489550.1098632812"), 1402763450520L, "complete", "Europe/London", "run", detail, summary);


		File expected = new File(getClass().getResource(RUN_2014_06_14_MINIMAL).getFile());
		assertRunJsonEquals(expected, runJson);
	}


	public static void assertRunJsonEquals(@Nonnull File expectedLocation, @Nonnull RunJson runJson) throws FileNotFoundException {
		JsonElement expected = new JsonParser().parse(Files.newReader(expectedLocation, StandardCharsets.UTF_8));

		// Compare the parsed (expected) version with our serialized (actual) version.
		// Not the best comparison (toString() output), but it appears to do the job.
		Assert.assertEquals("Parsed JSON from disk differs from serialization via GSON.", expected.toString(), new Gson().toJson(runJson));
	}
}
