package de.mpicbg.ulman.imgstreamer;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Fork( 1 )
@Warmup( iterations = 20, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@Measurement( iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS )
@State( Scope.Benchmark )
public class PixelStreamerBenchmark< T extends NativeType< T > >
{
	private static final List< NativeType< ? > > TYPES = Arrays.asList(
			new BitType(),
			new UnsignedByteType(),
			new UnsignedShortType(),
			new UnsignedIntType(),
			new UnsignedLongType(),
			new DoubleType(),
			new FloatType()
	);

	@Param( "" )
	private String type;

	private Img< T > image;

	private byte[] manyBytes;

	@Setup
	public void setup()
	{
		T nativeType = ( T ) TYPES.stream().filter( x -> x.getClass().getSimpleName().equals( type ) ).findAny().get();
		image = new ArrayImgFactory<>( nativeType ).create( 100, 100 );
		manyBytes = new byte[100 * 100 * 8];
	}

	@Benchmark
	public ByteArrayOutputStream benchmarkSend() throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PixelStreamer.send( image, output );
		return output;
	}

	@Benchmark
	public Img< T > benchmarkReceive() throws IOException
	{
		ByteArrayInputStream input = new ByteArrayInputStream( manyBytes );
		PixelStreamer.receive( input, image );
		return image;
	}

	public static void main( String... args ) throws RunnerException
	{
		Options options = new OptionsBuilder()
				.param( "type", TYPES.stream().map( type -> type.getClass().getSimpleName() ).toArray( String[]::new ) )
				.include( PixelStreamerBenchmark.class.getName() )
				.build();
		new Runner( options ).run();
	}
}
