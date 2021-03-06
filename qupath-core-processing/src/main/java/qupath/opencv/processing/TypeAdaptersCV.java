package qupath.opencv.processing;

import java.io.IOException;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.FileStorage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.SparseMat;
import org.bytedeco.javacpp.opencv_ml.ANN_MLP;
import org.bytedeco.javacpp.opencv_ml.Boost;
import org.bytedeco.javacpp.opencv_ml.DTrees;
import org.bytedeco.javacpp.opencv_ml.EM;
import org.bytedeco.javacpp.opencv_ml.KNearest;
import org.bytedeco.javacpp.opencv_ml.LogisticRegression;
import org.bytedeco.javacpp.opencv_ml.NormalBayesClassifier;
import org.bytedeco.javacpp.opencv_ml.RTrees;
import org.bytedeco.javacpp.opencv_ml.SVM;
import org.bytedeco.javacpp.opencv_ml.SVMSGD;
import org.bytedeco.javacpp.opencv_ml.StatModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;


/**
 * Helper classes for combining OpenCV's JSON serialization with Gson's.
 * <p>
 * Sample use:
 * <pre>
 * Gson gson = new GsonBuilder()
 * 				.registerTypeAdapterFactory(TypeAdaptersCV.getOpenCVTypeAdaptorFactory())
 * 				.setPrettyPrinting()
 * 				.create();
 * 
 * Mat mat1 = Mat.eye(3, 3, CV_32F1).asMat();
 * String json = gson.toJson(mat1);
 * Mat mat2 = gson.fromJson(json, Mat.class);
 * </pre>
 * 
 * @author Pete Bankhead
 *
 */
public class TypeAdaptersCV {
	
	/**
	 * Get a TypeAdapterFactory to pass to a GsonBuilder to aid with serializing OpenCV objects 
	 * (e.g. Mat, StatModel).
	 * 
	 * @return
	 */
	public static TypeAdapterFactory getOpenCVTypeAdaptorFactory() {
		return new OpenCVTypeAdaptorFactory();
	}
	
	
	/**
	 * Get a TypeAdapter to pass to a GsonBuilder for a specific supported OpenCV class, 
	 * i.e. Mat, SparseMat or StatModel.
	 * 
	 * @param cls
	 * @return the required TypeAdaptor, or null if no supported adapter is available for the class.
	 */
	public static <T> TypeAdapter<T> getTypeAdaptor(Class<T> cls) {
		if (Mat.class == cls)
			return (TypeAdapter<T>)new MatTypeAdapter();
		if (SparseMat.class == cls)
			return (TypeAdapter<T>)new SparseMatTypeAdapter();
		if (StatModel.class.isAssignableFrom(cls))
			return (TypeAdapter<T>)new StatModelTypeAdapter();
		return null;
	}
	
	
	
	public static class OpenCVTypeAdaptorFactory implements TypeAdapterFactory {

		@Override
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
			return getTypeAdaptor((Class<T>)type.getRawType());
		}
		
	}
	
	
	public static abstract class OpenCVTypeAdapter<T> extends TypeAdapter<T> {

		@Override
		public void write(JsonWriter out, T value) throws IOException {
			boolean lenient = out.isLenient();
			try (var fs = new FileStorage()) {
				fs.open("anything.json", FileStorage.FORMAT_JSON + FileStorage.WRITE + FileStorage.MEMORY);
				write(fs, value);
				var json = fs.releaseAndGetString().getString();
				out.jsonValue(json.trim());
			} finally {
				out.setLenient(lenient);
			}
		}
		
		abstract void write(FileStorage fs, T value);
		
		abstract T read(FileStorage fs);

		@Override
		public T read(JsonReader in) throws IOException {
			boolean lenient = in.isLenient();
			try {
				var element = new JsonParser().parse(in);
				var obj = element.getAsJsonObject();
				var inputString = obj.toString();//obj.get("mat").toString();
				try (var fs = new FileStorage()) {
					fs.open(inputString, FileStorage.FORMAT_JSON + FileStorage.READ + FileStorage.MEMORY);
					return read(fs);
				}
			} finally {
				in.setLenient(lenient);
			}
		}
		
	}
	
	
	private static class MatTypeAdapter extends OpenCVTypeAdapter<Mat> {

		@Override
		void write(FileStorage fs, Mat value) {
			opencv_core.write(fs, "mat", value);
		}

		@Override
		Mat read(FileStorage fs) {
			return fs.getFirstTopLevelNode().mat();
		}
		
	}
	
	private static class SparseMatTypeAdapter extends OpenCVTypeAdapter<SparseMat> {

		@Override
		void write(FileStorage fs, SparseMat value) {
			opencv_core.write(fs, "sparsemat", value);
		}

		@Override
		SparseMat read(FileStorage fs) {
			var mat = new SparseMat();
			opencv_core.read(fs.getFirstTopLevelNode(), mat);
			return mat;
		}
		
	}
	
	
	
//	static class MatTypeAdapter extends TypeAdapter<Mat> {
//
//		@Override
//		public void write(JsonWriter out, Mat value) throws IOException {
//			try (var fs = new FileStorage()) {
//				fs.open("anything.json", FileStorage.FORMAT_JSON + FileStorage.WRITE + FileStorage.MEMORY);
//				fs.write("mat", value);
////				opencv_core.
////				opencv_core.write(fs, "", mat);
//				var json = fs.releaseAndGetString().getString();
//				out.jsonValue(json.trim());
//			}
//		}
//
//		@Override
//		public Mat read(JsonReader in) throws IOException {
//			boolean lenient = in.isLenient();
//			try {
//				var element = new JsonParser().parse(in);
//				var obj = element.getAsJsonObject();
//				var matString = obj.toString();//obj.get("mat").toString();
//				
//				try (var fs = new FileStorage()) {
//					fs.open(matString, FileStorage.FORMAT_JSON + FileStorage.READ + FileStorage.MEMORY);
////					var fn = fs.root();
//					var fn = fs.getFirstTopLevelNode();
//					return fn.mat();
//				}
//			} finally {
//				in.setLenient(lenient);
//			}
//		}
//		
//	}
	
	
	private static class StatModelTypeAdapter extends TypeAdapter<StatModel> {

		@Override
		public void write(JsonWriter out, StatModel value) throws IOException {
			try (var fs = new FileStorage()) {
				fs.open("anything.json", FileStorage.FORMAT_JSON + FileStorage.WRITE + FileStorage.MEMORY);
				value.write(fs);
				var json = fs.releaseAndGetString().getString();
				
				out.beginObject();
				out.name("class");
				out.value(value.getClass().getSimpleName());
				out.name("statmodel");
				
				// jsonValue works for JsonWriter but not JsonTreeWriter, so we try to work around this...
				var gson = new GsonBuilder().setLenient().create();
				var element = gson.fromJson(json.trim(), JsonObject.class);
				gson.toJson(element, out);
//				out.jsonValue(obj.toString());
//				out.jsonValue(json);
				out.endObject();
			}
		}

		@Override
		public StatModel read(JsonReader in) throws IOException {
			
			boolean lenient = in.isLenient();
			
			try {
				var element = new JsonParser().parse(in);
				
				var obj = element.getAsJsonObject();
				
				var className = obj.get("class").getAsString();
				
				// It's a bit roundabout... but toString() gives Strings that are too long and unsupported 
				// by OpenCV, so we take another tour through Gson.
//				var modelString = obj.get("statmodel").toString();
				var modelString = new GsonBuilder().setPrettyPrinting().create().toJson(obj.get("statmodel"));
				
				StatModel model = null;
				
				if (RTrees.class.getSimpleName().equals(className))
					model = RTrees.create();
				else if (DTrees.class.getSimpleName().equals(className))
					model = DTrees.create();
				else if (Boost.class.getSimpleName().equals(className))
					model = Boost.create();
				else if (EM.class.getSimpleName().equals(className))
					model = EM.create();
				else if (LogisticRegression.class.getSimpleName().equals(className))
					model = LogisticRegression.create();
				else if (SVM.class.getSimpleName().equals(className))
					model = SVM.create();
				else if (SVMSGD.class.getSimpleName().equals(className))
					model = SVMSGD.create();
				else if (NormalBayesClassifier.class.getSimpleName().equals(className))
					model = NormalBayesClassifier.create();
				else if (KNearest.class.getSimpleName().equals(className))
					model = KNearest.create();
				else if (ANN_MLP.class.getSimpleName().equals(className))
					model = ANN_MLP.create();
				else
					throw new IOException("Unknown StatModel class name " + className);
				
				// Load from the JSON data
				try (var fs = new FileStorage()) {
					fs.open(modelString, FileStorage.FORMAT_JSON + FileStorage.READ + FileStorage.MEMORY);
					var fn = fs.root();
					model.read(fn);
					return model;
				}
			} finally {
				in.setLenient(lenient);
			}
		}
		
	}

}
