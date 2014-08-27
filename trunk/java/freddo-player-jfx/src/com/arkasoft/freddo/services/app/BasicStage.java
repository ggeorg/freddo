package com.arkasoft.freddo.services.app;

import java.util.ResourceBundle;

import freddo.dtalk.util.LOG;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * Basic dialog.
 */
public abstract class BasicStage extends Stage {
	private static final String TAG = LOG.tag(BasicStage.class);

	protected Scene mScene;

	public BasicStage(final Window owner, String fxml, ResourceBundle rb) {
		this(owner, fxml, rb, Modality.WINDOW_MODAL);
	}

	public BasicStage(final Window owner, String fxml, ResourceBundle rb, Modality modality) {
		Pane root = null;
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxml));
			if (rb != null) {
				fxmlLoader.setResources(rb);
			}
			fxmlLoader.setController(this);
			root = (Pane) fxmlLoader.load();
			// root.getStylesheets().add("");
		} catch (Exception ex) {
			LOG.e(TAG, "Root UI not found", ex);
		}

		// mScene = new Scene(this, StageStyle.UTILITY, root, null);
		setScene(mScene);
		super.initModality(modality);
		super.initOwner(owner);

		// No API to center on stage??
		if (owner != null) {
			super.setOnShown(new EventHandler<WindowEvent>() {
				@Override
				public void handle(WindowEvent event) {
					center(owner);
				}
			});
		}

		// Set sizes based on client area's sizes
		// TODO
		sizeToScene();
	}

	protected void center(Window owner) {
		double x = owner.getX() + (owner.getWidth() / 2) - (getWidth() / 2);
		double y = owner.getY() + (owner.getHeight() / 2) - (getHeight() / 2);
		super.setX(x);
		super.setY(y);
	}
}
