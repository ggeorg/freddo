package com.arkasoft.freddo.services;

import org.json.JSONObject;

import com.arkasoft.freddo.FdPlayerActivity;
import com.arkasoft.freddo.services.accelerometer.FdAccelerometerFactory;
import com.arkasoft.freddo.services.app.FdAppViewFactory;
import com.arkasoft.freddo.services.compass.FdCompassFactory;
import com.arkasoft.freddo.services.connection.FdConnectionFactory;
import com.arkasoft.freddo.services.contacts.FdContactsFactory;
import com.arkasoft.freddo.services.device.FdDeviceFactory;
import com.arkasoft.freddo.services.geolocation.FdGeolocationFactory;
import com.arkasoft.freddo.services.globalization.FdGlobalizationFactory;
import com.arkasoft.freddo.services.mediacapture.FdMediaCaptureFactory;
import com.arkasoft.freddo.services.notification.FdNotificationFactory;
import com.arkasoft.freddo.services.presence.FdPresenceFactory;
import com.arkasoft.freddo.services.settings.FdSettingsFactory;
import com.arkasoft.freddo.services.video.FdVideoFactory;

import freddo.dtalk.services.FdServiceMgr;

public class AndroidFdServiceMgr extends FdServiceMgr  {

  public AndroidFdServiceMgr(FdPlayerActivity context, JSONObject options) {
    super(context, options);
  }
  
  @Override
  protected void runOnUiThread(Runnable r) {
    getContext().runOnUiThread(r);
  }

  @Override
  public void start() {
    registerService(new FdAppViewFactory());
    registerService(new FdCompassFactory());    
    registerService(new FdMediaCaptureFactory());
    registerService(new FdPresenceFactory());
    registerService(new FdSettingsFactory());
    registerService(new FdVideoFactory());
    registerService(new FdGeolocationFactory());
    registerService(new FdDeviceFactory());
    registerService(new FdConnectionFactory());
    registerService(new FdAccelerometerFactory());
    registerService(new FdGlobalizationFactory());
    registerService(new FdNotificationFactory());
    registerService(new FdContactsFactory());
  }

}
