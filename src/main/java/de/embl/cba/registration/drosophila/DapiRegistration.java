package de.embl.cba.registration.drosophila;


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
import java.util.*;

import static de.embl.cba.registration.bdv.BdvImageViewer.show;
import static de.embl.cba.registration.geometry.EllipsoidParameters.PHI;
import static de.embl.cba.registration.geometry.EllipsoidParameters.PSI;
import static de.embl.cba.registration.geometry.EllipsoidParameters.THETA;
import static de.embl.cba.registration.utils.Constants.*;
import static de.embl.cba.registration.utils.Transforms.createArrayCopy;
import static java.lang.Math.*;

public class DapiRegistration
{
	final DapiRegistrationSettings settings;

	public DapiRegistration( DapiRegistrationSettings settings )
	{
		this.settings = settings;
	}

	public < T extends RealType< T > & NativeType< T > >
	AffineTransform3D compute( RandomAccessibleInterval< T > dapiRaw, double[] calibration  )
	{

		AffineTransform3D registration = new AffineTransform3D();

		if ( settings.showIntermediateResults ) show( dapiRaw, "raw input data", null, calibration, false );

		correctCalibrationForRefractiveIndexMismatch( calibration, settings.refractiveIndexCorrectionAxialScalingFactor );

		if ( settings.showIntermediateResults ) show( dapiRaw, "calibration corrected view on raw input data", null, calibration, false );

		AffineTransform3D scalingToRegistrationResolution = getScalingTransform( settings.resolutionDuringRegistrationInMicrometer, calibration );

		final RandomAccessibleInterval< T > binnedView = Transforms.view( dapiRaw, scalingToRegistrationResolution );

		final RandomAccessibleInterval< T > binned = createArrayCopy( binnedView );

		calibration = getIsotropicCalibration( settings.resolutionDuringRegistrationInMicrometer );

		if ( settings.showIntermediateResults ) show( binned, "binned copy ( " + settings.resolutionDuringRegistrationInMicrometer + " um )", null, calibration, false );

		correctIntensityAlongZ( binned, calibration[ Z ] );

		final RandomAccessibleInterval< BitType > binaryImage = createBinaryImage( binned, settings.threshold );

		if ( settings.showIntermediateResults ) show( binaryImage, "binary", null, calibration, false );

		final EllipsoidParameters ellipsoidParameters = EllipsoidParameterComputer.compute( binaryImage );

		registration.preConcatenate( createEllipsoidAlignmentTransform( binned, ellipsoidParameters ) );

		if ( settings.showIntermediateResults ) show( Transforms.view( binned, registration ), "ellipsoid aligned", null, calibration, false );

		registration.preConcatenate( createOrientationTransformation(
				Transforms.view( binned, registration ), X,
				settings.derivativeDeltaInMicrometer,
				settings.resolutionDuringRegistrationInMicrometer,
				settings.showIntermediateResults ) );

		if ( settings.showIntermediateResults ) show( Transforms.view( binned, registration ), "oriented", null, calibration, false );

		final RandomAccessibleInterval< T > longAxisProjection = createAverageProjection(
				Transforms.view( binned, registration ), X,
				settings.projectionRangeMinDistanceToCenterInMicrometer,
				settings.projectionRangeMaxDistanceToCenterInMicrometer,
				settings.resolutionDuringRegistrationInMicrometer);

		if ( settings.showIntermediateResults ) show( longAxisProjection, "perpendicular projection", null, new double[]{ calibration[ Y ], calibration[ Z ] }, false );

		final RandomAccessibleInterval< T > blurred = createBlurredRai(
				longAxisProjection,
				settings.sigmaForBlurringAverageProjectionInMicrometer,
				settings.resolutionDuringRegistrationInMicrometer );

		final Point maximum = Algorithms.findMaximum( blurred, new double[]{ calibration[ Y ], calibration[ Z ] });
		final List< RealPoint > realPoints = asRealPointList( maximum );
		realPoints.add( new RealPoint( new double[]{ 0, 0 } ) );

		if ( settings.showIntermediateResults ) show( blurred, "perpendicular projection - blurred ", realPoints, new double[]{ calibration[ Y ], calibration[ Z ] }, false );

		registration.preConcatenate( createRollTransform( Transforms.view( binned, registration ), maximum ) );

		if ( settings.showIntermediateResults ) show( Transforms.view( binned, registration ), "registered binned dapi", null, calibration, false );

		final AffineTransform3D scalingToFinalResolution = getScalingTransform( settings.finalResolutionInMicrometer / settings.resolutionDuringRegistrationInMicrometer, new double[]{ 1, 1, 1 } );

		registration = scalingToRegistrationResolution.preConcatenate( registration ).preConcatenate( scalingToFinalResolution );

		return registration;

	}

	private static AffineTransform3D getScalingTransform( double binning, double[] calibration )
	{
		double[] downScaling = new double[3];

		for ( int d : XYZ )
		{
			downScaling[ d ] = calibration[ d ] / binning;
		}

		final AffineTransform3D scalingTransform = createScalingTransform( downScaling );

		return scalingTransform;
	}

	private static AffineTransform3D createScalingTransform( double[] calibration )
	{
		AffineTransform3D scaling = new AffineTransform3D();

		for ( int d : XYZ )
		{
			scaling.set( calibration[ d ], d, d );
		}

		return scaling;
	}

	private static double[] getIsotropicCalibration( double value )
	{
		double[] calibration = new double[ 3 ];
		for ( int d : XYZ )
		{
			calibration[ d ] = value;
		}
		return calibration;
	}

	private static < T extends RealType< T > & NativeType< T > >
	AffineTransform3D createRollTransform( RandomAccessibleInterval< T > rai, Point maximum )
	{
		double angleToZAxisInDegrees = getAngleToZAxis( maximum );
		AffineTransform3D rollTransform = new AffineTransform3D();
		rollTransform.rotate( X, toRadians( angleToZAxisInDegrees ) );

		return rollTransform;
	}

	private static double getAngleToZAxis( Point maximum )
	{
		final double angleToYAxis;

		if ( maximum.getIntPosition( Y ) == 0 )
		{
			angleToYAxis = 90;
		}
		else
		{
			angleToYAxis = toDegrees( atan( maximum.getDoublePosition( X ) / maximum.getDoublePosition( Y ) ) );
		}

		return angleToYAxis;
	}

	private static List< RealPoint > asRealPointList( Point maximum )
	{
		List< RealPoint > realPoints = new ArrayList<>();
		final double[] doubles = new double[ maximum.numDimensions() ];
		maximum.localize( doubles );
		realPoints.add( new RealPoint( doubles) );

		return realPoints;
	}


	private static < T extends RealType< T > & NativeType< T > >
	List< RealPoint > computeMaximumLocation( RandomAccessibleInterval< T > blurred, int sigmaForBlurringAverageProjection )
	{
		Shape shape = new HyperSphereShape( sigmaForBlurringAverageProjection );

		List< RealPoint > points = Algorithms.findMaxima( blurred, shape );

		return points;
	}

	private static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createBlurredRai( RandomAccessibleInterval< T > rai, double sigma, double scaling )
	{
		ImgFactory< T > imgFactory = new ArrayImgFactory( rai.randomAccess().get()  );

		RandomAccessibleInterval< T > blurred = imgFactory.create( Intervals.dimensionsAsLongArray( rai ) );

		blurred = Views.translate( blurred, Intervals.minAsLongArray( rai ) );

		Gauss3.gauss( sigma / scaling, Views.extendBorder( rai ), blurred ) ;

		return blurred;
	}

	private static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createAverageProjection(
			RandomAccessibleInterval< T > rai, int d, double min, double max, double scaling )
	{
		Projection< T > projection = new Projection< T >(  rai, d,  new FinalInterval( new long[]{ (long) ( min / scaling) },  new long[]{ (long) ( max / scaling ) } ) );
		return projection.average();
	}

	private static < T extends RealType< T > & NativeType< T > >
	AffineTransform3D createEllipsoidAlignmentTransform( RandomAccessibleInterval< T > rai, EllipsoidParameters ellipsoidParameters )
	{

		AffineTransform3D translation = new AffineTransform3D();
		translation.translate( ellipsoidParameters.center  );
		translation = translation.inverse();

		AffineTransform3D rotation = new AffineTransform3D();
		rotation.rotate( Z, - toRadians( ellipsoidParameters.anglesInDegrees[ PHI ] ) );
		rotation.rotate( Y, - toRadians( ellipsoidParameters.anglesInDegrees[ THETA ] ) );
		rotation.rotate( X, - toRadians( ellipsoidParameters.anglesInDegrees[ PSI ] ) );

		AffineTransform3D combinedTransform = translation.preConcatenate( rotation );

		return combinedTransform;

	}

	private static < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval< BitType > createBinaryImage(
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

	private static < T extends RealType< T > & NativeType< T > >
	AffineTransform3D createOrientationTransformation(
			RandomAccessibleInterval< T > rai, int longAxisDimension, double derivativeDelta, double scaling,
			boolean showPlots )
	{
		double[ ] averages = new double[ (int) rai.dimension( longAxisDimension ) ];
		double[ ] coordinates = new double[ (int) rai.dimension( longAxisDimension ) ];

		computeAverageIntensitiesInPlanesPerpendicularToLongAxis( rai, longAxisDimension, averages, coordinates );

		double[] absoluteDerivatives = computeAbsoluteDerivatives( averages, (int) (derivativeDelta / scaling ));

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

			return affineTransform3D;
		}
		else
		{
			return new AffineTransform3D();
		}

	}

	private static < T extends RealType< T > & NativeType< T > > void computeAverageIntensitiesInPlanesPerpendicularToLongAxis(
			RandomAccessibleInterval< T > rai, int longAxisDimension,double[] averages, double[] coordinates )
	{
		for ( long coordinate = rai.min( longAxisDimension ), i = 0; coordinate <= rai.max( longAxisDimension ); ++coordinate, ++i )
		{
			final IntervalView< T > slice = Views.hyperSlice( rai, longAxisDimension, coordinate );
			averages[ (int) i ] = computeAverage( slice );
			coordinates[ (int) i ] = coordinate;
		}
	}



	private static double[] computeAbsoluteDerivatives( double[] values, int di )
	{
		double[ ] derivatives = new double[ values.length ];

		for ( int i = di / 2 + 1; i < values.length - di / 2 - 1; ++i )
		{
			derivatives[ i ] = abs( values[ i + di / 2 ] - values[ i - di / 2 ] );
		}

		return derivatives;
	}


	private static double computeMaxLoc( double[] coordinates, double[] values )
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


	private static double[] getCalibration( Dataset dataset )
	{
		double[] scalings = new double[ 3 ];

		for ( int d : XYZ )
		{
			scalings[ d ] = ( ( LinearAxis ) dataset.getImgPlus().axis( d ) ).scale();

		}

		return scalings;
	}

	private static void correctCalibrationForSubSampling( double[] calibration, int subSampling )
	{
		for ( int d : XYZ )
		{
			calibration[ d ] *= subSampling;
		}
	}

	private static void correctCalibrationForRefractiveIndexMismatch( double[] calibration, double correctionFactor )
	{
		calibration[ Z ] *= correctionFactor;
	}



	private static < T extends RealType< T > & NativeType< T > >
	void correctIntensityAlongZ( RandomAccessibleInterval< T > rai, double zScalingInMicrometer )
	{

		for ( long z = rai.min( Z ); z < rai.max( Z ); ++z )
		{
			RandomAccessibleInterval< T > slice = Views.hyperSlice( rai, Z, z );

			double intensityCorrectionFactor = getIntensityCorrectionFactorAlongZ( z, zScalingInMicrometer );

//			System.out.println( z + "," + intensityCorrectionFactor );

			Views.iterable( slice ).forEach( t -> t.mul( intensityCorrectionFactor )  );

		}

	}


	private static < T extends RealType< T > & NativeType< T > >
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


	private static double getIntensityCorrectionFactorAlongZ( long z, double zScalingInMicrometer )
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

		double generalIntensityScaling = 0.3; // TODO: what to use here?

		double offsetInMicrometer = 10.0D; // TODO: might differ between samples?

		double intensityDecayLengthInMicrometer = 100.0D;

		double zInMicrometer = z * zScalingInMicrometer - offsetInMicrometer;

		double correctionFactor = generalIntensityScaling / exp( - zInMicrometer / intensityDecayLengthInMicrometer );

		return correctionFactor;
	}


	public static < T extends RealType< T > & NativeType< T > >
	void main( String... args ) throws IOException
	{
		//		String path = "/Users/tischer/Documents/fiji-plugin-imageRegistration/src/test/resources/crocker-7-2-scale0.25-rot_z_60.zip";
		String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 7-2 rotated.tif";
//		String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 6-3 Dapi iso1um.tif";
//		String path = "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 4-3 Dapi iso1um.tif";

		ImageJ imagej = new ImageJ();
		imagej.ui().showUI();

		DatasetIOService datasetIOService = imagej.scifio().datasetIO();

		Dataset dataset = datasetIOService.open( path );

		double[] calibration = getCalibration( dataset );

		final RandomAccessibleInterval< T > dapiRaw = (RandomAccessibleInterval< T >) dataset.getImgPlus().copy();


		DapiRegistrationSettings settings = new DapiRegistrationSettings();

		settings.showIntermediateResults = true;

		DapiRegistration registration = new DapiRegistration( settings );

		final AffineTransform3D registrationTransform = registration.compute( dapiRaw, calibration );

		show( Transforms.view( dapiRaw, registrationTransform ), "registered input ( " + settings.finalResolutionInMicrometer + " um )", asRealPointList( new Point( 0,0,0 ) ), new double[]{ 1, 1, 1 }, false );

	}



}
