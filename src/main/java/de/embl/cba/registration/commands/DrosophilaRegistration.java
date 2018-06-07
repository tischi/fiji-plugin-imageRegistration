package de.embl.cba.registration.commands;


import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import de.embl.cba.registration.plotting.Plots;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.*;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.binary.Thresholder;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.preibisch.mvrecon.fiji.plugin.Specify_Calibration;
import org.scijava.ui.UIService;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;

public class DrosophilaRegistration
{
	static final int X = 0, Y = 1, Z = 2;


	// https://github.com/ijpb/MorphoLibJ/blob/master/src/main/java/inra/ijpb/measure/GeometricMeasures3D.java#L41
	// Jama


	public  < T extends RealType< T > > void sandbox() throws IOException
	{
		ImageJ imagej = new ImageJ();
		imagej.ui().showUI();
		DatasetIOService datasetIOService = imagej.scifio().datasetIO();
		DatasetService datasetService = imagej.dataset();
		UIService uiService = imagej.ui();

		// Open
//		Dataset dataset = datasetIOService.open( "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 4-3 Dapi iso1um.tif" );
//		Dataset dataset = datasetIOService.open( "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 6-3 Dapi iso1um.tif" );
//		Dataset dataset = datasetIOService.open( "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 7-1 Dapi iso1um.tif" );
		Dataset dataset = datasetIOService.open( "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 4-1.tif" );

		// TODO: convert whole dataset to floating point?

		// Correct intensity (in DAPI) decrease along z-axis

		final int channelDim = 2;
		final int dapiChannel = 1;
		final ImgPlus< T > singleChannelImg = ImgPlusViews.hyperSlice( (ImgPlus< T >) dataset.getImgPlus(), channelDim, dapiChannel );

		for ( long z = 0; z < singleChannelImg.dimension( Z ); ++z )
		{
			final ImgPlus< T > slice = ImgPlusViews.hyperSlice( singleChannelImg, Z, z );
			double intensityCorrectionFactor = getFactor( z );
			// System.out.println( "" + z + ": " + factor );
			slice.forEach( t -> t.mul( intensityCorrectionFactor ) );
		}


		ArrayList< Double > averages = new ArrayList<>(  );
		for ( long x = 0; x < singleChannelImg.dimension( X ); ++x )
		{
			final ImgPlus< T > slice = ImgPlusViews.hyperSlice( singleChannelImg, X, x );
			averages.add( average( (IterableInterval) slice ) );
			//System.out.println( "average(" + x + ") = " + average );
			//slice.forEach( t -> t.mul( factor ) );
		}

		T threshold = singleChannelImg.firstElement().copy();
		threshold.setReal( 10.0 );
		final Img< BitType > binaryImg = Thresholder.threshold( singleChannelImg, threshold, true, 4 );


		//jama: singluar value decomposition


		Plots.plot( averages );
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


		((LinearAxis)singleChannelImg.axis( 2 )).setScale( 1.6D );
		final BdvStackSource bdvStackSource = show3DImgPlusInBdv( singleChannelImg );


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

	public double getFactor( long z )
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
		double decayLengthInPixels = 64.0D;
		double generalIntensityScaling = 0.3; // TODO: what to use here?
		double correction = generalIntensityScaling / Math.exp( - ( z - z0 ) / decayLengthInPixels );
		return correction;
	}


	public static void main( String... args ) throws IOException
	{
		DrosophilaRegistration registration = new DrosophilaRegistration();
		registration.sandbox();
	}



}
