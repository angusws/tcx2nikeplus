package com.awsmithson.tcx2nikeplus.http;

import java.io.UnsupportedEncodingException;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.StringBody;

public class SpoofFileBody extends StringBody {
	private String _filename;

	public SpoofFileBody(final String text, String filename) throws UnsupportedEncodingException {
		super(text, ContentType.APPLICATION_XML);
		_filename = filename;
	}

	@Override
	public String getFilename() {
		return _filename;
	}
}
