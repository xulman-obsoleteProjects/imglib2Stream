package de.mpicbg.ulman.imgstreamer;

import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.test.ImgLib2Assert;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;

public class TestStreams
{
	@Test
	public void testImageTransfer()
	{
		for ( Class< ? extends NativeType > aClass : ImgStreamer.SUPPORTED_VOXEL_CLASSES )
		{
			try
			{
				NativeType< ? > type = aClass.newInstance();

				testImgPlus( fillImg(
						new ArrayImgFactory( type ).create( 200, 100, 5 )
				) );

				testImgPlus( fillImg(
						new PlanarImgFactory( type ).create( 200, 100, 5 )
				) );

				testImgPlus( fillImg(
						new CellImgFactory( type, 50, 20, 5 ).create( 200, 100, 5 )
				) );
			}
			catch ( InstantiationException | IllegalAccessException e )
			{
				throw new RuntimeException( e );
			}
		}
	}

	private static class myLogger implements ProgressCallback
	{
		@Override
		public void info( String msg )
		{
			System.out.println( msg );
		}

		@Override
		public void setProgress( float howFar )
		{
			System.out.println( "progress: " + howFar );
		}
	}

	private static < T extends RealType< T > & NativeType< T > > void testImgPlus( final Img< T > img )
	{
		try
		{
			//create ImgPlus out of the input Img
			ImgPlus< T > imgP = new ImgPlus<>( img );
			imgP.setName( "qwerty" );
			imgP.setSource( "qwertyiiii" );
			imgP.setValidBits( 1024 );

			//sample output stream
			final OutputStream os = new FileOutputStream( "/tmp/out.dat" );
			//
			//ImgStreamer must always be instantiated if you want to know the complete
			//stream size before the actual serialization into an OutputStream
			//ImgStreamer isv = new ImgStreamer( new myLogger() );
			ImgStreamer isv = new ImgStreamer( null );
			isv.setImageForStreaming( imgP );
			System.out.print( "stream length: " + isv.getOutputStreamLength() );
			isv.write( os );
			//
			os.close();

			//sample input stream
			final InputStream is = new FileInputStream( "/tmp/out.dat" );
			//
			ImgPlus< ? extends RealType< ? > > imgPP = isv.readAsRealTypedImg( is );
			//
			is.close();
			System.out.println( "got this image: " + imgPP.getImg().toString()
					+ " of " + imgPP.getImg().firstElement().getClass().getSimpleName() );
			ImgLib2Assert.assertImageEquals( imgP, imgPP, Object::equals );

		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	@Test
	public void testQueingOfObjectsInAStream() throws IOException
	{
		// setup
		File tmpFile = File.createTempFile( "tmp", ".dat" );
		tmpFile.deleteOnExit();
		String expected1 = "ahoj";
		String expected2 = "clovece";
		// write
		final ObjectOutputStream os = new ObjectOutputStream( new FileOutputStream( tmpFile ) );
		os.writeUTF( expected1 );
		os.writeUTF( expected2 );
		os.close();
		// read
		final ObjectInputStream is = new ObjectInputStream( new FileInputStream( tmpFile ) );
		String actual1 = is.readUTF();
		String actual2 = is.readUTF();
		is.close();
		// test
		assertEquals(expected1, actual1);
		assertEquals(expected2, actual2);
	}

	private static < T extends RealType< T > > Img< T > fillImg( final Img< T > img )
	{
		long counter = 0;

		Cursor< T > imgC = img.cursor();
		while ( imgC.hasNext() )
			imgC.next().setReal( counter++ );

		return img;
	}
}
