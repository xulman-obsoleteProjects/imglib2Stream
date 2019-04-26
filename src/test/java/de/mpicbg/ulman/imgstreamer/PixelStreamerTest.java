package de.mpicbg.ulman.imgstreamer;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.LongAccess;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.test.ImgLib2Assert;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
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
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PixelStreamerTest
{

	private static final List< Pair< NativeType< ? >, byte[] > > TYPES = Arrays.asList(
			new ValuePair<>( new BitType( true ), new byte[] { 1 } ),
			new ValuePair<>( new ByteType( ( byte ) 42 ), new byte[] { 42 } ),
			new ValuePair<>( new UnsignedByteType( ( byte ) 42 ), new byte[] { 42 } ),
			new ValuePair<>( new ShortType( ( short ) 42 ), new byte[] { 0, 42 } ),
			new ValuePair<>( new UnsignedShortType( ( short ) 42 ), new byte[] { 0, 42 } ),
			new ValuePair<>( new IntType( 42 ), new byte[] { 0, 0, 0, 42 } ),
			new ValuePair<>( new UnsignedIntType( 42 ), new byte[] { 0, 0, 0, 42 } ),
			new ValuePair<>( new LongType( 42 ), new byte[] { 0, 0, 0, 0, 0, 0, 0, 42 } ),
			new ValuePair<>( new UnsignedLongType( 42 ), new byte[] { 0, 0, 0, 0, 0, 0, 0, 42 } ),
			new ValuePair<>( new FloatType( 42 ), asBytes( 42f ) ),
			new ValuePair<>( new DoubleType( 42 ), asBytes( 42.0 ) )
	);

	@Test
	public void testSending() throws IOException
	{
		for( Pair< NativeType< ? >, byte[] > typeAndBytes : TYPES )	{
			NativeType< ? > value = typeAndBytes.getA();
			byte[] bytes = typeAndBytes.getB();
			testSendingOnePixel( (NativeType) value, bytes);
		}
	}

	private < T extends NativeType< T > > void testSendingOnePixel( T pixelValue, byte[] bytes ) throws IOException
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		final Img< T > img = new ArrayImgFactory<>( pixelValue ).create( 1 );
		img.cursor().next().set( pixelValue );
		PixelStreamer.send( img, stream );
		assertArrayEquals( bytes, stream.toByteArray() );
	}

	@Test
	public void testReceiving() throws IOException
	{
		for( Pair< NativeType< ? >, byte[] > typeAndBytes : TYPES )	{
			NativeType< ? > value = typeAndBytes.getA();
			byte[] bytes = typeAndBytes.getB();
			testReceivingOnPixel( (NativeType) value, bytes);
		}
	}

	private <T extends NativeType< T > > void testReceivingOnPixel( T pixelValue, byte[] bytes ) throws IOException
	{
		ByteArrayInputStream stream = new ByteArrayInputStream( bytes );
		final Img< T > img = new ArrayImgFactory<>( pixelValue ).create( 1 );
		PixelStreamer.receive( stream, img );
		assertEquals( pixelValue, img.iterator().next() );
	}

	private static byte[] asBytes( float v )
	{
		return ByteBuffer.allocate( 4 ).putFloat( v ).array();
	}

	private static byte[] asBytes( double v )
	{
		return ByteBuffer.allocate( 8 ).putDouble( v ).array();
	}

	@Test
	public void testSendBits() throws IOException
	{
		Img< BitType > source = ArrayImgs.bits( new LongArray( new long[] { 0x456 } ), 15 );
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		PixelStreamer.send( source, output );
		assertArrayEquals( new byte[]{ 0x56, 0x04 }, output.toByteArray());
	}

	@Test
	public void testReceiveBits() throws IOException
	{
		InputStream input = new ByteArrayInputStream( new byte[] { 0x56, 0x04 } );
		Img< BitType > result = ArrayImgs.bits( 15 );
		Img< BitType > expected = ArrayImgs.bits( new LongArray( new long[] { 0x456 } ), 15 );
		PixelStreamer.receive( input, result );
		ImgLib2Assert.assertImageEquals( expected, result );
	}
}
