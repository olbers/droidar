package de.rwth;

import geo.GeoObj;
import gl.Color;
import gl.GLFactory;
import system.ArActivity;
import system.DefaultARSetup;
import system.ErrorHandler;
import system.Setup;
import tests.SimpleTesting;
import util.Vec;
import worldData.World;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;

import commands.ui.CommandShowToast;

import de.rwth.setups.ARNavigatorSetup;
import de.rwth.setups.CollectItemsSetup;
import de.rwth.setups.DebugSetup;
import de.rwth.setups.IndoorSetup;
import de.rwth.setups.MyMarkerDetectionSetup;
import de.rwth.setups.PlaceObjectsSetup;
import de.rwth.setups.SensorTestSetup;

public class TechDemoLauncher extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.demoselector);

		showSetup("Animation Demo", new DebugSetup());
		showSetup("Collecting Items Demo", new CollectItemsSetup());
		showSetup("Placing objects Demo", new PlaceObjectsSetup());
		showSetup("AR Navigator", new ARNavigatorSetup());
		showSetup("Sensor Processing Demo", new SensorTestSetup());

		LinearLayout l = ((LinearLayout) findViewById(R.id.demoScreenLinView));

		l.addView(new SimpleButton("Run tests") {
			public void onButtonPressed() {
				runTests();
			}
		});

		showSetup("Indoor Navigator (Needs special localization service!)",
				new IndoorSetup());
		showSetup("Marker Detection (in development)",
				new MyMarkerDetectionSetup());
	}

	private void showSetup(String string, final Setup aSetupInstance) {
		((LinearLayout) findViewById(R.id.demoScreenLinView))
				.addView(new SimpleButton(string) {
					public void onButtonPressed() {
						Activity theCurrentActivity = TechDemoLauncher.this;
						ArActivity.startWithSetup(theCurrentActivity,
								aSetupInstance);
					}
				});
	}

	private abstract class SimpleButton extends Button {
		public SimpleButton(String text) {
			super(TechDemoLauncher.this);
			setText(text);
			setTextColor(Color.red().toIntRGB());
			setBackgroundResource(android.R.drawable.editbox_background);
			setHeight(50);
			setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onButtonPressed();
				}
			});
		}

		public abstract void onButtonPressed();
	}

	private void runTests() {
		// execute all tests defined in the ARTestSuite:
		try {
			SimpleTesting.runAllTests(this);
			new CommandShowToast(this, "All tests succeded on this device :)")
					.execute();
		} catch (Exception e) {
			ErrorHandler.showErrorLog(this, e, true);
		}
	}

}