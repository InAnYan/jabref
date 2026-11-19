package org.jabref.gui.ai.statuspane;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class UniversalStatusPaneViewModelTest {

    private UniversalStatusPaneViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new UniversalStatusPaneViewModel();
    }

    @Test
    void defaultValues() {
        assertEquals("", viewModel.titleProperty().get());
        assertEquals("", viewModel.descriptionProperty().get());
        assertFalse(viewModel.showTextAreaProperty().get());
        assertEquals("", viewModel.textAreaContentProperty().get());
        assertFalse(viewModel.showSpinnerProperty().get());
        assertFalse(viewModel.showButton1Property().get());
        assertEquals("", viewModel.button1TextProperty().get());
        assertFalse(viewModel.showButton2Property().get());
        assertEquals("", viewModel.button2TextProperty().get());
    }

    @Test
    void executeButton1Action() {
        AtomicBoolean clicked = new AtomicBoolean(false);
        viewModel.button1ActionProperty().set(_ -> clicked.set(true));

        viewModel.executeButton1Action();

        assertTrue(clicked.get());
    }

    @Test
    void executeButton2Action() {
        AtomicBoolean clicked = new AtomicBoolean(false);
        viewModel.button2ActionProperty().set(_ -> clicked.set(true));

        viewModel.executeButton2Action();

        assertTrue(clicked.get());
    }
}
