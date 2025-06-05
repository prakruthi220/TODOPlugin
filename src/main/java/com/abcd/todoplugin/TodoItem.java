package com.abcd.todoplugin;

import com.intellij.openapi.vfs.VirtualFile;
import java.util.Objects;

/**
 * Represents a TODO item found in a Kotlin file
 */
public class TodoItem {

    /**
     * Priority levels for TODO items
     */
    public enum Priority {
        HIGH("High", 1),
        MEDIUM("Medium", 2),
        LOW("Low", 3);

        private final String displayName;
        private final int level;

        Priority(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getLevel() {
            return level;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final VirtualFile file;
    private final int lineNumber;
    private final String text;
    private final Priority priority;

    /**
     * Creates a new TodoItem
     * @param file the file containing the TODO
     * @param lineNumber the line number (1-based)
     * @param text the TODO text content
     * @param priority the priority level
     */
    public TodoItem(VirtualFile file, int lineNumber, String text, Priority priority) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.text = text != null ? text : "";
        this.priority = priority != null ? priority : Priority.LOW;
    }

    /**
     * Gets the file containing this TODO
     * @return the virtual file
     */
    public VirtualFile getFile() {
        return file;
    }

    /**
     * Gets the line number of this TODO
     * @return the line number (1-based)
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Gets the text content of this TODO
     * @return the TODO text
     */
    public String getText() {
        return text;
    }

    /**
     * Gets the priority of this TODO
     * @return the priority level
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * Gets the filename for display purposes
     * @return the filename
     */
    public String getFileName() {
        return file != null ? file.getName() : "Unknown";
    }

    /**
     * Gets the full path of the file
     * @return the file path
     */
    public String getFilePath() {
        return file != null ? file.getPath() : "Unknown";
    }

    /**
     * Gets a display string for this TODO item
     * @return formatted display string
     */
    public String getDisplayText() {
        return String.format("[%s:%d] %s (%s)",
                getFileName(),
                lineNumber,
                text,
                priority.getDisplayName()
        );
    }

    /**
     * Gets the start offset of this TODO in the document
     * Note: This is calculated based on line number
     * @return the start offset
     */
    public int getStartOffset() {
        // This would need to be calculated from the document
        // For now, return 0 - should be set during creation if needed
        return 0;
    }

    /**
     * Gets the end offset of this TODO in the document
     * Note: This is calculated based on line number and text length
     * @return the end offset
     */
    public int getEndOffset() {
        // This would need to be calculated from the document
        // For now, return text length - should be set during creation if needed
        return text.length();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TodoItem todoItem = (TodoItem) obj;
        return lineNumber == todoItem.lineNumber &&
                Objects.equals(file, todoItem.file) &&
                Objects.equals(text, todoItem.text) &&
                priority == todoItem.priority;
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, lineNumber, text, priority);
    }

    @Override
    public String toString() {
        return getDisplayText();
    }
}