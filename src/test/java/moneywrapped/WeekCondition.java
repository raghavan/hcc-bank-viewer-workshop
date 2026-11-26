package moneywrapped;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/** Skips checks from weeks after the one you asked for with -Dweek=N (default 1). */
public class WeekCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        int asked = Integer.getInteger("week", 1);
        return context.getElement()
                .flatMap(e -> AnnotationSupport.findAnnotation(e, Week.class))
                .map(w -> w.value() <= asked
                        ? ConditionEvaluationResult.enabled("week " + w.value() + " check")
                        : ConditionEvaluationResult.disabled("week " + w.value() + " check: run ./mvnw test -Dweek=" + w.value()))
                .orElse(ConditionEvaluationResult.enabled("not tied to a week"));
    }
}
