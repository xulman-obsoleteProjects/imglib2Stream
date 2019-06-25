package de.mpicbg.ulman.imgstreamer;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.test.ImgLib2Assert;
import net.imglib2.test.RandomImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class RandomAccessibleIntervalStreamerTest
{
	@Test
	public void testSerializeImage() throws IOException
	{
		RandomAccessibleInterval< UnsignedShortType > image = Views.translate( ArrayImgs.unsignedShorts( 10, 5 ), 1, 2 );
		RandomImgs.seed( 42 ).randomize( image );
		testSerialization( image );
	}

	@Test
	public void testTypes() throws IOException
	{
		testSerialization( ArrayImgs.bytes( new byte[] { 42 }, 1 ) );
		testSerialization( ArrayImgs.unsignedBytes( new byte[] { 42 }, 1 ) );
		testSerialization( ArrayImgs.shorts( new short[] { 42 }, 1 ) );
		testSerialization( ArrayImgs.unsignedShorts( new short[] { 42 }, 1 ) );
		testSerialization( ArrayImgs.ints( new int[] { 42 }, 1 ) );
		testSerialization( ArrayImgs.unsignedInts( new int[] { 42 }, 1 ) );
		testSerialization( ArrayImgs.longs( new long[] { 42 }, 1 ) );
		testSerialization( ArrayImgs.unsignedLongs( new long[] { 42 }, 1 ) );
		testSerialization( ArrayImgs.floats( new float[] { 42 }, 1 ) );
		testSerialization( ArrayImgs.doubles( new double[] { 42 }, 1 ) );
	}

	private void testSerialization( RandomAccessibleInterval< ? extends NativeType< ? > > image ) throws IOException
	{
		// serialize
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		RandomAccessibleIntervalStreamer.write( image, out );
		// deserialize
		final InputStream in = new ByteArrayInputStream( out.toByteArray() );
		RandomAccessibleInterval< ? > result = RandomAccessibleIntervalStreamer.read( in );
		// test
		ImgLib2Assert.assertImageEquals( image, result, Object::equals );
	}
}
