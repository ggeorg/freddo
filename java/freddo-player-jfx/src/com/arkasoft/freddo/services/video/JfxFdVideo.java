package com.arkasoft.freddo.services.video;

import java.io.IOException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.web.WebEngine;

import org.json.JSONObject;

import com.arkasoft.freddo.JfxFreddoApplication;

import freddo.dtalk.DTalkException;
import freddo.dtalk.services.FdVideoService;

public class JfxFdVideo extends FdVideoService {
	
	public JfxFdVideo(JfxFreddoApplication context, JSONObject options) {
		super(context, options);

		WebEngine webEngine = getApplication().getWebView().getEngine();
		webEngine.locationProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				onLocationChanged(observable, oldValue, newValue);
			}
		});
	}

	protected void onLocationChanged(ObservableValue<? extends String> observable, String oldValue, String newValue) {
		try {
			getApplication().injectJavascript("services/video.js");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected JfxFreddoApplication getApplication() {
		return (JfxFreddoApplication) getContext();
	}

	@Override
	protected JSONObject getInfo() throws DTalkException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Object getVolume() throws DTalkException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected JSONObject getItem() throws DTalkException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void setSrc(String string) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setStartTimePercent(double startPositionPercent) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setItemImpl(JSONObject jsonObject) {
		// TODO Auto-generated method stub

	}

	private static final String MEDIA_URL =
			"http://download.oracle.com/otndocs/products/javafx/oow2010-2.flv";

	@Override
	protected boolean play() throws DTalkException {
	// TODO Auto-generated method stub
			return false;
	}

	@Override
	protected boolean pause() throws DTalkException {
		// TODO Auto-generated method stub
		return false;
	}

}
