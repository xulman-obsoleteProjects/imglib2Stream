package de.mpicbg.ulman.imgstreamer;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TypedPixelStreamer< T >
{

	private final T type;

	private final String typeString;

	private final PixelConverter< T > converter;

	private TypedPixelStreamer( T type, String typeString, PixelConverter< T > converter )
	{
		this.type = type;
		this.typeString = typeString;
		this.converter = converter;
	}

	public T type()
	{
		return this.type;
	}

	public String typeString()
	{
		return this.typeString;
	}

	public void write( RandomAccessibleInterval< T > image, OutputStream output ) throws IOException
	{
		converter.write( output, Views.flatIterable( image ).cursor() );
	}

	public RandomAccessibleInterval< T > read( InputStream input, RandomAccessibleInterval< T > image ) throws IOException
	{
		converter.read( input, Views.flatIterable( image ).cursor() );
		return image;
	}

	public static < T extends NativeType< ? > > TypedPixelStreamer< T > forType( T type )
	{
		for ( TypedPixelStreamer converter : CONVERTERS )
			if ( converter.type().getClass().isInstance( type ) )
				return converter;
		throw new UnsupportedOperationException( "Unsupported pixel type: " + type.getClass().getSimpleName() );
	}

	public static TypedPixelStreamer< ? > forType( String typeString )
	{
		for ( TypedPixelStreamer< ? > converter : CONVERTERS )
			if ( converter.typeString().equals( typeString ) )
				return converter;
		throw new UnsupportedOperationException( "Unsupported pixel type: " + typeString );
	}

	private static final List< TypedPixelStreamer< ? > > CONVERTERS = Arrays.asList(
			new TypedPixelStreamer<>( new BitType(), "bit", new BooleanTypeConverter() ),
			new TypedPixelStreamer<>( new UnsignedByteType(), "uint8", new GenericByteTypeConverter() ),
			new TypedPixelStreamer<>( new ByteType(), "int8", new GenericByteTypeConverter() ),
			new TypedPixelStreamer<>( new UnsignedShortType(), "uint16", new GenericShortTypeConverter() ),
			new TypedPixelStreamer<>( new ShortType(), "int16", new GenericShortTypeConverter() ),
			new TypedPixelStreamer<>( new UnsignedIntType(), "uint32", new GenericIntTypeConverter() ),
			new TypedPixelStreamer<>( new IntType(), "int32", new GenericIntTypeConverter() ),
			new TypedPixelStreamer<>( new UnsignedLongType(), "uint64", new GenericLongTypeConverter() ),
			new TypedPixelStreamer<>( new LongType(), "int64", new GenericLongTypeConverter() ),
			new TypedPixelStreamer<>( new FloatType(), "float32", new FloatTypeConverter() ),
			new TypedPixelStreamer<>( new DoubleType(), "float64", new DoubleTypeConverter() ),
			new TypedPixelStreamer<>( new ARGBType(), "arbg8", new ARGBTypeConverter() )
	);

	interface PixelConverter< T >
	{
		void write( OutputStream output, Iterator< T > cursor ) throws IOException;

		void read( InputStream input, Iterator< T > cursor ) throws IOException;
	}

	private static class BooleanTypeConverter implements PixelConverter< BooleanType >
	{

		@Override
		public void write( OutputStream output, Iterator< BooleanType > cursor ) throws IOException
		{
			while ( cursor.hasNext() )
			{
				int value = 0;
				if ( cursor.next().get() )
					value |= 1;
				if ( cursor.hasNext() && cursor.next().get() )
					value |= 1 << 1;
				if ( cursor.hasNext() && cursor.next().get() )
					value |= 1 << 2;
				if ( cursor.hasNext() && cursor.next().get() )
					value |= 1 << 3;
				if ( cursor.hasNext() && cursor.next().get() )
					value |= 1 << 4;
				if ( cursor.hasNext() && cursor.next().get() )
					value |= 1 << 5;
				if ( cursor.hasNext() && cursor.next().get() )
					value |= 1 << 6;
				if ( cursor.hasNext() && cursor.next().get() )
					value |= 1 << 7;
				output.write( value );
			}
		}

		@Override
		public void read( InputStream input, Iterator< BooleanType > cursor ) throws IOException
		{
			while ( cursor.hasNext() )
			{
				int value = input.read();
				if ( value < -1 )
					throw new EOFException();
				cursor.next().set( ( value & 1 ) != 0 );
				if ( cursor.hasNext() )
					cursor.next().set( ( value & ( 1 << 1 ) ) != 0 );
				if ( cursor.hasNext() )
					cursor.next().set( ( value & ( 1 << 2 ) ) != 0 );
				if ( cursor.hasNext() )
					cursor.next().set( ( value & ( 1 << 3 ) ) != 0 );
				if ( cursor.hasNext() )
					cursor.next().set( ( value & ( 1 << 4 ) ) != 0 );
				if ( cursor.hasNext() )
					cursor.next().set( ( value & ( 1 << 5 ) ) != 0 );
				if ( cursor.hasNext() )
					cursor.next().set( ( value & ( 1 << 6 ) ) != 0 );
				if ( cursor.hasNext() )
					cursor.next().set( ( value & ( 1 << 7 ) ) != 0 );
			}
		}
	}

	private static class GenericByteTypeConverter implements PixelConverter< GenericByteType >
	{

		@Override
		public void write( OutputStream output, Iterator< GenericByteType > cursor ) throws IOException
		{
			while ( cursor.hasNext() )
				output.write( cursor.next().getByte() );
		}

		@Override
		public void read( InputStream input, Iterator< GenericByteType > cursor ) throws IOException
		{
			final DataInputStream stream = new DataInputStream( input );
			while ( cursor.hasNext() )
				cursor.next().setByte( stream.readByte() );
		}
	}

	static class GenericShortTypeConverter implements PixelConverter< GenericShortType >
	{

		@Override
		public void write( OutputStream output, Iterator< GenericShortType > cursor ) throws IOException
		{
			final DataOutputStream stream = new DataOutputStream( output );
			while ( cursor.hasNext() )
				stream.writeShort( cursor.next().getShort() );
		}

		@Override
		public void read( final InputStream input, final Iterator< GenericShortType > cursor ) throws IOException
		{
			while ( cursor.hasNext() )
			{
				final int a = input.read();
				final int b = input.read();
				if ( b < 0 )
					throw new EOFException();
				cursor.next().setShort( ( short ) ( ( a << 8 ) | b ) );
			}
		}
	}

	private static class GenericIntTypeConverter implements PixelConverter< GenericIntType >
	{
		@Override
		public void write( OutputStream output, Iterator< GenericIntType > cursor ) throws IOException
		{
			while ( cursor.hasNext() )
				writeInt( output, cursor.next().getInt() );
		}

		@Override
		public void read( InputStream input, Iterator< GenericIntType > cursor ) throws IOException
		{
			while ( cursor.hasNext() )
				cursor.next().setInt( readInt( input ) );
		}
	}

	private static class GenericLongTypeConverter implements PixelConverter< GenericLongType >
	{
		@Override
		public void write( OutputStream output, Iterator< GenericLongType > cursor ) throws IOException
		{
			final DataOutputStream stream = new DataOutputStream( output );
			while ( cursor.hasNext() )
				stream.writeLong( cursor.next().getLong() );
		}

		@Override
		public void read( InputStream input, Iterator< GenericLongType > cursor ) throws IOException
		{
			final DataInputStream stream = new DataInputStream( input );
			while ( cursor.hasNext() )
				cursor.next().setLong( stream.readLong() );
		}
	}

	private static class FloatTypeConverter implements PixelConverter< FloatType >
	{
		@Override
		public void write( OutputStream output, Iterator< FloatType > cursor ) throws IOException
		{
			while ( cursor.hasNext() )
				writeInt( output, Float.floatToIntBits( cursor.next().getRealFloat() ) );
		}

		@Override
		public void read( InputStream input, Iterator< FloatType > cursor ) throws IOException
		{
			while ( cursor.hasNext() )
				cursor.next().setReal( Float.intBitsToFloat( readInt( input ) ) );
		}
	}

	private static class DoubleTypeConverter implements PixelConverter< DoubleType >
	{
		@Override
		public void write( OutputStream output, Iterator< DoubleType > cursor ) throws IOException
		{
			final DataOutputStream stream = new DataOutputStream( output );
			while ( cursor.hasNext() )
				stream.writeDouble( cursor.next().getRealDouble() );
		}

		@Override
		public void read( InputStream input, Iterator< DoubleType > cursor ) throws IOException
		{
			final DataInputStream stream = new DataInputStream( input );
			while ( cursor.hasNext() )
				cursor.next().setReal( stream.readDouble() );
		}
	}

	private static class ARGBTypeConverter implements PixelConverter< ARGBType >
	{
		@Override
		public void write( OutputStream output, Iterator< ARGBType > cursor ) throws IOException
		{
			while ( cursor.hasNext() )
				writeInt( output, cursor.next().get() );
		}

		@Override
		public void read( InputStream input, Iterator< ARGBType > cursor ) throws IOException
		{
			while ( cursor.hasNext() )
				cursor.next().set( readInt( input ) );
		}
	}

	private static void writeInt( OutputStream output, int value ) throws IOException
	{
		output.write( value >> 24 );
		output.write( value >> 16 );
		output.write( value >> 8 );
		output.write( value );
	}

	private static int readInt( InputStream input ) throws IOException
	{
		int a = input.read();
		int b = input.read();
		int c = input.read();
		int d = input.read();
		if ( d < -1 )
			throw new EOFException();
		return a << 24 | b << 16 | c << 8 | d;
	}
}
