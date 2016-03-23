package dev.ugasoft.android.gps.actions;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.res.Resources;

import dev.ugasoft.android.gps.db.*;
import dev.baalmart.potholespot.R;

public class PotholeSpotLabel {
	
	private String tag;
	private long  id;
	private float xValue, yValue, zValue;
	private float zThreshold, xThreshold, yThreshold;
	
	private DatabaseHelper dbm;
	
	
	private ArrayList<PotholeSpotLabel> LoadUnTaggedLabels(){
		
		ArrayList<PotholeSpotLabel> labels = this.dbm.getUntaggedLabels();
		return labels;
	}
	
	public void TagSpotLabel(DatabaseHelper dbm){
		this.dbm = dbm;
		
		ArrayList<PotholeSpotLabel> labels = LoadUnTaggedLabels();
		
		for(Iterator<PotholeSpotLabel> lbl = labels.iterator(); lbl.hasNext(); ) {
			PotholeSpotLabel lable = lbl.next();
			TagSpotLabel(lable);
		}
		updateSpotLabelTag(labels);
	}
	
	
	public void TagSpotLabel(PotholeSpotLabel label){

		if(label.zValue >= zThreshold && label.xValue >= xThreshold && label.yValue == yThreshold){
			label.tag = Resources.getSystem().getString(R.string.label_pothole);
		}
		else label.tag = Resources.getSystem().getString(R.string.label_others);
	}
	
	public void updateSpotLabelTag(ArrayList<PotholeSpotLabel> labels){
	
		for (PotholeSpotLabel label : labels) {
			this.dbm.UpdateLabelTag(label);	
		}
	}
	/*
	private void speedFilter(ArrayList<PotholeSpotLabel> labels){
		
	
	}
	
	private void highPassFilter(ArrayList<PotholeSpotLabel> labels){
		
	
	}
	
	private ArrayList<PotholeSpotLabel> zPeakFilter(ArrayList<PotholeSpotLabel> labels){
		  
		
		return labels;
	} */
	
	public void setID(long id){
		this.id = id;
	}
	
	public void setXValue(float xValue){
		this.xValue = xValue;
	}
	
	public void setYValue(float yValue){
		this.yValue = yValue;
	}
	
	public void setZValue(float zValue){
		this.zValue = zValue;
	}
	
	public void setTag(String tag){
		this.tag = tag;
	}
	
	public String getTag(){
		return this.tag;
	}
	
	public long getID(){
		return this.id;
	}
	  
    }
 