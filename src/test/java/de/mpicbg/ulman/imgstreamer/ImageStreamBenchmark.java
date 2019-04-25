package de.mpicbg.ulman.imgstreamer;

import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Fork( 0 )
@Warmup( iterations = 4, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@State( Scope.Benchmark )
public class ImageStreamBenchmark
{
	Img< ShortType > image = new CellImgFactory<>( new ShortType() ).create( 100, 100 );

	ProgressCallback dummyProgress = new ProgressCallback()
	{
		@Override
		public void info( String msg )
		{

		}

		@Override
		public void setProgress( float howFar )
		{

		}
	};

	@Benchmark
	public ImgPlus< ? > benchmarkStreamer() throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		final ImgStreamer streamer = new ImgStreamer( dummyProgress );
		streamer.setImageForStreaming( new ImgPlus<>( image ) );
		streamer.write( output );
		//InputStream input = new ByteArrayInputStream( output.toByteArray() );
		return null;//streamer.read( input );
	}

	@Benchmark
	public RandomAccessibleInterval<ShortType> simple() throws IOException
	{
		// setup
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		// run
		send( image, output );
		//InputStream input = new ByteArrayInputStream( output.toByteArray() );
		return null;//receive( input );
	}

	private static void send( Img< ShortType > image, ByteArrayOutputStream output ) throws IOException
	{
		DataOutputStream stream = new DataOutputStream( output );
		final Cursor< ShortType > cursor = Views.flatIterable( image ).cursor();
		while ( cursor.hasNext() )
			stream.writeShort( cursor.next().getShort() );
	}

	private RandomAccessibleInterval<ShortType> receive( InputStream input ) throws IOException
	{
		DataInputStream stream = new DataInputStream( input );
		final Img< ShortType > image = ArrayImgs.shorts( Intervals.dimensionsAsLongArray( this.image ) );
		Cursor < ShortType > cursor = image.cursor();
		while ( cursor.hasNext() ) {
			cursor.next().set( stream.readShort() );
		}
		return image;
	}

	public static void main( String... args ) throws RunnerException
	{
		final Options options = new OptionsBuilder()
				.include( ImageStreamBenchmark.class.getName() )
				.build();
		new Runner( options ).run();
	}
}
