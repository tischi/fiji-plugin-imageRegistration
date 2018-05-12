package de.embl.cba.registration.ui;

import bdv.util.*;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.registration.ViewTransformAffine;
import net.imagej.ImageJ;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Interactive;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>EMBL>ProSPr", initializer = "init")
public class ProSPr extends DynamicCommand implements Interactive
{
    public static final String GENE_FILE_SUFFIX = ".xml";
    public static final double PROSPR_SCALING_IN_MICROMETER = 0.5;
    public static final String EM_FILE_ID = "em";

    @Parameter
    public LogService logService;

    ArrayList< String > genes;

    SpimData emData;

    Bdv bdv;

    Map< String, BdvSource > geneSourceMap;
    Map< String, File > geneFileMap;

    public static String GENE_SELECTION_UI = "Genes";

    private static ARGBType defaultGeneColor = new ARGBType( ARGBType.rgba( 255, 0, 255, 255 ) );


    private void init() throws SpimDataException
    {
        //File directory = new File( IJ.getDirectory( "Select ProSPr directory" ) );

        File directory = new File( "/Users/tischer/Documents/detlev-arendt-clem-registration--data" );

        initGenes( directory );

        initEM( directory );

        initBdv();

        addActionButtons();

    }

    private void initBdv()
    {
        bdv.getBdvHandle().getViewerPanel().setInterpolation( Interpolation.NLINEAR );
        //bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( new AffineTransform3D() );
    }

    private void addActionButtons()
    {
        addShowGeneButton();

        addHideGeneButton();

        addChangeColorButton();
    }

    private void initEM( File directory ) throws SpimDataException
    {
        openEmData( directory );

        setEmDataSimilarityTransform( );

        bdv = showWithBdv( emData );


    }

    private void initGenes( File directory )
    {
        geneSourceMap = new TreeMap<>(  );

        setGeneFileMap( directory );

        createGeneSelectionUI( );
    }

    private void addShowGeneButton()
    {
        final MutableModuleItem< Button > button = addInput( "Show", Button.class );
        button.setCallback( "showGene" );
        button.setRequired( false );
    }

    private void showGene() throws SpimDataException
    {
        showGene( ( String ) this.getInput( GENE_SELECTION_UI ) );
    }

    private void showGene( String gene ) throws SpimDataException
    {

        if ( geneSourceMap.keySet().contains( gene ) )
        {
            geneSourceMap.get( gene ).setActive( true );
        }
        else
        {
            switch ( GENE_FILE_SUFFIX )
            {
                case ".tif": addSourceFromTiffFile( gene ); break;
                case ".xml": addSourceFromBdvFile( gene ); break;
                default: logService.error( "Unsupported format: " + GENE_FILE_SUFFIX );
            }
        }

    }

    private void addSourceFromBdvFile( String gene ) throws SpimDataException
    {
        SpimData geneData = new XmlIoSpimData().load( geneFileMap.get( gene ).toString() );

        geneData.getSequenceDescription()
                .getViewDescription( 0,0  )
                .getViewSetup().getChannel().setName( "AAA" );

        geneData.getSequenceDescription()
                .getViewSetups().get(  0  )
                .getChannel().setName( "AAA" );

        BdvSource source = BdvFunctions.show( geneData, BdvOptions.options().addTo( bdv ) ).get( 0 );
        source.setColor( defaultGeneColor );
        source.setActive( true );


        geneSourceMap.put( gene, source );
    }

    private void addSourceFromTiffFile( String gene )
    {
        ImagePlus imp = IJ.openImage( geneFileMap.get( gene ).toString() );
        Img img = ImageJFunctions.wrap( imp );

        AffineTransform3D prosprScaling = new AffineTransform3D();
        prosprScaling.scale( PROSPR_SCALING_IN_MICROMETER );

        final BdvSource source = BdvFunctions.show( img, gene,
                Bdv.options()
                        .addTo( bdv )
                        .sourceTransform( prosprScaling ) );

        source.setColor( defaultGeneColor );
        source.setActive( true );
        geneSourceMap.put( gene, source );
    }

    private void addHideGeneButton()
    {
        final MutableModuleItem< Button > button = addInput( "Hide", Button.class );
        button.setCallback( "hideGene" );
        button.setRequired( false );
    }

    private void hideGene( )
    {
        final String gene = ( String ) this.getInput( GENE_SELECTION_UI );

        if ( geneSourceMap.keySet().contains( gene ) )
        {
            geneSourceMap.get( gene ).setActive( false );
        }
    }


    private void setGeneColor( ) throws SpimDataException
    {
        final String gene = ( String ) this.getInput( GENE_SELECTION_UI );

        showGene( gene );

        Color color = JColorChooser.showDialog( null,
                "Select color for " + gene, null );

        geneSourceMap.get( gene ).setColor( getArgbType( color ) );

    }

    private ARGBType getArgbType( Color color )
    {
        return new ARGBType(
                    ARGBType.rgba( color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() ) );
    }


    private void addChangeColorButton()
    {
        final MutableModuleItem< Button > button = addInput( "Set color", Button.class );
        button.setCallback( "setGeneColor" );
        button.setRequired( false );
    }

    private Bdv showWithBdv( SpimData emData )
    {
        Bdv bdv = BdvFunctions.show( emData ).get( 0 );
        //bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( new AffineTransform3D() );
        return bdv;
    }

    private void setEmDataSimilarityTransform( )
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
        viewRegistration.concatenateTransform( viewTransform );
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

    private void openEmData( File directory ) throws SpimDataException
    {
        final File emFile = getEmFile( directory );
        emData = new XmlIoSpimData().load( emFile.toString() );
    }

    private void createGeneSelectionUI( )
    {
        final MutableModuleItem< String > typeItem = addInput( GENE_SELECTION_UI, String.class );
        typeItem.setPersisted( false );
        typeItem.setLabel( GENE_SELECTION_UI );

        List< String > genes = new ArrayList<>(  );
        genes.addAll( geneFileMap.keySet() );
        typeItem.setChoices( genes );
    }

    public void run()
    {
        // ...
    }

    private void setGeneFileMap( File directory )
    {
        File[] files = directory.listFiles();

        geneFileMap = new TreeMap<>( );

        for ( File file : files )
        {
            if ( file.getName().endsWith( GENE_FILE_SUFFIX )
                    && ! file.getName().contains( EM_FILE_ID ))
            {
                String geneName = file.getName().replaceAll( GENE_FILE_SUFFIX, "" );

                geneFileMap.put( geneName, file );
            }
        }

    }

    private File getEmFile( File directory )
    {
        File[] files = directory.listFiles();
        ArrayList< File > emFiles = new ArrayList<>(  );

        for ( File file : files )
        {
            if ( file.getName().endsWith( ".xml" )
                    && file.getName().toLowerCase().contains( EM_FILE_ID )  )
            {
                emFiles.add( file );
            }
        }

        if ( emFiles.size() < 1 )
        {
            logService.error( "No *em*.xml file found." );
            return null;
        }
        else if ( emFiles.size() > 1 )
        {
            logService.warn(
                    "Multiple *em*.xml files found.\n" +
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