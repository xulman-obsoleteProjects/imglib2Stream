package de.mpicbg.ulman.imgstreamer;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PixelStreamer
{

	public static < T extends NativeType< ? > > void send( RandomAccessibleInterval< T > image, OutputStream output ) throws IOException
	{
		TypedPixelStreamer.forType( Util.getTypeFromInterval( image ) ).write( image, output );
	}

	public static < T extends NativeType< ? > > RandomAccessibleInterval< T > receive( InputStream input, RandomAccessibleInterval< T > image ) throws IOException
	{
		return TypedPixelStreamer.forType( Util.getTypeFromInterval( image ) ).read( input, image );
	}
}
