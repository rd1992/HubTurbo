package ui.issuepanel.comments;

import java.lang.ref.WeakReference;

import service.ServiceManager;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker.State;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

public class GithubCommentsDisplay extends VBox{
	protected static String JQUERY_IMPORT = "var script = document.createElement(\"script\");"
	        + "script.type = \"text/javascript\";"
	        + "script.src = \"https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js\";"
	        + "document.documentElement.childNodes[0].appendChild(script);";
	protected static String JS_SCRIPT =
	          " var item = $(\".discussion-timeline\");"
			+ " item.add(item.parentsUntil(\"body\")).siblings().hide();";
	
	private int issueID;
	private WebView display;
	private ChangeListener<State> listener;
	
	public GithubCommentsDisplay(int issueID){
		this.issueID = issueID;
		setupDisplay();
		this.setPadding(new Insets(5));
		this.getChildren().add(display);
		loadContents();
	}
	
	private void setupDisplay(){
		display = new WebView();
		setupEngineLoadListener();
	}
	
	private void loadContents(){
		String repo = ServiceManager.getInstance().getRepoId().generateId();
		String urlPath = "https://www.github.com/"+repo+"/issues/"+issueID;
		display.getEngine().load(urlPath);
		display.getEngine().executeScript(JQUERY_IMPORT);
	}
	
	private void setupEngineLoadListener(){
		WeakReference<WebView> ref = new WeakReference<>(display);
		listener = (ov, oldState, newState)->{
			if(newState == State.SUCCEEDED){
				System.out.println(ref.get().getEngine().executeScript(JS_SCRIPT));
			}
		};
		display.getEngine().getLoadWorker().stateProperty().addListener(listener);
	}
}
