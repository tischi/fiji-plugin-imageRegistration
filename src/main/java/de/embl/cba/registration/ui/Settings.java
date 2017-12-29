package de.embl.cba.registration.ui;

import de.embl.cba.registration.Axes;
import de.embl.cba.registration.OutputIntervalType;
import de.embl.cba.registration.RegistrationAxisType;
import de.embl.cba.registration.Services;
import de.embl.cba.registration.filter.FilterSettings;
import de.embl.cba.registration.filter.FilterType;
import de.embl.cba.registration.filter.ImageFilterParameters;
import de.embl.cba.registration.transformfinder.TransformFinderSettings;
import de.embl.cba.registration.transformfinder.TransformFinderType;
import net.imglib2.FinalInterval;
import net.imglib2.util.Intervals;
import org.scijava.module.Module;
import org.scijava.ui.DialogPrompt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class Settings
{
    private final RegistrationPlugin plugin;
    private final Module module; // TODO: what is the difference between plugin and module?

    public RegistrationAxisType[] registrationAxisTypes;
    public Map< String, Object > filterParameters;
    public TransformFinderSettings transformSettings;
    public FilterSettings filterSettings;
    public OutputIntervalType outputIntervalType;
    public FinalInterval interval;
    public ExecutorService executorService;
    public Axes axes;


    public Settings( RegistrationPlugin plugin )
    {
        this.plugin = plugin;
        this.module = plugin;
        updateParameters();
    }

    public void updateParameters()
    {
        setRegistrationAxesTypes();
        setImageFilterParameters();
        setTransformSettings();
        setRegistrationAxesInterval();
        setOutputInterval();
        setNumThreads();
        this.axes = new Axes( plugin.dataset, registrationAxisTypes, interval );
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
        registrationAxisTypes = new RegistrationAxisType[ plugin.dataset.numDimensions() ];

        for ( int d = 0; d < plugin.dataset.numDimensions(); ++d )
        {
            String axisTypeName = ( String ) plugin.typeInput( d ).getValue( module );
            registrationAxisTypes[ d ] = RegistrationAxisType.valueOf( axisTypeName );
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
        filterParameters = new HashMap<>();
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
        String[] tmp;
        tmp = plugin.imageFilterSubSampling.split( "," );
        long[] subSampling = Arrays.stream( tmp ).mapToLong( i -> Long.parseLong( i ) ).toArray();
        filterSettings.subSampling = subSampling;
    }

    private void setGauss()
    {
        filterSettings.gaussSigma = new double[]{ 10.0D, 1.0D };
        filterSettings.gaussSigmaSmaller = new double[]{ 2.0D, 2.0D };
        filterSettings.gaussSigmaLarger = new double[]{ 5.0D, 5.0D };
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
        transformSettings = new TransformFinderSettings();
        transformSettings.transformFinderType = TransformFinderType.valueOf( plugin.transformationTypeInput );
        setTranslationRange();
        setRotationRange();
    }

    private void setRotationRange()
    {
        String[] tmp;
        tmp = plugin.transformationParameterMaximalRotationsInput.split( "," );
        double[] transformationParametersMaximalRotations = new double[ tmp.length ];
        for ( int i = 0; i < tmp.length; ++i )
        {
            transformationParametersMaximalRotations[ i ] = Double.parseDouble( tmp[ i ] );
        }
        transformSettings.maximalRotations = transformationParametersMaximalRotations;
    }

    private void setTranslationRange()
    {
        String[] tmp;
        tmp = plugin.transformationParametersMaximalTranslationsInput.split( "," );
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