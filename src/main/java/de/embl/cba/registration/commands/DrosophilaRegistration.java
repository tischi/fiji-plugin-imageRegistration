package de.embl.cba.registration.commands;


import de.embl.cba.registration.algorithm.Algorithms;
import de.embl.cba.registration.geometry.EllipsoidParameterComputer;
import de.embl.cba.registration.geometry.EllipsoidParameters;
import de.embl.cba.registration.plotting.Plots;
import de.embl.cba.registration.projection.Projection;
import de.embl.cba.registration.utils.Transforms;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.*;
import net.imglib2.*;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static de.embl.cba.registration.bdv.BdvImageViewer.show;
import static de.embl.cba.registration.geometry.EllipsoidParameters.PHI;
import static de.embl.cba.registration.geometry.EllipsoidParameters.PSI;
import static de.embl.cba.registration.geometry.EllipsoidParameters.THETA;
import static de.embl.cba.registration.utils.Constants.*;
import static java.lang.Math.*;

public class DrosophilaRegistration
{


	// https://github.com/ijpb/MorphoLibJ/blob/master/src/main/java/inra/ijpb/measure/GeometricMeasures3D.java#L41
	// Jama

//
//		DatasetService datasetService = imagej.dataset();
//		UIService uiService = imagej.ui();


	public  < T extends RealType< T >  & NativeType < T > > void sandbox() throws IOException
	{

//		String path = "/Users/tischer/Documents/fiji-plugin-imageRegistration/src/test/resources/crocker-7-2-scale0.25-rot_z_60.zip";
		String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 7-2 rotated.tif";
//		String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 6-3 Dapi iso1um.tif";
//		String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 4-3 Dapi iso1um.tif";

		double threshold = 10.0D;
		int subSampling = 4;
		boolean showPlots = true;

		double refractiveIndexMismatchCorrectionFactor = 1.6;

		int derivativeDelta = 20 / subSampling;
		long projectionRangeMin = +20 / subSampling ;
		long projectionRangeMax = +80 / subSampling;
		int sigmaForBlurringAverageProjection = 10 / subSampling ;

		ImageJ imagej = new ImageJ();
		imagej.ui().showUI();
		DatasetIOService datasetIOService = imagej.scifio().datasetIO();

		Dataset dataset = datasetIOService.open( path );

		double[] calibration = getCalibration( dataset );

		show( (RandomAccessibleInterval<T> ) dataset.getImgPlus(), "input data", calibration );

		RandomAccessibleInterval< T > rai = getDapiChannel( dataset );
		rai = Views.subsample( rai, subSampling );

		correctCalibrationForSubSampling( calibration, subSampling );
		correctCalibrationForRefractiveIndexMismatchAlongZAxis( calibration, refractiveIndexMismatchCorrectionFactor );

		correctIntensityAlongZ( rai, calibration[ Z ] );

		show( rai, "intensity and calibration corrected", calibration );

		final RandomAccessibleInterval< BitType >
				binaryImage = createBinaryImage( rai, threshold );

		show( binaryImage, "binary", calibration );

		final EllipsoidParameters
				ellipsoidParameters = EllipsoidParameterComputer.compute( binaryImage );

		final RandomAccessibleInterval< T >
				longAxisAligned = alignToEllipsoid( rai, ellipsoidParameters, calibration );

		show( longAxisAligned, "longAxisAligned", calibration );

		final RandomAccessibleInterval< T >
				longAxisOriented = registerLongAxisOrientation( longAxisAligned, X, derivativeDelta, showPlots );

		show( longAxisOriented, "longAxisOriented", calibration );

		final RandomAccessibleInterval< T >
				longAxisProjection = computeAverageProjection( longAxisOriented, X, projectionRangeMin, projectionRangeMax  );

		show( longAxisProjection, "longAxisProjection", new double[]{ calibration[ Y ], calibration[ Z ] });

		final RandomAccessibleInterval< T > blurred
				= blur( longAxisProjection, sigmaForBlurringAverageProjection );
		final Point maximum
				= Algorithms.findMaximum( blurred );

		// show
		final List< RealPoint > realPoints = asRealPointList( maximum );
		realPoints.add( new RealPoint( new double[]{ 0, 0 } ) );
		show( blurred, realPoints, false );

		final RandomAccessibleInterval< T >
				registered = registerRotationAroundLongAxis( longAxisOriented, maximum, calibration );

		show( registered, "registered", calibration );

		//((LinearAxis)singleChannelImg.axis( 2 )).setScale( 1.6D );
		//final BdvStackSource bdvStackSource = show3DImgPlusInBdv( singleChannelImg );


	}

	public < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > registerRotationAroundLongAxis( RandomAccessibleInterval< T > rai, Point maximum, double[] calibration )
	{
		double angleToZAxisInDegrees = getAngleToYAxis( maximum );
		AffineTransform3D rotationAroundLongAxis = new AffineTransform3D();
		rotationAroundLongAxis.rotate( X, toRadians( angleToZAxisInDegrees ) );

		rotationAroundLongAxis.apply( calibration, calibration );

		final RandomAccessibleInterval< T > transformed =
				Transforms.createTransformedRAIWithAdjustedBounds( rai, rotationAroundLongAxis );

		return Views.interval( transformed, rai );
	}

	public double getAngleToYAxis( Point maximum )
	{
		final double angleToYAxis;

		if ( maximum.getIntPosition( Y ) == 0 )
		{
			angleToYAxis = 90;
		}
		else
		{
			angleToYAxis = atan( maximum.getDoublePosition( X ) / maximum.getDoublePosition( Y ) );
		}

		return angleToYAxis;
	}

	public List< RealPoint > asRealPointList( Point maximum )
	{
		List< RealPoint > realPoints = new ArrayList<>();
		final double[] doubles = new double[ maximum.numDimensions() ];
		maximum.localize( doubles );
		realPoints.add( new RealPoint( doubles) );

		return realPoints;
	}


	public  < T extends RealType< T > & NativeType< T > >
	List< RealPoint > computeMaximumLocation( RandomAccessibleInterval< T > blurred, int sigmaForBlurringAverageProjection )
	{
		Shape shape = new HyperSphereShape( sigmaForBlurringAverageProjection );

		List< RealPoint > points = Algorithms.findMaxima( blurred, shape );

		return points;
	}

	public < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > blur( RandomAccessibleInterval< T > rai, double sigma )
	{
		ImgFactory< T > imgFactory = new ArrayImgFactory( rai.randomAccess().get()  );

		RandomAccessibleInterval< T > blurred = imgFactory.create( Intervals.dimensionsAsLongArray( rai ) );

		blurred = Views.translate( blurred, Intervals.minAsLongArray( rai ) );

		Gauss3.gauss( sigma, Views.extendBorder( rai ), blurred ) ;

		return blurred;
	}

	public < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > computeAverageProjection( RandomAccessibleInterval< T > rai, int d, long min, long max )
	{
		Projection< T > projection = new Projection< T >(  rai, d,  new FinalInterval( new long[]{ min },  new long[]{ max } ) );
		return projection.average();
	}

	public < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > alignToEllipsoid( RandomAccessibleInterval< T > rai, EllipsoidParameters ellipsoidParameters, double[] calibration )
	{

		AffineTransform3D translation = new AffineTransform3D();
		translation.translate( ellipsoidParameters.center  );
		translation = translation.inverse();

		AffineTransform3D rotation = new AffineTransform3D();
		rotation.rotate( Z, - toRadians( ellipsoidParameters.anglesInDegrees[ PHI ] ) );
		rotation.rotate( Y, - toRadians( ellipsoidParameters.anglesInDegrees[ PSI ] ) );
		rotation.rotate( X, - toRadians( ellipsoidParameters.anglesInDegrees[ THETA ] ) );

		AffineTransform3D combinedTransform = translation.preConcatenate( rotation );

		rotation.apply( calibration, calibration );

		final RandomAccessibleInterval transformedRAI = Transforms.createTransformedRAIWithAdjustedBounds( rai, combinedTransform );

		return transformedRAI;

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

			final RandomAccessible transformedRA = Transforms.createTransformedRA( rai, affineTransform3D );

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
		return (RandomAccessibleInterval< T >) dataset.getImgPlus().copy();
	}

	public static double[] getCalibration( Dataset dataset )
	{
		double[] scalings = new double[ 3 ];

		for ( int d : XYZ )
		{
			scalings[ d ] = ( ( LinearAxis ) dataset.getImgPlus().axis( d ) ).scale();

		}

		return scalings;
	}

	public static void correctCalibrationForSubSampling( double[] calibration, int subSampling )
	{
		for ( int d : XYZ )
		{
			calibration[ d ] *= subSampling;
		}
	}

	public static void correctCalibrationForRefractiveIndexMismatchAlongZAxis( double[] calibration, double correctionFactor )
	{
		calibration[ Z ] *= correctionFactor;
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


	public static < T extends RealType< T > >
	double computeAverage( final RandomAccessibleInterval< T > rai )
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
		double decayLengthInMicrometer = 64.0D * 1.6D; // refractiveIndexMismatch
		double generalIntensityScaling = 0.3; // TODO: what to use here?

		double scaledZ = z * zScalingInMicrometer;

		double correctionFactor = generalIntensityScaling / exp( - ( scaledZ - z0 ) / decayLengthInMicrometer );

		return correctionFactor;
	}


	public static void main( String... args ) throws IOException
	{
		DrosophilaRegistration registration = new DrosophilaRegistration();
		registration.sandbox();
	}



}
