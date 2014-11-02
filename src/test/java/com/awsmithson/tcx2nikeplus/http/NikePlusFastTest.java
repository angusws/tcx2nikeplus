package com.awsmithson.tcx2nikeplus.http;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;

public class NikePlusFastTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testGenerateFormNameValuePairsInputNull() throws UnsupportedEncodingException {
		// Test we handle null input correctly.
		expectedException.expect(NullPointerException.class);
		expectedException.expectMessage("inputKeyValues argument is null.");
		NikePlus.generateFormNameValuePairs(null);
	}

	@Test
	public void testGenerateFormNameValuePairsInputEmpty() throws UnsupportedEncodingException {
		// Test we handle no input correctly.
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("No input key/values specified.");
		NikePlus.generateFormNameValuePairs();
	}

	@Test
	public void testGenerateFormNameValuePairsInputLengthOdd() throws UnsupportedEncodingException {
		// Test we handle 'null' input correctly (actually a single-element array, where the element value is null).
		try {
			String input = null;
			NikePlus.generateFormNameValuePairs(input);
			Assert.fail(String.format("Expected %s", IllegalArgumentException.class.getCanonicalName()));
		} catch (IllegalArgumentException iae) {
			Assert.assertEquals("Odd number of name-value pairs: 1.", iae.getMessage());
		}

		// Test we handle a one--element String[] correctly.
		try {
			NikePlus.generateFormNameValuePairs("test");
			Assert.fail(String.format("Expected %s", IllegalArgumentException.class.getCanonicalName()));
		} catch (IllegalArgumentException iae) {
			Assert.assertEquals("Odd number of name-value pairs: 1.", iae.getMessage());
		}

		// Test we handle a three-element String[] correctly.
		try {
			NikePlus.generateFormNameValuePairs("test1", "test2", "test3");
			Assert.fail(String.format("Expected %s", IllegalArgumentException.class.getCanonicalName()));
		} catch (IllegalArgumentException iae) {
			Assert.assertEquals("Odd number of name-value pairs: 3.", iae.getMessage());
		}
	}

	@Test
	public void testGenerateFormNameValuePairsInput() throws IOException {
		verifyFormNameValuePairsOutput("key=value", "key", "value");
		verifyFormNameValuePairsOutput("key1=value1&key2=value2", "key1", "value1", "key2", "value2");
		verifyFormNameValuePairsOutput("key1=value1&test%3C%3Ehello=some%26other%40characters%24", "key1", "value1", "test<>hello", "some&other@characters$");
	}

	private void verifyFormNameValuePairsOutput(@Nonnull String expected, @Nonnull String ... input) throws IOException {
		UrlEncodedFormEntity urlEncodedFormEntity = NikePlus.generateFormNameValuePairs(input);
		Assert.assertEquals("application/x-www-form-urlencoded; charset=UTF-8", urlEncodedFormEntity.getContentType().getValue());
		Assert.assertEquals(expected, IOUtils.toString(urlEncodedFormEntity.getContent()));
	}

	@Test
	public void testIsPasswordValid() {
		testIsPasswordValid(true, "test");
		testIsPasswordValid(true, "te`st");
		testIsPasswordValid(false, "te\"st");
		testIsPasswordValid(false, "te&st");
		testIsPasswordValid(false, "te'st");
		testIsPasswordValid(false, "te<st");
		testIsPasswordValid(false, "te>st");

		// Test with a random 50-char ACSCII password, our illegal chars removed (replace them with spaces).
		String randomASCIIPassword = generateRandomASCIIPassword(50)
				.replace('"', ' ')
				.replace('&', ' ')
				.replace('\'', ' ')
				.replace('<', ' ')
				.replace('>', ' ');

		testIsPasswordValid(true, randomASCIIPassword);
	}

	private void testIsPasswordValid(boolean expected, @Nonnull String input) {
		Assert.assertEquals(input, expected, NikePlus.isPasswordValid(input.toCharArray()));
	}

	private @Nonnull String generateRandomASCIIPassword(int length) {
		Random random = new Random();
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < length; ++i) {
			output.append((char) (random.nextInt(96) + 32));
		}
		return output.toString();
	}

}
