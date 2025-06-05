package com.abcd.todoplugin;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.openapi.vfs.VfsUtilCore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scanner for finding TODO comments in Kotlin files
 */
public class KotlinTodoScanner {

    // Regex patterns for different TODO types
    private static final Pattern TODO_PATTERN = Pattern.compile(
            "//\\s*(TODO|FIXME|HACK|NOTE|BUG)\\s*:?\\s*(.*)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Checks if a file is a Kotlin file
     * @param file the virtual file to check
     * @return true if it's a Kotlin file, false otherwise
     */
    public static boolean isKotlinFile(VirtualFile file) {
        if (file == null) {
            return false;
        }
        String name = file.getName().toLowerCase();
        return name.endsWith(".kt") || name.endsWith(".kts");
    }

    /**
     * Scans a document for TODO comments
     * @param document the document to scan
     * @param file the virtual file associated with the document
     * @return list of TodoItem objects found in the document
     */
    public static List<TodoItem> scanDocumentForTodos(Document document, VirtualFile file) {
        List<TodoItem> todos = new ArrayList<TodoItem>();

        if (document == null || file == null) {
            return todos;
        }

        String text = document.getText();
        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = TODO_PATTERN.matcher(line);

            if (matcher.find()) {
                String type = matcher.group(1).toUpperCase();
                String content = matcher.group(2);

                TodoItem.Priority priority = determinePriority(type);

                TodoItem todo = new TodoItem(
                        file,
                        i + 1, // Line numbers are 1-based
                        type + ": " + content.trim(),
                        priority
                );

                todos.add(todo);
            }
        }

        return todos;
    }

    /**
     * Finds all Kotlin files in the project
     * @param project the project to search
     * @return list of Kotlin virtual files
     */
    public static List<VirtualFile> findAllKotlinFilesInProject(Project project) {
        List<VirtualFile> kotlinFiles = new ArrayList<VirtualFile>();

        try {
            // Search for .kt files
            Collection<VirtualFile> ktFiles = FilenameIndex.getAllFilesByExt(
                    project, "kt", GlobalSearchScope.projectScope(project)
            );
            kotlinFiles.addAll(ktFiles);

            // Search for .kts files (Kotlin script files)
            Collection<VirtualFile> ktsFiles = FilenameIndex.getAllFilesByExt(
                    project, "kts", GlobalSearchScope.projectScope(project)
            );
            kotlinFiles.addAll(ktsFiles);

        } catch (Exception e) {
            // Fallback: empty list if there's an error
        }

        return kotlinFiles;
    }

    /**
     * Filters a list of TODOs based on a search keyword
     * @param todos the list of TODOs to filter
     * @param filterKeyword the keyword to filter by (case-insensitive)
     * @return filtered list of TODOs that match the keyword
     */
    public static List<TodoItem> filterTodos(List<TodoItem> todos, String filterKeyword) {
        List<TodoItem> filtered = new ArrayList<TodoItem>();

        if (todos == null) {
            return filtered;
        }

        // If no filter keyword, return all todos
        if (filterKeyword == null || filterKeyword.trim().isEmpty()) {
            return new ArrayList<TodoItem>(todos);
        }

        String lowerCaseFilter = filterKeyword.toLowerCase().trim();

        for (TodoItem todo : todos) {
            if (todo != null && matchesTodo(todo, lowerCaseFilter)) {
                filtered.add(todo);
            }
        }

        return filtered;
    }

    /**
     * Helper method to check if a TODO item matches the filter criteria
     * @param todo the TODO item to check
     * @param filterKeyword the lowercase filter keyword
     * @return true if the TODO matches the filter, false otherwise
     */
    private static boolean matchesTodo(TodoItem todo, String filterKeyword) {
        // Check if the TODO text contains the filter keyword
        if (todo.getText() != null && todo.getText().toLowerCase().contains(filterKeyword)) {
            return true;
        }

        // Check if the file name contains the filter keyword
        if (todo.getFile() != null && todo.getFile().getName().toLowerCase().contains(filterKeyword)) {
            return true;
        }

        // Check if the display text contains the filter keyword
        if (todo.getDisplayText() != null && todo.getDisplayText().toLowerCase().contains(filterKeyword)) {
            return true;
        }

        return false;
    }

    /**
     * Determines the priority of a TODO based on its type
     * @param type the TODO type (TODO, FIXME, etc.)
     * @return the priority level
     */
    private static TodoItem.Priority determinePriority(String type) {
        switch (type.toUpperCase()) {
            case "FIXME":
            case "BUG":
                return TodoItem.Priority.HIGH;
            case "HACK":
                return TodoItem.Priority.MEDIUM;
            case "TODO":
            case "NOTE":
            default:
                return TodoItem.Priority.LOW;
        }
    }
}