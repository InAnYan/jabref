package org.jabref.gui.util;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Tab;

import org.jabref.gui.LibraryTab;
import org.jabref.model.database.BibDatabaseContext;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.PreboundBinding;
import com.tobiasdiez.easybind.Subscription;

/**
 * Helper methods for javafx binding. Some methods are taken from https://bugs.openjdk.java.net/browse/JDK-8134679
 */
public class BindingsHelper {

    private BindingsHelper() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    /**
     * Registers {@code runnable} as a change listener on every one of the given {@code observables}
     * and also invokes it once immediately.
     * <p>
     * Use this to deduplicate the common pattern of:
     * <pre>
     *     Runnable r = this::rebuild;
     *     obs1.addListener(_ -&gt; r.run());
     *     obs2.addListener(_ -&gt; r.run());
     *     r.run();
     * </pre>
     */
    public static void subscribeToChanges(Runnable runnable, Observable... observables) {
        for (Observable observable : observables) {
            observable.addListener(_ -> runnable.run());
        }
        runnable.run();
    }

    public static Subscription includePseudoClassWhen(Node node, PseudoClass pseudoClass, ObservableValue<? extends Boolean> condition) {
        Consumer<Boolean> changePseudoClass = value -> node.pseudoClassStateChanged(pseudoClass, value);
        Subscription subscription = EasyBind.subscribe(condition, changePseudoClass);

        // Put the pseudo class there depending on the current value
        changePseudoClass.accept(condition.getValue());
        return subscription;
    }

    public static <T, U> ObservableList<U> map(ObservableValue<T> source, Function<T, List<U>> mapper) {
        PreboundBinding<List<U>> binding = new PreboundBinding<>(source) {
            @Override
            protected List<U> computeValue() {
                return mapper.apply(source.getValue());
            }
        };

        ObservableList<U> list = FXCollections.observableArrayList();
        binding.addListener((observable, oldValue, newValue) -> list.setAll(newValue));
        return list;
    }

    /**
     * Binds propertyA bidirectional to propertyB using the provided map functions to convert between them.
     */
    public static <A, B> void bindBidirectional(Property<A> propertyA, Property<B> propertyB, Function<A, B> mapAtoB, Function<B, A> mapBtoA) {
        Consumer<B> updateA = newValueB -> propertyA.setValue(mapBtoA.apply(newValueB));
        Consumer<A> updateB = newValueA -> propertyB.setValue(mapAtoB.apply(newValueA));
        bindBidirectional(propertyA, propertyB, updateA, updateB);
    }

    /**
     * Binds propertyA bidirectional to propertyB while using updateB to update propertyB when propertyA changed.
     */
    public static <A> void bindBidirectional(Property<A> propertyA, ObservableValue<A> propertyB, Consumer<A> updateB) {
        bindBidirectional(propertyA, propertyB, propertyA::setValue, updateB);
    }

    /**
     * Binds propertyA bidirectional to propertyB using updateB to update propertyB when propertyA changed and similar for updateA.
     */
    public static <A, B> void bindBidirectional(ObservableValue<A> propertyA, ObservableValue<B> propertyB, Consumer<B> updateA, Consumer<A> updateB) {
        final BidirectionalBinding<A, B> binding = new BidirectionalBinding<>(propertyA, propertyB, updateA, updateB);

        // use updateB as initial source
        updateA.accept(propertyB.getValue());

        propertyA.addListener(binding.getChangeListenerA());
        propertyB.addListener(binding.getChangeListenerB());
    }

    public static <A, B> void bindContentBidirectional(ObservableList<A> propertyA, ListProperty<B> propertyB, Consumer<ObservableList<B>> updateA, Consumer<List<A>> updateB) {
        bindContentBidirectional(
                propertyA,
                (ObservableValue<ObservableList<B>>) propertyB,
                updateA,
                updateB);
    }

    public static <A, B> void bindContentBidirectional(ObservableList<A> propertyA, ObservableValue<B> propertyB, Consumer<B> updateA, Consumer<List<A>> updateB) {
        final BidirectionalListBinding<A, B> binding = new BidirectionalListBinding<>(propertyA, propertyB, updateA, updateB);

        // use property as initial source
        updateA.accept(propertyB.getValue());

        propertyA.addListener(binding);
        propertyB.addListener(binding);
    }

    public static <A, B> void bindContentBidirectional(ListProperty<A> listProperty, Property<B> property, Function<List<A>, B> mapToB, Function<B, List<A>> mapToList) {
        Consumer<B> updateList = newValueB -> listProperty.setAll(mapToList.apply(newValueB));
        Consumer<List<A>> updateB = newValueList -> property.setValue(mapToB.apply(newValueList));

        bindContentBidirectional(
                listProperty,
                property,
                updateList,
                updateB);
    }

    public static <A, V, B> void bindContentBidirectional(ObservableMap<A, V> propertyA, ObservableValue<B> propertyB, Consumer<B> updateA, Consumer<Map<A, V>> updateB) {
        final BidirectionalMapBinding<A, V, B> binding = new BidirectionalMapBinding<>(propertyA, propertyB, updateA, updateB);

        // use list as initial source
        updateB.accept(propertyA);

        propertyA.addListener(binding);
        propertyB.addListener(binding);
    }

    public static <A, V, B> void bindContentBidirectional(ObservableMap<A, V> propertyA, Property<B> propertyB, Consumer<B> updateA, Function<Map<A, V>, B> mapToB) {
        Consumer<Map<A, V>> updateB = newValueList -> propertyB.setValue(mapToB.apply(newValueList));
        bindContentBidirectional(
                propertyA,
                propertyB,
                updateA,
                updateB);
    }

    public static <T> ObservableValue<T> constantOf(T value) {
        return new ObjectBinding<>() {
            @Override
            protected T computeValue() {
                return value;
            }
        };
    }

    public static ObservableValue<Boolean> constantOf(boolean value) {
        return new BooleanBinding() {
            @Override
            protected boolean computeValue() {
                return value;
            }
        };
    }

    public static ObservableValue<? extends String> emptyString() {
        return new StringBinding() {
            @Override
            protected String computeValue() {
                return "";
            }
        };
    }

    /**
     * Returns a wrapper around the given list that posts changes on the JavaFX thread.
     */
    public static <T> ObservableList<T> forUI(ObservableList<T> list) {
        return new UiThreadList<>(list);
    }

    public static <T> ObservableValue<T> ifThenElse(ObservableValue<Boolean> condition, T value, T other) {
        return EasyBind.map(condition, conditionValue -> {
            if (conditionValue) {
                return value;
            } else {
                return other;
            }
        });
    }

    /**
     * Invokes {@code subscriber} for the every new value of {@code observable}, but not for the current value.
     *
     * @param observable observable value to subscribe to
     * @param subscriber action to invoke for values of {@code observable}.
     * @return a subscription that can be used to stop invoking subscriber for any further {@code observable} changes.
     * @apiNote {@link EasyBind#subscribe(ObservableValue, Consumer)} is similar but also invokes the {@code subscriber} for the current value
     */
    public static <T> Subscription subscribeFuture(ObservableValue<T> observable, Consumer<? super T> subscriber) {
        ChangeListener<? super T> listener = (obs, oldValue, newValue) -> subscriber.accept(newValue);
        observable.addListener(listener);
        return () -> observable.removeListener(listener);
    }

    public static void bindContentFiltered(ObservableList<Tab> source, ObservableList<BibDatabaseContext> target, Predicate<Tab> filter) {
        // FIXME: See https://github.com/JabRef/jabref-koppor/pull/713 - workaround in place until issue is resolved.
        // Original code used FilteredList and EasyBind to filter and map tabs directly:
        // FilteredList<Tab> filteredTabs = new FilteredList<>(tabbedPane.getTabs());
        // filteredTabs.setPredicate(LibraryTab.class::isInstance);
        // openDatabaseList = EasyBind.map(filteredTabs, tab -> ((LibraryTab) tab).getBibDatabaseContext());
        // EasyBind.bindContent(stateManager.getOpenDatabases(), openDatabaseList);
        // Once JabRef#713 is fixed, remove this comment and the bindContentFiltered() method, and restore the original code

        Function<Tab, BibDatabaseContext> tabToContext = tab -> ((LibraryTab) tab).getBibDatabaseContext();
        // Initial sync
        target.setAll(source.stream()
                            .filter(filter)
                            .map(tabToContext)
                            .toList());

        source.addListener((ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.wasPermutated()) {
                    // We need a fresh copy as permutation is much harder to mirror
                    List<BibDatabaseContext> reordered = source.stream()
                                                               .filter(filter)
                                                               .map(tabToContext)
                                                               .toList();
                    target.setAll(reordered);
                }

                if (change.wasRemoved()) {
                    for (Tab removed : change.getRemoved()) {
                        if (filter.test(removed)) {
                            target.remove(tabToContext.apply(removed));
                        }
                    }
                }

                if (change.wasAdded()) {
                    int sourceIndex = change.getFrom();
                    int targetIndex = 0;

                    // We need to add at the correct place - therefore, we need to find out the correct position
                    for (int i = 0; i < sourceIndex; i++) {
                        Tab tab = source.get(i);
                        if (filter.test(tab)) {
                            targetIndex++;
                        }
                    }

                    for (Tab added : change.getAddedSubList()) {
                        if (filter.test(added)) {
                            target.add(targetIndex++, tabToContext.apply(added));
                        }
                    }
                }
            }
        });
    }

    public static <A, B, R> ObjectBinding<R> map(
            ObservableValue<A> a,
            ObservableValue<B> b,
            BiFunction<? super A, ? super B, ? extends R> f
    ) {
        return Bindings.createObjectBinding(
                () -> f.apply(a.getValue(), b.getValue()),
                a, b
        );
    }

    /// Please check for nulls in f.
    public static <A, R> ObjectBinding<R> mapChange(
            ObservableValue<A> a,
            Function<? super A, ? extends R> f
    ) {
        return Bindings.createObjectBinding(
                () -> f.apply(a.getValue()),
                a
        );
    }

    public static <E extends Enum<E>> void bindEnum(
            Property<E> target,
            E value1, ObservableValue<Boolean> cond1,
            E value2, ObservableValue<Boolean> cond2,
            E otherwise
    ) {
        target.bind(
                Bindings.createObjectBinding(
                        () -> {
                            if (cond1.getValue()) {
                                return value1;
                            }
                            if (cond2.getValue()) {
                                return value2;
                            }
                            return otherwise;
                        },
                        cond1, cond2
                )
        );
    }

    public static <E extends Enum<E>> void bindEnum(
            Property<E> target,
            E val1, ObservableValue<Boolean> cond1,
            E val2, ObservableValue<Boolean> cond2,
            E val3, ObservableValue<Boolean> cond3,
            E val4, ObservableValue<Boolean> cond4,
            E otherwise
    ) {
        ObjectBinding<E> binding = Bindings.createObjectBinding(() -> {
            if (cond1.getValue())
                return val1;
            if (cond2.getValue())
                return val2;
            if (cond3.getValue())
                return val3;
            if (cond4.getValue())
                return val4;
            return otherwise;
        }, cond1, cond2, cond3, cond4);

        target.bind(binding);
    }

    public static <E extends Enum<E>> void bindEnum(
            Property<E> target,
            E val1, ObservableValue<Boolean> cond1,
            E val2, ObservableValue<Boolean> cond2,
            E val3, ObservableValue<Boolean> cond3,
            E val4, ObservableValue<Boolean> cond4,
            E val5, ObservableValue<Boolean> cond5,
            E val6, ObservableValue<Boolean> cond6,
            E val7, ObservableValue<Boolean> cond7,
            E val8, ObservableValue<Boolean> cond8,
            E val9, ObservableValue<Boolean> cond9,
            E val10, ObservableValue<Boolean> cond10,
            E otherwise
    ) {
        ObjectBinding<E> binding = Bindings.createObjectBinding(() -> {
            if (cond1 != null && Boolean.TRUE.equals(cond1.getValue()))
                return val1;
            if (cond2 != null && Boolean.TRUE.equals(cond2.getValue()))
                return val2;
            if (cond3 != null && Boolean.TRUE.equals(cond3.getValue()))
                return val3;
            if (cond4 != null && Boolean.TRUE.equals(cond4.getValue()))
                return val4;
            if (cond5 != null && Boolean.TRUE.equals(cond5.getValue()))
                return val5;
            if (cond6 != null && Boolean.TRUE.equals(cond6.getValue()))
                return val6;
            if (cond7 != null && Boolean.TRUE.equals(cond7.getValue()))
                return val7;
            if (cond8 != null && Boolean.TRUE.equals(cond8.getValue()))
                return val8;
            if (cond9 != null && Boolean.TRUE.equals(cond9.getValue()))
                return val9;
            if (cond10 != null && Boolean.TRUE.equals(cond10.getValue()))
                return val10;
            return otherwise;
        }, cond1, cond2, cond3, cond4, cond5, cond6, cond7, cond8, cond9, cond10);

        target.bind(binding);
    }

    public static <E extends Enum<E>> void bindEnum(
            Property<E> target,
            E val1, ObservableValue<Boolean> cond1,
            E val2, ObservableValue<Boolean> cond2,
            E val3, ObservableValue<Boolean> cond3,
            E val4, ObservableValue<Boolean> cond4,
            E val5, ObservableValue<Boolean> cond5,
            E val6, ObservableValue<Boolean> cond6,
            E val7, ObservableValue<Boolean> cond7,
            E val8, ObservableValue<Boolean> cond8,
            E otherwise
    ) {
        ObjectBinding<E> binding = Bindings.createObjectBinding(() -> {
            if (Boolean.TRUE.equals(cond1.getValue()))
                return val1;
            if (Boolean.TRUE.equals(cond2.getValue()))
                return val2;
            if (Boolean.TRUE.equals(cond3.getValue()))
                return val3;
            if (Boolean.TRUE.equals(cond4.getValue()))
                return val4;
            if (Boolean.TRUE.equals(cond5.getValue()))
                return val5;
            if (Boolean.TRUE.equals(cond6.getValue()))
                return val6;
            if (Boolean.TRUE.equals(cond7.getValue()))
                return val7;
            if (Boolean.TRUE.equals(cond8.getValue()))
                return val8;
            return otherwise;
        }, cond1, cond2, cond3, cond4, cond5, cond6, cond7, cond8);

        target.bind(binding);
    }

    public static <T, V, R extends ObservableValue<V>> void bindInternalListener(
            ObservableValue<T> parentProperty,
            Function<T, R> propertyExtractor,
            ChangeListener<? super V> listener
    ) {

        parentProperty.addListener((_, oldVal, newVal) -> {
            if (oldVal != null) {
                propertyExtractor.apply(oldVal).removeListener(listener);
            }
            if (newVal != null) {
                propertyExtractor.apply(newVal).addListener(listener);
            }
        });
    }

    @SafeVarargs
    public static <T> void onChangeNonNull(ObservableValue<T> observable, Consumer<T>... actions) {
        observable.addListener((_, _, newVal) -> {
            if (newVal != null) {
                for (var action : actions) {
                    action.accept(newVal);
                }
            }
        });
    }

    public static <T> void onChangeNonNull(ObservableValue<T> observable, Runnable... actions) {
        observable.addListener((_, _, newVal) -> {
            if (newVal != null) {
                for (var action : actions) {
                    action.run();
                }
            }
        });
    }

    @SafeVarargs
    public static <T> void onChangeNonNullWhen(
            ObservableValue<T> observable,
            ObservableValue<Boolean> condition,
            Consumer<T>... actions
    ) {
        observable.addListener((_, _, newVal) -> {
            if (newVal != null && Boolean.TRUE.equals(condition.getValue())) {
                for (var action : actions) {
                    action.accept(newVal);
                }
            }
        });
    }

    public static <T, U> void onChangeNonNullWhen(
            ObservableValue<T> observable1,
            ObservableValue<U> observable2,
            ObservableValue<Boolean> condition,
            Runnable... actions
    ) {
        InvalidationListener listener = _ -> {
            boolean obs1Present = observable1.getValue() != null;
            boolean obs2Present = observable2.getValue() != null;
            boolean isConditionMet = Boolean.TRUE.equals(condition.getValue());

            if (obs1Present && obs2Present && isConditionMet) {
                for (var action : actions) {
                    action.run();
                }
            }
        };

        observable1.addListener(listener);
        observable2.addListener(listener);
    }

    public static <T> void onListContentsChange(ListProperty<T> listProperty, Consumer<T> onAdded, Consumer<T> onRemoved) {
        listProperty.addListener((ListChangeListener<T>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    c.getRemoved().forEach(onRemoved);
                }
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(onAdded);
                }
            }
        });
    }

    public static <T> void runWhenListChanges(ListProperty<T> listProperty, Runnable... actions) {
        listProperty.addListener((ListChangeListener<T>) _ -> {
            for (Runnable action : actions) {
                action.run();
            }
        });
    }

    public static <T> void runWhenListChangesWithPrecondition(
            ListProperty<T> listProperty,
            ObservableBooleanValue condition,
            Runnable... actions
    ) {
        runWhenListChanges(listProperty, () -> {
            if (condition.get()) {
                for (Runnable action : actions) {
                    action.run();
                }
            }
        });
    }

    private static class BidirectionalBinding<A, B> {

        private final ObservableValue<A> propertyA;
        private final Consumer<B> updateA;
        private final Consumer<A> updateB;
        private boolean updating = false;

        public BidirectionalBinding(ObservableValue<A> propertyA, ObservableValue<B> propertyB, Consumer<B> updateA, Consumer<A> updateB) {
            this.propertyA = propertyA;
            this.updateA = updateA;
            this.updateB = updateB;
        }

        public ChangeListener<? super A> getChangeListenerA() {
            return this::changedA;
        }

        public ChangeListener<? super B> getChangeListenerB() {
            return this::changedB;
        }

        public void changedA(ObservableValue<? extends A> observable, A oldValue, A newValue) {
            updateLocked(updateB, oldValue, newValue);
        }

        public void changedB(ObservableValue<? extends B> observable, B oldValue, B newValue) {
            updateLocked(updateA, oldValue, newValue);
        }

        private <T> void updateLocked(Consumer<T> update, T oldValue, T newValue) {
            if (!updating) {
                try {
                    updating = true;
                    update.accept(newValue);
                } finally {
                    updating = false;
                }
            }
        }
    }

    private static class BidirectionalListBinding<A, B> implements ListChangeListener<A>, ChangeListener<B> {

        private final ObservableList<A> listProperty;
        private final ObservableValue<B> property;
        private final Consumer<B> updateA;
        private final Consumer<List<A>> updateB;
        private boolean updating = false;

        public BidirectionalListBinding(ObservableList<A> listProperty, ObservableValue<B> property, Consumer<B> updateA, Consumer<List<A>> updateB) {
            this.listProperty = listProperty;
            this.property = property;
            this.updateA = updateA;
            this.updateB = updateB;
        }

        @Override
        public void changed(ObservableValue<? extends B> observable, B oldValue, B newValue) {
            if (!updating) {
                try {
                    updating = true;
                    updateA.accept(newValue);
                } finally {
                    updating = false;
                }
            }
        }

        @Override
        public void onChanged(Change<? extends A> c) {
            if (!updating) {
                try {
                    updating = true;
                    updateB.accept(listProperty);
                } finally {
                    updating = false;
                }
            }
        }
    }

    private static class BidirectionalMapBinding<A, V, B> implements MapChangeListener<A, V>, ChangeListener<B> {

        private final ObservableMap<A, V> mapProperty;
        private final ObservableValue<B> property;
        private final Consumer<B> updateA;
        private final Consumer<Map<A, V>> updateB;
        private boolean updating = false;

        public BidirectionalMapBinding(ObservableMap<A, V> mapProperty, ObservableValue<B> property, Consumer<B> updateA, Consumer<Map<A, V>> updateB) {
            this.mapProperty = mapProperty;
            this.property = property;
            this.updateA = updateA;
            this.updateB = updateB;
        }

        @Override
        public void changed(ObservableValue<? extends B> observable, B oldValue, B newValue) {
            if (!updating) {
                try {
                    updating = true;
                    updateA.accept(newValue);
                } finally {
                    updating = false;
                }
            }
        }

        @Override
        public void onChanged(Change<? extends A, ? extends V> c) {
            if (!updating) {
                try {
                    updating = true;
                    updateB.accept(mapProperty);
                } finally {
                    updating = false;
                }
            }
        }
    }
}
