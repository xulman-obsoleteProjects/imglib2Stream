package de.mpicbg.ulman.imgstreamer;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PixelStreamer
{

	public static < T extends NativeType< ? > > void send( RandomAccessibleInterval< T > image, OutputStream output ) throws IOException
	{
		final Cursor< T > cursor = Views.flatIterable( image ).cursor();
		if ( !cursor.hasNext() )
			return;
		T first = cursor.next();
		PixelSerializer< T > serializer = getSerializer( first );
		cursor.reset();
		serializer.write( output, cursor );
	}

	public static < T extends NativeType< ? > > RandomAccessibleInterval< T > receive( InputStream input, RandomAccessibleInterval< T > image ) throws IOException
	{
		final Cursor< T > cursor = Views.flatIterable( image ).cursor();
		if ( !cursor.hasNext() )
			return image;
		T first = cursor.next();
		PixelSerializer< T > serializer = getSerializer( first );
		cursor.reset();
		serializer.read( input, cursor );
		return image;
	}

	private static < T extends NativeType< ? > > PixelSerializer< T > getSerializer( T type )
	{
		for ( PixelSerializer serializer : SERIALIZERS )
			if ( serializer.clazz().isInstance( type ) )
				return serializer;
		throw new UnsupportedOperationException( "Unsupported pixel type: " + type.getClass().getSimpleName() );
	}

	private static final List< PixelSerializer > SERIALIZERS = Arrays.asList(
			new BooleanTypeStreamer(),
			new GenericByteTypeStreamer(),
			new GenericShortTypeStreamer(),
			new GenericIntTypeStreamer(),
			new GenericLongTypeStreamer(),
			new DoubleTypeStreamer(),
			new FloatTypeStreamer()
	);

	interface PixelSerializer< T >
	{

		Class< T > clazz();

		void write( OutputStream output, Iterator< T > cursor ) throws IOException;

		void read( InputStream input, Iterator< T > cursor ) throws IOException;
	}

	private static class BooleanTypeStreamer implements PixelSerializer<BooleanType>
	{

		@Override
		public Class< BooleanType > clazz()
		{
			return BooleanType.class;
		}

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
				cursor.next().set( (value & 1) != 0 );
				if(cursor.hasNext()) cursor.next().set( (value & (1 << 1)) != 0 );
				if(cursor.hasNext()) cursor.next().set( (value & (1 << 2)) != 0 );
				if(cursor.hasNext()) cursor.next().set( (value & (1 << 3)) != 0 );
				if(cursor.hasNext()) cursor.next().set( (value & (1 << 4)) != 0 );
				if(cursor.hasNext()) cursor.next().set( (value & (1 << 5)) != 0 );
				if(cursor.hasNext()) cursor.next().set( (value & (1 << 6)) != 0 );
				if(cursor.hasNext()) cursor.next().set( (value & (1 << 7)) != 0 );
			}
		}
	}

	private static class GenericByteTypeStreamer implements PixelSerializer< GenericByteType >
	{

		@Override
		public Class< GenericByteType > clazz()
		{
			return GenericByteType.class;
		}

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

	static class GenericShortTypeStreamer implements PixelSerializer< GenericShortType >
	{

		@Override
		public Class< GenericShortType > clazz()
		{
			return GenericShortType.class;
		}

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

	private static class GenericIntTypeStreamer implements PixelSerializer< GenericIntType >
	{

		@Override
		public Class< GenericIntType > clazz()
		{
			return GenericIntType.class;
		}

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

	private static class GenericLongTypeStreamer implements PixelSerializer< GenericLongType >
	{

		@Override
		public Class< GenericLongType > clazz()
		{
			return GenericLongType.class;
		}

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

	private static class FloatTypeStreamer implements PixelSerializer< FloatType >
	{

		@Override
		public Class< FloatType > clazz()
		{
			return FloatType.class;
		}

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

	private static class DoubleTypeStreamer implements PixelSerializer< DoubleType >
	{

		@Override
		public Class< DoubleType > clazz()
		{
			return DoubleType.class;
		}

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
