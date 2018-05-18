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
import net.imagej.Dataset;
import net.imagej.ImageJ;
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
import java.util.*;
import java.util.List;

@Plugin(type = Command.class, menuPath = "Plugins>Registration>EMBL>ProSPr", initializer = "init")
public class ProSPr extends DynamicCommand implements Interactive
{
    private static final String DATA_SOURCE_SUFFIX = ".xml";
    private static final double PROSPR_SCALING_IN_MICROMETER = 0.5;
    private static final String EM_RAW_FILE_ID = "em-raw";
    private static final String EM_SEGMENTED_FILE_ID = "em-segmented";
    private static final String GENE_SELECTION_UI = "Genes";
    private static final ARGBType defaultGeneColor = new ARGBType( ARGBType.rgba( 255, 0, 255, 255 ) );

    @Parameter
    public LogService logService;

    Bdv bdv;

    Map< String, DataSource > dataSourcesMap;

    String emRawDataID;

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

        createSourceSelectionUI( );

        //initEm( directory );

        initBdvWithEmRawData( );

        addActionButtons();

    }

    public void run()
    {

    }



    private void addActionButtons()
    {
        addActionButton( "Show", "showGene" );
        addActionButton( "Hide", "hideGene" );
        addActionButton( "Color", "setGeneColor" );
        addActionButton( "Brightness", "setGeneBrightness" );
        addActionButton( "Show legend", "showLegend" );

    }


    private void addActionButton( String buttonName, String callback)
    {
        final MutableModuleItem< Button > button = addInput( buttonName, Button.class );
        button.setCallback(callback );
        button.setRequired( false );
    }

    private void showGene() throws SpimDataException
    {
        showGene( ( String ) this.getInput( GENE_SELECTION_UI ) );
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

    private void showGene( String gene ) throws SpimDataException
    {

        if ( dataSourcesMap.get( gene ).bdvSource == null )
        {
            switch ( DATA_SOURCE_SUFFIX )
            {
                case ".tif":
                    addSourceFromTiffFile( gene );
                    break;
                case ".xml":
                    setSourceFromBdvFile( gene );
                    break;
                default:
                    logService.error( "Unsupported format: " + DATA_SOURCE_SUFFIX );
            }
        }

        dataSourcesMap.get( gene ).bdvSource.setActive( true );
        dataSourcesMap.get( gene ).isActive =  true ;

    }

    private void hideGene( )
    {
        final String gene = ( String ) this.getInput( GENE_SELECTION_UI );

        if ( dataSourcesMap.get( gene ).bdvSource != null  )
        {
            dataSourcesMap.get( gene ).bdvSource.setActive( false );
            dataSourcesMap.get( gene ).isActive = false;
        }
    }

    private void setGeneColor( ) throws SpimDataException
    {
        final String gene = ( String ) this.getInput( GENE_SELECTION_UI );

        showGene( gene );

        Color color = JColorChooser.showDialog( null, "Select color for " + gene, null );

        if ( color != null )
        {
            dataSourcesMap.get( gene ).bdvSource.setColor( getArgbType( color ) );
            dataSourcesMap.get( gene ).color = getArgbType( color );

        }

    }

    private void setGeneBrightness( ) throws SpimDataException
    {
        final String gene = ( String ) this.getInput( GENE_SELECTION_UI );

        showGene( gene );

        GenericDialog gd = new GenericDialog("LUT max value");
        gd.addNumericField("LUT max value: ", dataSourcesMap.get( gene ).maxLutValue, 0 );
        gd.showDialog();
        if (gd.wasCanceled()) return;

        int max  = (int) gd.getNextNumber();

        dataSourcesMap.get( gene ).bdvSource.setDisplayRange( 0.0, max  );
        dataSourcesMap.get( gene ).maxLutValue = max;

    }


    private void setSourceFromBdvFile( String dataSourceName )
    {

        DataSource dataSource = dataSourcesMap.get( dataSourceName );

        SpimData geneData = openSpimData( dataSource.file  ) ;

        geneData.getSequenceDescription()
                .getViewDescription( 0,0  )
                .getViewSetup().getChannel().setName( "AAA" );

        geneData.getSequenceDescription()
                .getViewSetups().get(  0  )
                .getChannel().setName( "AAA" );

        BdvSource source = BdvFunctions.show( geneData, BdvOptions.options().addTo( bdv ) ).get( 0 );

        source.setColor( defaultGeneColor );
        source.setDisplayRange( 0.0, dataSource.maxLutValue );

        dataSource.color = defaultGeneColor;
        dataSource.bdvSource = source;

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

        setSourceFromBdvFile( emRawDataID );

        bdv = BdvFunctions.show( dataSourcesMap.get( emRawDataID ).spimData ).get( 0 ).getBdvHandle();

        bdv.getBdvHandle().getViewerPanel().setInterpolation( Interpolation.NLINEAR );

        //bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( new AffineTransform3D() );
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
        final MutableModuleItem< String > typeItem = addInput( GENE_SELECTION_UI, String.class );
        typeItem.setPersisted( false );
        typeItem.setLabel( GENE_SELECTION_UI );

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
                    ProSPrRegistration.setEmSimilarityTransform( dataSource.spimData );

                    if ( file.getName().contains( EM_RAW_FILE_ID ) )
                    {
                        emRawDataID = dataSourceName;
                    }
                }
                else // prospr gene
                {
                    dataSource.bdvSource = null;
                    dataSource.spimData = null;
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