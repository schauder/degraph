package de.schauderhaft.degraph.gui;

import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import de.schauderhaft.degraph.gui.util.FXMLUtil;
import de.schauderhaft.degraph.java.JavaHierarchicGraph;
import de.schauderhaft.degraph.model.Node;

/**
 * Controller for the main window which includes 0-n nodeViews
 * 
 */
public class MainViewController extends ScrollPane {

	@FXML
	private ResourceBundle resources;

	@FXML
	private URL location;

	private final JavaHierarchicGraph graph;

	public MainViewController(JavaHierarchicGraph graph) {
		this.graph = graph;
		FXMLUtil.loadAndSetController(this, "MainView.fxml");
	}

	@FXML
	void onMouseClicked(MouseEvent event) {
		System.out.println("KLickMainView");
	}

	@FXML
	void initialize() {

		Pane pane = new StackPane();

		Set<Node> topNodes = graph.topNodes();
		int xx = 0;
		for (Node parent : topNodes) {
			Set<Node> childrenOfParent = graph.contentsOf(parent);

			VisualizeNode visualizeNode = new VisualizeNode(parent,
					childrenOfParent);
			NodeController parentController = visualizeNode.createController();

			AnchorPane parentContentPane = (AnchorPane) parentController
					.lookup(NodeController.PANE_NAME);
			int x = 0;
			for (Node childrenNode : childrenOfParent) {

				VisualizeNode visualizeChildController = new VisualizeNode(
						childrenNode, new HashSet<Node>());
				NodeController childController = visualizeChildController
						.createController();
				parentContentPane.getChildren().add(childController);
				childController.setLayoutXForAllPanes(x);
				x += 160;
			}

			parentController.setLayoutXForAllPanes(xx);
			pane.getChildren().add(parentController);
			xx += 160;
			parentController.fitToSize();
		}

		this.setContent(pane);
	}

}
