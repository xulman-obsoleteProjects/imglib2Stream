/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */
package de.mpicbg.ulman.imgstreamer;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.AbstractImg;
import net.imglib2.img.Img;
import net.imglib2.img.WrappedImg;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.NativeType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;

public class ImgStreamer
{
	/// default logger that logs nowhere
	static public final class EmptyProgressCallback implements ProgressCallback
	{
		@Override
		public void info( String msg )
		{
		}

		@Override
		public void setProgress( float howFar )
		{
		}
	}

	/// currently used logger
	private
	final ProgressCallback logger;

	public ImgStreamer( final ProgressCallback _log )
	{
		logger = _log == null ? new EmptyProgressCallback() : _log;
	}

	// -------- streaming stuff OUT --------
	//reference on the image to be streamed
	private Img< ? extends NativeType< ? > > img;

	//reference on the TypedPixelStreamer that will serialize the pixel data
	private TypedPixelStreamer< ? extends NativeType< ? > > relevantPixelStreamer;

	//header corresponding to the image
	private String headerMsg;

	//metadata (from ImgPlus) corresponding to the image
	private byte[] metadataBytes;

	//image data size in bytes
	private long voxelBytesCount;

	public < T extends NativeType< T >, A extends ArrayDataAccess< A > >
	ImgStreamer setImageForStreaming( final ImgPlus< T > imgToBeStreamed )
	{
		img = getUnderlyingImg( imgToBeStreamed );
		final NativeType<?> pixelType = img.firstElement();

		//test: is the input image non-empty?
		if ( img.size() == 0 )
			throw new UnsupportedOperationException( "Refusing to stream an empty image..." );

		//also a test: throws an exception if the image is of non-supported voxel type
		relevantPixelStreamer = TypedPixelStreamer.forType( pixelType );

		//test (due to the limit of the protocol): bytes per pixel must be an integer
		if ( (relevantPixelStreamer.getEntitiesPerPixel().getNumerator() % relevantPixelStreamer.getEntitiesPerPixel().getDenominator()) != 0 )
			throw new UnsupportedOperationException(
					"Refusing to stream an image where pixels do not align to bytes (e.g. 12bits/pixel)..." );

		/* this test is covered by the above test
		if ( relevantPixelStreamer.getEntitiesPerPixel().getRatio() < 1.0 )
			throw new RuntimeException( "Refusing to stream an image with multiple pixels per one byte (e.g. boolean pixels)..." );
		*/

		//build the corresponding header:
		//protocol version
		headerMsg = "v2";

		//dimensionality of the image data (and its size in bytes)
		voxelBytesCount  = relevantPixelStreamer.getEntitiesPerPixel().getNumerator();
		voxelBytesCount /= relevantPixelStreamer.getEntitiesPerPixel().getDenominator();
		headerMsg += " dimNumber " + img.numDimensions();
		for ( int i = 0; i < img.numDimensions(); ++i )
		{
			headerMsg += " " + img.dimension( i );
			voxelBytesCount *= img.dimension( i );
		}

		//decipher the voxel type
		headerMsg += " " + pixelType.getClass().getSimpleName();

		//check we can handle the storage model of this image
		if ( img instanceof ArrayImg )
		{
			headerMsg += " ArrayImg ";
		}
		else if ( img instanceof PlanarImg )
		{
			headerMsg += " PlanarImg ";
		}
		else
			//if (img instanceof CellImg || img instanceof LazyCellImg)
			if ( img instanceof AbstractCellImg )
			{
				//DiskCachedCellImg<> and SCIFIOCellImg<> extends (besides other) LazyCellImg<>
				//LazyCellImg<> extends AbstractCellImg<T, A, Cell<A>, LazyCellImg.LazyCells<Cell<A>>>
				//  CellImg<>   extends AbstractCellImg<T, A, Cell<A>, ListImg<Cell<A>>>
				//both ListImg<C> and LazyCellImg.LazyCells<C> extends AbstractImg<C>
				final AbstractCellImg< ?, A, Cell< A >, ? extends AbstractImg< Cell< A > > > cellImg
						= ( AbstractCellImg< ?, A, Cell< A >, ? extends AbstractImg< Cell< A > > > ) img;

				//export also the internal configuration of tiles (that make up this image)
				headerMsg += " CellImg ";
				for ( int i = 0; i < cellImg.numDimensions(); ++i )
					headerMsg += cellImg.getCellGrid().cellDimension( i ) + " ";
			}
			else
				throw new UnsupportedOperationException( "Cannot determine the type of image backend, cannot stream it." );

		//process the metadata....
		metadataBytes = packAndSendPlusData( imgToBeStreamed );

		return this;
	}

	public long getOutputStreamLength()
	{
		return headerMsg.length() + metadataBytes.length + 4 + voxelBytesCount;
	}

	public
	void write( final OutputStream os )
			throws IOException
	{
		final DataOutputStream dos = new DataOutputStream( os );

		logger.info( "streaming the header: " + headerMsg );
		dos.writeUTF( headerMsg );
		if ( dos.size() != headerMsg.length() + 2 )
		{
			//System.out.println("dos.size()="+dos.size());
			throw new RuntimeException( "Header size calculation mismatch." );
		}

		logger.info( "streaming the metadata..." );
		dos.writeShort( metadataBytes.length );
		dos.write( metadataBytes );
		if ( dos.size() != headerMsg.length() + metadataBytes.length + 4 )
		{
			//System.out.println("dos.size()="+dos.size());
			throw new RuntimeException( "Metadata size calculation mismatch." );
		}
		dos.flush();

		logger.info( "streaming the image data..." );
		relevantPixelStreamer.write( (RandomAccessibleInterval)img, os );

		logger.info( "streaming finished." );
	}

	// -------- streaming stuff IN --------
	public ImgPlus< ? > read( final InputStream is )
			throws IOException
	{
		final DataInputStream dis = new DataInputStream( is );

		//read the header
		final String header = dis.readUTF();

		//read the metadata
		final byte[] metadata = new byte[ dis.readShort() ];
		dis.read( metadata );

		//process the header
		logger.info( "found header: " + header );
		StringTokenizer headerST = new StringTokenizer( header, " " );
		if ( !headerST.nextToken().startsWith( "v2" ) )
			throw new UnsupportedOperationException( "Unknown protocol, expecting protocol v2." );

		if ( !headerST.nextToken().startsWith( "dimNumber" ) )
			throw new RuntimeException( "Incorrect protocol, expecting dimNumber." );
		final int n = Integer.valueOf( headerST.nextToken() );

		//fill the dimensionality data
		final int[] dims = new int[ n ];
		for ( int i = 0; i < n; ++i )
			dims[ i ] = Integer.valueOf( headerST.nextToken() );

		final String typeStr = headerST.nextToken();
		final String backendStr = headerST.nextToken();

		//if CellImg is stored in the stream, unveil also its tile (cells) configuration
		int[] cellDims = null;
		if ( backendStr.startsWith( "CellImg" ) )
		{
			//parse out also the cellDims configuration
			cellDims = new int[ n ];
			for ( int i = 0; i < n; ++i )
				cellDims[ i ] = Integer.valueOf( headerST.nextToken() );
		}

		//also a test: throws an exception if the image is of non-supported voxel type
		relevantPixelStreamer = TypedPixelStreamer.forNativeTypeClassName( typeStr );

		//envelope/header message is (mostly) parsed,
		//start creating the output image of the appropriate type
		//NB: we can afford casting via raw types because the variable
		//    was assigned from a method that returns the wanted type
		Img< ? > img = createImg( dims, backendStr, (NativeType)relevantPixelStreamer.type(), cellDims );

		if ( img == null )
			throw new RuntimeException( "Failed to create an image of the requested image backend, sorry." );

		if ( img.size() == 0 )
			throw new UnsupportedOperationException( "Refusing to stream an empty image..." );

		//the core Img is prepared, lets extend it with metadata and fill with voxel values afterwards
		//create the ImgPlus from it -- there is fortunately no deep coping
		ImgPlus< ? > imgP = new ImgPlus<>( img );

		//process the metadata
		logger.info( "processing the incoming metadata..." );
		receiveAndUnpackPlusData( metadata, imgP );

		logger.info( "processing the incoming image data..." );
		relevantPixelStreamer.read( is, (RandomAccessibleInterval)img );

		logger.info( "processing finished." );
		return imgP;
	}

	// -------- support for the transmission of the image metadata --------
	protected byte[] packAndSendPlusData( final ImgPlus< ? > img )
	{
		//process (only some) metadata from the img
		final byte[] md_name = img.getName() != null ? img.getName().getBytes() : null;
		final int md_name_length = md_name != null ? md_name.length : 0;

		final byte[] md_source = img.getSource() != null ? img.getSource().getBytes() : null;
		final int md_source_length = md_source != null ? md_source.length : 0;

		final int md_validBits = img.getValidBits();

		//final output buffer with "encoded" metadata:
		//NB: might not work out if both 'name' and 'source' parts are veeery long
		final byte[] metadata = new byte[ 4 + md_name_length + 4 + md_source_length + 4 ];
		int offset = 0;

		//part 'name'
		intToByteArray( md_name_length, metadata, offset );
		offset += 4;
		for ( int i = 0; i < md_name_length; ++i )
			metadata[ offset++ ] = md_name[ i ];

		//part 'source'
		intToByteArray( md_source_length, metadata, offset );
		offset += 4;
		for ( int i = 0; i < md_source_length; ++i )
			metadata[ offset++ ] = md_source[ i ];

		//part 'validBits'
		intToByteArray( md_validBits, metadata, offset );

		return metadata;
	}

	protected void receiveAndUnpackPlusData( final byte[] metadata, final ImgPlus< ? > img )
	{
		//set the metadata into img
		int offset = 0;

		//part 'name'
		final byte[] md_name = new byte[ byteArrayToInt( metadata, offset ) ];
		offset += 4;
		for ( int i = 0; i < md_name.length; ++i )
			md_name[ i ] = metadata[ offset++ ];

		//part 'source'
		final byte[] md_source = new byte[ byteArrayToInt( metadata, offset ) ];
		offset += 4;
		for ( int i = 0; i < md_source.length; ++i )
			md_source[ i ] = metadata[ offset++ ];

		//part 'validBits'
		final int md_validBits = byteArrayToInt( metadata, offset );

		img.setName( new String( md_name ) );
		img.setSource( new String( md_source ) );
		img.setValidBits( md_validBits );
	}

	private void intToByteArray( final int i, final byte[] ba, final int baOffset )
	{
		//MSB format
		ba[ baOffset + 0 ] = ( byte ) ( ( i >> 24 ) & 0xFF );
		ba[ baOffset + 1 ] = ( byte ) ( ( i >> 16 ) & 0xFF );
		ba[ baOffset + 2 ] = ( byte ) ( ( i >> 8 ) & 0xFF );
		ba[ baOffset + 3 ] = ( byte ) ( i & 0xFF );
	}

	private int byteArrayToInt( final byte[] ba, final int baOffset )
	{
		//MSB format
		int i = ( ( int ) ba[ baOffset + 0 ] << 24 )
				| ( ( int ) ba[ baOffset + 1 ] << 16 )
				| ( ( int ) ba[ baOffset + 2 ] << 8 )
				| ( int ) ba[ baOffset + 3 ];

		return i;
	}

	// -------- the types war --------
	/*
	 * Keeps unwrapping the input image \e img
	 * until it gets to the underlying pure imglib2.Img.
	 */
	@SuppressWarnings( "unchecked" )
	private static < Q >
	Img< Q > getUnderlyingImg( final Img< Q > img )
	{
		if ( img instanceof Dataset )
			return ( Img< Q > ) getUnderlyingImg( ( ( Dataset ) img ).getImgPlus() );
		else if ( img instanceof WrappedImg )
			return getUnderlyingImg( ( ( WrappedImg< Q > ) img ).getImg() );
		else
			return img;
	}

	private static < T extends NativeType< T > >
	Img< T > createImg( int[] dims, String backendStr, T type, int... cellDims )
	{
		if ( backendStr.startsWith( "ArrayImg" ) )
			return new ArrayImgFactory<>( type ).create( dims );
		if ( backendStr.startsWith( "PlanarImg" ) )
			return new PlanarImgFactory<>( type ).create( dims );
		if ( backendStr.startsWith( "CellImg" ) )
			return new CellImgFactory<>( type, cellDims ).create( dims );
		throw new UnsupportedOperationException( "Unsupported image backend, sorry." );
	}
}
