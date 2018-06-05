package de.embl.cba.registration.commands;


import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.scijava.ui.UIService;

import java.io.IOException;

public class DrosophilaRegistration
{

	public void sandbox() throws IOException
	{
		ImageJ imagej = new ImageJ();
		imagej.ui().showUI();
		DatasetIOService datasetIOService = imagej.scifio().datasetIO();
		DatasetService datasetService = imagej.dataset();
		UIService uiService = imagej.ui();

		// Open
		Dataset dataset = datasetIOService.open( "/Users/tischer/Documents/justin-crocker-drosophila-registration--data/processed_images/Image 4-3 Dapi iso1um.tif" );

		// Change calibration
		CalibratedAxis[] calibratedAxes = new CalibratedAxis[3];
		//calibratedAxes[ 0 ] = new DefaultLinearAxis(  )
		double avgScale = dataset.getImgPlus().axis( 0 ).averageScale( 0, 1.0 );
		String unit = dataset.getImgPlus().axis( 0 ).unit();



		// Correct intensity decrease along z-axis

		for ( long z = 0; z < dataset.dimension( 2 ); ++z )
		{
			final IntervalView< RealType< ? > > intervalView = Views.hyperSlice( dataset, 2, z );
			double factor = getFactor( z );
			System.out.println( "" + z + ": " + factor );
			intervalView.forEach( t -> t.mul( factor ) );
		}


		uiService.show( dataset );

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
		double generalIntensityScaling = 0.5;
		double correction = generalIntensityScaling / Math.exp( - ( z - z0 ) / decayLengthInPixels );
		return correction;
	}


	public static void main( String... args ) throws IOException
	{

		DrosophilaRegistration registration = new DrosophilaRegistration();
		registration.sandbox();

	}
}
