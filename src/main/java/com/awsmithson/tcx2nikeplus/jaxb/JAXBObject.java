package com.awsmithson.tcx2nikeplus.jaxb;

import com.garmin.xmlschemas.trainingcenterdatabase.v2.TrainingCenterDatabaseT;
import com.google.common.base.Preconditions;
import com.topografix.gpx._1._1.GpxType;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

public enum JAXBObject {

	TRAINING_CENTER_DATABASE {
		private final JAXBContext JAXB_CONTEXT = createTrainingCenterDatabaseJAXBContect();
		private @Nonnull JAXBContext createTrainingCenterDatabaseJAXBContect() {
			try {
				return JAXBContext.newInstance(TrainingCenterDatabaseT.class);
			} catch (JAXBException je) {
				throw new ExceptionInInitializerError(je);
			}
		}
		private final @Nonnull ThreadLocal<Unmarshaller> UNMARSHALLER = new ThreadLocal<Unmarshaller>() {
			protected synchronized Unmarshaller initialValue() {
				try {
					return JAXB_CONTEXT.createUnmarshaller();
				} catch (JAXBException e) {
					throw new ExceptionInInitializerError(e);
				}
			}
		};
		private final @Nonnull ThreadLocal<Marshaller> MARSHALLER = new ThreadLocal<Marshaller>() {
			protected synchronized Marshaller initialValue() {
				try {
					Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
					marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
					return marshaller;
				} catch (JAXBException e) {
					throw new ExceptionInInitializerError(e);
				}
			}
		};

		@Override
		@Nonnull Unmarshaller getUnmarshaller() {
			return UNMARSHALLER.get();
		}

		@Override
		@Nonnull Class getUnmarshalledClass() {
			return TrainingCenterDatabaseT.class;
		}

		@Override
		public @Nonnull Marshaller getMarshaller() {
			return MARSHALLER.get();
		}
	},

	GPX_TYPE {
		private final JAXBContext JAXB_CONTEXT = createGpxTypeJAXBContect();
		private @Nonnull JAXBContext createGpxTypeJAXBContect() {
			try {
				return JAXBContext.newInstance(GpxType.class);
			} catch (JAXBException je) {
				throw new ExceptionInInitializerError(je);
			}
		}
		private final @Nonnull ThreadLocal<Unmarshaller> UNMARSHALLER = new ThreadLocal<Unmarshaller>() {
			protected synchronized Unmarshaller initialValue() {
				try {
					return JAXB_CONTEXT.createUnmarshaller();
				} catch (JAXBException e) {
					throw new ExceptionInInitializerError(e);
				}
			}
		};
		private final @Nonnull ThreadLocal<Marshaller> MARSHALLER = new ThreadLocal<Marshaller>() {
			protected synchronized Marshaller initialValue() {
				try {
					Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
					marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
					return marshaller;
				} catch (JAXBException e) {
					throw new ExceptionInInitializerError(e);
				}
			}
		};

		@Override
		@Nonnull Unmarshaller getUnmarshaller() {
			return UNMARSHALLER.get();
		}

		@Override
		@Nonnull Class getUnmarshalledClass() {
			return GpxType.class;
		}

		@Override
		public @Nonnull Marshaller getMarshaller() {
			return MARSHALLER.get();
		}
	};

	abstract @Nonnull Unmarshaller getUnmarshaller();
	abstract @Nonnull Class getUnmarshalledClass();
	public abstract @Nonnull Marshaller getMarshaller();

	public @Nonnull <T> T unmarshall(@Nonnull InputStream inputStream) throws JAXBException {
		Preconditions.checkNotNull(inputStream, "inputStream argument is null.");

		//noinspection unchecked
		return (T) getUnmarshaller().unmarshal(new StreamSource(inputStream), getUnmarshalledClass()).getValue();
	}
}
