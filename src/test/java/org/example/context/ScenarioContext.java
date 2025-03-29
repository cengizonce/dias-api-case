package org.example.context;

import java.util.HashMap;
import java.util.Map;

public class ScenarioContext {
    private static ScenarioContext instance;
    private Map<String, Object> context;

    private ScenarioContext() {
        context = new HashMap<>();
    }

    public static ScenarioContext getInstance() {
        if (instance == null) {
            instance = new ScenarioContext();
        }
        return instance;
    }

    public void setContext(String key, Object value) {
        context.put(key, value);
    }

    public Object getContext(String key) {
        return context.get(key);
    }

    public String getFilterFirstname() {
        return (String) context.get("filterFirstname");
    }

    public String getFilterLastname() {
        return (String) context.get("filterLastname");
    }

    public String getFilterCheckin() {
        return (String) context.get("filterCheckin");
    }

    public String getFilterCheckout() {
        return (String) context.get("filterCheckout");
    }
}