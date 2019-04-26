package de.mpicbg.ulman.imgstreamer;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertArrayEquals;

public class PixelStreamerTest
{

	@Test
	public void testTypes() throws IOException
	{
		testType( new ByteType( ( byte ) 42 ), new byte[] { 42 } );
		testType( new UnsignedByteType( ( byte ) 42 ), new byte[] { 42 } );
		testType( new ShortType( ( short ) 42 ), new byte[] { 0, 42 } );
		testType( new UnsignedShortType( ( short ) 42 ), new byte[] { 0, 42 } );
		testType( new IntType( 42 ), new byte[] { 0, 0, 0, 42 } );
		testType( new UnsignedIntType( 42 ), new byte[] { 0, 0, 0, 42 } );
		testType( new LongType( 42 ), new byte[] { 0, 0, 0, 0, 0, 0, 0, 42 } );
		testType( new UnsignedLongType( 42 ), new byte[] { 0, 0, 0, 0, 0, 0, 0, 42 } );
		testType( new FloatType( 42 ), asBytes( 42f ) );
		testType( new DoubleType( 42 ), asBytes( 42.0 ) );
	}

	private < T extends NativeType< T > & RealType< T > > void testType( T type, byte[] bytes ) throws IOException
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		final Img< T > img = new ArrayImgFactory< T >( type ).create( 1 );
		img.cursor().next().set( type );
		PixelStreamer.send( img, stream );
		assertArrayEquals( bytes, stream.toByteArray() );
	}

	private byte[] asBytes( float v )
	{
		return ByteBuffer.allocate( 4 ).putFloat( v ).array();
	}

	private byte[] asBytes( double v )
	{
		return ByteBuffer.allocate( 8 ).putDouble( v ).array();
	}
}
