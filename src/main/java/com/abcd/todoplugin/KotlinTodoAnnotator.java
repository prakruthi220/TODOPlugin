package com.abcd.todoplugin;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Annotator for highlighting TODO comments in Kotlin files
 */
public class KotlinTodoAnnotator implements Annotator {

    private static final TextAttributesKey TODO_ATTRIBUTES = DefaultLanguageHighlighterColors.LINE_COMMENT;
    private static final Pattern TODO_PATTERN = Pattern.compile(
            "//\\s*(TODO|FIXME|HACK|NOTE|BUG)\\s*:?\\s*(.*)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        PsiFile file = element.getContainingFile();
        if (file == null || !KotlinTodoScanner.isKotlinFile(file.getVirtualFile())) {
            return;
        }

        String elementText = element.getText();
        if (elementText.contains("//")) {
            annotateTodosInElement(element, holder);
        }
    }

    private void annotateTodosInElement(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        String text = element.getText();
        String[] lines = text.split("\n");
        int startOffset = element.getTextOffset();
        int currentOffset = startOffset;

        for (String line : lines) {
            Matcher matcher = TODO_PATTERN.matcher(line);
            if (matcher.find()) {
                String todoType = matcher.group(1).toUpperCase();
                int lineStartOffset = currentOffset + matcher.start();
                int lineEndOffset = currentOffset + matcher.end();

                TextRange range = TextRange.create(lineStartOffset, lineEndOffset);

                // Create annotation based on TODO type
                HighlightSeverity severity = getSeverityForTodoType(todoType);
                String message = "TODO: " + matcher.group(2).trim();

                holder.newAnnotation(severity, message)
                        .range(range)
                        .textAttributes(TODO_ATTRIBUTES)
                        .create();
            }
            currentOffset += line.length() + 1; // +1 for newline character
        }
    }

    private HighlightSeverity getSeverityForTodoType(String todoType) {
        switch (todoType) {
            case "FIXME":
            case "BUG":
                return HighlightSeverity.WARNING;
            case "HACK":
                return HighlightSeverity.WEAK_WARNING;
            case "TODO":
            case "NOTE":
            default:
                return HighlightSeverity.INFORMATION;
        }
    }
}