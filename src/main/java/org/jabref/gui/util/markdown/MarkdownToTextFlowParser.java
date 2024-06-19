package org.jabref.gui.util.markdown;

import javafx.scene.text.TextFlow;

import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ast.TextBase;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;

public class MarkdownToTextFlowParser {
    private final TextFlow textFlow;

    private final NodeVisitor visitor = new NodeVisitor(
            new VisitHandler<>(Document.class, this::visit),
            new VisitHandler<>(TextBase.class, this::visit),
            new VisitHandler<>(Text.class, this::visit)
    );

    private final static Parser parser = Parser.builder().build();

    public MarkdownToTextFlowParser(TextFlow textFlow) {
        this.textFlow = textFlow;
    }

    public void addMarkdown(String content) {
        Document document = parser.parse(content);
        visit(document);
    }

    private void visit(Document document) {
        visitor.visitChildren(document);
    }

    private void visit(TextBase textBase) {
        visitor.visitChildren(textBase);
    }

    private void visit(Text text) {
        javafx.scene.text.Text textNode = new javafx.scene.text.Text(text.getChars().unescape());
        textFlow.getChildren().add(textNode);
    }
}
