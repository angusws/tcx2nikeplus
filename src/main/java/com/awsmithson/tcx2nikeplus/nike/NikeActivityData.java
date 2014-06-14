package com.awsmithson.tcx2nikeplus.nike;

import com.google.common.base.Preconditions;
import org.w3c.dom.Document;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class NikeActivityData {
	private final @Nonnull Document runXML;
	private final @Nullable Document gpxXML;

	public NikeActivityData(@Nonnull Document runXML, @Nullable Document gpxXML) {
		this.runXML = Preconditions.checkNotNull(runXML, "runXML argument is null.");
		this.gpxXML = gpxXML;
	}

	public @Nonnull Document getRunXML() {
		return runXML;
	}

	public @Nullable Document getGpxXML() {
		return gpxXML;
	}
}
