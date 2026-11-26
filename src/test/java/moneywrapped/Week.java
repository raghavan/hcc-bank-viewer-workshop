package moneywrapped;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a check as belonging to one week. Checks for later weeks are skipped until you
 * ask for them:  ./mvnw test -Dweek=2  runs the week 1 and week 2 checks.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(WeekCondition.class)
public @interface Week {
    int value();
}
