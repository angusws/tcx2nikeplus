package com.awsmithson.tcx2nikeplus.nike;


import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.topografix.gpx._1._1.GpxType;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NikePlusSyncData {
	private final @Nonnull JsonElement runJson;
	private final @Nonnull GpxType gpxXML;
	private @Nullable String responseEntityContent;
	private int responseStatusCode = -1;

	public NikePlusSyncData(@Nonnull JsonElement runJson, @Nonnull GpxType gpxXML) {
		this.runJson = Preconditions.checkNotNull(runJson, "RunJson argument is null.");
		this.gpxXML = Preconditions.checkNotNull(gpxXML, "gpxXML argument is null.");
	}

	public @Nonnull JsonElement getRunJson() {
		return runJson;
	}

	public @Nonnull GpxType getGpxXML() {
		return gpxXML;
	}

	public @Nullable String getResponseEntityContent() {
		return responseEntityContent;
	}

	public void setResponseEntityContent(@Nullable String responseEntityContent) {
		this.responseEntityContent = responseEntityContent;
	}

	public int getResponseStatusCode() {
		return responseStatusCode;
	}

	public void setResponseStatusCode(int responseStatusCode) {
		this.responseStatusCode = responseStatusCode;
	}
}
