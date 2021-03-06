/**
 * Copyright 2009-2017 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.javacrumbs.shedlock.spring;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.junit.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class SpringLockConfigurationExtractorTest {
    public static final Duration DEFAULT_LOCK_TIME = Duration.of(30, ChronoUnit.MINUTES);
    public static final Duration DEFAULT_LOCK_AT_LEAST_FOR = Duration.of(5, ChronoUnit.MINUTES);
    private final SpringLockConfigurationExtractor extractor = new SpringLockConfigurationExtractor(DEFAULT_LOCK_TIME, DEFAULT_LOCK_AT_LEAST_FOR);


    @Test
    public void shouldNotLockUnannotatedMethod() throws NoSuchMethodException {
        ScheduledMethodRunnable runnable = new ScheduledMethodRunnable(this, "methodWithoutAnnotation");
        Optional<LockConfiguration> lockConfiguration = extractor.getLockConfiguration(runnable);
        assertThat(lockConfiguration).isEmpty();
    }

    @Test
    public void shouldGetNameAndLockTimeFromAnnotation() throws NoSuchMethodException {
        ScheduledMethodRunnable runnable = new ScheduledMethodRunnable(this, "annotatedMethod");
        LockConfiguration lockConfiguration = extractor.getLockConfiguration(runnable).get();
        assertThat(lockConfiguration.getName()).isEqualTo("lockName");
        assertThat(lockConfiguration.getLockAtMostUntil()).isLessThanOrEqualTo(now().plus(10, MILLIS));
        assertThat(lockConfiguration.getLockAtLeastUntil()).isGreaterThan(now().plus(DEFAULT_LOCK_AT_LEAST_FOR).minus(1, SECONDS));
    }

    @Test
    public void shouldLockForDefaultTimeIfNoAnnotation() throws NoSuchMethodException {
        SchedulerLock annotation = getAnnotation("annotatedMethodWithoutLockAtMostFor");
        TemporalAmount lockAtMostFor = extractor.getLockAtMostFor(annotation);
        assertThat(lockAtMostFor).isEqualTo(DEFAULT_LOCK_TIME);
    }

    @Test
    public void shouldLockTimeFromAnnotation() throws NoSuchMethodException {
        SchedulerLock annotation = getAnnotation("annotatedMethod");
        TemporalAmount lockAtMostFor = extractor.getLockAtMostFor(annotation);
        assertThat(lockAtMostFor).isEqualTo(Duration.of(10, MILLIS));
    }

    @Test
    public void shouldGetZeroGracePeriodFromAnnotation() throws NoSuchMethodException {
        SchedulerLock annotation = getAnnotation("annotatedMethodWithZeroGracePeriod");
        TemporalAmount gracePeriod = extractor.getLockAtLeastFor(annotation);
        assertThat(gracePeriod).isEqualTo(Duration.ZERO);
    }

    @Test
    public void shouldGetPositiveGracePeriodFromAnnotation() throws NoSuchMethodException {
        SchedulerLock annotation = getAnnotation("annotatedMethodWithPositiveGracePeriod");
        TemporalAmount gracePeriod = extractor.getLockAtLeastFor(annotation);
        assertThat(gracePeriod).isEqualTo(Duration.of(10, MILLIS));
    }

    protected SchedulerLock getAnnotation(String method) throws NoSuchMethodException {
        return AnnotationUtils.findAnnotation(getClass().getMethod(method), SchedulerLock.class);
    }

    public void methodWithoutAnnotation() {

    }

    @SchedulerLock(name = "lockName", lockAtMostFor = 10)
    public void annotatedMethod() {

    }

    @SchedulerLock(name = "lockName")
    public void annotatedMethodWithoutLockAtMostFor() {

    }

    @SchedulerLock(name = "lockName", lockAtLeastFor = 0)
    public void annotatedMethodWithZeroGracePeriod() {

    }

    @SchedulerLock(name = "lockName", lockAtLeastFor = 10)
    public void annotatedMethodWithPositiveGracePeriod() {

    }
}