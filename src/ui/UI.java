package ui;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import service.ServiceManager;
import storage.DataManager;
import ui.components.StatusBar;
import ui.issuecolumn.ColumnControl;
import ui.issuepanel.expanded.BrowserComponent;
import ui.sidepanel.SidePanel;
import util.Utility;
import util.events.ColumnChangeEvent;
import util.events.ColumnChangeEventHandler;
import util.events.Event;
import util.events.EventHandler;
import util.events.LoginEvent;

import com.google.common.eventbus.EventBus;

public class UI extends Application {

	private static final int VERSION_MAJOR = 0;
	private static final int VERSION_MINOR = 8;
	private static final int VERSION_PATCH = 0;
	
	private static final double WINDOW_DEFAULT_PROPORTION = 0.6;

	private static final Logger logger = LogManager.getLogger(UI.class.getName());

	// Main UI elements
	
	private Stage mainStage;
	private ColumnControl columns;
	private SidePanel sidePanel;
	private MenuControl menuBar;
	private BrowserComponent browserComponent;

	// Events
	
	private EventBus events;
		
	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage stage) throws IOException {
		//log all uncaught exceptions
		Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            logger.error(throwable.getMessage(), throwable);
        });
		
		events = new EventBus();
		
		browserComponent = new BrowserComponent(this);
		browserComponent.initialise();
		initCSS();
		mainStage = stage;
		stage.setMaximized(false);
		Scene scene = new Scene(createRoot());
		setupMainStage(scene);
		loadFonts();
		applyCSS(scene);
		getUserCredentials();
	}
	
	private void getUserCredentials() {
		new LoginDialog(mainStage, columns).show().thenApply(success -> {
			if (success) {
				columns.loadIssues();
				sidePanel.refresh();
				triggerEvent(new LoginEvent());
				setExpandedWidth(false);
			} else {
				quit();
			}
			return true;
		}).exceptionally(e -> {
			logger.error(e.getLocalizedMessage(), e);
			return false;
		});
	}
	
	private static String CSS = "";
	
	public void initCSS() {
		CSS = this.getClass().getResource("hubturbo.css").toString();
	}

	public static void applyCSS(Scene scene) {
		scene.getStylesheets().clear();
		scene.getStylesheets().add(CSS);
	}
	
	public static void loadFonts(){
		Font.loadFont(UI.class.getResource("/resources/octicons/octicons-local.ttf").toExternalForm(), 32);
	}
	
	private void setupMainStage(Scene scene) {
		
		mainStage.setTitle("HubTurbo " + Utility.version(VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH));
		setExpandedWidth(false);
		mainStage.setScene(scene);
		mainStage.show();
		mainStage.setOnCloseRequest(e -> quit());
		mainStage.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean was, Boolean is) {
				if (is) {
					ServiceManager.getInstance().getModel().refresh();
				}
			}
		});
		
		registerEvent(new ColumnChangeEventHandler() {
			@Override
			public void handle(ColumnChangeEvent e) {
				// We let this event fire once so the browser window is resized
				// as soon as the issues are loaded into a column.
				// We don't want this to happen more than once, however, as it
				// would cause the browser window to resize when switching project,
				// and when making changes to columns.
				setExpandedWidth(false);
				events.unregister(this);
			}
		});
	}

	private void quit() {
		ServiceManager.getInstance().stopModelUpdate();
		columns.saveSession();
		DataManager.getInstance().saveSessionConfig();
		browserComponent.quit();
		Platform.exit();
		System.exit(0);
	}
	
	private Parent createRoot() throws IOException {

		sidePanel = new SidePanel(this, mainStage, ServiceManager.getInstance().getModel());
		columns = new ColumnControl(this, mainStage, ServiceManager.getInstance().getModel(), sidePanel);
		sidePanel.setColumns(columns);
		
		UIReference.getInstance().setUI(this);

		ScrollPane columnsScroll = new ScrollPane(columns);
		columnsScroll.getStyleClass().add("transparent-bg");
		columnsScroll.setFitToHeight(true);
		columnsScroll.setVbarPolicy(ScrollBarPolicy.NEVER);
		HBox.setHgrow(columnsScroll, Priority.ALWAYS);
		
		menuBar = new MenuControl(this, columns, columnsScroll);

		HBox centerContainer = new HBox();
		centerContainer.setPadding(new Insets(5,0,5,0));
		centerContainer.getChildren().addAll(sidePanel.getControlLabel(), columnsScroll);

		BorderPane root = new BorderPane();
		root.setTop(menuBar);
		root.setLeft(sidePanel);
		root.setCenter(centerContainer);
		root.setBottom(StatusBar.getInstance());

		return root;
	}

	/**
	 * Sets the dimensions of the stage to the maximum usable size
	 * of the desktop, or to the screen size if this fails.
	 * @param mainStage
	 */
	private Rectangle getDimensions() {
		Optional<Rectangle> dimensions = Utility.getUsableScreenDimensions();
		if (dimensions.isPresent()) {
			return dimensions.get();
		} else {
			return Utility.getScreenDimensions();
		}
	}
	
	/**
	 * UI operations
	 */

	/**
	 * Publish/subscribe API making use of Guava's EventBus.
	 * Takes a lambda expression to be called upon an event being fired.
	 * @param handler
	 */
	public <T extends Event> void registerEvent(EventHandler handler) {
		events.register(handler);
	}
	
	/**
	 * Publish/subscribe API making use of Guava's EventBus.
	 * Triggers all events of a certain type. EventBus will ensure that the
	 * event is fired for all subscribers whose parameter is either the same
	 * or a super type.
	 * @param handler
	 */
	public <T extends Event> void triggerEvent(T event) {
		events.post(event);
	}
	
	public BrowserComponent getBrowserComponent() {
		return browserComponent;
	}
	
	/**
	 * Tracks whether or not the window is in an expanded state.
	 */
	private boolean expanded = false;

	public boolean isExpanded() {
		return expanded;
	}

	/**
	 * Toggles the expansion state of the window.
	 * Returns a boolean value indicating the state.
	 */
	public boolean toggleExpandedWidth() {
		expanded = !expanded;
		setExpandedWidth(expanded);
		return expanded;
	}

	/**
	 * Returns the X position of the edge of the collapsed window.
	 * This function may be called before the main stage is initialised, in
	 * which case it simply returns a reasonable default.
	 */
	public double getCollapsedX() {
		if (mainStage == null) {
			return getDimensions().getWidth() * WINDOW_DEFAULT_PROPORTION;
		}
		return mainStage.getWidth();
	}
	
	/**
	 * Returns the dimensions of the screen available for use when
	 * the main window is in a collapsed state.
	 * This function may be called before the main stage is initialised, in
	 * which case it simply returns a reasonable default.
	 */
	public Rectangle getAvailableDimensions() {
		Rectangle dimensions = getDimensions();
		if (mainStage == null) {
			return new Rectangle(
					(int) (dimensions.getWidth() * WINDOW_DEFAULT_PROPORTION),
					(int) dimensions.getHeight());
		}
		return new Rectangle(
				(int) (dimensions.getWidth() - mainStage.getWidth()),
				(int) dimensions.getHeight());
	}

	/**
	 * Controls whether or not the main window is expanded (occupying the
	 * whole screen) or not (occupying a percentage).
	 * @param expanded
	 */
	private void setExpandedWidth(boolean expanded) {
		this.expanded = expanded;
		Rectangle dimensions = getDimensions();
		double width = expanded
				? dimensions.getWidth()
				: dimensions.getWidth() * WINDOW_DEFAULT_PROPORTION;

		mainStage.setMinWidth(sidePanel.getWidth() + columns.getColumnWidth());
		mainStage.setMinHeight(dimensions.getHeight());
		mainStage.setMaxWidth(width);
		mainStage.setMaxHeight(dimensions.getHeight());
		mainStage.setX(0);
		mainStage.setY(0);
		
		Platform.runLater(() -> {
			mainStage.setMaxWidth(dimensions.getWidth());
			browserComponent.resize(mainStage.getWidth());
		});
	}
}
