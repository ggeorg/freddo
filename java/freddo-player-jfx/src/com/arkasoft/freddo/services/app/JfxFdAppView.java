package com.arkasoft.freddo.services.app;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import org.json.JSONObject;

import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdAppView;
import freddo.dtalk.util.LOG;

public class JfxFdAppView extends FdAppView {

	private final WebView mWebView;

	public JfxFdAppView(DTalkServiceContext context, WebView webview, JSONObject options) {
		super(context, options);
		mWebView = webview;

		WebEngine webEngine = mWebView.getEngine();
		webEngine.locationProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				onLocationChanged(observable, oldValue, newValue);
			}
		});

		webEngine.load("http://www.youtube.com");
		//webEngine.load("http://localhost:8080/demos/blank.html");
	}

	protected void onLocationChanged(ObservableValue<? extends String> observable, String oldValue, String newValue) {
		LOG.v(mName, ">>> onLocationChanged: %s", newValue);
	}

	@Override
	protected void setUrl(String url) {
		mWebView.getEngine().load(url);
	}

}
