package test.bug;




import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;



public class ExampleImage
{


	public static Path lenaAsTempFile()
	{
		return downloadToTmpFile( "https://upload.wikimedia.org/wikipedia/en/7/7d/Lenna_%28test_image%29.png" );
	}

	private static Path downloadToTmpFile(String url) {
		try (InputStream is = new URL(url).openStream()) {
			final File tempFile = File.createTempFile( "tmp", getSuffix(url) );
			tempFile.deleteOnExit();
			Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return tempFile.toPath();
		}
		catch (IOException exc) {
			throw new RuntimeException(exc);
		}
	}
	
	private static String getSuffix(String filename) {
		return filename.substring(filename.lastIndexOf('.'), filename.length());
	}
}
