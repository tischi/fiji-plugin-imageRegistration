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
import net.imglib2.*;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.io.IOException;

import static de.embl.cba.registration.geometry.EllipsoidParameters.PHI;
import static de.embl.cba.registration.geometry.EllipsoidParameters.PSI;
import static de.embl.cba.registration.geometry.EllipsoidParameters.THETA;
import static de.embl.cba.registration.util.Constants.*;
import static java.lang.Math.abs;
import static java.lang.Math.toRadians;

public class DrosophilaRegistration
{


	// https://github.com/ijpb/MorphoLibJ/blob/master/src/main/java/inra/ijpb/measure/GeometricMeasures3D.java#L41
	// Jama

//
//		DatasetService datasetService = imagej.dataset();
//		UIService uiService = imagej.ui();


	public  < T extends RealType< T > > void sandbox() throws IOException
	{

//		String path = "/Users/tischer/Documents/fiji-plugin-imageRegistration/src/test/resources/crocker-7-2-scale0.25-rot_z_60.zip";
//		String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 7-1 Dapi iso1um.tif";
		String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 6-3 Dapi iso1um.tif";
//		String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 4-3 Dapi iso1um.tif";

		double threshold = 10.0D;

		ImageJ imagej = new ImageJ();
		imagej.ui().showUI();
		DatasetIOService datasetIOService = imagej.scifio().datasetIO();

		Dataset dataset = datasetIOService.open( path );

		double[] scalingsInMicrometer = getScalingsInMicrometer( dataset );

		RandomAccessibleInterval< T > rai = getDapiChannel( dataset );

		show( rai, false );

		correctIntensityAlongZ( rai, scalingsInMicrometer[ Z ] );

		final RandomAccessibleInterval< BitType > binaryImage = createBinaryImage( rai, threshold );

		show( binaryImage, false );

		final EllipsoidParameters ellipsoidParameters = EllipsoidParameterComputer.compute( binaryImage );

		final RandomAccessibleInterval< T > longAxisAlongX = align( rai, ellipsoidParameters );

		show( longAxisAlongX, false );

		boolean showPlots = true;
		int derivativeDelta = 20;

		final RandomAccessibleInterval< T > longAxisInXOriented = registerLongAxisOrientation( longAxisAlongX, X, derivativeDelta, showPlots );

		show( longAxisInXOriented, false );

		//((LinearAxis)singleChannelImg.axis( 2 )).setScale( 1.6D );
		//final BdvStackSource bdvStackSource = show3DImgPlusInBdv( singleChannelImg );


	}

	public < T extends RealType< T > > RandomAccessibleInterval< T > align(
			RandomAccessibleInterval< T > rai, EllipsoidParameters ellipsoidParameters )
	{

		AffineTransform3D translation = new AffineTransform3D();
		translation.translate( ellipsoidParameters.center  );
		translation = translation.inverse();

		AffineTransform3D rotation = new AffineTransform3D();
		rotation.rotate( Z, - toRadians( ellipsoidParameters.anglesInDegrees[ PHI ] ) );
		rotation.rotate( Y, - toRadians( ellipsoidParameters.anglesInDegrees[ PSI ] ) );
		rotation.rotate( X, - toRadians( ellipsoidParameters.anglesInDegrees[ THETA ] ) );

		AffineTransform3D combinedTransform = translation.preConcatenate( rotation );

		final RandomAccessible transformedRA = createTransformedRandomAccessible( rai, combinedTransform );
		final FinalInterval transformedBounds = computeTransformedBounds( rai, combinedTransform );
		final RandomAccessibleInterval transformedRAI = Views.interval( transformedRA, transformedBounds );

		return transformedRAI;

	}

	public < T extends RealType< T > > RandomAccessible createTransformedRandomAccessible( RandomAccessibleInterval< T > rai, AffineTransform3D combinedTransform )
	{
		RealRandomAccessible rra = Views.interpolate( Views.extendZero( rai ), new NLinearInterpolatorFactory() );
		rra = RealViews.transform( rra, combinedTransform );
		return Views.raster( rra );
	}

	public < T extends RealType< T > > FinalInterval computeTransformedBounds( RandomAccessibleInterval< T > rai, AffineTransform3D transform )
	{
		final FinalRealInterval realInterval = transform.estimateBounds( rai );

		double[] realMin = new double[ 3 ];
		double[] realMax = new double[ 3 ];
		realInterval.realMin( realMin );
		realInterval.realMax( realMax );

		long[] min = new long[ 3 ];
		long[] max = new long[ 3 ];

		for ( int d : XYZ )
		{
			min[ d ] = (long) realMin[ d ];
			max[ d ] = (long) realMax[ d ];
		}

		return new FinalInterval( min, max );
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

	public < T extends RealType< T > > RandomAccessibleInterval< T > registerLongAxisOrientation(
			RandomAccessibleInterval< T > rai, int longAxisDimension, int derivativeDelta,
			boolean showPlots )
	{
		double[ ] averages = new double[ (int) rai.dimension( longAxisDimension ) ];
		double[ ] coordinates = new double[ (int) rai.dimension( longAxisDimension ) ];

		computeAverageIntensitiesInPlanesPerpendicularToLongAxis( rai, longAxisDimension, averages, coordinates );

		double[] absoluteDerivatives = computeAbsoluteDerivatives( averages, derivativeDelta );

		double maxLoc = computeMaxLoc( coordinates, absoluteDerivatives );

		System.out.println( "maxLoc = " + maxLoc );

		if ( showPlots )
		{
			Plots.plot( coordinates, averages );
			Plots.plot( coordinates, absoluteDerivatives );
		}

		if ( maxLoc > 0 )
		{
			AffineTransform3D affineTransform3D = new AffineTransform3D();
			affineTransform3D.rotate( Z, toRadians( 180.0D ) );

			final RandomAccessible transformedRA = createTransformedRandomAccessible( rai, affineTransform3D );

			return Views.interval( transformedRA, rai );
		}
		else
		{
			return rai;
		}

	}

	public < T extends RealType< T > > void computeAverageIntensitiesInPlanesPerpendicularToLongAxis(
			RandomAccessibleInterval< T > rai, int longAxisDimension,double[] averages, double[] coordinates )
	{
		for ( long coordinate = rai.min( longAxisDimension ), i = 0; coordinate <= rai.max( longAxisDimension ); ++coordinate, ++i )
		{
			final IntervalView< T > slice = Views.hyperSlice( rai, longAxisDimension, coordinate );
			averages[ (int) i ] = computeAverage( slice );
			coordinates[ (int) i ] = coordinate;
		}
	}

	public double[] computeAbsoluteDerivatives( double[] values, int di )
	{
		double[ ] derivatives = new double[ values.length ];

		for ( int i = di / 2 + 1; i < values.length - di / 2 - 1; ++i )
		{
			derivatives[ i ] = abs( values[ i + di / 2 ] - values[ i - di / 2 ] );
		}

		return derivatives;
	}


	public double computeMaxLoc( double[] coordinates, double[] values )
	{
		double max = Double.MIN_VALUE;
		double maxLoc = coordinates[ 0 ];

		for ( int i = 0; i < values.length; ++i )
		{
			if ( values[ i ] > max )
			{
				max = values[ i ];
				maxLoc = coordinates[ i ];
			}
		}

		return maxLoc;
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


	public static < T extends RealType< T > > double computeAverage( final RandomAccessibleInterval< T > rai )
	{
		final Cursor< T > cursor = Views.iterable( rai ).cursor();

		double average = 0;

		while ( cursor.hasNext() )
		{
			average += cursor.next().getRealDouble();
		}

		average /= Views.iterable( rai ).size();

		return average;
	}

	private static void show( RandomAccessibleInterval rai, boolean resetViewTransform )
	{
		final Bdv bdv = BdvFunctions.show( rai, "" );

		if ( resetViewTransform ) resetViewTransform( bdv );

	}


	private static void resetViewTransform( Bdv bdv )
	{
		final AffineTransform3D affineTransform3D = new AffineTransform3D();

		affineTransform3D.scale( 2.5D );

		bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( affineTransform3D );
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
