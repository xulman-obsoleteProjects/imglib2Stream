package de.mpicbg.ulman.imgstreamer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

public class RandomAccessibleIntervalStreamer
{

	private static final ObjectMapper mapper = new ObjectMapper();

	private static final ObjectReader reader = mapper.readerFor( Metadata.class );

	private static final ObjectWriter writer = mapper.writerFor( Metadata.class );

	public static void write( RandomAccessibleInterval< ? extends NativeType< ? > > image, OutputStream output ) throws IOException
	{
		TypedPixelStreamer pixelStreamer = TypedPixelStreamer.forType( Util.getTypeFromInterval( image ) );
		Metadata metadata = new Metadata();
		metadata.type = pixelStreamer.typeString();
		metadata.offset = Intervals.minAsLongArray( image );
		metadata.size = Intervals.dimensionsAsLongArray( image );
		final byte[] bytes = writer.writeValueAsBytes( metadata );
		writeByteArray( output, bytes );
		pixelStreamer.write( image, output );
	}

	public static RandomAccessibleInterval< ? > read( InputStream inputStream ) throws IOException
	{
		Metadata metadata = reader.readValue( readByteArray( inputStream ) );
		TypedPixelStreamer pixelStreamer = TypedPixelStreamer.forType( metadata.type );
		final Img< ? > image = new ArrayImgFactory<>( ( NativeType ) pixelStreamer.type() ).create( metadata.size );
		pixelStreamer.read( inputStream, image );
		return optionalTranslate( image, metadata.offset );
	}

	// helper methods

	private static < T > RandomAccessibleInterval< T > optionalTranslate( RandomAccessibleInterval< T > image, long[] offset )
	{
		if ( allZero( offset ) )
			return image;
		return Views.translate( image, offset );
	}

	private static boolean allZero( long[] offset )
	{
		for ( int i = 0; i < offset.length; i++ )
			if ( offset[ i ] != 0 )
				return false;
		return true;
	}

	static byte[] readByteArray( InputStream in ) throws IOException
	{
		int length = new DataInputStream( in ).readInt();
		byte[] bytes = new byte[ length ];
		if ( length != in.read( bytes ) )
			throw new EOFException();
		return bytes;
	}

	static void writeByteArray( OutputStream out, byte[] bytes ) throws IOException
	{
		new DataOutputStream( out ).writeInt( bytes.length );
		out.write( bytes );
	}

	// helper class

	public static class Metadata
	{
		public String type;

		public long[] size;

		public long[] offset;

		@Override
		public boolean equals( Object o )
		{
			if ( !( o instanceof Metadata ) )
				return false;
			Metadata other = ( Metadata ) o;
			return Objects.equals( type, other.type ) && Arrays.equals( size, other.size ) && Arrays.equals( offset, other.offset );
		}
	}
}
