package com.awsmithson.tcx2nikeplus.nike;


import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.topografix.gpx._1._1.GpxType;

import javax.annotation.Nonnull;

public class NikePlusSyncData {
	private final @Nonnull JsonElement runJson;
	private final @Nonnull GpxType gpxXML;

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
}
