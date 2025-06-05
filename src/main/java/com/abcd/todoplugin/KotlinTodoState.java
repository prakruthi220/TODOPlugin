package com.abcd.todoplugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(name = "KotlinTodoState", storages = @Storage("kotlinTodoState.xml"))
public class KotlinTodoState implements PersistentStateComponent<KotlinTodoState> {

    public String lastFilterKeyword = "";
    public boolean toolWindowVisible = true;
    public List<String> recentKeywords = new ArrayList<>();

    // Transient fields (not persisted)
    private transient List<TodoStateListener> listeners = new ArrayList<>();

    public static KotlinTodoState getInstance() {
        return ApplicationManager.getApplication().getService(KotlinTodoState.class);
    }

    @Nullable
    @Override
    public KotlinTodoState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull KotlinTodoState state) {
        XmlSerializerUtil.copyBean(state, this);

        // Initialize transient fields
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
    }

    public void setLastFilterKeyword(String keyword) {
        this.lastFilterKeyword = keyword != null ? keyword : "";

        // Add to recent keywords if not empty and not already present
        if (!keyword.trim().isEmpty() && !recentKeywords.contains(keyword)) {
            recentKeywords.add(0, keyword);

            // Keep only last 10 keywords
            if (recentKeywords.size() > 10) {
                recentKeywords = recentKeywords.subList(0, 10);
            }
        }

        notifyListeners();
    }

    public String getLastFilterKeyword() {
        return lastFilterKeyword != null ? lastFilterKeyword : "";
    }

    public void setToolWindowVisible(boolean visible) {
        this.toolWindowVisible = visible;
        notifyListeners();
    }

    public boolean isToolWindowVisible() {
        return toolWindowVisible;
    }

    public List<String> getRecentKeywords() {
        return recentKeywords != null ? new ArrayList<>(recentKeywords) : new ArrayList<>();
    }

    public void addStateListener(TodoStateListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
    }

    public void removeStateListener(TodoStateListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners() {
        if (listeners != null) {
            for (TodoStateListener listener : listeners) {
                try {
                    listener.onStateChanged();
                } catch (Exception e) {
                    // Log but don't let one bad listener break others
                }
            }
        }
    }

    /**
     * Interface for components that want to be notified of state changes
     */
    public interface TodoStateListener {
        void onStateChanged();
    }
}