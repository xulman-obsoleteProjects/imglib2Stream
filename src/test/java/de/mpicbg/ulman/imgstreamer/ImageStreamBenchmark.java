package de.mpicbg.ulman.imgstreamer;

import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.test.RandomImgs;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.integer.ShortType;
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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Fork( 1 )
@Warmup( iterations = 20, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@State( Scope.Benchmark )
public class ImageStreamBenchmark
{
	private Img< ShortType > image = RandomImgs.seed(42).randomize( ArrayImgs.shorts( 100, 100 ) );

	private byte[] manyBytes = initBytes( () -> benchmarkImgStreamerSend() );

	private byte[] raiBytes = initBytes( () -> benchmarkRaiStreamerSend() );

	private byte[] initBytes( Callable<ByteArrayOutputStream> supplier )
	{
		try
		{
			return supplier.call().toByteArray();
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
	}

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
	public ByteArrayOutputStream benchmarkImgStreamerSend() throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		final ImgStreamer streamer = new ImgStreamer( dummyProgress );
		streamer.setImageForStreaming( new ImgPlus<>( image ) );
		streamer.write( output );
		return output;
	}

	@Benchmark
	public ImgPlus< ? > benchmarkImgStreamerReceive() throws IOException
	{
		final ImgStreamer streamer = new ImgStreamer( dummyProgress );
		InputStream input = initStream();
		return streamer.read( input );
	}

	@Benchmark
	public ByteArrayOutputStream benchmarkPixelStreamerSend() throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PixelStreamer.send( image, output );
		return output;
	}

	@Benchmark
	public Img< ShortType > benchmarkPixelStreamerReceive() throws IOException
	{
		final InputStream input = initStream();
		final Img< ShortType > image = ArrayImgs.shorts( 100, 100 );
		PixelStreamer.receive( input, image )	;
		return image;
	}

	@Benchmark
	public ByteArrayOutputStream benchmarkRaiStreamerSend() throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		RandomAccessibleIntervalStreamer.write( image, output );
		return output;
	}

	@Benchmark
	public RandomAccessibleInterval<?> benchmarkRaiStreamerReceive() throws IOException
	{
		InputStream input = new ByteArrayInputStream( raiBytes );
		return RandomAccessibleIntervalStreamer.read( input );
	}

	@Benchmark
	public ByteArrayOutputStream benchmarkBaselineSend() throws IOException
	{
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		final DataOutputStream stream = new DataOutputStream( output );
		final Cursor< ShortType > cursor = Views.flatIterable( image ).cursor();
		while ( cursor.hasNext() )
			stream.writeShort( cursor.next().getShort() );
		return output;
	}

	@Benchmark
	public Img< ShortType > benchmarkBaselineReceive() throws IOException
	{
		final InputStream inputStream = initStream();
		final Img< ShortType > image1 = ArrayImgs.shorts( 100, 100 );
		final Cursor< ? extends GenericShortType > cursor = Views.flatIterable( image1 ).cursor();
		final DataInputStream input = new DataInputStream( inputStream );
		while ( cursor.hasNext() )
			cursor.next().setShort(input.readShort());
		return image1;
	}

	private InputStream initStream()
	{
		return new ByteArrayInputStream( manyBytes );
	}

	public static void main( String... args ) throws RunnerException
	{
		final Options options = new OptionsBuilder()
				.include( ImageStreamBenchmark.class.getName() )
				.build();
		new Runner( options ).run();
	}
}
