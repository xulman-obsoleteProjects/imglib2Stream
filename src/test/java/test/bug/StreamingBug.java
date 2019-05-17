
package test.bug;

import io.scif.services.DatasetIOService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.type.numeric.RealType;

import org.scijava.Context;
import org.scijava.ui.UIService;

import de.mpicbg.ulman.imgstreamer.ImgStreamer;

public class StreamingBug {

	
	
	

	private static Context context;
	private static DatasetIOService ioService;
	private static UIService uiService;
	private static ImageJ ij;

	public static void main(String[] args) throws IOException {
		prepareImageJ();
		
		Dataset dataset = ioService.open(ExampleImage.lenaAsTempFile()
			.toString());
		dataset.setName("original.png");
		uiService.show(dataset);
		
		ImgStreamer is = new ImgStreamer(null);
		is.setImageForStreaming((ImgPlus) dataset.getImgPlus());
		Path path = Paths.get("/tmp/out.dat");
		try (OutputStream os = Files.newOutputStream(path)) {
			is.write(os);
		}
		is = new ImgStreamer(null);
		Dataset out;
		try (InputStream ins = Files.newInputStream(path)) {
			ImgPlus<? extends RealType<?>> img = is.readAsRealTypedImg(ins);
			out = new DefaultDataset(context, img);
		}
		
		Files.deleteIfExists(path);
		out.setName("restreamed.png");
		uiService.show(out);
		ij.ui().getDefaultUI().dispose();
	}

	private static void prepareImageJ() {
		ij = new ImageJ();
		context = ij.context();
		ioService = context.service(DatasetIOService.class);
		uiService = context.service(UIService.class);
	}

	
}
