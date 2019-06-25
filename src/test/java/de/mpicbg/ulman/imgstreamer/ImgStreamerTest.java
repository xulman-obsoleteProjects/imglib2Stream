package de.mpicbg.ulman.imgstreamer;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import net.imglib2.test.ImgLib2Assert;
import net.imglib2.test.RandomImgs;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class ImgStreamerTest
{
	private ProgressCallback dummyProgress = new ProgressCallback()
	{
		@Override
		public void info( String msg ) {
			//System.out.println(msg);
		}
		@Override
		public void setProgress( float howFar ) {}
	};

	private final ImgStreamer streamer = new ImgStreamer( dummyProgress );

	@Test
	public void checkSendAndReceive()
	{
		//voxel types that have integer number of bytes per pixel
		final List<NativeType< ? >> TYPES = Arrays.asList(
				new ByteType( ( byte ) 42 ),
				new UnsignedByteType( ( byte ) 42 ),
				new ShortType( ( short ) 42 ),
				new UnsignedShortType( ( short ) 42 ),
				new IntType( 42 ),
				new UnsignedIntType( 42 ),
				new LongType( 42 ),
				new UnsignedLongType( 42 ),
				new FloatType( 42 ),
				new DoubleType( 42 ),
				new ARGBType( 42 )
		);

		for (NativeType< ? > type : TYPES)
		{
			try
			{
				//------- ArrayImg: -------
				Img< ? > refImage = RandomImgs.seed(42).randomize(
						new ArrayImgFactory( type ).create( 201, 100, 5 ) );

				System.out.println("Trying ArrayImg of type "+type.getClass().getSimpleName()
						+" and size of "+refImage.size()+" pixels");

				Img< ? > gotImage = sendAndReceiveImg( refImage );

				//compare with the reference and obtained image
				ImgLib2Assert.assertImageEquals( (RandomAccessibleInterval)gotImage, (RandomAccessibleInterval)refImage );

				//------- PlanarImg: -------
				refImage = RandomImgs.seed(42).randomize(
						new PlanarImgFactory( type ).create( 200, 102, 5 ) );

				System.out.println("Trying PlanarImg of type "+type.getClass().getSimpleName()
						+" and size of "+refImage.size()+" pixels");

				gotImage = sendAndReceiveImg( refImage );

				//compare with the reference and obtained image
				ImgLib2Assert.assertImageEquals( (RandomAccessibleInterval)gotImage, (RandomAccessibleInterval)refImage );

				//------- CellImg: -------
				refImage = RandomImgs.seed(42).randomize(
						new CellImgFactory( type, 50, 20, 6 ).create( 200, 119, 5 ) );

				System.out.println("Trying CellImg of type "+type.getClass().getSimpleName()
						+" and size of "+refImage.size()+" pixels");

				gotImage = sendAndReceiveImg( refImage );

				//compare with the reference and obtained image
				ImgLib2Assert.assertImageEquals( (RandomAccessibleInterval)gotImage, (RandomAccessibleInterval)refImage );
			}
			catch ( IOException e )
			{
				System.out.println("IOException for "+type.getClass().getSimpleName());
				e.printStackTrace();
			}
		}
	}

	@Test
	public void complainOnARGBasRealType()
	{
		Img< ARGBType > refImage = RandomImgs.seed(42).randomize(
				new ArrayImgFactory( new ARGBType( 42 ) ).create( 201, 100, 5 ) );

		System.out.println("Trying ArrayImg of type "+refImage.firstElement().getClass().getSimpleName()
				+" and size of "+refImage.size()+" pixels -- should get an exception");
		boolean wasExceptionTriggered = false;

		try
		{
			streamer.setImageForStreaming( new ImgPlus( refImage ) );
			ByteArrayOutputStream output = new ByteArrayOutputStream( (int)streamer.getOutputStreamLength() );
			streamer.write( output );

			assertEquals( output.size(), streamer.getOutputStreamLength() );

			ByteArrayInputStream input = new ByteArrayInputStream( output.toByteArray() );
			streamer.readAsRealTypedImg( input ).getImg();
		}
		catch ( IOException e )
		{
			wasExceptionTriggered = true;
			System.out.println("IOException for "+refImage.firstElement().getClass().getSimpleName()
					+": "+e.getMessage());
		}

		assertEquals(true, wasExceptionTriggered);
	}

	@Test
	public void goWellOnUnsignedShortasRealType()
	{
		Img< UnsignedShortType > refImage = RandomImgs.seed(42).randomize(
				new ArrayImgFactory( new UnsignedShortType( 42 ) ).create( 201, 100, 5 ) );

		System.out.println("Trying ArrayImg of type "+refImage.firstElement().getClass().getSimpleName()
				+" and size of "+refImage.size()+" pixels -- should go well (w/o exception)");
		boolean wasExceptionTriggered = false;

		try
		{
			streamer.setImageForStreaming( new ImgPlus( refImage ) );
			ByteArrayOutputStream output = new ByteArrayOutputStream( (int)streamer.getOutputStreamLength() );
			streamer.write( output );

			assertEquals( output.size(), streamer.getOutputStreamLength() );

			ByteArrayInputStream input = new ByteArrayInputStream( output.toByteArray() );
			streamer.readAsRealTypedImg( input ).getImg();
		}
		catch ( IOException e )
		{
			wasExceptionTriggered = true;
			System.out.println("IOException for "+refImage.firstElement().getClass().getSimpleName()
					+": "+e.getMessage());
		}

		assertEquals(false, wasExceptionTriggered);
	}

	private Img< ? > sendAndReceiveImg( final Img< ? > image ) throws IOException
	{
		streamer.setImageForStreaming( new ImgPlus( image ) );
		ByteArrayOutputStream output = new ByteArrayOutputStream( (int)streamer.getOutputStreamLength() );
		streamer.write( output );

		assertEquals( output.size(), streamer.getOutputStreamLength() );

		ByteArrayInputStream input = new ByteArrayInputStream( output.toByteArray() );
		return streamer.read( input ).getImg();
	}
}
