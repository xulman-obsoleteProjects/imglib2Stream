package de.mpicbg.ulman.imgstreamer;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class PixelStreamer
{

	public static void send( RandomAccessibleInterval< ? extends RealType< ? > > image, OutputStream output ) throws IOException
	{
		DataOutputStream stream = new DataOutputStream( output );
		final Cursor< ? extends RealType< ? > > cursor = Views.flatIterable( image ).cursor();
		if( ! cursor.hasNext() )
			return;
		RealType< ? > first = cursor.next();
		PixelSerializer< RealType<?> > serializer = getSerializer( first );
		serializer.writer( stream, first );
		while ( cursor.hasNext() )
			serializer.writer( stream, cursor.next() );
	}

	private static PixelSerializer<RealType<?>> getSerializer( RealType<?> type )
	{
		for(PixelSerializer<?> serializer : SERIALIZERS )
			if( serializer.clazz().isInstance( type  ) )
				return ( PixelSerializer< RealType< ? > > ) serializer;
		throw new UnsupportedOperationException( "Unsupported pixel type: " + type.getClass().getSimpleName() );
	}

	public static RandomAccessibleInterval< ShortType > receive( InputStream input, Img< ShortType > image ) throws IOException
	{
		DataInputStream stream = new DataInputStream( input );
		Cursor< ShortType > cursor = Views.flatIterable(image).cursor();
		while ( cursor.hasNext() )
			cursor.next().set( stream.readShort() );
		return image;
	}

	private static final List< PixelSerializer<?> > SERIALIZERS = Arrays.asList(
			new ByteSerializer(),
			new ShortSerializer(),
			new IntSerializer(),
			new LongSerializer(),
			new FloatSerializer(),
			new DoubleSerializer()
	);

	interface PixelSerializer<T> {

		Class<? extends T> clazz();

		void writer( DataOutputStream stream, T type ) throws IOException;

		void reader( DataInputStream stream, T type ) throws IOException;
	}

	private static class ByteSerializer implements PixelSerializer< GenericByteType > {

		@Override
		public Class< ? extends GenericByteType > clazz()
		{
			return GenericByteType.class;
		}

		@Override
		public void writer( DataOutputStream stream, GenericByteType type ) throws IOException
		{
			stream.writeByte( type.getByte() );
		}

		@Override
		public void reader( DataInputStream stream, GenericByteType type ) throws IOException
		{
			type.setByte( stream.readByte() );
		}
	}

	private static class ShortSerializer implements PixelSerializer< GenericShortType > {

		@Override
		public Class< ? extends GenericShortType > clazz()
		{
			return GenericShortType.class;
		}

		@Override
		public void writer( DataOutputStream stream, GenericShortType type ) throws IOException
		{
			stream.writeShort( type.getShort() );
		}

		@Override
		public void reader( DataInputStream stream, GenericShortType type ) throws IOException
		{
			type.setShort( stream.readShort() );
		}
	}

	private static class IntSerializer implements PixelSerializer< GenericIntType > {

		@Override
		public Class< ? extends GenericIntType > clazz()
		{
			return GenericIntType.class;
		}

		@Override
		public void writer( DataOutputStream stream, GenericIntType type ) throws IOException
		{
			stream.writeInt( type.getInteger() );
		}

		@Override
		public void reader( DataInputStream stream, GenericIntType type ) throws IOException
		{
			type.setInteger( stream.readInt() );
		}
	}

	private static class LongSerializer implements PixelSerializer< GenericLongType > {

		@Override
		public Class< ? extends GenericLongType > clazz()
		{
			return GenericLongType.class;
		}

		@Override
		public void writer( DataOutputStream stream, GenericLongType type ) throws IOException
		{
			stream.writeLong( type.getLong() );
		}

		@Override
		public void reader( DataInputStream stream, GenericLongType type ) throws IOException
		{
			type.setLong( stream.readLong() );
		}
	}

	private static class FloatSerializer implements PixelSerializer< FloatType > {

		@Override
		public Class< ? extends FloatType > clazz()
		{
			return FloatType.class;
		}

		@Override
		public void writer( DataOutputStream stream, FloatType type ) throws IOException
		{
			stream.writeFloat( type.getRealFloat() );
		}

		@Override
		public void reader( DataInputStream stream, FloatType type ) throws IOException
		{
			type.setReal( stream.readFloat() );
		}
	}

	private static class DoubleSerializer implements PixelSerializer< DoubleType > {

		@Override
		public Class< ? extends DoubleType > clazz()
		{
			return DoubleType.class;
		}

		@Override
		public void writer( DataOutputStream stream, DoubleType type ) throws IOException
		{
			stream.writeDouble( type.getRealDouble() );
		}

		@Override
		public void reader( DataInputStream stream, DoubleType type ) throws IOException
		{
			type.setReal( stream.readDouble() );
		}
	}
}
