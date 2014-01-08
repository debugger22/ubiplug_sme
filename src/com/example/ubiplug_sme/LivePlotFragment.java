package com.example.ubiplug_sme;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import android.support.v4.app.Fragment;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;


public class LivePlotFragment extends Fragment implements OnClickListener{
	
	
	private static Random RAND = new Random();
    private static final String TIME = "HH:mm:ss";
    private static final String[] ITEMS = { "A", "B", "C", "D", "E", "F" };
    private final int[] COLORS = { Color.rgb(100, 250, 150), Color.BLUE, Color.CYAN, Color.DKGRAY, Color.GRAY, Color.GREEN };

    private static final int[] THRESHOLD_VALUES = { 500, 1000, 1500 };
    private static final int[] THRESHOLD_COLORS = { Color.RED, Color.YELLOW, Color.GREEN };
    private static final String[] THRESHOLD_LABELS = { "Bad", "Good", "Excellent" };

    private static final int TEN_SEC = 10000;
    private static final int TWO_SEC = 2000;
    private static final float RATIO = 0.618033988749895f;
    
    private View mViewZoomIn;
    private View mViewZoomOut;
    private View mViewZoomReset;
    private GraphicalView mChartView;
    private XYSeriesRenderer[] mThresholdRenderers;
    private XYMultipleSeriesRenderer mRenderer;
    private XYMultipleSeriesDataset mDataset;
    private HashMap<String, TimeSeries> mSeries;
    private TimeSeries[] mThresholds;
    private ArrayList<String> mItems;
    private double mYAxisMin = Double.MAX_VALUE;
    private double mYAxisMax = Double.MIN_VALUE;
    private double mZoomLevel = 1;
    private double mLastItemChange;
    private int mItemIndex;
    private int mYAxisPadding = 2;
    
	private ConnectDevice act;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mItems = new ArrayList<String>();
		mSeries = new HashMap<String, TimeSeries>();
		mDataset = new XYMultipleSeriesDataset();
		mRenderer = new XYMultipleSeriesRenderer();

		mRenderer.setLabelsColor(Color.LTGRAY);
		mRenderer.setAxesColor(Color.LTGRAY);
		mRenderer.setGridColor(Color.rgb(70, 70, 70));
		mRenderer.setBackgroundColor(Color.BLACK);
		mRenderer.setApplyBackgroundColor(true);

		mRenderer.setLegendTextSize(25);
		mRenderer.setLabelsTextSize(35);
		mRenderer.setPointSize(8);
		mRenderer.setMargins(new int[] { 60, 60, 130, 60 });
		
		mRenderer.setFitLegend(true);
		mRenderer.setShowGrid(true);
		mRenderer.setZoomEnabled(true);
		mRenderer.setExternalZoomEnabled(true);
		mRenderer.setAntialiasing(true);
		mRenderer.setInScroll(true);

		mLastItemChange = new Date().getTime();
		mItemIndex = 0;//Math.abs(RAND.nextInt(ITEMS.length));
	}
	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
 
		final LinearLayout rootView = (LinearLayout) inflater.inflate(R.layout.fragment_live_plot, container, false);
        if (Configuration.ORIENTATION_PORTRAIT == getResources().getConfiguration().orientation) {
			mYAxisPadding = 9;
			mRenderer.setYLabels(15);
		}
        
		mChartView = ChartFactory.getTimeChartView(getActivity(), mDataset, mRenderer, TIME);
		mChartView.addZoomListener(mZoomListener, true, false);
		rootView.addView(mChartView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return rootView;
    }
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Typeface tf = Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(),"fonts/roboto-regular.ttf");
        act = (ConnectDevice)getActivity();
		mViewZoomIn = getActivity().findViewById(R.id.zoom_in);
		mViewZoomOut = getActivity().findViewById(R.id.zoom_out);
		mViewZoomReset = getActivity().findViewById(R.id.zoom_reset);
		mViewZoomIn.setOnClickListener(this);
		mViewZoomOut.setOnClickListener(this);
		mViewZoomReset.setOnClickListener(this);

		mThresholds = new TimeSeries[3];
		mThresholdRenderers = new XYSeriesRenderer[3];
		
		for (int i = 0; i < THRESHOLD_COLORS.length; i++) {
			mThresholdRenderers[i] = new XYSeriesRenderer();
			mThresholdRenderers[i].setColor(THRESHOLD_COLORS[i]);
			mThresholdRenderers[i].setLineWidth(3);

			mThresholds[i] = new TimeSeries(THRESHOLD_LABELS[i]);
			final long now = new Date().getTime();
			mThresholds[i].add(new Date(now - 1000 * 60 * 10), THRESHOLD_VALUES[i]);
			mThresholds[i].add(new Date(now + 1000 * 60 * 10), THRESHOLD_VALUES[i]);

			mDataset.addSeries(mThresholds[i]);
			mRenderer.addSeriesRenderer(mThresholdRenderers[i]);
		}
		
		mTimer.start();

	}

	
	private final CountDownTimer mTimer = new CountDownTimer(1 * 60 * 1000, 10) {
        @Override
        public void onTick(final long millisUntilFinished) {
                addValue();
        }
        
        @Override
        public void onFinish() {
        	Log.v("TIMER", "timer finished ----------------------------");
        	mTimer.start();
        }
	};

	private final ZoomListener mZoomListener = new ZoomListener() {
        @Override
        public void zoomReset() {
                mZoomLevel = 1;
                scrollGraph(new Date().getTime());
        }

        @Override
        public void zoomApplied(final ZoomEvent event) {
                if (event.isZoomIn()) {
                        mZoomLevel /= 2;
                }
                else {
                        mZoomLevel *= 2;
                }
                scrollGraph(new Date().getTime());
        }
	};

	
	@Override
	public void onStart(){
		super.onStart();

		mTimer.start();
	}
	
	@Override
	public void onResume(){
		super.onResume();

	
	}
	
	@Override
	public void onStop(){
		super.onStop();
		if (null != mTimer) {
			mTimer.cancel();
		}
	}
	
	private double randomValue() {
		final int value = Math.abs(RAND.nextInt(32));
		final double percent = (value * 100) / 31.0;
		return ((int) (percent * 10)) / 10.0;
	}
	
	private double calculateAverage(List <Integer> marks) {
		  Integer sum = 0;
		  if(!marks.isEmpty()) {
		    for (Integer mark : marks) {
		        sum += mark;
		    }
		    return sum.doubleValue() / marks.size();
		  }
		  return sum;
	}
	private void addValue() {
		double value = 0;
		try{
			value = act.dataPoints.get(act.dataPoints.size()-1);
			float zerval = (float) calculateAverage(act.dataPoints);
			value -= zerval;
			value *= 3;	//VADC value
			value /= Math.pow(2, 11);	//BITS = 11
			value /= 0.185;
			value *= 1000;
			value = (Math.round(value * 100.0) / 100.0);
			
		}catch(IndexOutOfBoundsException e){
        	value = 0;
        }
        if (mYAxisMin > value) mYAxisMin = value;
        if (mYAxisMax < value) mYAxisMax = value;

        final Date now = new Date();
        final long time = now.getTime();
        /*
        if (time - mLastItemChange > 10000) {
                mLastItemChange = time;
                mItemIndex = Math.abs(RAND.nextInt(ITEMS.length));
        }
         */
        final String item = ITEMS[mItemIndex];
        final int color = COLORS[mItemIndex];
        final int lastItemIndex = mItems.lastIndexOf(item);
        mItems.add(item);
        
        if (lastItemIndex > -1) {
                boolean otherItemBetween = false;
                for (int i = lastItemIndex + 1; i < mItems.size(); i++) {
                        if (!item.equals(mItems.get(i))) {
                                otherItemBetween = true;
                                break;
                        }
                }
                if (otherItemBetween) {
                        addSeries(null, now, value, item, color);
                }
                else {
                        mSeries.get(item).add(now, value);
                }
        }
        else {
                addSeries(item, now, value, item, color);
        }
		
        scrollGraph(time);
        mChartView.repaint();
}

private void addSeries(final String title, final Date time, final double value, final String item, final int color) {
        for (int i = 0; i < THRESHOLD_COLORS.length; i++) {
                mThresholds[i].add(new Date(time.getTime() + 1000 * 60 * 5), THRESHOLD_VALUES[i]);
        }

        final TimeSeries series = new TimeSeries(title);
        series.add(time, value);
        mSeries.put(item, series);
        mDataset.addSeries(series);
        mRenderer.addSeriesRenderer(getSeriesRenderer(color));
}

private void scrollGraph(final long time) {
        final double[] limits = new double[] { time - TEN_SEC * mZoomLevel, time + TWO_SEC * mZoomLevel, mYAxisMin - mYAxisPadding,
                        mYAxisMax + mYAxisPadding };
        mRenderer.setRange(limits);
}

private XYSeriesRenderer getSeriesRenderer(final int color) {
        final XYSeriesRenderer r = new XYSeriesRenderer();
        r.setDisplayChartValues(true);
        r.setChartValuesTextSize(30);
        r.setPointStyle(PointStyle.POINT);
        r.setColor(color);
        r.setFillPoints(true);
        r.setLineWidth(4);
        return r;
}

private static int randomColor() {
        final float hue = (RAND.nextInt(360) + RATIO);
        return Color.HSVToColor(new float[] { hue, 0.8f, 0.9f });
}

@Override
public void onClick(final View v) {
        switch (v.getId()) {
        case R.id.zoom_in:
                mChartView.zoomIn();
                break;

        case R.id.zoom_out:
                mChartView.zoomOut();
                break;

        case R.id.zoom_reset:
                mChartView.zoomReset();
                break;

        default:
                break;
        }

}

}