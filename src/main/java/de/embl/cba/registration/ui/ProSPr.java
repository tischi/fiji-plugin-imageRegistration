package de.embl.cba.registration.ui;

import bdv.util.*;
import bdv.viewer.Interpolation;
import de.embl.cba.registration.prospr.ProSPrRegistration;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imagej.ImageJ;
import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Util;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Interactive;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;
import org.scijava.widget.Button;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>EMBL>ProSPr", initializer = "init")
public class ProSPr extends DynamicCommand implements Interactive
{
    private static final String DATA_SOURCE_SUFFIX = ".xml";
    private static final double PROSPR_SCALING_IN_MICROMETER = 0.5;
    private static final String EM_RAW_FILE_ID = "em-raw";
    private static final String EM_SEGMENTED_FILE_ID = "em-segmented";
    private static final String SELECTION_UI = "Genes";
    private static final ARGBType defaultGeneColor = new ARGBType( ARGBType.rgba( 255, 0, 255, 255 ) );
    private static final ARGBType defaultEmColor = new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) );


    private TriggerBehaviourBindings triggerbindings;

    @Parameter
    public LogService logService;

    Bdv bdv;

    Map< String, DataSource > dataSourcesMap;

    String emRawDataID;
    AffineTransform3D emRawDataTransform;

    private class DataSource
    {
        public SpimData spimData;
        public BdvSource bdvSource;
        public File file;
        public Integer maxLutValue;
        public boolean isActive;
        public ARGBType color;
    }


    public void init()
    {
        //File directory = new File( IJ.getDirectory( "Select ProSPr directory" ) );

        //File directory = new File( "/Users/tischer/Documents/detlev-arendt-clem-registration--data" );

        //File directory = new File( "/Volumes/cba/tischer/projects/detlev-arendt-clem-registration--data/data/em-raw/bdv" );

        //File directory = new File( "/Volumes/arendt/EM_6dpf_segmentation/bigdataviewer" );

        String dir = IJ.getDirectory( "Please choose ProSPr directory" );

        File directory = new File( dir );

        initDataSources( directory );

        initBdvWithEmRawData( );

        createSourceSelectionUI( );

        addActionButtons();

        addBehaviors();

    }

    private void addOverlay()
    {
        //bdv.getViewer().addTransformListener( lo );
        //bdv.getViewer().getDisplay().addOverlayRenderer( lo );
        //bdv.getViewerFrame().setVisible( true );
        //bdv.getViewer().requestRepaint();
        //https://github.com/PreibischLab/BigStitcher/blob/master/src/main/java/net/preibisch/stitcher/gui/overlay/LinkOverlay.java
    }

    private void addBehaviors()
    {
        Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
        behaviours.install( bdv.getBdvHandle().getTriggerbindings(), "my-new-behaviours" );

        behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
            printCoordinates( );
        }, "print global pos", "P" );

    }

    private void printCoordinates( )
    {

        // global coordinates: (68.18255, 64.08652, 43.013805)

        final RealPoint posInMicrometer = new RealPoint( 3 );
        bdv.getBdvHandle().getViewerPanel().getGlobalMouseCoordinates( posInMicrometer );

        IJ.log("global coordinates (in micrometer): " + Util.printCoordinates( posInMicrometer ) );

        final RealPoint posInverse = new RealPoint( 3 );
        emRawDataTransform.inverse().apply( posInMicrometer, posInverse  );

        IJ.log("transformed coordinates (in pixels) : " + Util.printCoordinates( posInverse ) );

        //IJ.log("global em-raw coordinates: " + Util.printCoordinates(pos2) );
        //IJ.log("global em-raw coordinates: " + Util.printCoordinates(pos3) );

    }

    public void run()
    {

    }

    private void addActionButtons()
    {
        addActionButton( "Show", "showDataSourceInBdv" );
        addActionButton( "Hide", "hideGene" );
        addActionButton( "Color", "setDataSourceColorUI" );
        addActionButton( "Brightness", "setBrightness" );
        addActionButton( "Show legend", "showLegend" );

    }


    private void addActionButton( String buttonName, String callback)
    {
        final MutableModuleItem< Button > button = addInput( buttonName, Button.class );
        button.setCallback(callback );
        button.setRequired( false );
    }

    private void showDataSourceInBdv() throws SpimDataException
    {
        showDataSourceInBdv( ( String ) this.getInput( SELECTION_UI ) );
    }

    private void print( String text )
    {
        logService.info( text );
        IJ.log( text );
    }


    private void showLegend()
    {
        print( "Currently shown genes: " );

        for ( String gene : dataSourcesMap.keySet() )
        {
            if ( dataSourcesMap.get( gene ).isActive )
            {
                String color = dataSourcesMap.get( gene ).color.toString();
                print( gene + ", color " + color );
            }
        }
    }

    private void showDataSourceInBdv( String dataSourceName ) throws SpimDataException
    {
        DataSource dataSource = dataSourcesMap.get( dataSourceName );

        if ( dataSource.bdvSource == null )
        {
            switch ( DATA_SOURCE_SUFFIX )
            {
                case ".tif":
                    addSourceFromTiffFile( dataSourceName );
                    break;
                case ".xml":
                    loadAndShowSourceInBdv( dataSourceName );
                    break;
                default:
                    logService.error( "Unsupported format: " + DATA_SOURCE_SUFFIX );
            }
        }

        dataSource.bdvSource.setActive( true );
        dataSource.isActive =  true ;
        dataSource.bdvSource.setColor( dataSource.color );

    }

    private void hideGene( )
    {
        final String gene = ( String ) this.getInput( SELECTION_UI );

        if ( dataSourcesMap.get( gene ).bdvSource != null  )
        {
            dataSourcesMap.get( gene ).bdvSource.setActive( false );
            dataSourcesMap.get( gene ).isActive = false;
        }
    }

    private void setDataSourceColorUI( ) throws SpimDataException
    {
        final String source = ( String ) this.getInput( SELECTION_UI );

        showDataSourceInBdv( source );

        Color color = JColorChooser.showDialog( null, "Select color for " + source, null );

        if ( color != null )
        {
            setDataSourceColor( source, getArgbType( color )  );
        }

    }

    private void setDataSourceColor( String sourceName, ARGBType color )
    {
        dataSourcesMap.get( sourceName ).bdvSource.setColor( color );
        dataSourcesMap.get( sourceName ).color = color;
    }


    private void setBrightness( ) throws SpimDataException
    {
        final String sourceName = ( String ) this.getInput( SELECTION_UI );

        showDataSourceInBdv( sourceName );

        GenericDialog gd = new GenericDialog("LUT max value");
        gd.addNumericField("LUT max value: ", dataSourcesMap.get( sourceName ).maxLutValue, 0 );
        gd.showDialog();
        if (gd.wasCanceled()) return;

        int max  = (int) gd.getNextNumber();

        dataSourcesMap.get( sourceName ).bdvSource.setDisplayRange( 0.0, max  );
        dataSourcesMap.get( sourceName ).maxLutValue = max;

    }


    private void loadAndShowSourceInBdv( String dataSourceName )
    {

        DataSource dataSource = dataSourcesMap.get( dataSourceName );

        if ( dataSource.spimData == null )
        {
            dataSource.spimData = openSpimData( dataSource.file );
        }

        setNames( dataSourceName, dataSource );

        dataSource.bdvSource = BdvFunctions.show( dataSource.spimData, BdvOptions.options().addTo( bdv ) ).get( 0 );

        dataSource.bdvSource.setColor( dataSource.color );
        dataSource.bdvSource.setDisplayRange( 0.0, dataSource.maxLutValue );

        bdv = dataSource.bdvSource.getBdvHandle();

    }

    private void setNames( String dataSourceName, DataSource dataSource )
    {
        dataSource.spimData.getSequenceDescription()
                .getViewDescription( 0,0  )
                .getViewSetup().getChannel().setName( dataSourceName );

        dataSource.spimData.getSequenceDescription()
                .getViewSetups().get(  0  )
                .getChannel().setName( dataSourceName );
    }


    private void addSourceFromTiffFile( String gene )
    {
        ImagePlus imp = IJ.openImage( dataSourcesMap.get( gene ).file.toString() );
        Img img = ImageJFunctions.wrap( imp );

        AffineTransform3D prosprScaling = new AffineTransform3D();
        prosprScaling.scale( PROSPR_SCALING_IN_MICROMETER );

        final BdvSource source = BdvFunctions.show( img, gene, Bdv.options().addTo( bdv ).sourceTransform( prosprScaling ) );

        source.setColor( defaultGeneColor );

        dataSourcesMap.get( gene ).color = defaultGeneColor;
        dataSourcesMap.get( gene ).bdvSource = source;

    }

    private ARGBType getArgbType( Color color )
    {
        return new ARGBType( ARGBType.rgba( color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() ) );
    }

    private void initBdvWithEmRawData(  )
    {
        System.setProperty( "apple.laf.useScreenMenuBar", "true" );

        loadAndShowSourceInBdv( emRawDataID );

        bdv.getBdvHandle().getViewerPanel().setInterpolation( Interpolation.NLINEAR );

        bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( new AffineTransform3D() );
    }

    private SpimData openSpimData( File file )
    {
        try
        {
            SpimData data = new XmlIoSpimData().load( file.toString() );
            return data;
        }
        catch ( SpimDataException e )
        {
            e.printStackTrace();
            return null;
        }
    }

    private void createSourceSelectionUI( )
    {
        final MutableModuleItem< String > typeItem = addInput( SELECTION_UI, String.class );
        typeItem.setPersisted( false );
        typeItem.setLabel( SELECTION_UI );

        List< String > genes = new ArrayList<>(  );
        genes.addAll( dataSourcesMap.keySet() );
        typeItem.setChoices( genes );
    }

    private void initDataSources( File directory )
    {

        dataSourcesMap = new TreeMap<>(  );

        File[] files = directory.listFiles();

        for ( File file : files )
        {

            if ( file.getName().endsWith( DATA_SOURCE_SUFFIX ) )
            {

                String dataSourceName = file.getName().replaceAll( DATA_SOURCE_SUFFIX, "" );;

                DataSource dataSource = new DataSource();
                dataSource.file = file;
                dataSource.maxLutValue = 255;

                if ( file.getName().contains( EM_RAW_FILE_ID ) || file.getName().contains( EM_SEGMENTED_FILE_ID ) )
                {
                    dataSource.spimData = openSpimData( file );

                    AffineTransform3D affineTransform3D = ProSPrRegistration.setEmSimilarityTransform( dataSource.spimData );

                    if ( file.getName().contains( EM_RAW_FILE_ID ) )
                    {
                        emRawDataID = dataSourceName;
                        emRawDataTransform = affineTransform3D;
                    }

                    dataSource.color = defaultEmColor;
                }
                else // prospr gene
                {
                    dataSource.color = defaultGeneColor;
                }

                dataSourcesMap.put( dataSourceName, dataSource );

            }
        }

    }



    public static void main( String... args )
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run( ProSPr.class, true );
    }


}