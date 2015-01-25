package com.awsmithson.tcx2nikeplus.servlet;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import javax.annotation.Nonnull;

public class ServletsFastTest {

	@Test
	public void testGetGarminActivityId() throws IOException {
		testGetGarminActivityId(123, "123");
		testGetGarminActivityId(123, "a/b/c/123");
		testGetGarminActivityId(123, "ab/c//123");
		testGetGarminActivityId(123, "123/456/789/123");
		testGetGarminActivityId(34448379, "34448379");
		testGetGarminActivityId(34448379, "34448379#");
		testGetGarminActivityId(34448379, "https://connect.garmin.com/activity/34448379");
		testGetGarminActivityId(34448379, "https://connect.garmin.com/activity/34448379#");
		testGetGarminActivityId(34448379, "https://connect.garmin.com/activity/34448379?foo=bar");
		testGetGarminActivityId(34448379, "https://10.11.12.13/activity/34448379?foo=bar");
	}

	private void testGetGarminActivityId(int expected, @Nonnull String input) throws IOException {
		Assert.assertEquals(expected, Servlets.getGarminActivityId(input));
	}
}
