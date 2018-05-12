package de.embl.cba.registration.ui;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.registration.ViewTransformAffine;
import net.imagej.ImageJ;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Interactive;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>EMBL>ProSPr", initializer = "init")
public class ProSPr extends DynamicCommand implements Interactive
{
    @Parameter
    public LogService logService;

    ArrayList< String > genes;

    SpimData emData;

    Bdv bdv;

    private void init() throws SpimDataException
    {
        //File directory = new File( IJ.getDirectory( "Select ProSPr directory" ) );

        File directory = new File( "/Users/tischer/Documents/detlev-arendt-clem-registration--data" );

        genes = getGeneList( directory );

        createGeneSelectionUI( genes );

        emData = openEmData( directory );

        setSimilarityTransform( emData );

        bdv = showWithBdv( emData );

    }

    private Bdv showWithBdv( SpimData emData )
    {
        Bdv bdv = BdvFunctions.show( emData ).get( 0 );
        bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( new AffineTransform3D() );
        return bdv;
    }

    private void setSimilarityTransform( SpimData emData )
    {

        AffineTransform3D transformJRotation = getTransformJRotation( emData );

        AffineTransform3D elastixSimilarityTransform = getElastixSimilarityTransform();

        AffineTransform3D combinedTransform = getCombinedTransform( transformJRotation, elastixSimilarityTransform );

        adaptViewRegistration( emData, combinedTransform );

    }

    private void adaptViewRegistration( SpimData emData, AffineTransform3D transform )
    {
        // the ViewRegistration in the file contains the scaling relative to 1 micrometer
        ViewRegistration viewRegistration = emData.getViewRegistrations().getViewRegistration( 0, 0 );
        ViewTransform viewTransform = new ViewTransformAffine( "transform",  transform );
        viewRegistration.preconcatenateTransform( viewTransform );
    }

    private static AffineTransform3D getCombinedTransform( AffineTransform3D firstTransform, AffineTransform3D secondTransform )
    {
        AffineTransform3D combinedTransform = firstTransform.copy();
        combinedTransform.preConcatenate( secondTransform );
        return combinedTransform;
    }

    private AffineTransform3D getElastixSimilarityTransform()
    {
        AffineTransform3D elastixFixedToMoving = new AffineTransform3D();

        elastixFixedToMoving.set(
                1.16299445, -0.04662481, -0.02350539, 0.0,
                0.05221191,  1.04386046,  0.51274918, 0.0,
                0.00054074, -0.51328738,  1.04490107, 0.0 );

        elastixFixedToMoving.translate( new double[]{ 152.92726078, -157.76850918,  466.72468048 } );

        return elastixFixedToMoving.inverse();
    }

    private AffineTransform3D getTransformJRotation( SpimData emData )
    {

        AffineTransform3D transformJTransform = new AffineTransform3D(  );

        transformJTransform.set(
                -0.6427876097,   0.7660444431,   0.0,   0.0,
                0.0, 0.0, 1.0, 0.0,
                0.7660444431,  0.6427876097,  0.0, 0.0 );


        FinalRealInterval boundingIntervalAfterTransformation = getBoundsAfterRotation( emData, transformJTransform );

        double[] transformJTranslation = new double[]{
                - boundingIntervalAfterTransformation.realMin( 0 ),
                - boundingIntervalAfterTransformation.realMin( 1 ) ,
                - boundingIntervalAfterTransformation.realMin( 2 ) };

        transformJTransform.translate( transformJTranslation );

        return transformJTransform;
    }

    private FinalRealInterval getBoundsAfterRotation( SpimData emData, AffineTransform3D transformJTransform )
    {
        RandomAccessibleInterval< ? > image = emData.getSequenceDescription().getImgLoader().getSetupImgLoader( 0 ).getImage( 0 );
        return transformJTransform.estimateBounds( image );
    }

    private SpimData openEmData( File directory ) throws SpimDataException
    {
        final File emFile = getEmFile( directory );
        final String xmlFilename = "/Users/tischer/Documents/detlev-arendt-clem-registration/data/em-raw-500nm.xml";
        return new XmlIoSpimData().load( emFile.toString() );
    }

    private void createGeneSelectionUI( ArrayList< String > genes )
    {
        final MutableModuleItem< String > typeItem = addInput( "Genes", String.class );
        typeItem.setPersisted( false );
        typeItem.setLabel( "Genes" );
        typeItem.setChoices( genes );
    }

    public void run()
    {
        // ...
    }

    public static ArrayList< String > getGeneList( File directory )
    {
        File[] files = directory.listFiles();
        ArrayList< String > genes = new ArrayList<>(  );

        for ( File file : files )
        {
            if ( file.toString().endsWith( ".tif" ) )
            {
                genes.add( file.getName().replaceAll( ".tif", "" ) );
            }
        }

        return genes;
    }

    private File getEmFile( File directory )
    {
        File[] files = directory.listFiles();
        ArrayList< File > emFiles = new ArrayList<>(  );

        for ( File file : files )
        {
            if ( file.toString().endsWith( ".xml" ) )
            {
                emFiles.add( file );
            }
        }

        if ( emFiles.size() < 1 )
        {
            logService.error( "No .xml file (representing the EM data ) found." );
            return null;
        }
        else if ( emFiles.size() > 1 )
        {
            logService.warn(
                    "Multiple .xml file (representing the EM data ) found.\n" +
                    "Using this one: " + emFiles.get( 0 ).getName() );

            return emFiles.get( 0 );
        }
        else
        {
            return emFiles.get( 0 );
        }

    }

    public static void main( String... args )
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run( ProSPr.class, true );
    }


}