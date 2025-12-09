package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 * A custom ScrollPane that automatically renders and manages a vertical list of items.
 *
 * <p>This component synchronizes the internal view with the provided data list.
 * It supports efficient updates (add, remove) without rebuilding the entire view.</p>
 */
public class ListScrollPane<T> extends ScrollPane {

    private final VBox contentContainer;
    private final ObjectProperty<ObservableList<T>> itemsProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<Function<T, Node>> rendererProperty = new SimpleObjectProperty<>();
    private final BooleanProperty autoScrollToBottomProperty = new SimpleBooleanProperty(false);
    private final ListChangeListener<T> listChangeListener = this::handleListChange;

    public ListScrollPane() {
        this.contentContainer = new VBox();
        this.contentContainer.setFillWidth(true);
        setContent(this.contentContainer);
        setFitToWidth(true);
        setupItemListener();
        setupAutoScroll();
    }

    /**
     * Sets up the listener for the item property to switch the ListChangeListener
     * when the underlying ObservableList reference changes.
     */
    private void setupItemListener() {
        itemsProperty.addListener((obs, oldList, newList) -> {
            if (oldList != null)
                oldList.removeListener(listChangeListener);
            if (newList != null) {
                newList.addListener(listChangeListener);
                rebuildView();
            } else {
                contentContainer.getChildren().clear();
            }
        });
    }

    /**
     * Sets up the logic for automatic scrolling.
     */
    private void setupAutoScroll() {
        contentContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (isAutoScrollToBottom()) {
                this.setVvalue(1.0);
            }
        });
    }

    /**
     * Handles specific changes in the observable list to update the VBox children
     * efficiently without a full rebuild.
     */
    private void handleListChange(ListChangeListener.Change<? extends T> change) {
        final Function<T, Node> renderer = getRenderer();
        if (renderer == null)
            return;

        while (change.next()) {
            if (change.wasPermutated()) {
                rebuildView();
            } else if (change.wasUpdated()) {
                for (int i = change.getFrom(); i < change.getTo(); i++) {
                    T item = change.getList().get(i);
                    contentContainer.getChildren().set(i, renderer.apply(item));
                }
            } else {
                if (change.wasRemoved()) {
                    contentContainer.getChildren().remove(change.getFrom(), change.getFrom() + change.getRemovedSize());
                }
                if (change.wasAdded()) {
                    List<Node> newNodes = new ArrayList<>();
                    for (T item : change.getAddedSubList()) {
                        newNodes.add(renderer.apply(item));
                    }
                    contentContainer.getChildren().addAll(change.getFrom(), newNodes);
                }
            }
        }
    }

    /**
     * Completely clears and rebuilds the VBox children based on current items.
     */
    private void rebuildView() {
        contentContainer.getChildren().clear();
        ObservableList<T> list = getItems();
        Function<T, Node> renderer = getRenderer();
        if (list != null && renderer != null) {
            List<Node> nodes = new ArrayList<>();
            for (T item : list)
                nodes.add(renderer.apply(item));
            contentContainer.getChildren().setAll(nodes);
        }
    }

    public final ObservableList<T> getItems() {
        return itemsProperty.get();
    }

    public final void setItems(ObservableList<T> value) {
        itemsProperty.set(value);
    }

    public final Function<T, Node> getRenderer() {
        return rendererProperty.get();
    }

    public final void setRenderer(Function<T, Node> value) {
        rendererProperty.set(value);
        rebuildView();
    }

    public final boolean isAutoScrollToBottom() {
        return autoScrollToBottomProperty.get();
    }

    public final void setAutoScrollToBottom(boolean value) {
        autoScrollToBottomProperty.set(value);
        if (value)
            setVvalue(1.0);
    }

    public final BooleanProperty autoScrollToBottomProperty() {
        return autoScrollToBottomProperty;
    }

    public VBox getContentContainer() {
        return contentContainer;
    }
}
