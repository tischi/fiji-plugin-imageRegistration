package de.embl.cba.registration.ui;

import de.embl.cba.registration.Axes;
import de.embl.cba.registration.OutputIntervalSizeType;
import de.embl.cba.registration.RegistrationAxisType;
import de.embl.cba.registration.Services;
import de.embl.cba.registration.filter.FilterSettings;
import de.embl.cba.registration.filter.FilterType;
import de.embl.cba.registration.projection.ProjectionType;
import de.embl.cba.registration.transformfinder.TransformSettings;
import de.embl.cba.registration.transformfinder.TransformFinderType;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Intervals;
import org.scijava.ui.DialogPrompt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

public class Settings
{

    public ArrayList< RegistrationAxisType > registrationAxisTypes;
    public ArrayList< AxisType > axisTypes;

    public TransformSettings transformSettings;
    public FilterSettings filterSettings;
    public OutputIntervalSizeType outputIntervalSizeType;
    public FinalInterval interval;
    public ExecutorService executorService;  // TODO: how does this relate to the Services.executorService?
    public Axes axes;
    public RandomAccessibleInterval rai;

    public ProjectionType projectionType;

    public int inputImageNumDimensions;

    private RegistrationPlugin plugin;
    //private Module module; // TODO: what is the difference between plugin and module?


    public Settings( )
    {
    }

    public Settings( RegistrationPlugin plugin )
    {
        this.plugin = plugin;
        updatePluginParameters();
    }

    public void updatePluginParameters()
    {
        this.rai = plugin.rai;
        this.axisTypes = plugin.axisTypes;
        setRegistrationAxisTypes();
        setRegistrationAxesInterval();
        setProjectionMode();
        setAxes();
        setImageFilterParameters();
        setTransformSettings();
        setOutputInterval();
        setNumThreads();
    }

    private void setProjectionMode()
    {
        this.projectionType = plugin.getOtherAxesProjectionType();
    }

    public void setAxes()
    {
        this.axes = new Axes( rai, registrationAxisTypes, axisTypes, interval );
    }

    public boolean check()
    {
        if ( filterSettings.subSampling.length != axes.numRegistrationDimensions() )
        {
            Services.uiService.showDialog( "Sub-sampling dimensions does not equal number " +
                    "of registration dimensions.", DialogPrompt.MessageType.ERROR_MESSAGE );
            return false;
        }

        if ( transformSettings.maximalTranslations != null && transformSettings.maximalTranslations.length != axes.numRegistrationDimensions() )
        {
            Services.uiService.showDialog( "Maximal translation dimensions does not equal number " +
                    "of registration dimensions.", DialogPrompt.MessageType.ERROR_MESSAGE );
            return false;
        }

        return true;

    }

    private void setNumThreads()
    {
        executorService = plugin.threadService.getExecutorService(); // TODO
    }

    private void setRegistrationAxisTypes()
    {
        registrationAxisTypes = new ArrayList<>();

        for ( int d = 0; d < rai.numDimensions(); ++d )
        {
            String axisTypeName = ( String ) plugin.typeInput( d ).getValue( plugin );
            registrationAxisTypes.add( RegistrationAxisType.valueOf( axisTypeName ) );
        }

    }

    private void setRegistrationAxesInterval()
    {
        long[] min = Intervals.minAsLongArray( rai );
        long[] max = Intervals.maxAsLongArray( rai );

        for ( int d = 0; d < rai.numDimensions(); ++d )
        {
            min[ d ] = (long) plugin.varInput( d, "min" ).getValue( plugin );
            max[ d ] = (long) plugin.varInput( d, "max" ).getValue( plugin );

            min[ d ] -= (long) plugin.offsetForZeroOrOneBasedAxes.get( d );
            max[ d ] -= (long) plugin.offsetForZeroOrOneBasedAxes.get( d );

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
        setGradient();
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
        else if ( filterType.equals( FilterType.ThresholdAndGradient ) )
        {
            filterSettings.filterTypes.add( FilterType.Threshold );
            filterSettings.filterTypes.add( FilterType.Gradient );
        }
        else
        {
            filterSettings.filterTypes.add( filterType );
        }

        //filterSettings.filterTypes.add( FilterType.AsArrayImg );

    }

    private void setSubSampling()
    {
        //setSubSamplingFromUI();
        setNoSubSampling();
    }

    private void setNoSubSampling()
    {
        filterSettings.subSampling = new long[ axes.numRegistrationDimensions() ];
        Arrays.fill( filterSettings.subSampling, 1L );
    }

    private void setSubSamplingFromUI()
    {
        //String[] tmp = plugin.imageFilterSubSampling.split( "," );
        //filterSettings.subSampling = Arrays.stream( tmp ).mapToLong( i -> Long.parseLong( i ) ).toArray();
    }

    private void setGauss()
    {
        int n = axes.numRegistrationDimensions();

        filterSettings.gaussSigma = new double[ n ];
        Arrays.fill( filterSettings.gaussSigma, plugin.gaussianFilterSize );
    }

    private void setGradient()
    {
        filterSettings.gradientAxis = getGradientAxisIndexWithinRegistrationAxes();
    }

    private int getGradientAxisIndexWithinRegistrationAxes()
    {
        String gradientAxisName = plugin.getGradientAxisName();

        ArrayList< Integer > registrationAxesIds = axes.registrationAxes();
        ArrayList< AxisType > axisTypes = axes.inputAxisTypes();

        for ( int i = 0; i < registrationAxesIds.size(); ++i )
        {
            String registrationAxisName = axisTypes.get( registrationAxesIds.get( i ) ).toString();

            if ( registrationAxisName.equals( gradientAxisName ) )
            {
                return( i );
            }
        }

        return -1;

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
        //setRotationRange( plugin.transformationParameterMaximalRotationsInput );
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
        // outputIntervalSizeType = OutputIntervalSizeType.valueOf( plugin.outputViewIntervalSizeTypeInput );
        outputIntervalSizeType = OutputIntervalSizeType.TransformationsEncompassing; // TODO
    }
}