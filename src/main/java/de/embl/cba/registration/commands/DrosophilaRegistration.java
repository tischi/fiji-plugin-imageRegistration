package de.embl.cba.registration.commands;


import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import de.embl.cba.registration.geometry.EllipsoidParameterComputer;
import de.embl.cba.registration.geometry.EllipsoidParameters;
import de.embl.cba.registration.plotting.Plots;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.*;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.io.IOException;
import java.util.ArrayList;

import static de.embl.cba.registration.util.Constants.X;
import static de.embl.cba.registration.util.Constants.XYZ;
import static de.embl.cba.registration.util.Constants.Z;

public class DrosophilaRegistration
{


	// https://github.com/ijpb/MorphoLibJ/blob/master/src/main/java/inra/ijpb/measure/GeometricMeasures3D.java#L41
	// Jama

//
//		DatasetService datasetService = imagej.dataset();
//		UIService uiService = imagej.ui();


	public  < T extends RealType< T > > void sandbox() throws IOException
	{

		String path = "/Users/tischer/Documents/fiji-plugin-imageRegistration/src/test/resources/crocker-7-2-scale0.25-rot_z_45.zip";
		double threshold = 10.0D;

		ImageJ imagej = new ImageJ();
		imagej.ui().showUI();
		DatasetIOService datasetIOService = imagej.scifio().datasetIO();

		Dataset dataset = datasetIOService.open( path );

		double[] scalingsInMicrometer = getScalingsInMicrometer( dataset );

		RandomAccessibleInterval< T > rai = getDapiChannel( dataset );

		correctIntensityAlongZ( rai, scalingsInMicrometer[ Z ] );

		final RandomAccessibleInterval< BitType > binaryImage = createBinaryImage( rai, threshold );

		show( binaryImage );

		final EllipsoidParameters compute = EllipsoidParameterComputer.compute( binaryImage );


		// / registerLongAxisOrientation( rai );

		//plot( averages );


		/*
		final Img< UnsignedByteType > copy = new PlanarImgFactory<>(new UnsignedByteType()).create( Intervals.dimensionsAsLongArray( channel ) );
		LoopBuilder.setImages( channel, copy ).forEachPixel( (in, out) -> {
			out.setReal(in.getRealDouble());
		} );
		*/

		// uiService.show( channel );

		//((LinearAxis)copy.axis( 2 )).setScale( 1.6D );
		//uiService.show( copy );


		//((LinearAxis)singleChannelImg.axis( 2 )).setScale( 1.6D );
		//final BdvStackSource bdvStackSource = show3DImgPlusInBdv( singleChannelImg );


	}

	public < T extends RealType< T > > RandomAccessibleInterval< BitType > createBinaryImage(
			RandomAccessibleInterval< T > input, double doubleThreshold )
	{
		final ArrayImg< BitType, LongArray > binaryImage = ArrayImgs.bits( Intervals.dimensionsAsLongArray( input ) );

		T threshold = input.randomAccess().get().copy();
		threshold.setReal( doubleThreshold );

		final BitType one = new BitType( true );
		final BitType zero = new BitType( false );

		LoopBuilder.setImages( input, binaryImage ).forEachPixel( ( i, b ) ->
				{
					b.set( i.compareTo( threshold ) > 0 ?  one : zero );
				}
		);

		return binaryImage;

	}

	public < T extends RealType< T > > void registerLongAxisOrientation( RandomAccessibleInterval< T > rai )
	{
		ArrayList< Double > averages = new ArrayList<>(  );

		for ( long x = 0; x < rai.dimension( X ); ++x )
		{
			final IntervalView< T > slice = Views.hyperSlice( rai, X, x );
			averages.add( average( slice ) );
		}

		Plots.plot( averages );


	}

	public < T extends RealType< T > > RandomAccessibleInterval< T > getDapiChannel( Dataset dataset )
	{
		return (RandomAccessibleInterval< T >) dataset.getImgPlus();
	}

	public double[] getScalingsInMicrometer( Dataset dataset )
	{
		double[] scalings = new double[ 3 ];

		for ( int d : XYZ )
		{
			scalings[ d ] = ( ( LinearAxis ) dataset.getImgPlus().axis( d ) ).scale();
		}

		return scalings;
	}

	public < T extends RealType< T > > void correctIntensityAlongZ( RandomAccessibleInterval< T > rai, double zScalingInMicrometer )
	{
		for ( long z = 0; z < rai.dimension( Z ); ++z )
		{
			final RandomAccessibleInterval< T > slice = Views.hyperSlice( rai, Z, z );

			final double intensityCorrectionFactor = getIntensityCorrectionFactorAlongZ( z, zScalingInMicrometer );

			Views.iterable( slice ).forEach( t -> t.mul( intensityCorrectionFactor )  );
		}
	}


	public static < T extends RealType< T > > double average( final IterableInterval< T > iterable )
	{
		final Cursor< T > cursor = iterable.cursor();

		double average = 0;

		int n = 0;
		while ( cursor.hasNext() )
		{
			average += cursor.next().getRealDouble();
			++n;
		}

		average /= n;

		return average;
	}

	private static void show( RandomAccessibleInterval rai )
	{
		final Bdv bdv = BdvFunctions.show( rai, "" );

		resetViewTransform( bdv );

	}

	private static void resetViewTransform( Bdv bdv )
	{
		bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( new AffineTransform3D() );
	}


	private static BdvStackSource show3DImgPlusInBdv( ImgPlus imgPlus )
	{

		BdvStackSource bdvStackSource = BdvFunctions.show(
				imgPlus,
				imgPlus.getName(),
				Bdv.options().sourceTransform(
						((LinearAxis)imgPlus.axis( 0 )).scale(),
						((LinearAxis)imgPlus.axis( 1 )).scale(),
						((LinearAxis)imgPlus.axis( 2 )).scale() )
		);

		return bdvStackSource;
	}

	public double getIntensityCorrectionFactorAlongZ( long z, double zScalingInMicrometer )
	{

		/*
		f( 10 ) = 93; f( 83 ) = 30;
		f( z1 ) = v1; f( z2 ) = v2;
		f( z ) = A * exp( -z / d );

		=> d = ( z2 - z1 ) / ln( v1 / v2 );

		> log ( 93 / 30 )
		[1] 1.1314

		=> d = 	73 / 1.1314 = 64.52172;

		=> correction = 1 / exp( -z / d );

		at z = 10 we want value = 93 => z0  = 10;

		=> correction = 1 / exp( - ( z - 10 ) / d );
		 */

		double z0 = 10.0D;
		double decayLengthInMicrometer = 64.0D;
		double generalIntensityScaling = 0.3; // TODO: what to use here?

		double scaledZ = z * zScalingInMicrometer;

		double correctionFactor = generalIntensityScaling / Math.exp( - ( scaledZ - z0 ) / decayLengthInMicrometer );

		return correctionFactor;
	}


	public static void main( String... args ) throws IOException
	{
		DrosophilaRegistration registration = new DrosophilaRegistration();
		registration.sandbox();
	}



}
