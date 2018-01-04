package de.embl.cba.registration.ui;

import de.embl.cba.registration.Axes;
import de.embl.cba.registration.OutputIntervalType;
import de.embl.cba.registration.RegistrationAxisType;
import de.embl.cba.registration.Services;
import de.embl.cba.registration.filter.FilterSettings;
import de.embl.cba.registration.filter.FilterType;
import de.embl.cba.registration.transformfinder.TransformSettings;
import de.embl.cba.registration.transformfinder.TransformFinderType;
import net.imagej.Dataset;
import net.imglib2.FinalInterval;
import net.imglib2.util.Intervals;
import org.scijava.module.Module;
import org.scijava.ui.DialogPrompt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

public class Settings
{

    public ArrayList< RegistrationAxisType > registrationAxisTypes;
    public TransformSettings transformSettings;
    public FilterSettings filterSettings;
    public OutputIntervalType outputIntervalType;
    public FinalInterval interval;
    public ExecutorService executorService;  // TODO: how does this relate to the Services.executorService?
    public Dataset dataset;
    public Axes axes;

    private RegistrationPlugin plugin;
    private Module module; // TODO: what is the difference between plugin and module?

    public Settings( )
    {
    }

    public Settings( RegistrationPlugin plugin )
    {
        this.plugin = plugin;
        this.module = plugin;
        this.dataset = plugin.dataset;
        updateParameters();
    }

    public void updateParameters()
    {
        setRegistrationAxesTypes();
        setRegistrationAxesInterval();
        setAxes();
        setImageFilterParameters();
        setTransformSettings();
        setOutputInterval();
        setNumThreads();

    }

    public void setAxes()
    {
        this.axes = new Axes( dataset, registrationAxisTypes, interval );
    }

    public boolean check()
    {
        if ( filterSettings.subSampling.length != axes.numTransformableDimensions() )
        {
            Services.uiService.showDialog( "Sub-sampling dimensions does not equal number " +
                    "of transformable dimensions.", DialogPrompt.MessageType.ERROR_MESSAGE );
            return false;
        }
        if ( transformSettings.maximalTranslations.length != axes.numTransformableDimensions() )
        {
            Services.uiService.showDialog( "Maximal translation dimensions does not equal number " +
                    "of transformable dimensions.", DialogPrompt.MessageType.ERROR_MESSAGE );
            return false;
        }
        return true;
    }

    private void setNumThreads()
    {
        executorService = plugin.threadService.getExecutorService(); // TODO
    }

    private void setRegistrationAxesTypes()
    {
        registrationAxisTypes = new ArrayList<>();

        for ( int d = 0; d < plugin.dataset.numDimensions(); ++d )
        {
            String axisTypeName = ( String ) plugin.typeInput( d ).getValue( module );
            registrationAxisTypes.add( RegistrationAxisType.valueOf( axisTypeName ) );
        }

    }

    private void setRegistrationAxesInterval()
    {
        long[] min = Intervals.minAsLongArray( plugin.dataset );
        long[] max = Intervals.maxAsLongArray( plugin.dataset );

        for ( int d = 0; d < plugin.dataset.numDimensions(); ++d )
        {
            min[ d ] = (long) plugin.varInput( d, "min" ).getValue( module );
            max[ d ] = (long) plugin.varInput( d, "max" ).getValue( module );
        }

        interval = new FinalInterval( min, max );
    }

    private void setImageFilterParameters()
    {
        filterSettings = new FilterSettings();
        setFilters();
        setSubSampling();
        setThreshold();
        setGauss();
    }

    private void setFilters()
    {
        filterSettings.filterTypes  = new ArrayList<>(  );
        filterSettings.filterTypes.add( FilterType.SubSample );

        FilterType filterType = FilterType.valueOf( plugin.imageFilterType );

        if ( filterType.equals( FilterType.ThresholdAndDifferenceOfGaussian ) )
        {
            filterSettings.filterTypes.add( FilterType.Threshold );
            filterSettings.filterTypes.add( FilterType.DifferenceOfGaussian );
        }
        else
        {
            filterSettings.filterTypes.add( FilterType.valueOf( plugin.imageFilterType ) );
        }


        //filterSettings.filterTypes.add( FilterType.AsArrayImg );

    }

    private void setSubSampling()
    {
        String[] tmp = plugin.imageFilterSubSampling.split( "," );
        long[] subSampling = Arrays.stream( tmp ).mapToLong( i -> Long.parseLong( i ) ).toArray();
        filterSettings.subSampling = subSampling;
    }

    private void setGauss()
    {
        int n = axes.numTransformableDimensions();

        filterSettings.gaussSigma = new double[ n ];
        Arrays.fill( filterSettings.gaussSigma, 3.0D );

        filterSettings.gaussSigmaSmaller = new double[ n ];
        Arrays.fill( filterSettings.gaussSigmaSmaller, 3.0D );

        filterSettings.gaussSigmaLarger = new double[ n ];
        Arrays.fill( filterSettings.gaussSigmaLarger, 9.0D );
    }


    private void setThreshold()
    {
        String[] tmp;
        tmp = plugin.imageFilterThreshold.split( "," );
        double[] thresholdMinMax = Arrays.stream( tmp ).mapToDouble( i -> Double.parseDouble( i ) ).toArray();

        filterSettings.thresholdMin = thresholdMinMax[ 0 ];
        filterSettings.thresholdMax = thresholdMinMax[ 1 ];

    }

    private void setTransformSettings()
    {
        transformSettings = new TransformSettings();
        transformSettings.transformFinderType = TransformFinderType.valueOf( plugin.transformationTypeInput );
        //setTranslationRange( plugin.transformationParametersMaximalTranslationsInput );
        setRotationRange( plugin.transformationParameterMaximalRotationsInput );
    }

    private void setRotationRange( String rangeString )
    {
        String[] tmp = rangeString.split( "," );
        double[] transformationParametersMaximalRotations = new double[ tmp.length ];
        for ( int i = 0; i < tmp.length; ++i )
        {
            transformationParametersMaximalRotations[ i ] = Double.parseDouble( tmp[ i ] );
        }
        transformSettings.maximalRotations = transformationParametersMaximalRotations;
    }

    private void setTranslationRange( String rangeString )
    {
        String[] tmp = rangeString.split( "," );
        double[] maximalTranslations = new double[ tmp.length ];
        for ( int i = 0; i < tmp.length; ++i )
        {
            maximalTranslations[ i ] = Double.parseDouble( tmp[ i ].trim() );
        }
        transformSettings.maximalTranslations = maximalTranslations;
    }

    private void setOutputInterval()
    {
        outputIntervalType = OutputIntervalType.valueOf( plugin.outputViewIntervalSizeTypeInput );
    }
}